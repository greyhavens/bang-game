//
// $Id$

package com.threerings.bang.bounty.client;

import java.util.ArrayList;

import com.jmex.bui.BButton;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.bounty.data.OfficeCodes;

/**
 * Displays configuration for a single player in a bounty game.
 */
public class BountyPlayerEditor extends BDecoratedWindow
    implements ActionListener
{
    public BountyPlayerEditor (BangContext ctx, BountyGameEditor editor, int oppcount, int pidx,
                               BangConfig.Player player)
    {
        super(ctx.getStyleSheet(), ctx.xlate(OfficeCodes.OFFICE_MSGS, "m.create_bounty_player"));
        setLayoutManager(GroupLayout.makeVert(GroupLayout.CENTER).setGap(20));
        setModal(true);

        _ctx = ctx;
        _editor = editor;
        _pidx = pidx;
        _player = player;

        _msgs = ctx.getMessageManager().getBundle(OfficeCodes.OFFICE_MSGS);

        // enumerate our available units
        String townId = ctx.getUserObject().townId;
        ArrayList<BComboBox.Item> bsunits = new ArrayList<BComboBox.Item>();
        ArrayList<BComboBox.Item> runits = new ArrayList<BComboBox.Item>();
        bsunits.add(new BComboBox.Item(null, _msgs.get("m.none")));
        for (UnitConfig uconf : UnitConfig.getTownUnits(townId, UnitConfig.Rank.BIGSHOT)) {
            bsunits.add(new BComboBox.Item(uconf.type, _msgs.xlate(uconf.getName())));
        }
        runits.add(new BComboBox.Item(null, _msgs.get("m.none")));
        for (UnitConfig uconf : UnitConfig.getTownUnits(townId, UnitConfig.Rank.NORMAL)) {
            runits.add(new BComboBox.Item(uconf.type, _msgs.xlate(uconf.getName())));
        }

        // create our configuration interface
        BContainer cpanel = new BContainer(new TableLayout(2, 5, 5));

        addRow(cpanel, "m.bplayer_start_spot").add(_start = new BComboBox(START_SPOTS[oppcount]));
        _start.selectItem(Integer.valueOf(player.startSpot));

        _teams.add(new BComboBox.Item(-1, _msgs.xlate("m.no_teams")));
        for (int ii = 0; ii <= oppcount; ii++) {
            _teams.add(new BComboBox.Item(ii, _msgs.get("m.on_team", String.valueOf(ii+1))));
        }
        addRow(cpanel, "m.bplayer_team").add(_team = new BComboBox(_teams));
        _team.selectValue(Integer.valueOf(player.teamIdx));

        if (pidx > 0) {
            addRow(cpanel, "m.bplayer_skill").add(_skill = new BComboBox(SKILLS));
            _skill.selectItem(Integer.valueOf(player.skill));
        }

        addRow(cpanel, "m.bplayer_bigshot").add(_bigshot = new BComboBox(bsunits));
        _bigshot.selectValue(player.bigShot);

        cpanel.add(new BLabel(_msgs.get("m.bplayer_units")));
        BContainer tpanel = new BContainer(new TableLayout(3, 5, 5));
        _units = new BComboBox[MAX_BOUNTY_UNITS];
        for (int ii = 0; ii < _units.length; ii++) {
            tpanel.add(_units[ii] = new BComboBox(runits));
            if (_player.units != null && _player.units.length > ii) {
                _units[ii].selectValue(_player.units[ii]);
            } else {
                _units[ii].selectItem(0);
            }
        }
        cpanel.add(tpanel);
        add(cpanel);

        // add some control buttons
        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        buttons.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
        add(buttons);
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("dismiss".equals(event.getAction())) {
            // write our data back out to our player record
            _player.bigShot = (String)_bigshot.getSelectedValue();
            _player.units = getUnits();
            _player.startSpot = (Integer)_start.getSelectedItem();
            _player.teamIdx = (Integer)_team.getSelectedValue();
            if (_pidx > 0) {
                _player.skill = (Integer)_skill.getSelectedItem();
            }
            // tell the game editor we're dismissed and to update its display, then disappear
            _editor.playerUpdated(_pidx);
            _ctx.getBangClient().clearPopup(this, true);
        }
    }

    protected BContainer addRow (BContainer box, String label)
    {
        box.add(new BLabel(_msgs.get(label), "table_label"));
        BContainer row = GroupLayout.makeHBox(GroupLayout.LEFT);
        box.add(row);
        return row;
    }

    protected String[] getUnits ()
    {
        ArrayList<String> team = new ArrayList<String>();
        for (int ii = 0; ii < _units.length; ii++) {
            String unit = (String)_units[ii].getSelectedValue();
            if (unit != null) {
                team.add(unit);
            }
        }
        return team.toArray(new String[team.size()]);
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs  ;
    protected BountyGameEditor _editor;
    protected int _pidx;
    protected BangConfig.Player _player;

    protected BComboBox _start, _team, _skill, _bigshot;
    protected BComboBox[] _units;

    protected static final Integer[][] START_SPOTS = {
        new Integer[] { -1, 0, 1 },
        new Integer[] { -1, 0, 1 },
        new Integer[] { -1, 0, 1, 2 },
        new Integer[] { -1, 0, 1, 2, 3 },
    };

    protected ArrayList<BComboBox.Item> _teams = new ArrayList<BComboBox.Item>();

    protected static final Integer[] SKILLS = new Integer[9];
    static {
        for (int ii = 0; ii < SKILLS.length; ii++) {
            SKILLS[ii] = (ii+1) * 10;
        }
    };

    // default to max team size plus some extra for when we want to get jiggy
    protected static final int MAX_BOUNTY_UNITS = GameCodes.MAX_TEAM_SIZE + 1;
}
