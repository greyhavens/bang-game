//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BComponent;
import com.jmex.bui.BTextField;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.Predicate;
import com.samskivert.util.StringUtil;

import static com.threerings.bang.Log.log;

import com.threerings.bang.client.bui.IconPalette.Inspector;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PosterInfo;
import com.threerings.bang.util.BangContext;

/**
 * Displays an interface for a player to edit their wanted poster.
 */
public class EditPosterView extends BContainer
{
    /**
     * Creates a new Wanted Poster edit popup for the given handle.
     */
    public static void editWantedPoster (BangContext ctx, Handle handle)
    {
        BangClient client = ctx.getBangClient();
        if (!client.canDisplayPopup(MainView.Type.POSTER_DISPLAY)) {
            BangUI.play(BangUI.FeedbackSound.INVALID_ACTION);
            return;
        }

        BWindow popup = new BWindow(ctx.getStyleSheet(), new BorderLayout());
        popup.setModal(true);
        popup.setStyleClass("poster_edit_popup");
        popup.add(new EditPosterView(ctx, handle, popup), BorderLayout.CENTER);
        client.displayPopup(popup, true);
    }

    public EditPosterView (BangContext ctx, Handle handle, final BWindow popup)
    {
        _ctx = ctx;

        GroupLayout layout = GroupLayout.makeHoriz(GroupLayout.TOP);
        layout.setGap(0);
        setLayoutManager(layout);
        setPreferredSize(new Dimension(950, 710));

        // create a poster view and request the poster data
        _posterView = new EditablePosterView(_ctx);
        _posterView.setHandle(handle);

        _right = new BContainer(new BorderLayout(0, 15));

        // we need an InventoryPalette that only shows Badges
        Predicate<Item> predicate = new Predicate.InstanceOf<Item>(Badge.class);
        _badges = new PosterPalette(_ctx, predicate, 2, 3);
        _badges.setPaintBorder(true);
        _badges.setSelectable(PosterInfo.BADGES);
        _right.add(_badges, BorderLayout.CENTER);

        // and buttons to cancel changes or commit them
        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        BButton cancel = new BButton(
            _ctx.xlate(BangCodes.BANG_MSGS, "m.cancel"));
        cancel.addListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                _ctx.getBangClient().clearPopup(popup, false);
            }
        });
        buttons.add(cancel);

        BButton commit = new BButton(
            _ctx.xlate(BangCodes.BANG_MSGS, "m.poster_commit"));
        commit.addListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                PlayerService.ConfirmListener listener =
                    new PlayerService.ConfirmListener() {
                    public void requestProcessed() {
                        _ctx.getBangClient().clearPopup(popup, false);
                    }
                    public void requestFailed(String cause) {
                        log.warning("Poster commit failed: " + cause);
                        _ctx.getChatDirector().displayFeedback(
                            BangCodes.BANG_MSGS, "m.poster_commit_failed");
                    }
                };
                _posterView.storePoster(listener);
            }
        });
        buttons.add(commit);
        _right.add(buttons, BorderLayout.SOUTH);
    }

    /**
     * Called from the {@link EditablePosterView} when the poster request
     * finishes and we can finalize the UI.
     */
    protected void posterIsReady ()
    {
        // respond to new poster data
        add(_posterView, GroupLayout.FIXED);
        add(_right);

        // add the inspector after the palette is initialized
        _badges.setInspector(new Inspector() {
            public void iconUpdated (SelectableIcon icon, boolean selected) {
                PosterInfo poster = _posterView._poster;
                int idx = 0;
                for ( ; idx < poster.badgeIds.length; idx++) {
                    SelectableIcon sicon = _badges.getSelectedIcon(idx);
                    if (sicon == null) {
                        break;
                    }
                    Badge badge = (Badge) ((ItemIcon) sicon).getItem();
                    poster.badgeIds[idx] = badge.getType().code();
                }
                for ( ; idx < poster.badgeIds.length; idx++) {
                    poster.badgeIds[idx] = -1;
                }
                _posterView.buildPoster();
            }
        });
    }

    /**
     * Extends the {@link InventoryPalette} with logic to initialize icons to
     * be selected if they're on the player's current poster.
     */
    protected class PosterPalette extends InventoryPalette
    {
        public PosterPalette (
            BangContext ctx, Predicate<Item> itemp, int columns, int rows)
        {
            super(ctx, itemp, columns, rows);
        }

        @Override // from IconPalette
        public void addIcon (SelectableIcon icon)
        {
            super.addIcon(icon);

            // select the icons that are configured in the player's poster
            PosterInfo poster = _posterView._poster;
            int code = ((Badge) ((ItemIcon) icon).getItem()).getType().code();
            boolean selected = false;
            for (int selcode : poster.badgeIds) {
                selected |= (selcode == code);
            }
            icon.setSelected(selected);
        }
    }

    /**
     * Extends the poster view with editable elements.
     */
    protected class EditablePosterView extends WantedPosterView
    {
        @Override // from BComponent
        public void invalidate ()
        {
            // if we're invalidated, update the palette view too
            if (_badges != null) {
                _badges.invalidate();
            }
            super.invalidate();
        }

        @Override // from WantedPosterView
        public void setPoster (PosterInfo poster)
        {
            super.setPoster(poster);
            posterIsReady();
        }

        @Override // from WantedPosterView
        public BComponent buildStatementView()
        {
            _statement = new BTextField(
                _poster.statement, MAX_STATEMENT_LENGTH);
            _statement.setPreferredWidth(150);
            _statement.setStyleClass("poster_statement_field");
            return _statement;
        }

        protected EditablePosterView (BangContext ctx)
        {
            super(ctx);
        }

        protected void storePoster (PlayerService.ConfirmListener listener)
        {
            // filter our statement
            String stmt = _ctx.getChatDirector().filter(
                _statement.getText(), null, true);
            stmt = StringUtil.truncate(stmt, MAX_STATEMENT_LENGTH);

            // and send the update request
            PlayerService psvc = _ctx.getClient().requireService(PlayerService.class);
            psvc.updatePosterInfo(_ctx.getUserObject().playerId, stmt, _poster.badgeIds, listener);
        }

        protected BTextField _statement;
    }

    protected BangContext _ctx;

    protected EditablePosterView _posterView;
    protected PosterPalette _badges;
    protected BContainer _right;

    protected static final int MAX_STATEMENT_LENGTH = 40;
}
