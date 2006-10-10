package com.threerings.bang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BComponent;
import com.jmex.bui.BTextField;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.event.MouseAdapter;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.Predicate;

import static com.threerings.bang.Log.log;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PosterInfo;
import com.threerings.bang.client.BangUI.FeedbackSound;
import com.threerings.bang.client.bui.IconPalette.Inspector;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.client.MainView.Type;
import com.threerings.bang.util.BangContext;

/**
 * Displays an interface for a player to edit their wanted poster.
 */
public class EditPosterView extends BContainer
{
    /**
     * Creates a new wanted poster edit popup for the given handle.
     */
    public static void editWantedPoster(final BangContext ctx,
                                        Handle handle)
    {
        final BangClient bangClient = ctx.getBangClient();
        if (!bangClient.canDisplayPopup(MainView.Type.POSTER_DISPLAY)) {
            BangUI.play(BangUI.FeedbackSound.INVALID_ACTION);
            return;
        }

        BWindow popup = new BWindow(ctx.getStyleSheet(), new BorderLayout());
        popup.setStyleClass("poster_edit_popup");
        popup.add(new EditPosterView(ctx, handle, popup), BorderLayout.CENTER);
        bangClient.displayPopup(popup, true);
    }

    public EditPosterView (final BangContext ctx, Handle handle,
                           final BWindow popup)
    {
        super();
        _ctx = ctx;

        GroupLayout layout = GroupLayout.makeHoriz(GroupLayout.TOP);
        layout.setGap(0);
        setLayoutManager(layout);
        setPreferredSize(new Dimension(950, 710));

        // create a poster view and request the poster data
        _posterView = new EditablePosterView(_ctx);
        _posterView.setHandle(handle);

        _right = new BContainer(new BorderLayout());

        // we need an InventoryPalette that only shows Badges
        Predicate<Item> predicate = new Predicate.InstanceOf<Item>(Badge.class);
        _palette = new PosterPalette(ctx, predicate, 2, 3);
        _palette.setPaintBorder(true);
        _palette.setSelectable(PosterInfo.BADGES);
        _right.add(_palette, BorderLayout.CENTER);

        // and buttons to commit changes or cancel them
        BContainer buttonBox = GroupLayout.makeVBox(GroupLayout.RIGHT);
        final BButton cancelButton = new BButton("Cancel");
        cancelButton.addListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                ctx.getBangClient().clearPopup(popup, false);
            }
        });
        final BButton commitButton = new BButton(
            ctx.xlate(BangCodes.BANG_MSGS, "m.poster_commit"));
        commitButton.addListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                _posterView.storePoster(
                    new PlayerService.ConfirmListener() {
                        public void requestProcessed() {
                            ctx.getBangClient().clearPopup(popup, false);
                        }
                        public void requestFailed(String cause) {
                            log.warning("Poster commit failed: " + cause);
                            ctx.getChatDirector().displayFeedback(
                                BangCodes.BANG_MSGS, "m.poster_commit_failed");
                        }
                    });
            }
        });
        buttonBox.add(cancelButton);
        buttonBox.add(commitButton);
        _right.add(buttonBox, BorderLayout.SOUTH);
    }

    /**
     * Called from the {@link EditablePosterView} when the poster
     * request finishes and we can finalize the UI.
     */
    protected void posterIsReady()
    {
        // respond to new poster data
        add(_posterView, GroupLayout.FIXED);
        add(_right);

        // add the inspector after the palette is initialized
        _palette.setInspector(new Inspector() {
            public void iconUpdated(SelectableIcon icon, boolean selected) {
                PosterInfo poster = _posterView._poster;

                // make sure the poster view is ready
                if (poster == null) {
                    BangUI.play(BangUI.FeedbackSound.INVALID_ACTION);
                    return;
                }

                Badge badge = (Badge) ((ItemIcon) icon).getItem();
                int badgeCode = badge.getType().code();

                for (int i = 0; i < PosterInfo.BADGES; i ++) {
                    if (poster.badgeIds[i] == badgeCode && !selected) {
                        poster.badgeIds[i] = -1;
                    } else if (poster.badgeIds[i] == -1 && selected) {
                        poster.badgeIds[i] = badgeCode;
                    } else {
                        continue;
                    }
                    _posterView.buildPoster();
                    return;
                }
                // this should not happen
                BangUI.play(BangUI.FeedbackSound.INVALID_ACTION);
                return;
            }
        });
    }

    /**
     * Extends the {@InventoryPalette} with logic to initialize icons
     * to be selected if they're on the player's current poster.
     */
    protected class PosterPalette extends InventoryPalette
    {
        public PosterPalette (BangContext ctx, Predicate<Item> itemp,
                              int columns, int rows)
        {
            super(ctx, itemp, columns, rows);
        }

        @Override // from IconPalette
        public void addIcon (SelectableIcon icon)
        {
            super.addIcon(icon);

            PosterInfo poster = _posterView._poster;
            if (poster == null) {
                // poster is still being requested
                return;
            }
            int code = ((Badge) ((ItemIcon) icon).getItem()).getType().code();
            icon.setSelected(
                code == poster.badgeIds[0] || code == poster.badgeIds[1] ||
                code == poster.badgeIds[2] || code == poster.badgeIds[3]);
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
            if (_palette != null) {
                _palette.invalidate();
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
            if (_viewMode) {
                BComponent label = super.buildStatementView();
                label.addListener(new MouseAdapter() {
                    @Override // from MouseAdapter
                    public void mousePressed (MouseEvent event) {
                        _viewMode = false;
                        buildPoster();
                    }
                });
                return label;
            }
            BContainer statementBox = GroupLayout.makeHBox(GroupLayout.TOP);

            _textField = new BTextField(
                    _poster.statement, BangUI.TEXT_FIELD_MAX_LENGTH);
            _textField.setPreferredWidth(200);
            statementBox.add(_textField);

            final BButton updateButton = new BButton(
                _ctx.xlate(BangCodes.BANG_MSGS, "m.poster_update"));
            updateButton.addListener(
                new ActionListener() {
                    public void actionPerformed (ActionEvent event) {
                        if (event.getSource() == updateButton) {
                            _poster.statement = _textField.getText();
                            _viewMode = true;
                            buildPoster();
                        }
                    }
                });
            statementBox.add(updateButton);
            return statementBox;
        }

        protected EditablePosterView(BangContext ctx)
        {
            super(ctx);
        }

        /**
         * Commits our changes with a server request.
         */
        protected void storePoster(PlayerService.ConfirmListener listener)
        {
            PlayerService psvc = (PlayerService)
                _ctx.getClient().requireService(PlayerService.class);
            psvc.updatePosterInfo(
                _ctx.getClient(), _ctx.getUserObject().playerId,
                _poster.statement, _poster.badgeIds,
                listener);        
        }

        /** Determine if we're viewing or editing the statement. */
        protected boolean _viewMode;
        protected BTextField _textField;
    }

    protected BangContext _ctx;

    protected EditablePosterView _posterView;
    protected PosterPalette _palette;
    protected BContainer _right;
}
