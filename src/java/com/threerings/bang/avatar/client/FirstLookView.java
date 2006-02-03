//
// $Id$

package com.threerings.bang.avatar.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import com.jmex.bui.BButton;
import com.jmex.bui.BConstants;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.StringUtil;

import com.threerings.cast.CharacterComponent;
import com.threerings.cast.ComponentRepository;
import com.threerings.cast.NoSuchComponentException;
import com.threerings.util.MessageBundle;
import com.threerings.util.RandomUtil;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.Article;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.data.BarberCodes;
import com.threerings.bang.avatar.data.BarberObject;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.data.LookConfig;
import com.threerings.bang.avatar.util.AspectCatalog;
import com.threerings.bang.avatar.util.AvatarLogic;

/**
 * Allows the configuration of a player's first avatar look.
 */
public class FirstLookView extends BContainer
    implements ActionListener
{
    public FirstLookView (BangContext ctx, StatusLabel status)
    {
        super(new BorderLayout(50, 5));
        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(BarberCodes.BARBER_MSGS);
        _status = status;

        add(_avatar = new AvatarView(ctx), BorderLayout.WEST);

        _toggles = new BContainer(
            new TableLayout(4, 5, 20, TableLayout.LEFT, true));
        BContainer wrapper = GroupLayout.makeHBox(GroupLayout.CENTER);
        wrapper.add(_toggles);
        add(wrapper, BorderLayout.CENTER);

        setGender(_ctx.getUserObject().isMale);
    }

    /**
     * Called by the {@link BarberView} to give us a reference to our barber
     * object when needed.
     */
    public void setBarberObject (BarberObject barbobj)
    {
        _barbobj = barbobj;
    }

    /**
     * Can be used to change the gender of the avatar we're configuring. This
     * is only used when creating a character for the first time.
     */
    public void setGender (boolean isMale)
    {
        _gender = isMale ? "male/" : "female/";
        _toggles.removeAll();
        for (int ii = 0; ii < AvatarLogic.ASPECTS.length; ii++) {
            if (isMale || !AvatarLogic.ASPECTS[ii].maleOnly) {
                new AspectToggle(AvatarLogic.ASPECTS[ii], _toggles);
            }
        }
        updateAvatar();
    }

    /**
     * Creates a {@link LookConfig} for the currently configured look.
     */
    public LookConfig getLookConfig ()
    {
        LookConfig config = new LookConfig();
        config.name = (_name == null) ? "" : _name.getText();
        config.hair = _hair.getSelectedColor();
        config.skin = _skin.getSelectedColor();

        // look up the selection for each aspect class
        config.aspects = new String[AvatarLogic.ASPECTS.length];
        for (int ii = 0; ii < config.aspects.length; ii++) {
            Choice choice = _selections.get(AvatarLogic.ASPECTS[ii].name); 
            config.aspects[ii] = (choice == null) ? null : choice.aspect.name;
        }

        // TODO: get per-aspect colorizations
        config.colors = new int[config.aspects.length];

        return config;
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        // make sure they specified a name for the new look
        String name = _name.getText();
        if (StringUtil.isBlank(name)) {
            _status.setText(_msgs.get("m.name_required"));
            return;
        }

        // prevent double clicks or other lag related fuckolas
        _buy.setEnabled(false);

        BarberService.ConfirmListener cl = new BarberService.ConfirmListener() {
            public void requestProcessed () {
                _status.setText(_msgs.get("m.look_bought"));
                _name.setText("");
                _buy.setEnabled(true);
            }
            public void requestFailed (String reason) {
                _status.setText(_msgs.xlate(reason));
                _buy.setEnabled(true);
            }
        };
        _barbobj.service.purchaseLook(_ctx.getClient(), getLookConfig(), cl);
    }

    protected void updateAvatar ()
    {
        int scrip = AvatarCodes.BASE_LOOK_SCRIP_COST,
            coins = AvatarCodes.BASE_LOOK_COIN_COST;

        // obtain the component ids of the various aspect selections and total
        // up the cost of this look while we're at it
        ArrayIntSet compids = new ArrayIntSet();
        for (Choice choice : _selections.values()) {
            if (choice == null) {
                continue;
            }
            scrip += choice.aspect.scrip;
            coins += choice.aspect.coins;
            for (int ii = 0; ii < choice.components.length; ii++) {
                if (choice.components[ii] != null) {
                    compids.add(choice.components[ii].componentId);
                }
            }
        }

        // copy in any required articles from their active look
        PlayerObject user = _ctx.getUserObject();
        Look current = user.getLook();
        if (current != null && current.articles.length != 0) {
            for (int ii = 0; ii < AvatarLogic.SLOTS.length; ii++) {
                if (AvatarLogic.SLOTS[ii].optional) {
                    continue;
                }
                Article article = (Article)
                    user.inventory.get(current.articles[ii]);
                compids.add(article.getComponents());
            }
        }

        int[] avatar = new int[compids.size()+1];
        avatar[0] = (_hair.getSelectedColor() << 5) | _skin.getSelectedColor();
        compids.toIntArray(avatar, 1);

        // update the avatar and cost displays
        _avatar.setAvatar(avatar);
        if (_cost != null) {
            _cost.setMoney(scrip, coins, false);
        }
    }

    protected class AspectToggle
        implements ActionListener
    {
        public AspectToggle (AvatarLogic.Aspect aspect, BContainer table)
        {
            _aspect = aspect;

            BButton left = new BButton(BangUI.leftArrow, "down");
            left.setStyleClass("arrow_button");
            left.addListener(this);
            table.add(left);

            String lb = _ctx.xlate(AvatarCodes.AVATAR_MSGS, "m." + aspect.name);
            table.add(new BLabel(lb, "dialog_label"));

            BButton right = new BButton(BangUI.rightArrow, "up");
            right.setStyleClass("arrow_button");
            right.addListener(this);
            table.add(right);

            if (_aspect.name.equals("head")) {
                table.add(
                    _skin = new ColorSelector(_ctx, AvatarLogic.SKIN, this));
            } else if (_aspect.name.equals("hair")) {
                table.add(
                    _hair = new ColorSelector(_ctx, AvatarLogic.HAIR, this));
            } else if (_aspect.name.equals("eyes")) {
                table.add(
                    _eyes = new ColorSelector(_ctx, AvatarLogic.EYES, this));
// TODO: give women a colorization for lips?
//             } else if (_aspect.name.equals("mouth")) {
//                 table.add(_lips = new ColorSelector(_ctx, AvatarLogic.LIPS));
            } else {
                table.add(new Spacer(5, 5));
            }

            ComponentRepository crepo =
                _ctx.getCharacterManager().getComponentRepository();
            Collection<AspectCatalog.Aspect> aspects =
                _ctx.getAvatarLogic().getAspectCatalog().getAspects(
                    _gender + _aspect.name);
            for (AspectCatalog.Aspect entry : aspects) {
                // skip aspects that have non-zero cost
                if ((entry.scrip > 0 || entry.coins > 0)) {
                    continue;
                }

                Choice choice = new Choice();
                choice.aspect = entry;
                choice.components =
                    new CharacterComponent[_aspect.classes.length];
                for (int ii = 0; ii < _aspect.classes.length; ii++) {
                    String cclass = _gender + _aspect.classes[ii];
                    try {
                        choice.components[ii] =
                            crepo.getComponent(cclass, entry.name);
                    } catch (NoSuchComponentException nsce) {
                        // not a problem, not all aspects use all of their
                        // underlying component types
                    }
                }
                _choices.add(choice);
            }

            if (_aspect.optional) {
                _choices.add(null);
            }

            // TODO: sort components based on cost

            // configure our default selection
            if (_choices.size() > 0) {
                // TODO: configure _selidx based on existing avatar?
                _selidx = RandomUtil.getInt(_choices.size());
                noteSelection();
            }
        }

        // documentation inherited from interface ActionListener
        public void actionPerformed (ActionEvent event)
        {
            // bail if we have nothing to do
            if (_choices.size() == 0) {
                return;
            }

            String action = event.getAction();
            int csize = _choices.size();
            if (action.equals("down")) {
                _selidx = (_selidx + csize - 1) % csize;
            } else if (action.equals("up")) {
                _selidx = (_selidx + 1) % csize;
            } else if (action.equals("select")) {
                // nothing special, this is a colorization change
            }

            // note our new selection
            noteSelection();

            // and update the avatar portrait
            updateAvatar();
        }

        protected void noteSelection ()
        {
            _selections.put(_aspect.name, _choices.get(_selidx));
        }

        protected AvatarLogic.Aspect _aspect;
        protected int _selidx;
        protected ArrayList<Choice> _choices = new ArrayList<Choice>();
    }

    protected static class Choice
    {
        public AspectCatalog.Aspect aspect;
        public CharacterComponent[] components;
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected StatusLabel _status;

    protected AvatarView _avatar;
    protected BContainer _toggles;
    protected ColorSelector _skin, _hair, _eyes;

    protected BTextField _name;
    protected MoneyLabel _cost;
    protected BButton _buy;

    protected BarberObject _barbobj;
    protected String _gender;

    protected CharacterComponent _ccomp;
    protected HashMap<String,Choice> _selections = new HashMap<String,Choice>();
}
