//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.bui.SteelWindow;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

/**
 * Ensures a user is over 13 before allowing them into interactive areas.
 */
public class CoppaView extends SteelWindow
    implements ActionListener
{
    public CoppaView (BangContext ctx)
    {
        super(ctx, ctx.xlate(BangCodes.BANG_MSGS, "m.coppa_title"));
        setModal(true);
        _contents.setLayoutManager(GroupLayout.makeVert(GroupLayout.CENTER).setGap(15));
        _contents.setStyleClass("padded");

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);

        _contents.add(new BLabel(_msgs.get("m.coppa_info"), "dialog_text"));

        _contents.add(_status = new BLabel("", "dialog_text"));

        _buttons.add(_over = new BButton(_msgs.get("m.coppa_over"), this, "over"));
        _buttons.add(_under = new BButton(_msgs.get("m.coppa_under"), this, "under"));
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String cmd = event.getAction();
        if ("under".equals(cmd)) {
            _ctx.getBangClient().clearPopup(this, true);
            _ctx.getBangClient().resetTownView();
        } else if ("over".equals(cmd)) {
            declareAge();
        }
    }

    protected void declareAge ()
    {
        PlayerService psvc = (PlayerService)_ctx.getClient().requireService(PlayerService.class);
        PlayerService.ConfirmListener cl = new PlayerService.ConfirmListener() {
            public void requestProcessed () {
                _ctx.getBangClient().clearPopup(CoppaView.this, true);
                _ctx.getBangClient().continueMoveTo();
            }
            public void requestFailed (String reason) {
                _status.setText(_msgs.xlate(reason));
                _over.setEnabled(true);
                _under.setEnabled(true);
            }
        };
        _over.setEnabled(false);
        _under.setEnabled(false);
        psvc.declareOfAge(_ctx.getClient(), cl);
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BLabel _status;
    protected BButton _over, _under;
}
