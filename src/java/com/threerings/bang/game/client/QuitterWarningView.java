//
// $Id$

package com.threerings.bang.game.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BCheckBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.game.data.GameCodes;

/**
 * Displays a warning to user about leaving a ranked game early.
 */
public class QuitterWarningView extends BDecoratedWindow
    implements ActionListener
{
    /** The width to hint when laying out this window. */
    public static final int WIDTH_HINT = 400;

    public QuitterWarningView (BangContext ctx, BWindow options)
    {
        super(ctx.getStyleSheet(), ctx.xlate(GameCodes.GAME_MSGS, "m.quitter_title"));
        setLayer(10);
        setModal(true);
        ((GroupLayout)getLayoutManager()).setGap(15);

        _ctx = ctx;
        _options = options;

        MessageBundle msgs = ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);
        add(new BLabel(msgs.get("m.quitter_info")));
        add(_checkbox = new BCheckBox(msgs.get("m.quitter_check")), GroupLayout.FIXED);
        BContainer box = GroupLayout.makeHBox(GroupLayout.CENTER);
        box.add(new BButton(msgs.get("m.quitter_leave"), this, "leave"));
        box.add(new BButton(msgs.get("m.quitter_resume"), this, "resume"));
        add(box, GroupLayout.FIXED);
    }

    public void setTown (boolean town)
    {
        _town = town;
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if ("leave".equals(action)) {
            if (_town) {
                if (_ctx.getBangClient().showTownView()) {
                    clearPopups();
                }
            } else {
                if (_ctx.getLocationDirector().moveTo(_ctx.getBangClient().getPriorLocationOid())) {
                    clearPopups();
                }
            }
        } else {
            clearPopups();
        }
    }

    protected void clearPopups ()
    {
        if (_checkbox.isSelected()) {
            BangPrefs.setNoQuitterWarning(_ctx.getUserObject());
        }
        _ctx.getBangClient().clearPopup(_options, false);
        _ctx.getBangClient().clearPopup(this, true);
    }

    protected BangContext _ctx;
    protected boolean _town;
    protected BWindow _options;
    protected BCheckBox _checkbox;
}
