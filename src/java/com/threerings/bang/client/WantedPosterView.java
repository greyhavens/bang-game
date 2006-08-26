//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.Spacer;
import com.jmex.bui.border.LineBorder;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.BLayoutManager;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Point;
import com.jme.renderer.ColorRGBA;

import static com.threerings.bang.Log.log;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.client.BangUI.FeedbackSound;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.data.Badge;
import com.threerings.bang.data.Badge.Type;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PosterInfo;
import com.threerings.bang.util.BangContext;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService.ResultListener;

/**
 * Display a player's wanted poster.
*/
public class WantedPosterView extends BContainer
{
    /**
     * Creates a new wanted poster display popup for the given handle.
     */
    public static void displayWantedPoster(final BangContext ctx,
                                           final Handle handle)
    {
        final BangClient bangClient = ctx.getBangClient();
        if (!bangClient.canDisplayPopup(MainView.Type.POSTER_DISPLAY)) {
            BangUI.play(BangUI.FeedbackSound.INVALID_ACTION);
            return;
        }

        // first create the popup
        BLayoutManager layout = GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.BOTTOM, GroupLayout.EQUALIZE);
        final BWindow popup = new BWindow(ctx.getStyleSheet(), layout);
        popup.setStyleClass("dialog_window");

        // add the actual poster view
        WantedPosterView view = new WantedPosterView(ctx);
        view.setHandle(handle);
        popup.add(view);

        final BContainer buttonBox = GroupLayout.makeHBox(GroupLayout.CENTER);
        ((GroupLayout )buttonBox.getLayoutManager()).setGap(30);

        // add an edit button, if we're looking at ourselves
        if (handle.equals(ctx.getUserObject().handle)) {
            final BButton editButton = new BButton(
                ctx.xlate(BangCodes.BANG_MSGS, "m.poster_edit"));
            editButton.addListener(
                new ActionListener() {
                    public void actionPerformed (ActionEvent event) {
                        if (event.getSource() == editButton) {
                            ctx.getBangClient().clearPopup(popup, false);
                            EditPosterView.editWantedPoster(ctx, handle);
                        }
                    }
                });
            buttonBox.add(editButton);
        }

        // then a button that knows how to clear the popup
        final BButton backButton = new BButton(
            ctx.xlate(BangCodes.BANG_MSGS, "m.poster_goback"));
        backButton.addListener(
            new ActionListener() {
                public void actionPerformed (ActionEvent event) {
                    if (event.getSource() == backButton) {
                        ctx.getBangClient().clearPopup(popup, false);
                    }
                }
            });
        buttonBox.add(backButton);
        popup.add(buttonBox, GroupLayout.FIXED);

        bangClient.displayPopup(popup, true);
    }

    public WantedPosterView (BangContext ctx)
    {
        super(new AbsoluteLayout());

        // our preferred size is determined by the background image
        setStyleClass("poster_view");

        _ctx = ctx;
    }

    /**
     * Determines the poster to view through a handle, initiating a server
     * request for the {@link PosterInfo} object to use.
     */
    public void setHandle(Handle handle)
    {
        if (_handle != null && _handle.equals(handle)) {
            return;
        }
        _handle = handle;

        // request the poster record
        InvocationService.ResultListener listener = 
            new InvocationService.ResultListener() {
                public void requestProcessed(Object result) {
                    setPoster((PosterInfo) result);
                }
                public void requestFailed(String cause) {
                    log.warning("Wanted poster request failed: " + cause);
                    _ctx.getChatDirector().displayFeedback(
                        BangCodes.BANG_MSGS, "m.poster_failed");
                }
            };

        PlayerService psvc = (PlayerService)
            _ctx.getClient().requireService(PlayerService.class);
        psvc.getPosterInfo(
            _ctx.getClient(), handle, listener);        
    }

    /**
     * Determines the poster to view directly through a {@link PosterInfo}
     * object.
     */
    public void setPoster(PosterInfo poster)
    {
        if (_poster != null && _poster.equals(poster)) {
            return;
        }
        _poster = poster;
        buildPoster();
    }

    protected void buildPoster()
    {
        removeAll();

        add(buildWantedLabel(), new Point(310, 560));
        add(buildAvatarView(), new Point(40, 264));
        add(buildStatementView(), new Point(50, 220));
        add(buildBadgeView(), new Point(57, 33));
    }

    protected BComponent buildWantedLabel()
    {
        BContainer box = GroupLayout.makeVBox(GroupLayout.CENTER);
        box.setPreferredSize(new Dimension(320, 120));
        box.setStyleClass("poster_handle_box");

        BLabel handleLabel = new BLabel(_poster.handle.toString(),
                                        "poster_handle");
        box.add(handleLabel);

        if (true) {
            // TODO: disable until there's actually gangs
            BLabel gangLabel = new BLabel("Member of the \"DALTON GANG\"",
                                          "poster_gang");
            box.add(gangLabel);
        }
        return box;
    }

    protected BComponent buildAvatarView()
    {
        // TODO: add the sepia_avatar.png background
        AvatarView avatar = new AvatarView(_ctx, 2, false, false);
        avatar.setStyleClass("poster_avatar");
        if (_poster.avatar != null) {
            // TODO: should not happen, snapshots currently broken?
            avatar.setAvatar(_poster.avatar);
        }
        return avatar;
    }

    protected BComponent buildBadgeView()
    {
        IconPalette palette = new IconPalette(
            null, PosterInfo.BADGES, 1, ItemIcon.ICON_SIZE, 0);
        palette.setShowNavigation(false);
        for (int badgeIx = 0; badgeIx < PosterInfo.BADGES; badgeIx ++) {
            int id = _poster.badgeIds[badgeIx];
            if (id != -1) {
                Badge badge = Badge.getType(id).newBadge();
                palette.addIcon(new ItemIcon(_ctx, badge));
            }
        }
        return palette;
    }

    protected BComponent buildStatementView()
    {
        BContainer box = GroupLayout.makeVBox(GroupLayout.CENTER);
        box.setPreferredSize(new Dimension(250, 35));
        box.setStyleClass("poster_statement_box");

        BLabel label = new BLabel(_poster.statement != null ?
                                  "\"" + _poster.statement + "\"" : "",
                                  "poster_statement");
        box.add(label);
        return box;
    }

    protected BangContext _ctx;

    /** The handle of the poster being either displayed or requested */
    protected Handle _handle;

    /** After successul request, the PosterInfo record */
    protected PosterInfo _poster;
}
