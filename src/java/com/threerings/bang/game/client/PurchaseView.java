//
// $Id$

package com.threerings.bang.game.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import com.jme.bui.BButton;
import com.jme.bui.BComponent;
import com.jme.bui.BContainer;
import com.jme.bui.BDecoratedWindow;
import com.jme.bui.BLabel;
import com.jme.bui.event.ActionEvent;
import com.jme.bui.event.ActionListener;
import com.jme.bui.layout.BorderLayout;
import com.jme.bui.layout.GroupLayout;
import com.jme.bui.layout.TableLayout;

import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.util.BangContext;

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
        super(ctx.getLookAndFeel(), null);

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);
        _bangobj = bangobj;
        _pidx = pidx;

        setLayoutManager(GroupLayout.makeVStretch());
        add(SelectionView.createRoundHeader(ctx, config, bangobj),
            GroupLayout.FIXED);
        add(new BLabel(_msgs.get("m.pv_assemble")), GroupLayout.FIXED);

        BContainer ulist = new BContainer(new TableLayout(7, 5, 5));
        add(ulist);

        // sort our units by cost
        UnitConfig[] units = UnitConfig.getTownUnits(_bangobj.townId);
        Arrays.sort(units, new Comparator<UnitConfig>() {
            public int compare (UnitConfig u1, UnitConfig u2) {
                if (u1.scripCost == u2.scripCost) {
                    return u1.type.compareTo(u2.type);
                } else {
                    return u1.scripCost - u2.scripCost;
                }
            }
        });

        // add a palette of buttons for each available unit type
        for (int ii = 0; ii < units.length; ii++) {
            UnitConfig uc = units[ii];
            // skip bigshots and unrecruitable special units
            if (uc.rank == UnitConfig.Rank.BIGSHOT || uc.scripCost < 0) {
                continue;
            }
            ulist.add(createRecruitButton(uc));
        }

        BContainer team = new BContainer(
            new TableLayout(1 + config.teamSize, 3, 10));
        add(team, GroupLayout.FIXED);

        team.add(new BLabel(_msgs.get("m.pv_bigshot")));
        team.add(new BLabel(_msgs.get("m.pv_team")));
        for (int ii = 1; ii < config.teamSize; ii++) {
            team.add(new BLabel(""));
        }

        // display their selected big shot
        team.add(BangUI.createUnitLabel(_bangobj.bigShots[_pidx].getConfig()));

        // display slots for each potential team member
        _team = new BLabel[config.teamSize];
        _tconfigs = new UnitConfig[config.teamSize];
        for (int ii = 0; ii < _team.length; ii++) {
            team.add(_team[ii] = BangUI.createUnitLabel(null));
        }

        // add delete buttons
        team.add(new BLabel("")); // none for the big shot
        for (int ii = 0; ii < config.teamSize; ii++) {
            // TODO: make this an icon
            BButton delete = new BButton(_msgs.get("m.pv_delete"));
            delete.setAction("delete");
            delete.addListener(this);
            delete.setProperty("index", ii);
            team.add(delete);
        }

        GroupLayout glay = GroupLayout.makeHStretch();
        glay.setGap(25);
        BContainer footer = new BContainer(glay);
        add(footer, GroupLayout.FIXED);
        String cmsg =  _msgs.get("m.pv_cash", "" + _bangobj.funds[_pidx]);
        footer.add(new BLabel(cmsg), GroupLayout.FIXED);
        footer.add(_tlabel = new BLabel(_msgs.get("m.pv_cost", "0")));

        _ready = new BButton(_msgs.get("m.ready"));
        _ready.setAction("ready");
        _ready.addListener(this);
//         _ready.setEnabled(false);
        footer.add(_ready, GroupLayout.FIXED);
    }

    protected BComponent createRecruitButton (UnitConfig unit)
    {
        BButton button = BangUI.createUnitButton(unit);
        button.setAction("buy");
        button.addListener(this);
        button.setProperty("unit", unit);
        button.setText(button.getText() + " $" + unit.scripCost);
        return button;
    }

    protected void updateTotal ()
    {
        _total = 0;
        for (int ii = 0; ii < _tconfigs.length; ii++) {
            if (_tconfigs[ii] != null) {
                _total += _tconfigs[ii].scripCost;
            }
        }
        _tlabel.setText(_msgs.get("m.pv_cost", "" + _total));
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent e)
    {
        String cmd = e.getAction();
        if (cmd.equals("buy")) {
            BButton button = (BButton)e.getSource();
            UnitConfig config = (UnitConfig)button.getProperty("unit");
            for (int ii = 0; ii < _tconfigs.length; ii++) {
                if (_tconfigs[ii] == null) {
                    _tconfigs[ii] = config;
                    BangUI.configUnitLabel(_team[ii], config);
                    updateTotal();
                    return;
                }
            }

        } else if (cmd.equals("delete")) {
            BButton button = (BButton)e.getSource();
            int index = (Integer)button.getProperty("index");
            _tconfigs[index] = null;
            BangUI.configUnitLabel(_team[index], null);
            updateTotal();

        } else if (cmd.equals("ready")) {
            // _ready.setEnabled(false);
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

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BangObject _bangobj;
    protected int _pidx;

    protected int _total;
    protected BLabel _tlabel;
    protected BButton _ready;

    protected BLabel[] _team;
    protected UnitConfig[] _tconfigs;

    protected static final String[] COLUMNS = {
        "unit", "cost", "count", "", ""
    };
    protected static final UnitConfig[] UNIT_CONFIGS = {
        UnitConfig.getConfig("steamgunman"), UnitConfig.getConfig("artillery"),
        UnitConfig.getConfig("dirigible"), UnitConfig.getConfig("gunslinger"),
        UnitConfig.getConfig("sharpshooter"), UnitConfig.getConfig("shotgunner")
    };
}
