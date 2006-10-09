//
// $Id$

package com.threerings.bang.saloon.client;

import java.util.HashMap;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.Spacer;
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.SaloonObject;

import static com.threerings.bang.Log.log;

/**
 * Displays a list of back parlors and allows a player to enter those parlors
 * or create a new parlor.
 */
public class ParlorList extends BContainer
    implements ActionListener, SetListener
{
    public ParlorList (BangContext ctx)
    {
        super(new BorderLayout(10, 10));
        _ctx = ctx;

        _list = new BContainer(new TableLayout(5, 10, 5));
        _list.setStyleClass("parlor_list");
        ((TableLayout)_list.getLayoutManager()).setHorizontalAlignment(
            TableLayout.STRETCH);
        add(new BScrollPane(_list), BorderLayout.CENTER);

        BContainer buttons = GroupLayout.makeHBox(GroupLayout.RIGHT);
        String label = ctx.xlate(SaloonCodes.SALOON_MSGS, "m.create_parlor");
        buttons.add(_enterParlor = new BButton(label, this, "create"));
        buttons.add(new Spacer(20, 5));
        add(buttons, BorderLayout.SOUTH);
    }

    /**
     * Called by the {@link SaloonView} when we get our saloon object.
     */
    public void willEnterPlace (SaloonObject salobj)
    {
        _salobj = salobj;
        _salobj.addListener(this);

        // add info on the existing parlors (TODO: sort sensibly)
        for (ParlorInfo info : _salobj.parlors) {
            _rows.put(info.creator, new ParlorRow(info));
            if (_ctx.getUserObject().handle.equals(info.creator)) {
                updateEnterButton(info);
            }
        }
    }

    /**
     * Called by the {@link SaloonView} when the saloon is dismissed.
     */
    public void didLeavePlace ()
    {
        if (_salobj != null) {
            _salobj.removeListener(this);
            _salobj = null;
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("create".equals(event.getAction())) {
            _ctx.getBangClient().displayPopup(
                new CreateParlorDialog(_ctx, _salobj), true);

        } else if ("enter".equals(event.getAction())) {
            final ParlorInfo info = (ParlorInfo)
                ((BButton)event.getSource()).getProperty("info");
            if (!_ctx.getUserObject().tokens.isAdmin() &&
                !_ctx.getUserObject().handle.equals(info.creator) &&
                info.passwordProtected) {
                // ask for a password, then join
                OptionDialog.ResponseReceiver rr =
                    new OptionDialog.ResponseReceiver() {
                    public void resultPosted (int button, Object result) {
                        if (button == 0) {
                            joinParlor(info.creator, ((String)result).trim());
                        }
                    }
                };
                OptionDialog.showStringDialog(
                    _ctx, SaloonCodes.SALOON_MSGS, "m.enter_pass",
                    new String[] { "m.enter", "m.cancel" }, 100, "", rr);
            } else {
                joinParlor(info.creator, null);
            }
        }
    }

    // documentation inherited from interface SetListener
    public void entryAdded (EntryAddedEvent event)
    {
        if (SaloonObject.PARLORS.equals(event.getName())) {
            ParlorInfo info = (ParlorInfo)event.getEntry();
            // TODO: insert at appropriate spot
            _rows.put(info.creator, new ParlorRow(info));
            if(_ctx.getUserObject().handle.equals(info.creator)) {
                updateEnterButton(info);
            }
        }
    }

    // documentation inherited from interface SetListener
    public void entryUpdated (EntryUpdatedEvent event)
    {
        if (SaloonObject.PARLORS.equals(event.getName())) {
            ParlorInfo info = (ParlorInfo)event.getEntry();
            ParlorRow row = _rows.get(info.creator);
            if (row == null) {
                log.warning("No row for updated parlor " + info + ".");
            } else {
                row.update(info);
            }
            if(_ctx.getUserObject().handle.equals(info.creator)) {
                updateEnterButton(info);
            }
        }
    }

    // documentation inherited from interface SetListener
    public void entryRemoved (EntryRemovedEvent event)
    {
        if (SaloonObject.PARLORS.equals(event.getName())) {
            Handle handle = (Handle)event.getKey();
            ParlorRow row = _rows.remove(handle);
            if (row == null) {
                log.warning("No row for removed parlor " +
                            "[key=" + event.getKey() + "].");
            } else {
                row.clear();
            }
            if(_ctx.getUserObject().handle.equals(handle)) {
                updateEnterButton(null);
            }
        }
    }

    protected void updateEnterButton (ParlorInfo info)
    {
        if (info == null) {
            _enterParlor.setText(_ctx.xlate(
                        SaloonCodes.SALOON_MSGS, "m.create_parlor"));
            _enterParlor.setAction("create");
        } else {
            _enterParlor.setText(_ctx.xlate(
                        SaloonCodes.SALOON_MSGS, "m.to_parlor"));
            _enterParlor.setAction("enter");
            _enterParlor.setProperty("info", info);
        }
    }

    protected void joinParlor (Handle owner, String password)
    {
        _salobj.service.joinParlor(_ctx.getClient(), owner, password,
                                   new SaloonService.ResultListener() {
            public void requestProcessed (Object result) {
                _ctx.getLocationDirector().moveTo((Integer)result);
            }
            public void requestFailed (String cause) {
                _ctx.getChatDirector().displayFeedback(
                    SaloonCodes.SALOON_MSGS,
                    MessageBundle.compose("m.enter_parlor_failed", cause));
            }
        });
    }

    protected class ParlorRow
    {
        public ParlorRow (ParlorInfo info) {
            MessageBundle msgs = _ctx.getMessageManager().getBundle(
                SaloonCodes.SALOON_MSGS);
            String lbl = msgs.get("m.parlor_name", info.creator);
            _name = new BLabel(lbl, "parlor_label");
            _pards = new BLabel("");
            _lock = new BLabel("");
            _occs = new BLabel("");
            _enter = new BButton(msgs.get("m.enter"), ParlorList.this, "enter");
            _enter.setStyleClass("alt_button");
            update(info);
        }

        public void update (ParlorInfo info) {
            _occs.setText(String.valueOf(info.occupants));
            _pards.setIcon(info.pardnersOnly ?
                           new ImageIcon(_ctx.loadImage(PARDS_PATH)) :
                           new BlankIcon(16, 16));
            _lock.setIcon(info.passwordProtected ?
                          new ImageIcon(_ctx.loadImage(LOCKED_PATH)) :
                          new BlankIcon(16, 16));
            _enter.setProperty("info", info);

            // if we're the creator, or the creator is our pardner, always show
            // this parlor (regardless of whether it is empty)
            if (_ctx.getUserObject().handle.equals(info.creator) ||
                _ctx.getUserObject().pardners.containsKey(info.creator)) {
                add();

            } else if (info.pardnersOnly || info.occupants == 0) {
                // if it's pardners only or empty, don't show it
                clear();

            } else {
                add();
            }
        }

        public void add () {
            if (_name.getParent() == null) {
                _list.add(_name);
                _list.add(_pards);
                _list.add(_lock);
                _list.add(_occs);
                _list.add(_enter);
            }
        }

        public void clear () {
            if (_name.getParent() != null) {
                _list.remove(_name);
                _list.remove(_pards);
                _list.remove(_lock);
                _list.remove(_occs);
                _list.remove(_enter);
            }
        }

        protected BLabel _name, _occs, _pards, _lock;
        protected BButton _enter;
    }

    protected BangContext _ctx;
    protected SaloonObject _salobj;
    protected BContainer _list;
    protected BButton _enterParlor;

    protected HashMap<Handle,ParlorRow> _rows = new HashMap<Handle,ParlorRow>();

    protected static final String PARDS_PATH = "ui/saloon/pardners_only.png";
    protected static final String LOCKED_PATH = "ui/saloon/locked.png";
}
