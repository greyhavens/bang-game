//
// $Id$

package com.threerings.bang.client;

import java.awt.Image;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PardnerEntry;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

/**
 * Displays a player's pardners.
 */
public class PardnerView extends BContainer
    implements ActionListener, SetListener, BangCodes
{
    public PardnerView (BangContext ctx)
    {
        super(new BorderLayout(5, 5));
        setStyleClass("pardner_view");
        _ctx = ctx;
        _psvc = (PlayerService)ctx.getClient().requireService(
            PlayerService.class);
        
        _ptainer = new BContainer(GroupLayout.makeVert(GroupLayout.NONE,
            GroupLayout.TOP, GroupLayout.STRETCH));
        add(new BScrollPane(_ptainer), BorderLayout.CENTER);
        
        BContainer abox = new BContainer(GroupLayout.makeVert(GroupLayout.NONE,
            GroupLayout.CENTER, GroupLayout.STRETCH));
        abox.add(new BLabel(_ctx.xlate(BANG_MSGS, "m.pardner_add")));
        BContainer nbox = new BContainer(GroupLayout.makeHStretch());
        nbox.add(new BLabel(_ctx.xlate(BANG_MSGS, "m.pardner_name")),
            GroupLayout.FIXED);
        nbox.add(_name = new BTextField());
        nbox.add(_submit = new BButton(_ctx.xlate(BANG_MSGS,
            "m.pardner_submit"), this, "submit"), GroupLayout.FIXED);
        abox.add(nbox);
        abox.add(_status = new StatusLabel(_ctx));
        _status.setStyleClass("pardner_status");
        BComponent spacer = new BContainer();
        spacer.setPreferredSize(new Dimension(1, 30));
        abox.add(spacer);
        
        add(abox, BorderLayout.SOUTH);
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();

        // clear out and refresh our list of pardners
        _ptainer.removeAll();
        _ppanels.clear();
        PlayerObject user = _ctx.getUserObject();
        for (Iterator it = user.pardners.iterator(); it.hasNext(); ) {
            new PardnerPanel((PardnerEntry)it.next()).insert();
        }
        
        // register as a listener for changes to the pardner list
        user.addListener(this);
    }

    @Override // documentation inherited
    public void wasRemoved ()
    {
        super.wasRemoved();
        _ctx.getUserObject().removeListener(this);
    }
    
    // documentation inherited from interface SetListener
    public void entryAdded (EntryAddedEvent eae)
    {
        new PardnerPanel((PardnerEntry)eae.getEntry()).insert();
    }
    
    // documentation inherited from interface SetListener
    public void entryRemoved (EntryRemovedEvent ere)
    {
        _ppanels.get(ere.getKey()).remove();
    }
    
    // documentation inherited from interface SetListener
    public void entryUpdated (EntryUpdatedEvent eue)
    {
        PardnerEntry entry = (PardnerEntry)eue.getEntry();
        _ppanels.get(entry.getKey()).update(entry);
    }
    
    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent ae)
    {
        final Handle handle = new Handle(_name.getText());
        
        _submit.setEnabled(false);
        _psvc.invitePardner(_ctx.getClient(), handle,
            new InvocationService.ConfirmListener() {
                public void requestProcessed () {
                    _status.setStatus(BANG_MSGS, MessageBundle.tcompose(
                        "m.pardner_invited", handle), false);
                    _name.setText("");
                    _submit.setEnabled(true);
                }
                public void requestFailed (String cause) {
                    _status.setStatus(BANG_MSGS, cause, true);
                    _submit.setEnabled(true);
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
    
    /**
     * Displays the status and controls for a single pardner.
     */
    protected class PardnerPanel extends BContainer
        implements ActionListener
    {
        PardnerEntry entry;
        
        public PardnerPanel (PardnerEntry entry)
        {
            super(GroupLayout.makeHoriz(GroupLayout.STRETCH, GroupLayout.LEFT,
                GroupLayout.NONE));
            this.entry = entry;
            
            add(_avhandle = new BLabel(entry.handle.toString()));
            add(_status = new BLabel(""), GroupLayout.FIXED);
            add(_chat = new BButton(_ctx.xlate(BANG_MSGS,
                "m.pardner_chat")), GroupLayout.FIXED);
            _chat.addListener(this);
            add(_remove = new BButton(_ctx.xlate(BANG_MSGS,
                "m.pardner_remove")), GroupLayout.FIXED);
            _remove.addListener(this);
            
            updateAvatar();
            updateStatus();
        }
        
        public void actionPerformed (ActionEvent ae)
        {
            if (ae.getSource() == _chat) {
            
            } else { // ae.getSource() == _remove
                OptionDialog.showConfirmDialog(_ctx, BANG_MSGS,
                    MessageBundle.tcompose("m.confirm_remove", entry.handle),
                    new OptionDialog.DialogResponseReceiver() {
                        public void resultPosted (int button, Object result) {
                            if (button == OptionDialog.OK_BUTTON) {
                                removePardner(entry.handle);
                            }
                        }
                    });
            }
        }
        
        public void insert ()
        {
            addToContainer();
            _ppanels.put(entry.getKey(), this);
        }
        
        public void remove ()
        {
            _ptainer.remove(this);
            _ppanels.remove(entry.getKey());
        }
        
        public void update (PardnerEntry nentry)
        {
            PardnerEntry oentry = entry;
            entry = nentry;
            if (oentry.status != nentry.status) {
                updateStatus();
                _ptainer.remove(this);
                addToContainer();
            }
            if (!Arrays.equals(oentry.avatar, nentry.avatar)) {
                updateAvatar();
            }
        }
        
        protected void updateAvatar ()
        {
            if (entry.avatar == null) {
                _avhandle.setIcon(null);
                return;
            }
            Image image = AvatarView.getImage(_ctx,
                entry.avatar).getScaledInstance(AVATAR_WIDTH, AVATAR_HEIGHT,
                Image.SCALE_SMOOTH);
            _avhandle.setIcon(new ImageIcon(image));
        }
        
        protected void updateStatus ()
        {
            _status.setText(_ctx.xlate(BANG_MSGS,
                "m.pardner_status." + entry.status));
            _chat.setEnabled(entry.isAvailable());
        }
        
        protected void addToContainer ()
        {
            // insert according to order defined by PardnerEntry.compareTo
            for (int ii = 0, nn = _ptainer.getComponentCount(); ii < nn;
                    ii++) {
                PardnerPanel opanel = (PardnerPanel)_ptainer.getComponent(ii);
                if (entry.compareTo(opanel.entry) < 0) {
                    _ptainer.add(ii, this);
                    return;
                }
            }
            _ptainer.add(this);
        }
     
        BLabel _avhandle, _status;
        BButton _chat, _remove;
    }
    
    protected BangContext _ctx;
    protected PlayerService _psvc;
    protected BContainer _ptainer;
    protected BTextField _name;
    protected BButton _submit;
    protected StatusLabel _status;
    
    protected HashMap<Comparable, PardnerPanel> _ppanels =
        new HashMap<Comparable, PardnerPanel>();
    
    protected static final int AVATAR_WIDTH = 117;
        
    protected static final int AVATAR_HEIGHT = 150;
}
