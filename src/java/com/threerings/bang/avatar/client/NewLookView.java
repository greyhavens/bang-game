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
import com.jmex.bui.BTextArea;
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
import com.threerings.bang.data.Article;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.data.BarberCodes;
import com.threerings.bang.avatar.data.BarberObject;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.util.AspectCatalog;
import com.threerings.bang.avatar.util.AvatarMetrics;

/**
 * Allows the configuration of a new avatar look.
 */
public class NewLookView extends BContainer
    implements ActionListener
{
    public NewLookView (BangContext ctx, BTextArea status)
    {
        super(new BorderLayout(5, 5));
        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(BarberCodes.BARBER_MSGS);
        _status = status;

        add(_avatar = new AvatarView(ctx), BorderLayout.WEST);

        boolean isMale = _ctx.getUserObject().isMale;
        _gender = isMale ? "male/" : "female/";

        BContainer toggles = new BContainer(new TableLayout(4, 5, 5));
        add(toggles, BorderLayout.CENTER);
        for (int ii = 0; ii < AvatarMetrics.ASPECTS.length; ii++) {
            if (isMale || !AvatarMetrics.ASPECTS[ii].maleOnly) {
                new AspectToggle(AvatarMetrics.ASPECTS[ii], toggles);
            }
        }

        BContainer cost = GroupLayout.makeHBox(GroupLayout.RIGHT);
        add(cost, BorderLayout.SOUTH);
        cost.add(new BLabel(_msgs.get("m.look_name")));
        cost.add(_name = new BTextField(""));
        // TODO: limit length to BarberCodes.MAX_LOOK_NAME_LENGTH
        _name.setPreferredWidth(150);
        cost.add(new Spacer(25, 1));
        cost.add(new BLabel(_msgs.get("m.look_cost")));
        cost.add(_cost = new MoneyLabel(ctx));
        _cost.setMoney(0, 0, false);
        cost.add(new Spacer(25, 1));
        cost.add(_buy = new BButton(_msgs.get("m.buy"), this, "buy"));

        updateAvatar();
    }

    /**
     * Called by the {@link BarberView} to give us a reference to our barber
     * object when needed.
     */
    public void setBarberObject (BarberObject barbobj)
    {
        _barbobj = barbobj;
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        // make sure they specified a name for the new look
        String name = _name.getText();
        if (StringUtil.blank(name)) {
            _status.setText(_msgs.get("m.name_required"));
            return;
        }

        // look up the selection for each aspect class
        String[] choices = new String[AvatarMetrics.ASPECTS.length];
        for (int ii = 0; ii < choices.length; ii++) {
            Choice choice = _selections.get(AvatarMetrics.ASPECTS[ii].name); 
            choices[ii] = (choice == null) ? null : choice.aspect.name;
        }

        // TODO: get per-aspect colorizations
        int[] colors = new int[choices.length];

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
        _barbobj.service.purchaseLook(
            _ctx.getClient(), name, _hair.getSelectedColor(),
            _skin.getSelectedColor(), choices, colors, cl);
    }

    protected void updateAvatar ()
    {
        int scrip = BarberCodes.BASE_LOOK_SCRIP_COST,
            coins = BarberCodes.BASE_LOOK_COIN_COST;

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
        if (current != null) {
            for (int ii = 0; ii < AvatarMetrics.SLOTS.length; ii++) {
                if (AvatarMetrics.SLOTS[ii].optional) {
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
        _cost.setMoney(scrip, coins, false);
    }

    protected class AspectToggle
        implements ActionListener
    {
        public AspectToggle (AvatarMetrics.Aspect aspect, BContainer table)
        {
            _aspect = aspect;

            BButton left = new BButton(BangUI.leftArrow, "down");
            left.addListener(this);
            table.add(left);

            table.add(new BLabel(_ctx.xlate(AvatarCodes.AVATAR_MSGS,
                                            "m." + aspect.name)));

            BButton right = new BButton(BangUI.rightArrow, "up");
            right.addListener(this);
            table.add(right);

            if (_aspect.name.equals("head")) {
                table.add(_skin = new ColorSelector(_ctx, AvatarMetrics.SKIN));
                _skin.addListener(this);
            } else if (_aspect.name.equals("hair")) {
                table.add(_hair = new ColorSelector(_ctx, AvatarMetrics.HAIR));
                _hair.addListener(this);
            } else if (_aspect.name.equals("eyes")) {
                table.add(_eyes = new ColorSelector(_ctx, AvatarMetrics.EYES));
                _eyes.addListener(this);
// TODO: give women a colorization for lips?
//             } else if (_aspect.name.equals("mouth")) {
//                 table.add(_lips = new ColorSelector(_ctx, AvatarMetrics.LIPS));
//                 _lips.addListener(this);
            } else {
                table.add(new Spacer(5, 5));
            }

            ComponentRepository crepo =
                _ctx.getCharacterManager().getComponentRepository();
            Collection<AspectCatalog.Aspect> aspects =
                _ctx.getAspectCatalog().getAspects(_gender + _aspect.name);
            for (AspectCatalog.Aspect entry : aspects) {
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

        protected AvatarMetrics.Aspect _aspect;
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
    protected BTextArea _status;

    protected AvatarView _avatar;
    protected ColorSelector _skin, _hair, _eyes;

    protected BTextField _name;
    protected MoneyLabel _cost;
    protected BButton _buy;

    protected BarberObject _barbobj;
    protected String _gender;

    protected CharacterComponent _ccomp;
    protected HashMap<String,Choice> _selections = new HashMap<String,Choice>();
}
