//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BLayoutManager;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import static com.threerings.bang.Log.log;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PosterInfo;
import com.threerings.bang.util.BangContext;
import com.threerings.presents.client.InvocationService;

/**
 * Display a player's wanted poster.
 */
public class WantedPosterView extends BContainer
{
    public static void displayWantedPoster(final BangContext ctx,
                                           Handle handle)
    {
        final BangClient bangClient = ctx.getBangClient();
        if (!bangClient.canDisplayPopup(MainView.Type.POSTER_DISPLAY)) {
            BangUI.play(BangUI.FeedbackSound.INVALID_ACTION);
            return;
        }

        // first create the popup
        BLayoutManager layout = GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.EQUALIZE);
        final BWindow popup = new BWindow(ctx.getStyleSheet(), layout);
        popup.setStyleClass("dialog_window");

        // then a button that knows how to clear the popup
        final BButton okButton = new BButton("OK");
        okButton.addListener(
            new ActionListener()
            {
                public void actionPerformed (ActionEvent event)
                {
                    if (event.getSource() == okButton) {
                        ctx.getBangClient().clearPopup(popup, false);
                    }
                }
            });

        final BContainer buttonBox = GroupLayout.makeHBox(GroupLayout.RIGHT);
        buttonBox.add(okButton);

        // finally request the poster record
        InvocationService.ResultListener listener = 
            new InvocationService.ResultListener()
            {
                public void requestProcessed(Object result)
                {
                    // if all went well, create the view & display it
                    popup.add(new WantedPosterView(ctx, (PosterInfo) result));
                    popup.add(buttonBox);
                    bangClient.displayPopup(popup, true, 400);
                }
                public void requestFailed(String cause)
                {
                    log.warning("Wanted poster request failed: " + cause);
                    ctx.getChatDirector().displayFeedback(
                        BangCodes.BANG_MSGS, "m.display_poster_failed");
                }
            };

        PlayerService psvc = (PlayerService)
            ctx.getClient().requireService(PlayerService.class);
        psvc.getPosterInfo(ctx.getClient(), handle, listener);
    }

    public WantedPosterView (BangContext ctx, PosterInfo poster)
    {
        super(new BorderLayout());

        _ctx = ctx;
        _poster = poster;

        BLabel label = new BLabel("Wanted: " + _poster.handle.toString());
        label.setStyleClass("window_title");
        add(label, BorderLayout.NORTH);

        AvatarView avatar = new AvatarView(_ctx, 3, true, false);
        if (_poster.avatar != null) {
            // TODO: should not happen, snapshots currently broken?
            avatar.setAvatar(_poster.avatar);
        }
        add(avatar, BorderLayout.WEST);

        BContainer badgeBox = GroupLayout.makeHBox(GroupLayout.CENTER);
        for (int badgeIx = 0; badgeIx < 3; badgeIx ++) {
            int id = _poster.badgeIds[badgeIx];
            if (id != -1) {
                Badge badge = Badge.getType(id).newBadge();
                badgeBox.add(new ItemIcon(_ctx, badge));
            }
        }

        BContainer centerBox = GroupLayout.makeVBox(GroupLayout.TOP);
        centerBox.add(badgeBox);
        if (_poster.statement != null && _poster.statement.length() > 0) {
            centerBox.add(new BLabel("\"" + _poster.statement + "\""));
        }

        add(centerBox, BorderLayout.CENTER);
    }

    protected BangContext _ctx;
    protected PosterInfo _poster;
}
