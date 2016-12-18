//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.ComparableArrayList;
import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.client.NeedPremiumView;
import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.DeploymentConfig;

import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.SaloonObject;

/**
 * Displays a list of back parlors and allows a player to enter those parlors or create a new
 * parlor.
 */
public class ParlorList extends BContainer
    implements ActionListener, SetListener<ParlorInfo>
{
    public ParlorList (BangContext ctx)
    {
        super(new BorderLayout(10, 10));
        _ctx = ctx;

        _list = new BContainer(new TableLayout(4, 10, 5));
        _list.setStyleClass("parlor_list");
        ((TableLayout)_list.getLayoutManager()).setHorizontalAlignment(TableLayout.STRETCH);
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

        // add info on the existing parlors
        for (ParlorInfo info : _salobj.parlors) {
            maybeAddInfo(info);
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
            if (DeploymentConfig.usesOneTime() && !_ctx.getUserObject().holdsOneTime()) {
                NeedPremiumView.maybeShowNeedPremium(_ctx, BangCodes.E_LACK_ONETIME);
            } else {
                _ctx.getBangClient().displayPopup(new CreateParlorDialog(_ctx, _salobj), true);
            }

        } else if ("enter".equals(event.getAction())) {
            final BButton btn = (BButton)event.getSource();
            final ParlorInfo info = (ParlorInfo)btn.getProperty("info");
            if (!_ctx.getUserObject().tokens.isSupport() &&
                !_ctx.getUserObject().handle.equals(info.creator) &&
                info.type == ParlorInfo.Type.PASSWORD) {
                // ask for a password, then join
                OptionDialog.ResponseReceiver rr = new OptionDialog.ResponseReceiver() {
                    public void resultPosted (int button, Object result) {
                        if (button == 0) {
                            joinParlor(info.creator, ((String)result).trim());
                        }
                    }
                };
                OptionDialog.showStringDialog(_ctx, SaloonCodes.SALOON_MSGS, "m.enter_pass",
                                              new String[] { "m.enter", "m.cancel" }, 100, "", rr);
            } else {
                joinParlor(info.creator, null);
            }
        }
    }

    // documentation inherited from interface SetListener
    public void entryAdded (EntryAddedEvent<ParlorInfo> event)
    {
        if (SaloonObject.PARLORS.equals(event.getName())) {
            ParlorInfo info = event.getEntry();
            maybeAddInfo(info);
            if (_ctx.getUserObject().handle.equals(info.creator)) {
                updateEnterButton(info);
            }
        }
    }

    // documentation inherited from interface SetListener
    public void entryUpdated (EntryUpdatedEvent<ParlorInfo> event)
    {
        if (SaloonObject.PARLORS.equals(event.getName())) {
            ParlorInfo info = event.getEntry();
            if (_ctx.getUserObject().handle.equals(info.creator)) {
                updateEnterButton(info);
            }

            // see if we already have an entry that we can update
            for (ParlorRow row : _parlors) {
                if (row.info.creator.equals(info.creator)) {
                    // remove and reinsert in case our sort order changed
                    _parlors.remove(row);
                    if (getWeight(info) > 0) {
                        // then update the parlor
                        row.update(info);
                        _parlors.insertSorted(row);
                        row.reposition();
                    } else {
                        row.clear();
                    }
                    return;
                }
            }

            // otherwise we may need to readd
            maybeAddInfo(info);
        }
    }

    // documentation inherited from interface SetListener
    public void entryRemoved (EntryRemovedEvent<ParlorInfo> event)
    {
        if (SaloonObject.PARLORS.equals(event.getName())) {
            Handle handle = (Handle)event.getKey();
            for (int ii = 0; ii < _parlors.size(); ii++) {
                ParlorRow row = _parlors.get(ii);
                if (row.info.creator.equals(handle)) {
                    _parlors.remove(ii);
                    row.clear();
                    break;
                }
            }
            if (_ctx.getUserObject().handle.equals(handle)) {
                updateEnterButton(null);
            }
        }
    }

    protected void updateEnterButton (ParlorInfo info)
    {
        if (info == null) {
            _enterParlor.setText(_ctx.xlate(SaloonCodes.SALOON_MSGS, "m.create_parlor"));
            _enterParlor.setAction("create");
        } else {
            _enterParlor.setText(_ctx.xlate(SaloonCodes.SALOON_MSGS, "m.to_parlor"));
            _enterParlor.setAction("enter");
            _enterParlor.setProperty("info", info);
        }
    }

    protected void joinParlor (Handle owner, String password)
    {
        // if the saloon object is null, we're on our way out and should not try joining a parlor
        if (_salobj == null) {
            return;
        }

        _salobj.service.joinParlor(owner, password, new SaloonService.ResultListener() {
            public void requestProcessed (Object result) {
                _ctx.getLocationDirector().moveTo((Integer)result);
            }
            public void requestFailed (String cause) {
                _ctx.getChatDirector().displayFeedback(
                    SaloonCodes.SALOON_MSGS, MessageBundle.compose("m.enter_parlor_failed", cause));
            }
        });
    }

    protected void maybeAddInfo (ParlorInfo info)
    {
        if (getWeight(info) > 0) {
            ParlorRow row = new ParlorRow(info);
            _parlors.insertSorted(row);
            row.reposition();
        }
    }

    protected int getWeight (ParlorInfo info)
    {
        int weight = info.occupants;
        PlayerObject user = _ctx.getUserObject();
        if (user.handle.equals(info.creator)) {
            weight += 2000;
        } else if (user.pardners.containsKey(info.creator)) {
            weight += 1000;
        } else if (info.type == ParlorInfo.Type.RECRUITING) {
            if (user.gangId == info.gangId) {
                weight += 1000;
            } else {
                weight += (user.gangId <= 0) ? +250 : -250;
            }
        } else if (info.type == ParlorInfo.Type.SOCIAL) {
            weight += 500;
        } else if (info.type == ParlorInfo.Type.NORMAL) {
            weight += 100;
        } else if (info.type == ParlorInfo.Type.PARDNERS_ONLY) {
            weight -= 1000;
        }
        if (info.occupants == 0) {
            weight -= 900;
        }
        if (info.server) {
            weight += 5000;
        }
        return weight;
    }

    protected class ParlorRow
        implements Comparable<ParlorRow>
    {
        public ParlorInfo info;

        public ParlorRow (ParlorInfo info) {
            MessageBundle msgs = _ctx.getMessageManager().getBundle(SaloonCodes.SALOON_MSGS);
            String lbl = info.server ? msgs.get("m.server_parlor") :
                    msgs.get(info.matched ? "m.matched_name" : "m.parlor_name", info.creator);
            _name = new BLabel(lbl, "parlor_label");
            _name.setFit(BLabel.Fit.SCALE);
            _icon = new BLabel("");
            _occs = new BLabel("");
            _occs.setFit(BLabel.Fit.SCALE);
            _enter = new BButton(msgs.get("m.enter"), ParlorList.this, "enter");
            _enter.setStyleClass("alt_button");
            _enter.setPreferredSize(new Dimension(110,-1));
            update(info);
        }

        public void update (ParlorInfo info) {
            this.info = info;
            _occs.setText(String.valueOf(info.occupants));
            if (info.type != ParlorInfo.Type.NORMAL) {
                String ipath =
                    "ui/saloon/" + StringUtil.toUSLowerCase(info.type.toString()) + ".png";
                _icon.setIcon(new ImageIcon(_ctx.loadImage(ipath)));
            } else {
                _icon.setIcon(new BlankIcon(16, 16));
            }
            _enter.setProperty("info", info);
        }

        public void reposition () {
            // if we're not at our proper index, remove ourselves first
            int index = _parlors.indexOf(this) * 4;
            if (_name.getParent() != null && _list.getComponent(index) != _name) {
                clear();
            }

            if (_name.getParent() == null) {
                _list.add(index, _enter);
                _list.add(index, _occs);
                _list.add(index, _icon);
                _list.add(index, _name);
            }
        }

        public void clear () {
            if (_name.getParent() != null) {
                _list.remove(_name);
                _list.remove(_icon);
                _list.remove(_occs);
                _list.remove(_enter);
            }
        }

        public int compareTo (ParlorRow other) {
            return getWeight(other.info) - getWeight(info);
        }

        protected BLabel _name, _occs, _icon;
        protected BButton _enter;
    }

    protected BangContext _ctx;
    protected SaloonObject _salobj;
    protected BContainer _list;
    protected BButton _enterParlor;

    protected ComparableArrayList<ParlorRow> _parlors = new ComparableArrayList<ParlorRow>();
}
