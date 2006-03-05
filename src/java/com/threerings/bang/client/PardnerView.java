//
// $Id$

package com.threerings.bang.client;

import java.awt.Image;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import java.text.SimpleDateFormat;

import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
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

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.bang.avatar.client.AvatarView;
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

        // insert our controls between the palette and the buttons
        GroupLayout layout = GroupLayout.makeVert(GroupLayout.NONE,
            GroupLayout.BOTTOM, GroupLayout.STRETCH);
        layout.setGap(0);
        BContainer ccont = new BContainer(layout);
        ccont.add(_status = new StatusLabel(_ctx));
        _status.setStyleClass("pardner_status");
        ccont.add(new Spacer(1, 2));
        BContainer bcont = new BContainer(GroupLayout.makeHoriz(
            GroupLayout.CENTER));
        bcont.add(_chat = new BButton(_ctx.xlate(BANG_MSGS, "m.pardner_chat"),
            this, "chat"));
        bcont.add(_remove = new BButton(_ctx.xlate(BANG_MSGS,
            "m.pardner_remove"), this, "remove"));
        ccont.add(bcont);
        ccont.add(new Spacer(1, 15));
        layout = GroupLayout.makeHoriz(GroupLayout.CENTER);
        layout.setGap(10);
        bcont = new BContainer(layout);
        bcont.add(new BLabel(_ctx.xlate(BANG_MSGS, "m.pardner_add")));
        bcont.add(_name = new BTextField());
        _name.setPreferredWidth(324);
        bcont.add(_submit = new BButton(_ctx.xlate(BANG_MSGS,
            "m.pardner_submit"), this, "submit"));
        ccont.add(bcont);
        ccont.add(new Spacer(1, 15));
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
                icon.entry.handle);

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

        } else { // src == _name || src == _submit
            if (_submit.isEnabled()) {
                invitePardner(new Handle(_name.getText()));
            }
        }
    }

    // documentation inherited from interface SetListener
    public void entryAdded (EntryAddedEvent eae)
    {
        new PardnerIcon((PardnerEntry)eae.getEntry()).insert();
    }

    // documentation inherited from interface SetListener
    public void entryRemoved (EntryRemovedEvent ere)
    {
        _picons.get(ere.getKey()).remove();
    }

    // documentation inherited from interface SetListener
    public void entryUpdated (EntryUpdatedEvent eue)
    {
        PardnerEntry entry = (PardnerEntry)eue.getEntry();
        _picons.get(entry.getKey()).update(entry);
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // clear out and refresh our list of pardners
        clear();
        _picons.clear();
        PlayerObject user = _ctx.getUserObject();
        for (Iterator it = user.pardners.iterator(); it.hasNext(); ) {
            new PardnerIcon((PardnerEntry)it.next()).insert();
        }

        // these start out as disabled/empty
        _chat.setEnabled(false);
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
        _chat.setEnabled(((PardnerIcon)icon).entry.isAvailable());
        _remove.setEnabled(true);
    }

    @Override // documentation inherited
    protected void iconDeselected (SelectableIcon icon)
    {
        super.iconDeselected(icon);
        _chat.setEnabled(false);
        _remove.setEnabled(false);
    }

    /**
     * Attempts to invite the named player as a pardner.
     */
    protected void invitePardner (final Name handle)
    {
        _submit.setEnabled(false);
        _name.setEnabled(false);
        _psvc.invitePardner(_ctx.getClient(), handle,
            new InvocationService.ConfirmListener() {
                public void requestProcessed () {
                    _status.setStatus(BANG_MSGS, MessageBundle.tcompose(
                        "m.pardner_invited", handle), false);
                    _name.setText("");
                    _name.setEnabled(true);
                }
                public void requestFailed (String cause) {
                    _status.setStatus(BANG_MSGS, cause, true);
                    _submit.setEnabled(true);
                    _name.setEnabled(true);
                }
            });
    }

    /**
     * Requests that the named pardner be removed after having verified that
     * that's what the user really wants.
     */
    protected void removePardner (final Name handle)
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

            updateAvatar();
            updateStatus();
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

        public Dimension getPreferredSize (int whint, int hhint)
        {
            return ICON_SIZE;
        }

        @Override // documentation inherited
        protected void layout ()
        {
            super.layout();
            _label.layout(new Insets(25, 5, 25, 31));
        }

        // documentation inherited
        protected void renderComponent (Renderer renderer)
        {
            super.renderComponent(renderer);
            if (_location != null) {
                _location.render(renderer, 6, 67);
            }
            _scroll.render(renderer, 8, 8);
            _handle.render(renderer, (_width - _handle.getSize().width) / 2,
                _last == null ? 16 : 24);
            if (_last != null) {
                _last.render(renderer, (_width - _last.getSize().width) / 2,
                    12);
            }
        }

        protected void updateAvatar ()
        {
            if (entry.avatar == null) {
                setIcon(new ImageIcon(
                    _ctx.loadImage("ui/pardners/silhouette.png")));
            } else {
                int w = AVATAR_SIZE.width, h = AVATAR_SIZE.height;
                setIcon(new ImageIcon(AvatarView.getImage(
                                          _ctx, entry.avatar, w, h)));
            }
        }

        protected void updateStatus ()
        {
            // update the location icon
            if (entry.status == PardnerEntry.IN_GAME ||
                entry.status == PardnerEntry.IN_SALOON) {
                _location = new ImageIcon(_ctx.loadImage(
                    "ui/pardners/in_" + (entry.status == PardnerEntry.IN_GAME ?
                        "game" : "saloon") + ".png"));

            } else {
                _location = null;
            }

            // and the scroll icon
            _scroll = new ImageIcon(_ctx.loadImage(
                "ui/frames/" + (entry.status == PardnerEntry.OFFLINE ?
                    "taller" : "smaller") + "_scroll.png"));

            // and the last session date
            if (entry.status == PardnerEntry.OFFLINE) {
                String msg = _ctx.xlate(BANG_MSGS, MessageBundle.tcompose(
                    "m.pardner_last_session", LAST_SESSION_FORMAT.format(
                        entry.getLastSession())));
                _last = _ctx.getStyleSheet().getTextFactory(this,
                    "last_session").createText(msg,
                        _ctx.getStyleSheet().getColor(this, "last_session"));

            } else {
                _last = null;
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
    protected BButton _chat, _remove, _submit;
    protected BTextField _name;
    protected StatusLabel _status;

    protected HashMap<Comparable, PardnerIcon> _picons =
        new HashMap<Comparable, PardnerIcon>();

    protected static final Dimension ICON_SIZE = new Dimension(167, 186);
    protected static final Dimension AVATAR_SIZE = new Dimension(117, 150);

    protected static final SimpleDateFormat LAST_SESSION_FORMAT =
        new SimpleDateFormat("M/d/yy");
}
