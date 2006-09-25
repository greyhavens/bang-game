//
// $Id$

package com.threerings.bang.client;

import java.awt.Image;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import java.text.SimpleDateFormat;

import com.jme.renderer.Renderer;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.BTextField;
import com.jmex.bui.Label;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.text.BText;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Insets;

import com.samskivert.util.ResultListener;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;
import com.threerings.util.MessageBundle;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.client.bui.EnablingValidator;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PardnerEntry;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

/**
 * Displays a player's pardners.
 */
public class PardnerView extends IconPalette
    implements ActionListener, SetListener, BangCodes
{
    public PardnerView (BangContext ctx)
    {
        super(null, 4, 2, ICON_SIZE, 1);
        setStyleClass("pardner_view");
        _ctx = ctx;
        _psvc = (PlayerService)ctx.getClient().requireService(
            PlayerService.class);

        // add a status view below the pardner grid
        GroupLayout layout = GroupLayout.makeVert(GroupLayout.NONE,
            GroupLayout.BOTTOM, GroupLayout.STRETCH);
        layout.setGap(0);
        BContainer ccont = new BContainer(layout);
        ccont.add(_status = new StatusLabel(_ctx));
        _status.setStyleClass("pardner_status");
        ccont.add(new Spacer(1, 2));

        // then add chat and remove buttons
        BContainer bcont = new BContainer(GroupLayout.makeHoriz(
            GroupLayout.CENTER));
        bcont.add(_chat = new BButton(_ctx.xlate(BANG_MSGS, "m.pardner_chat"),
            this, "chat"));
        bcont.add(_watch = new BButton(_ctx.xlate(BANG_MSGS,
            "m.pardner_watch"), this, "watch"));
        bcont.add(_remove = new BButton(_ctx.xlate(BANG_MSGS,
            "m.pardner_remove"), this, "remove"));
        ccont.add(bcont);
        ccont.add(new Spacer(1, 13));

        // then controls for adding a new pardner
        layout = GroupLayout.makeHoriz(GroupLayout.CENTER);
        layout.setGap(10);
        bcont = new BContainer(layout);
        bcont.add(new BLabel(_ctx.xlate(BANG_MSGS, "m.pardner_add")));
        bcont.add(_name = new BTextField());
        _name.setPreferredWidth(324);
        bcont.add(_submit = new BButton(_ctx.xlate(BANG_MSGS,
            "m.pardner_submit"), this, "submit"));
        ccont.add(bcont);
        ccont.add(new Spacer(1, 12));
        add(ccont, BorderLayout.CENTER);

        // disable submit until a name is entered
        new EnablingValidator(_name, _submit);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent ae)
    {
        Object src = ae.getSource();
        if (src == _chat) {
            PardnerIcon icon = (PardnerIcon)getSelectedIcon();
            _ctx.getBangClient().getPardnerChatView().display(
                icon.entry.handle, true);

        } else if (src == _remove) {
            final PardnerIcon icon = (PardnerIcon)getSelectedIcon();
            OptionDialog.showConfirmDialog(_ctx, BANG_MSGS,
                MessageBundle.tcompose("m.confirm_remove", icon.entry.handle),
                new OptionDialog.ResponseReceiver() {
                    public void resultPosted (int button, Object result) {
                        if (button == OptionDialog.OK_BUTTON) {
                            removePardner(icon.entry.handle);
                        }
                    }
                });

        } else if (src == _watch) {
            PardnerIcon icon = (PardnerIcon)getSelectedIcon();
            if (icon.entry.gameOid > 0) {
                _ctx.getLocationDirector().moveTo(icon.entry.gameOid);
            }

        } else { // src == _name || src == _submit
            if (_submit.isEnabled()) {
                InvitePardnerDialog popup = new InvitePardnerDialog(
                    _ctx, new Handle(_name.getText()));
                _ctx.getBangClient().displayPopup(popup, true, 400);
                _name.setText("");
            }
        }
    }

    // documentation inherited from interface SetListener
    public void entryAdded (EntryAddedEvent eae)
    {
        if (PlayerObject.PARDNERS.equals(eae.getName())) {
            new PardnerIcon((PardnerEntry)eae.getEntry()).insert();
        }
    }

    // documentation inherited from interface SetListener
    public void entryRemoved (EntryRemovedEvent ere)
    {
        if (PlayerObject.PARDNERS.equals(ere.getName())) {
            _picons.get(ere.getKey()).remove();
        }
    }

    // documentation inherited from interface SetListener
    public void entryUpdated (EntryUpdatedEvent eue)
    {
        if (PlayerObject.PARDNERS.equals(eue.getName())) {
            PardnerEntry entry = (PardnerEntry)eue.getEntry();
            _picons.get(entry.getKey()).update(entry);
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // clear out and refresh our list of pardners
        clear();
        _picons.clear();
        PlayerObject user = _ctx.getUserObject();
        for (PardnerEntry entry : user.pardners) {
            new PardnerIcon(entry).insert();
        }

        // these start out as disabled/empty
        _chat.setEnabled(false);
        _watch.setEnabled(false);
        _remove.setEnabled(false);
        _submit.setEnabled(false);
        _name.setText("");

        // register as a listener for changes to the pardner list
        user.addListener(this);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _ctx.getUserObject().removeListener(this);
    }

    @Override // documentation inherited
    protected void iconSelected (SelectableIcon icon)
    {
        super.iconSelected(icon);
        PardnerEntry entry = ((PardnerIcon)icon).entry;
        _chat.setEnabled(entry.isAvailable());
        _watch.setEnabled(entry.gameOid > 0);
        _remove.setEnabled(true);
    }

    @Override // documentation inherited
    protected void iconDeselected (SelectableIcon icon)
    {
        super.iconDeselected(icon);
        _chat.setEnabled(false);
        _watch.setEnabled(false);
        _remove.setEnabled(false);
    }

    /**
     * Requests that the named pardner be removed after having verified that
     * that's what the user really wants.
     */
    protected void removePardner (final Handle handle)
    {
        _psvc.removePardner(_ctx.getClient(), handle,
            new PlayerService.ConfirmListener() {
                public void requestProcessed () {
                    _status.setStatus(BANG_MSGS, MessageBundle.tcompose(
                        "m.pardner_removed", handle), false);
                }
                public void requestFailed (String cause) {
                    _status.setStatus(BANG_MSGS, cause, true);
                }
            });
    }

    /** Displays a single pardner. */
    protected class PardnerIcon extends SelectableIcon
    {
        PardnerEntry entry;

        public PardnerIcon (PardnerEntry entry)
        {
            this.entry = entry;
            setStyleClass("pardner_icon");

            _handle = _ctx.getStyleSheet().getTextFactory(this,
                null).createText(entry.handle.toString(),
                    _ctx.getStyleSheet().getColor(this, null));
        }

        public void insert ()
        {
            addToPalette();
            _picons.put(entry.getKey(), this);
        }

        public void remove ()
        {
            removeIcon(this);
            _picons.remove(entry.getKey());
        }

        public void update (PardnerEntry nentry)
        {
            PardnerEntry oentry = entry;
            entry = nentry;
            if (oentry.status != nentry.status) {
                updateStatus();
                removeIcon(this);
                addToPalette();
            }
            if (!Arrays.equals(oentry.avatar, nentry.avatar)) {
                updateAvatar();
            }
        }

        @Override // documentation inherited
        protected Dimension computePreferredSize (int whint, int hhint)
        {
            return ICON_SIZE;
        }

        @Override // documentation inherited
        protected void layout ()
        {
            super.layout();

            // the not online icon is a different size from an actual avatar
            // icon so we have to adjust the insets
            int offtop = (entry.avatar == null) ? 20 : 5;
            _label.layout(new Insets(25, offtop, 25, 31));
        }

        @Override // documentation inherited
        protected void renderComponent (Renderer renderer)
        {
            super.renderComponent(renderer);
            if (_location != null) {
                _location.render(renderer, 6, 67, _alpha);
            }
            _scroll.render(renderer, 8, 8, _alpha);
            _handle.render(renderer, (_width - _handle.getSize().width) / 2,
                _last == null ? 16 : 24, _alpha);
            if (_last != null) {
                _last.render(renderer, (_width - _last.getSize().width) / 2,
                    12, _alpha);
            }
        }

        @Override // documentation inherited
        protected void wasAdded ()
        {
            super.wasAdded();

            // set up our imagery
            _handle.wasAdded();
            updateAvatar();
            updateStatus();
        }

        @Override // documentation inherited
        protected void wasRemoved ()
        {
            super.wasRemoved();

            // clean up our imagery
            _handle.wasRemoved();
            setLocation(null);
            setLastSeen(null);
            setScroll(null);
        }

        protected void updateAvatar ()
        {
            // start with the silhouette image
            setIcon(new ImageIcon(
                        _ctx.loadImage("ui/pardners/silhouette.png")));

            // then load our avatar image asynchronously if we have one
            if (entry.avatar != null) {
                int w = AVATAR_SIZE.width, h = AVATAR_SIZE.height;
                AvatarView.getImage(_ctx, entry.avatar, w, h, false,
                                    new ResultListener<BImage>() {
                    public void requestCompleted (BImage image) {
                        setIcon(new ImageIcon(image));
                    }
                    public void requestFailed (Exception cause) {
                        // not called
                    }
                });
            }
        }

        protected void updateStatus ()
        {
            // update the location icon
            ImageIcon location = null;
            if (entry.status == PardnerEntry.IN_GAME ||
                entry.status == PardnerEntry.IN_SALOON) {
                String path = "ui/pardners/in_" +
                    (entry.status == PardnerEntry.IN_GAME ? "game" : "saloon") +
                    ".png";
                location = new ImageIcon(_ctx.loadImage(path));
            }
            setLocation(location);

            // and the scroll icon
            String path = "ui/frames/" + (entry.status == PardnerEntry.OFFLINE ?
                                          "tall_" : "") + "small_scroll.png";
            setScroll(new ImageIcon(_ctx.loadImage(path)));

            // and the last session date
            BText last = null;
            if (entry.status == PardnerEntry.OFFLINE) {
                String msg = LAST_SESSION_FORMAT.format(entry.getLastSession());
                msg = MessageBundle.tcompose("m.pardner_last_session", msg);
                msg = _ctx.xlate(BANG_MSGS, msg);
                last = _ctx.getStyleSheet().getTextFactory(
                    this, "last_session").createText(
                        msg, _ctx.getStyleSheet().getColor(
                            this, "last_session"));
            }
            setLastSeen(last);
        }

        protected void setLocation (ImageIcon icon)
        {
            if (_location != null) {
                _location.wasRemoved();
            }
            _location = icon;
            if (_location != null) {
                _location.wasAdded();
            }
        }

        protected void setLastSeen (BText text)
        {
            if (_last != null) {
                _last.wasRemoved();
            }
            _last = text;
            if (_last != null) {
                _last.wasAdded();
            }
        }

        protected void setScroll (ImageIcon icon)
        {
            if (_scroll != null) {
                _scroll.wasRemoved();
            }
            _scroll = icon;
            if (_scroll != null) {
                _scroll.wasAdded();
            }
        }

        protected void addToPalette ()
        {
            // insert according to order defined by PardnerEntry.compareTo
            for (int ii = 0, nn = getIconCount(); ii < nn; ii++) {
                PardnerIcon oicon = (PardnerIcon)PardnerView.this.getIcon(ii);
                if (entry.compareTo(oicon.entry) < 0) {
                    addIcon(ii, this);
                    return;
                }
            }
            addIcon(this);
        }

        protected BIcon _scroll, _location;
        protected BText _handle, _last;
    }

    protected BangContext _ctx;
    protected PlayerService _psvc;
    protected BButton _chat, _remove, _submit, _watch;
    protected BTextField _name;
    protected StatusLabel _status;

    protected HashMap<Comparable, PardnerIcon> _picons =
        new HashMap<Comparable, PardnerIcon>();

    protected static final Dimension ICON_SIZE = new Dimension(167, 186);
    protected static final Dimension AVATAR_SIZE = new Dimension(
        AvatarLogic.WIDTH/4, AvatarLogic.HEIGHT/4);

    protected static final SimpleDateFormat LAST_SESSION_FORMAT =
        new SimpleDateFormat("M/d/yy");
}
