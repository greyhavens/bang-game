//
// $Id$

package com.threerings.bang.client;

import com.jme.bui.BButton;
import com.jme.bui.BCheckBox;
import com.jme.bui.BContainer;
import com.jme.bui.BLabel;
import com.jme.bui.BWindow;
import com.jme.bui.TintedBackground;
import com.jme.bui.event.ActionEvent;
import com.jme.bui.event.ActionListener;
import com.jme.bui.layout.GroupLayout;
import com.jme.renderer.ColorRGBA;

import com.threerings.util.MessageBundle;

import com.threerings.bang.util.BangContext;

/**
 * Allows options to be viewed and adjusted. Presently that's just video
 * mode and whether or not we're in full screen mode.
 */
public class OptionsView extends BWindow
    implements ActionListener
{
    public OptionsView (BangContext ctx)
    {
        super(ctx.getLookAndFeel(), GroupLayout.makeVStretch());
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("options");

        setBackground(new TintedBackground(10, 10, 10, 10, ColorRGBA.darkGray));

        add(new BLabel(_msgs.get("m.title")));

        BContainer cont = GroupLayout.makeButtonBox(GroupLayout.LEFT);
        cont.add(new BLabel(_msgs.get("m.video_mode")));
        cont.add(new BButton("<select>"));
        add(cont);

        cont = GroupLayout.makeButtonBox(GroupLayout.LEFT);
        cont.add(new BCheckBox(_msgs.get("m.fullscreen_mode")));
        add(cont);

        cont = GroupLayout.makeButtonBox(GroupLayout.RIGHT);
        BButton btn;
        cont.add(btn = new BButton(_msgs.get("m.dismiss"), "dismiss"));
        btn.addListener(this);
        add(cont);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("dismiss".equals(event.getAction())) {
            dismiss();
        }
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
}
