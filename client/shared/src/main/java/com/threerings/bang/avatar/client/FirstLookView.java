//
// $Id$

package com.threerings.bang.avatar.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.RandomUtil;

import com.threerings.cast.CharacterComponent;
import com.threerings.cast.ComponentRepository;
import com.threerings.cast.NoSuchComponentException;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.Article;
import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.data.BarberCodes;
import com.threerings.bang.avatar.data.LookConfig;
import com.threerings.bang.avatar.util.AspectCatalog;
import com.threerings.bang.avatar.util.AvatarLogic;

/**
 * Allows the configuration of a player's first avatar look.
 */
public class FirstLookView extends BContainer
{
    public FirstLookView (BangContext ctx, StatusLabel status)
    {
        super(new BorderLayout(50, 5));
        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(BarberCodes.BARBER_MSGS);
        _status = status;

        // create our default clothing articles
        _defarts[0] = ctx.getAvatarLogic().createDefaultClothing(ctx.getUserObject(), true);
        _defarts[1] = ctx.getAvatarLogic().createDefaultClothing(ctx.getUserObject(), false);

        // create our user interface
        add(_avatar = new AvatarView(ctx, 2, true, false), BorderLayout.WEST);
        TableLayout tlay = new TableLayout(4, 5, 20);
        tlay.setEqualRows(true);
        _toggles = new BContainer(tlay);
        BContainer wrapper = GroupLayout.makeVBox(GroupLayout.CENTER);
        ((GroupLayout)wrapper.getLayoutManager()).setGap(10);
        wrapper.add(_toggles);

        // do some jimmying to center the rando button nicely
        BContainer jimmy = GroupLayout.makeHBox(GroupLayout.CENTER);
        jimmy.add(new Spacer(122, 5));
        jimmy.add(BangUI.createDiceButton(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                for (AspectToggle at : _ats) {
                    at.pickRandom();
                }
                _skin.pickRandom();
                _hair.pickRandom();
                _eyes.pickRandom();
                updateAvatar();
            }
        }, "random"));
        wrapper.add(jimmy);
        add(wrapper, BorderLayout.CENTER);
    }

    /**
     * Used to change the gender of the avatar we're creating.
     */
    public void setGender (boolean isMale)
    {
        _gender = isMale ? "male/" : "female/";
        _defart = _defarts[isMale ? 0 : 1];
        _toggles.removeAll();
        _ats.clear();
        for (int ii = 0; ii < AvatarLogic.ASPECTS.length; ii++) {
            // we don't allow first time avatars to have mustaches or beards
            if (!AvatarLogic.ASPECTS[ii].maleOnly) {
                _ats.add(new AspectToggle(AvatarLogic.ASPECTS[ii], _toggles));
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
        config.name = "";
        config.hair = _hair.getSelectedColor();
        config.skin = _skin.getSelectedColor();

        // look up the selection for each aspect class
        config.aspects = new String[AvatarLogic.ASPECTS.length];
        config.colors = new int[config.aspects.length];
        for (int ii = 0; ii < config.aspects.length; ii++) {
            Choice choice = _selections.get(AvatarLogic.ASPECTS[ii].name);
            if (choice == null) {
                continue;
            }
            config.aspects[ii] = choice.aspect.name;
            config.colors[ii] = getColorizations(AvatarLogic.ASPECTS[ii].name);
        }

        return config;
    }

    /**
     * Returns the colorization info for the default clothing article used when
     * creating our avatar.
     */
    public int getDefaultArticleColorizations ()
    {
        // the top 16 bits of every component id are the colorizations
        return _defart.getComponents()[0] & 0xFFFF0000;
    }

    protected void updateAvatar ()
    {
        // obtain the component ids of the various aspect selections and total
        // up the cost of this look while we're at it
        ArrayIntSet compids = new ArrayIntSet();
        Iterator<Map.Entry<String,Choice>> iter =
            _selections.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String,Choice> entry = iter.next();
            String aspect = entry.getKey();
            Choice choice = entry.getValue();
            if (choice == null) {
                continue;
            }
            int zations = getColorizations(aspect);
            for (int ii = 0; ii < choice.components.length; ii++) {
                if (choice.components[ii] != null) {
                    compids.add(zations | choice.components[ii].componentId);
                }
            }
        }

        // copy in the components from the default article
        if (_defart != null) {
            compids.add(_defart.getComponents());
        }

        int[] avatar = new int[compids.size()+1];
        avatar[0] = (_hair.getSelectedColor() << 5) | _skin.getSelectedColor();
        compids.toIntArray(avatar, 1);

        // update the avatar display
        _avatar.setAvatar(new AvatarInfo(avatar));
    }

    protected int getColorizations (String aspect)
    {
        // for now we just suppotr eye color
        if (aspect.equals("eyes")) {
            return AvatarLogic.composeZation(
                _eyes.getColorClass(), _eyes.getSelectedColor());
        } else {
            return 0;
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
                // skip aspects that are above the threshold cost
                if (entry.scrip > AvatarCodes.MAX_STARTER_COST ||
                    entry.coins > 0) {
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

            // configure our default selection
            pickRandom();
        }

        public void pickRandom ()
        {
            if (_choices.size() > 0) {
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

    protected ArrayList<AspectToggle> _ats = new ArrayList<AspectToggle>();
    protected String _gender;

    protected Article[] _defarts = new Article[2];
    protected Article _defart;

    protected CharacterComponent _ccomp;
    protected HashMap<String,Choice> _selections = new HashMap<String,Choice>();
}
