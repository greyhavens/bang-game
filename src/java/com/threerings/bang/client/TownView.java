//
// $Id$

package com.threerings.bang.client;

import com.jme.renderer.ColorRGBA;

import com.jme.bui.BButton;
import com.jme.bui.BWindow;
import com.jme.bui.layout.GroupLayout;
import com.jme.bui.event.ActionEvent;
import com.jme.bui.event.ActionListener;

import com.threerings.util.MessageBundle;

import com.threerings.bang.ranch.client.RanchView;
import com.threerings.bang.util.BangContext;

/**
 * Displays the main "town" menu interface where a player can navigate to
 * the ranch, the saloon, the general store, the bank, the train station
 * and wherever else we might dream up.
 */
public class TownView extends BWindow
    implements ActionListener
{
    public TownView (BangContext ctx) // String townId
    {
        super(ctx.getLookAndFeel(), GroupLayout.makeVert(GroupLayout.TOP));
        _ctx = ctx;
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.gray);
        _msgs = ctx.getMessageManager().getBundle("town");

        // just add a bunch of buttons for now
        BButton btn;
        add(btn = new BButton(_msgs.get("m.to_ranch"), "to_ranch"));
        btn.addListener(this);
        add(btn = new BButton(_msgs.get("m.to_bank"), "to_bank"));
        btn.addListener(this);
        add(btn = new BButton(_msgs.get("m.to_store"), "to_store"));
        btn.addListener(this);
        add(btn = new BButton(_msgs.get("m.to_saloon"), "to_saloon"));
        btn.addListener(this);
        add(btn = new BButton(_msgs.get("m.logoff"), "logoff"));
        btn.addListener(this);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("logoff".equals(event.getAction())) {
            _ctx.getApp().stop();

        } else if ("to_ranch".equals(event.getAction())) {
            RanchView view = new RanchView(_ctx);
            view.setBounds(0, 0, _ctx.getDisplay().getWidth(),
                           _ctx.getDisplay().getHeight());
            _ctx.getInputDispatcher().addWindow(view);

        } else if ("to_bank".equals(event.getAction())) {

        } else if ("to_store".equals(event.getAction())) {

        } else if ("to_saloon".equals(event.getAction())) {
            _ctx.getLocationDirector().moveTo(2);
        }

        // in any case, we're outta here
        _ctx.getInputDispatcher().removeWindow(this);
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
}
