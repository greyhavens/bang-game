//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.bang.util.BangContext;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.util.EscapeListener;

/**
 * A base class for displaying little menus that pop up when the user
 * presses escape.
 */
public class EscapeMenuView extends BDecoratedWindow
    implements ActionListener
{
    public EscapeMenuView (BangContext ctx)
    {
        super(ctx.getLookAndFeel(), null);
        setLayoutManager(GroupLayout.makeVStretch());

        _modal = true;
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("options");

        addButtons();

        addListener(new EscapeListener() {
            public void escapePressed() {
                dismiss();
            }
        });
    }

    /**
     * Binds this menu to a component. The component should either be the
     * default event target, or a modal window.
     */
    public void bind (BComponent host)
    {
        host.addListener(new EscapeListener() {
            public void escapePressed () {
                _ctx.getRootNode().addWindow(EscapeMenuView.this);
                pack();
                center();
            }
        });
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if ("dismiss".equals(action)) {
            dismiss();
        } else if ("quit".equals(action)) {
            _ctx.getApp().stop();
        }
    }

    protected void addButtons ()
    {
        add(createButton("quit"));
    }

    protected BButton createButton (String action)
    {
        return createButton("m." + action, action);
    }

    protected BButton createButton (String label, String action)
    {
        BButton btn = new BButton(_msgs.get(label), action);
        btn.addListener(this);
        return btn;
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
}
