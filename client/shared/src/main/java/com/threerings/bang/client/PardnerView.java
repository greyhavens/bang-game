//
// $Id$

package com.threerings.bang.client;

import java.util.Map;

import java.text.SimpleDateFormat;

import com.google.common.collect.Maps;

import com.jme.renderer.Renderer;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.text.BText;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Insets;

import com.samskivert.util.ResultListener;
import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.client.bui.EnablingValidator;
import com.threerings.bang.client.bui.IconPalette;
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
    implements ActionListener, BangCodes
{
    public PardnerView (BangContext ctx)
    {
        this(ctx, null);
    }

    public PardnerView (BangContext ctx, StatusLabel status)
    {
        super(null, 4, 2, ICON_SIZE, 1);
        setStyleClass("pardner_view");
        setSelectable(0);
        _ctx = ctx;

        // add a status view below the pardner grid
        GroupLayout layout = GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.BOTTOM, GroupLayout.STRETCH);
        layout.setGap(0);
        BContainer ccont = new BContainer(layout);
        if (status == null) {
            ccont.add(_status = new StatusLabel(_ctx));
            _status.setStyleClass("pardner_status");
            ccont.add(new Spacer(1, 2));
        } else {
            _status = status;
        }

        // then controls for adding a new pardner
        layout = GroupLayout.makeHoriz(GroupLayout.CENTER);
        layout.setGap(10);
        BContainer acont = new BContainer(layout);
        PlayerObject user = ctx.getUserObject();
        if (user.tokens.isAnonymous() || !user.hasCharacter()) {
            acont.add(new BLabel(_ctx.xlate(BANG_MSGS, "m.pardner_anonymous")));
        } else {
            acont.add(new BLabel(_ctx.xlate(BANG_MSGS, "m.pardner_add")));
            acont.add(_name = new BTextField(BangUI.TEXT_FIELD_MAX_LENGTH));
            _name.setPreferredWidth(324);
            _submit = new BButton(_ctx.xlate(BANG_MSGS, "m.pardner_submit"), this, "submit");
            acont.add(_submit);

            // disable submit until a name is entered
            new EnablingValidator(_name, _submit);
        }
        ccont.add(acont);
        ccont.add(new Spacer(1, 12));
        add(ccont, BorderLayout.CENTER);

    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent ae)
    {
        Object src = ae.getSource();
        if (src == _name || src == _submit) {
            if (_submit.isEnabled()) {
                _ctx.getBangClient().displayPopup(
                    new InvitePardnerDialog(_ctx, _status, new Handle(_name.getText())), true, 400);
                _name.setText("");
            }
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // clear out and refresh our list of pardners
        clear();
        _picons.clear();
        for (PardnerEntry entry : _ctx.getUserObject().pardners) {
            new PardnerIcon(entry).insert();
        }

        // these start out as disabled/empty
        if (_submit != null) {
            _submit.setEnabled(false);
            _name.setText("");
        }

        // register as a listener for changes to the pardner list
        _ctx.getUserObject().addListener(_plist);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _ctx.getUserObject().removeListener(_plist);
    }

    /** Displays a single pardner. */
    protected class PardnerIcon extends SelectableIcon
    {
        PardnerEntry entry;

        public PardnerIcon (PardnerEntry entry)
        {
            this.entry = entry;
            setStyleClass("pardner_icon");
            _handle = _ctx.getStyleSheet().getTextFactory(this, null).createText(
                entry.handle.toString(), _ctx.getStyleSheet().getColor(this, null));
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
            if (oentry.avatar == null || !oentry.avatar.equals(nentry.avatar)) {
                updateAvatar();
            }
        }

        @Override // from BComponent
        public boolean dispatchEvent (BEvent event)
        {
            // pop up a player menu if they click the mouse on a pardner
            boolean handled = false;
            if (_handle != null) {
                handled = PlayerPopupMenu.checkPopup(_ctx, getWindow(), event, entry.handle, false);
            }
            return handled || super.dispatchEvent(event);
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

            // the not online icon is a different size from an actual avatar icon so we have to
            // adjust the insets
            int offtop = (entry.avatar == null) ? 20 : 5;
            _label.layout(new Insets(25, offtop, 25, 31), getWidth(), getHeight());
        }

        @Override // documentation inherited
        protected void renderComponent (Renderer renderer)
        {
            super.renderComponent(renderer);
            if (_location != null) {
                _location.render(renderer, 6, 67, _alpha);
            }
            _scroll.render(renderer, 8, 8, _alpha);
            Dimension size = _handle.getSize();
            int w = size.width, h = size.height;
            if (_width < size.width + 29) {
                h = size.height * (_width - 29) / size.width;
                w = _width - 29;
            }
            _handle.render(renderer, (_width - w) / 2, _last == null ? 16 : 24, w, h, _alpha);
            if (_last != null) {
                _last.render(renderer, (_width - _last.getSize().width) / 2, 12, _alpha);
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

        @Override // documentation inherited
        protected boolean changeCursor ()
        {
            return _hover && _enabled && _visible;
        }

        protected void updateAvatar ()
        {
            // start with the silhouette image
            setIcon(new ImageIcon(_ctx.loadImage("ui/pardners/silhouette.png")));

            // then load our avatar image asynchronously if we have one
            if (entry.avatar != null) {
                int w = AVATAR_SIZE.width, h = AVATAR_SIZE.height;
                AvatarView.getImage(_ctx, entry.avatar, w, h, false, new ResultListener<BImage>() {
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
            String locpath = "ui/pardners/";
            switch (entry.status) {
              case PardnerEntry.IN_GAME:
                locpath += "in_game.png";
                break;
              case PardnerEntry.IN_SALOON:
                locpath += "in_saloon.png";
                break;
              case PardnerEntry.IN_BOUNTY:
                locpath += "in_bounty.png";
                break;
              case PardnerEntry.IN_TUTORIAL:
                locpath += "in_tutorial.png";
                break;
              default:
                locpath = entry.getTownIndex() == -1 ? null :
                    locpath + BangCodes.TOWN_IDS[entry.getTownIndex()] + ".png";
            }
            setLocation(locpath == null ? null : new ImageIcon(_ctx.loadImage(locpath)));

            // and the scroll icon
            String path = "ui/frames/" +
                (entry.status == PardnerEntry.OFFLINE ? "tall_" : "") + "small_scroll0.png";
            setScroll(new ImageIcon(_ctx.loadImage(path)));

            // and the last session date
            BText last = null;
            if (entry.status == PardnerEntry.OFFLINE) {
                String msg = LAST_SESSION_FORMAT.format(entry.getLastSession());
                msg = MessageBundle.tcompose("m.pardner_last_session", msg);
                msg = _ctx.xlate(BANG_MSGS, msg);
                last = _ctx.getStyleSheet().getTextFactory(this, "last_session").createText(
                    msg, _ctx.getStyleSheet().getColor(this, "last_session"));
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

        protected ImageIcon _scroll, _location;
        protected BText _handle, _last;
    }

    protected BangContext _ctx;
    protected BButton _submit;
    protected BTextField _name;
    protected StatusLabel _status;

    /** Listens to the object containing the pardner set. */
    protected SetListener<PardnerEntry> _plist = new SetListener<PardnerEntry>() {
        public void entryAdded (EntryAddedEvent<PardnerEntry> eae) {
            if (PlayerObject.PARDNERS.equals(eae.getName())) {
                new PardnerIcon(eae.getEntry()).insert();
            }
        }
        public void entryRemoved (EntryRemovedEvent<PardnerEntry> ere) {
            if (PlayerObject.PARDNERS.equals(ere.getName())) {
                _picons.get(ere.getKey()).remove();
            }
        }
        public void entryUpdated (EntryUpdatedEvent<PardnerEntry> eue) {
            if (PlayerObject.PARDNERS.equals(eue.getName())) {
                PardnerEntry entry = eue.getEntry();
                PardnerIcon icon = _picons.get(entry.getKey());
                icon.update(entry);
            }
        }
    };

    protected Map<Comparable<?>, PardnerIcon> _picons = Maps.newHashMap();

    protected static final Dimension ICON_SIZE = new Dimension(167, 186);
    protected static final Dimension AVATAR_SIZE = new Dimension(
        AvatarLogic.WIDTH/4, AvatarLogic.HEIGHT/4);

    protected static final SimpleDateFormat LAST_SESSION_FORMAT =
        new SimpleDateFormat("M/d/yy");
}
