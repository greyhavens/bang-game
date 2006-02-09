//
// $Id$

package com.threerings.bang.game.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.samskivert.util.CollectionUtil;
import com.threerings.util.MessageBundle;

import com.threerings.bang.ranch.client.UnitIcon;
import com.threerings.bang.ranch.client.UnitPalette;
import com.threerings.bang.ranch.client.UnitView;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;

import static com.threerings.bang.Log.log;

/**
 * Displays an interface for purchasing units.
 */
public class PurchaseView extends BDecoratedWindow
    implements ActionListener
{
    public PurchaseView (BangContext ctx, BangConfig config,
                         BangObject bangobj, int pidx)
    {
        super(ctx.getStyleSheet(), null);

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);
        _bangobj = bangobj;
        _pidx = pidx;
        _tconfigs = new UnitConfig[config.teamSize];

        setLayoutManager(new BorderLayout(25, 15));
        add(SelectionView.createRoundHeader(ctx, config, bangobj),
            BorderLayout.NORTH);

        BContainer side = GroupLayout.makeVBox(GroupLayout.TOP);
        add(side, BorderLayout.WEST);
        side.add(_uname = new BLabel("", "pick_unit_name"));
        side.add(_uview = new UnitView(ctx, true));
        // TODO: we need our big shot's custom name
        _uname.setText(_bangobj.bigShots[_pidx].getConfig().type);
        _uview.setUnit(_bangobj.bigShots[_pidx].getConfig());

        // add a label for each selectable unit
        _team = new BLabel[config.teamSize];
        for (int ii = 0; ii < _team.length; ii++) {
            side.add(_team[ii] = new BLabel("", "pick_team_choice"));
        }

        BContainer cent = GroupLayout.makeVBox(GroupLayout.TOP);
        ((GroupLayout)cent.getLayoutManager()).setOffAxisJustification(
            GroupLayout.LEFT);
        add(cent, BorderLayout.CENTER);

        // create the big shot selection display
        cent.add(new BLabel(_msgs.get("m.pv_assemble"), "pick_subtitle"));
        _units = new UnitPalette(ctx, _inspector, 4, 2);
        _units.setPaintBorder(true);
        _units.setStyleClass("pick_palette");
        _units.setSelectable(config.teamSize);
        _units.selectFirstIcon();
        cent.add(_units);

        // determine which units are available for selection
        ArrayList<UnitConfig> units = new ArrayList<UnitConfig>();
        CollectionUtil.addAll(units, UnitConfig.getTownUnits(_bangobj.townId));
        for (Iterator<UnitConfig> iter = units.iterator(); iter.hasNext(); ) {
            // filter out bigshots and unrecruitable special units
            UnitConfig uc = iter.next();
            if (uc.rank == UnitConfig.Rank.BIGSHOT || uc.scripCost < 0) {
                iter.remove();
            }
        }
        // sort the units by cost
        Collections.sort(units, new Comparator<UnitConfig>() {
            public int compare (UnitConfig u1, UnitConfig u2) {
                if (u1.scripCost == u2.scripCost) {
                    return u1.type.compareTo(u2.type);
                } else {
                    return u1.scripCost - u2.scripCost;
                }
            }
        });
        _units.setUnits(units.toArray(new UnitConfig[units.size()]));

        // go through and jam the price onto the label of each of the units
        SelectableIcon[] icons = _units.getIcons();
        for (int ii = 0; ii < icons.length; ii++) {
            UnitIcon uicon = (UnitIcon)icons[ii];
            uicon.setText(uicon.getText() + " $" + uicon.getUnit().scripCost);
        }

        BContainer footer = GroupLayout.makeHBox(GroupLayout.CENTER);
        ((GroupLayout)footer.getLayoutManager()).setGap(25);
        add(footer, BorderLayout.SOUTH);
        String cmsg =  _msgs.get("m.pv_cash", "" + _bangobj.funds[_pidx]);
        footer.add(new BLabel(cmsg, "money_label"));
        cmsg = _msgs.get("m.pv_cost", "0");
        footer.add(_tlabel = new BLabel(cmsg, "money_label"));
        footer.add(_ready = new BButton(_msgs.get("m.ready"), this, "ready"));
        _ready.setEnabled(false);
    }

    protected void selectionUpdated ()
    {
        int uidx = 0, selected = 0;
        _total = 0;

        SelectableIcon[] icons = _units.getIcons();
        for (int ii = 0; ii < icons.length; ii++) {
            if (!icons[ii].isSelected()) {
                continue;
            }
            UnitIcon icon = (UnitIcon)icons[ii];
            _tconfigs[uidx] = icon.getUnit();
            _team[uidx].setText(_ctx.xlate("units", _tconfigs[uidx].getName()));
            _total += _tconfigs[uidx].scripCost;
            uidx++;
            selected++;
        }

        for (int ii = uidx; ii < _team.length; ii++) {
            _tconfigs[ii] = null;
            _team[ii].setText("");
        }

        _tlabel.setText(_msgs.get("m.pv_cost", "" + _total));
        _ready.setEnabled(selected > 0 && _total <= _bangobj.funds[_pidx]);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent e)
    {
        String cmd = e.getAction();
        if (cmd.equals("ready")) {
            _ready.setEnabled(false);
            ArrayList<String> units = new ArrayList<String>();
            for (int ii = 0; ii < _tconfigs.length; ii++) {
                if (_tconfigs[ii] != null) {
                    units.add(_tconfigs[ii].type);
                }
            }
            String[] uvec = units.toArray(new String[units.size()]);
            _bangobj.service.purchaseUnits(_ctx.getClient(), uvec);
        }
    }

    protected UnitPalette.Inspector _inspector = new UnitPalette.Inspector() {
        public void iconUpdated (SelectableIcon icon, boolean selected) {
            selectionUpdated();
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BangObject _bangobj;
    protected int _pidx;

    protected BLabel _uname;
    protected UnitView _uview;
    protected UnitPalette _units;

    protected BLabel[] _team;
    protected UnitConfig[] _tconfigs;

    protected int _total;
    protected BLabel _tlabel;
    protected BButton _ready;
}
