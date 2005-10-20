//
// $Id$

package com.threerings.bang.avatar.client;

import java.util.ArrayList;
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

import com.threerings.cast.CharacterComponent;
import com.threerings.cast.ComponentRepository;
import com.threerings.cast.NoSuchComponentException;
import com.threerings.util.MessageBundle;
import com.threerings.util.RandomUtil;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.util.AvatarMetrics;

import static com.threerings.bang.Log.log;

/**
 * Allows the configuration of a new avatar look.
 */
public class NewLookView extends BContainer
    implements ActionListener
{
    public NewLookView (BangContext ctx)
    {
        super(new BorderLayout(5, 5));
        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(AvatarCodes.AVATAR_MSGS);

        add(_avatar = new AvatarView(ctx), BorderLayout.WEST);

        // TODO: obtain gender based on the user object
        _gender = (RandomUtil.getInt(100) >= 50) ? "male/" : "female/";

        BContainer toggles = new BContainer(new TableLayout(5, 5, 5));
        add(toggles, BorderLayout.CENTER);
        for (int ii = 0; ii < AvatarMetrics.ASPECTS.length; ii++) {
            // TODO: skip male-only toggles if we are female
            new AspectToggle(AvatarMetrics.ASPECTS[ii], toggles);
        }

        BContainer cost = GroupLayout.makeHBox(GroupLayout.RIGHT);
        add(cost, BorderLayout.SOUTH);
        cost.add(new BLabel(_msgs.get("m.look_name")));
        cost.add(_name = new BTextField(""));
        _name.setPreferredWidth(150);
        cost.add(new Spacer(25, 1));
        cost.add(new BLabel(_msgs.get("m.look_cost")));
        cost.add(_cost = new MoneyLabel(ctx));
        _cost.setMoney(0, 0, false);
        cost.add(new Spacer(25, 1));
        cost.add(new BButton(_msgs.get("m.buy"), this, "buy"));

        // TEMP: select a default torso component
        ComponentRepository crepo =
            _ctx.getCharacterManager().getComponentRepository();
        Iterator iter = crepo.enumerateComponentIds(
            crepo.getComponentClass("male/clothing_back"));
        if (iter.hasNext()) {
            try {
                _selections.put("clothing_back",
                                crepo.getComponent((Integer)iter.next()));
            } catch (NoSuchComponentException nsce) {
            }
        }
        // END TEMP

        updateAvatar();
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
    }

    protected void updateAvatar ()
    {
        int[] avatar = new int[AvatarMetrics.SLOTS.length];
        // TODO: get global colorizations from proper place
        avatar[0] = (7 << 5) | 3;
        for (int ii = 1; ii < avatar.length; ii++) {
            CharacterComponent ccomp = _selections.get(AvatarMetrics.SLOTS[ii]);
            if (ccomp != null) {
                // TODO: add encoded colorizations
                avatar[ii] = ccomp.componentId;
            }
        }
        _avatar.setAvatar(avatar);
    }

    protected class AspectToggle
        implements ActionListener
    {
        public AspectToggle (AvatarMetrics.Aspect aspect, BContainer table)
        {
            _aspect = aspect;

            table.add(new BLabel(_msgs.get("m." + aspect.name)));

            BButton left = new BButton(BangUI.leftArrow, "down");
            left.addListener(this);
            table.add(left);

            table.add(_selection = new BLabel(""));

            BButton right = new BButton(BangUI.rightArrow, "up");
            right.addListener(this);
            table.add(right);

            // TODO: add a color selector for certain aspects
            table.add(new Spacer(5, 5));

            ComponentRepository crepo =
                _ctx.getCharacterManager().getComponentRepository();

            // we iterate over the first component class defined in the aspect
            // and assume all associated classes have components with the exact
            // same names (which they should unless someone fuppeduck)
            String cclass = _gender + _aspect.classes[0];
            Iterator iter = crepo.enumerateComponentIds(
                crepo.getComponentClass(cclass));
            while (iter.hasNext()) {
                int cid = (Integer)iter.next();
                CharacterComponent[] comps =
                    new CharacterComponent[_aspect.classes.length];
                try {
                    comps[0] = crepo.getComponent(cid);
                } catch (NoSuchComponentException nsce) {
                    log.warning("Missing component [aspect=" + aspect.name +
                                ", cclass=" + cclass + ", compId=" + cid + "].");
                }
                String cname = comps[0].name;
                for (int ii = 1; ii < _aspect.classes.length; ii++) {
                    cclass = _gender + _aspect.classes[ii];
                    try {
                        comps[ii] = crepo.getComponent(cclass, cname);
                    } catch (NoSuchComponentException nsce) {
                        // not a problem, these are optional
                    }
                }
                _components.add(comps);
            }

            if (_aspect.optional) {
                _components.add(null);
            }

            // TODO: sort components based on cost

            // configure our default selection
            if (_components.size() > 0) {
                // TODO: configure _selidx based on existing avatar;
                _selidx = RandomUtil.getInt(_components.size());
                noteSelection();
            }
        }

        // documentation inherited from interface ActionListener
        public void actionPerformed (ActionEvent event)
        {
            // bail if we have nothing to do
            if (_components.size() == 0) {
                return;
            }

            String action = event.getAction();
            int csize = _components.size();
            if (action.equals("down")) {
                _selidx = (_selidx + csize - 1) % csize;
            } else if (action.equals("up")) {
                _selidx = (_selidx + 1) % csize;
            }

            // note our new selection
            noteSelection();

            // and update the avatar portrait
            updateAvatar();
        }

        protected void noteSelection ()
        {
            CharacterComponent[] selcomp = _components.get(_selidx);
            for (int ii = 0; ii < _aspect.classes.length; ii++) {
                _selections.put(_aspect.classes[ii],
                                (selcomp == null) ? null : selcomp[ii]);
            }
            // TODO: translate
            _selection.setText(selcomp == null ?
                               _msgs.get("m.none") : selcomp[0].name);
        }

        protected AvatarMetrics.Aspect _aspect;
        protected BLabel _selection;
        protected int _selidx;
        protected ArrayList<CharacterComponent[]> _components =
            new ArrayList<CharacterComponent[]>();
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected AvatarView _avatar;
    protected BTextField _name;
    protected MoneyLabel _cost;

    protected String _gender;

    protected HashMap<String,CharacterComponent> _selections =
        new HashMap<String,CharacterComponent>();
}
