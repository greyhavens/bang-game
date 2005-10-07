//
// $Id$

package com.threerings.bang.avatar.client;

import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;

import com.jmex.bui.BButton;
import com.jmex.bui.BConstants;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.ImageIcon;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.media.util.MultiFrameImage;

import com.threerings.cast.ActionFrames;
import com.threerings.cast.CharacterComponent;
import com.threerings.cast.CharacterDescriptor;
import com.threerings.cast.ComponentRepository;
import com.threerings.cast.NoSuchComponentException;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.data.AvatarCodes;

import static com.threerings.bang.Log.log;

/**
 * A view for creating a new avatar.
 */
public class CreateAvatarView extends BDecoratedWindow
    implements ActionListener
{
    public CreateAvatarView (BangContext ctx)
    {
        super(ctx.getLookAndFeel(),
              ctx.xlate(AvatarCodes.AVATAR_MSGS, "m.create_title"));
        _ctx = ctx;

        add(_portrait = new BLabel(""), BorderLayout.CENTER);
        _portrait.setPreferredSize(new Dimension(350, 400));

        BContainer spinners = new BContainer(
            GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.CENTER,
                                 GroupLayout.STRETCH));
        spinners.add(new ComponentSelector("background"));
        spinners.add(new ComponentSelector("hat_back"));
        spinners.add(new ComponentSelector("clothing_back"));
        spinners.add(new ComponentSelector("hair_back"));
        spinners.add(new ComponentSelector("beard_back"));
        spinners.add(new ComponentSelector("head"));
        spinners.add(new ComponentSelector("mouth"));
        spinners.add(new ComponentSelector("eyebrows"));
        spinners.add(new ComponentSelector("nose"));
        spinners.add(new ComponentSelector("eyes"));
        spinners.add(new ComponentSelector("beard"));
        spinners.add(new ComponentSelector("mustache"));
        spinners.add(new ComponentSelector("hair_front"));
        spinners.add(new ComponentSelector("clothing_front"));
        spinners.add(new ComponentSelector("glasses"));
        spinners.add(new ComponentSelector("jewelery"));
        spinners.add(new ComponentSelector("hat"));
        spinners.add(new ComponentSelector("hat_band"));
        spinners.add(new ComponentSelector("familiar"));
        add(spinners, BorderLayout.EAST);

        BContainer controls = new BContainer(GroupLayout.makeHStretch());
        String text = ctx.xlate(AvatarCodes.AVATAR_MSGS, "m.cancel");
        controls.add(new BButton(text, this, "cancel"), GroupLayout.FIXED);
        controls.add(new Spacer());
        text = ctx.xlate(AvatarCodes.AVATAR_MSGS, "m.ok");
        controls.add(new BButton(text, this, "ok"), GroupLayout.FIXED);
        add(controls, BorderLayout.SOUTH);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("cancel")) {
            dismiss();
        } else if (action.equals("ok")) {
            dismiss();
        }
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();

        // configure our default portrait
        updatePortrait();
    }

    protected void updatePortrait ()
    {
        if (_selections.size() == 0) {
            return;
        }

        int[] cids = new int[_selections.size()];
        int cidx = 0;
        for (CharacterComponent ccomp : _selections.values()) {
            cids[cidx++] = ccomp.componentId;
        }
        CharacterDescriptor cdesc = new CharacterDescriptor(cids, null);

        ActionFrames af;
        try {
            af = _ctx.getCharacterManager().getActionFrames(cdesc, "default");
        } catch (Exception e) {
            log.log(Level.WARNING, "Unable to load action frames " +
                    "[cdesc=" + cdesc + "].", e);
            return;
        }

        MultiFrameImage mfi = af.getFrames(0);
        BufferedImage image = _ctx.getImageManager().createImage(
            mfi.getWidth(0), mfi.getHeight(0), Transparency.BITMASK);
        Graphics2D gfx = (Graphics2D)image.createGraphics();
        try {
            mfi.paintFrame(gfx, 0, 0, 0);
        } finally {
            gfx.dispose();
        }
        _portrait.setIcon(new ImageIcon(image));
    }

    protected class ComponentSelector extends BContainer
        implements ActionListener
    {
        public ComponentSelector (String cclass)
        {
            super(new TableLayout(4, 5, 5));
            _cclass = cclass;
        }

        @Override // documentation inherited
        public void wasAdded ()
        {
            super.wasAdded();

            String txt = _ctx.xlate(AvatarCodes.AVATAR_MSGS, "m." + _cclass);
            add(new BLabel(txt));

            BButton left = getLookAndFeel().createScrollButton(
                BConstants.HORIZONTAL, true);
            left.setAction("down");
            left.addListener(this);
            add(left);

            add(_selection = new BLabel(""));

            BButton right = getLookAndFeel().createScrollButton(
                BConstants.HORIZONTAL, false);
            right.setAction("up");
            right.addListener(this);
            add(right);

            ComponentRepository crepo =
                _ctx.getCharacterManager().getComponentRepository();
            Iterator iter = crepo.enumerateComponentIds(
                crepo.getComponentClass("male/" + _cclass));
            while (iter.hasNext()) {
                int cid = (Integer)iter.next();
                try {
                    _components.add(crepo.getComponent(cid));
                } catch (NoSuchComponentException nsce) {
                    log.warning("Class contains missing component " +
                                "[cclass=" + _cclass + ", compId=" + cid + "].");
                }
            }

            // configure our default selection
            if (_components.size() > 0) {
                CharacterComponent selcomp = _components.get(_selidx);
                _selections.put(_cclass, selcomp);
                _selection.setText(selcomp.name);
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
            CharacterComponent selcomp = _components.get(_selidx);
            _selections.put(_cclass, selcomp);
            _selection.setText(selcomp.name);

            // and update the portrait
            updatePortrait();
        }

        protected String _cclass;
        protected BLabel _selection;
        protected int _selidx;
        protected ArrayList<CharacterComponent> _components =
            new ArrayList<CharacterComponent>();
    }

    protected BangContext _ctx;
    protected BLabel _portrait;
    protected HashMap<String,CharacterComponent> _selections =
        new HashMap<String,CharacterComponent>();
}
