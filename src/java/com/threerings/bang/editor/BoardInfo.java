//
// $Id$

package com.threerings.bang.editor;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.samskivert.swing.VGroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.ScenarioCodes;
import com.threerings.bang.game.server.scenario.ScenarioFactory;
import com.threerings.bang.server.persist.BoardRecord;
import com.threerings.bang.util.BasicContext;

/**
 * Displays and allows editing of board metadata.
 */
public class BoardInfo extends JPanel
{
    public BoardInfo (BasicContext ctx)
    {
        setLayout(new VGroupLayout(VGroupLayout.NONE, VGroupLayout.STRETCH,
                                   5, VGroupLayout.TOP));
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("editor");
        MessageBundle gmsgs = ctx.getMessageManager().getBundle("game");

        add(new JLabel(_msgs.get("m.board_name")));
        add(_name = new JTextField());

        add(_pcount = new JLabel());
        updatePlayers(0);

        add(new JLabel(_msgs.get("m.scenarios")));
        JPanel spanel = new JPanel(
            new VGroupLayout(VGroupLayout.NONE, VGroupLayout.STRETCH,
                             2, VGroupLayout.TOP));
        String[] scids = ScenarioFactory.getScenarios(
            BangCodes.TOWN_IDS[BangCodes.TOWN_IDS.length-1]);
        JCheckBox box;
        for (int ii = 0; ii < scids.length; ii++) {
            String sname = gmsgs.get("m.scenario_" + scids[ii]);
            spanel.add(box = new JCheckBox(sname));
            _sboxes.put(scids[ii], box);
        }

        // add a selection for the tutorial scenario
        String sname = gmsgs.get("m.scenario_" + ScenarioCodes.TUTORIAL);
        spanel.add(box = new JCheckBox(sname));
        _sboxes.put(ScenarioCodes.TUTORIAL, box);

        add(new JScrollPane(spanel) {
            public Dimension getPreferredSize () {
                Dimension d = super.getPreferredSize();
                d.height = Math.min(d.height, 200);
                return d;
            }
        });
    }

    /**
     * Updates the "number of players" count displayed for this board.
     */
    public void updatePlayers (int players)
    {
        _players = players;
        _pcount.setText(_msgs.get("m.players", "" + players));
    }

    /**
     * Reads the supplied board's metadata and configures the UI
     * appropriately.
     */
    public void fromBoard (BoardRecord board)
    {
        _name.setText(board.name);
        updatePlayers(board.players);

        // first turn off all the scenario check boxes
        for (String scid : _sboxes.keySet()) {
            _sboxes.get(scid).setSelected(false);
        }
        // then turn on the ones that are valid for this board
        String[] scids = board.getScenarios();
        for (int ii = 0; ii < scids.length; ii++) {
            _sboxes.get(scids[ii]).setSelected(true);
        }
    }

    /**
     * Configures the supplied board's metadata with the values from the
     * user interface.
     */
    public void toBoard (BoardRecord board)
    {
        board.name = _name.getText();
        board.players = _players;
        ArrayList<String> scenids = new ArrayList<String>();
        for (String scid : _sboxes.keySet()) {
            if (_sboxes.get(scid).isSelected()) {
                scenids.add(scid);
            }
        }
        board.setScenarios(scenids.toArray(new String[scenids.size()]));
    }

    /**
     * Clears the user interface.
     */
    public void clear ()
    {
        _name.setText("");
        for (Iterator<JCheckBox> it = _sboxes.values().iterator();
                it.hasNext(); ) {
            it.next().setSelected(false);
        }
    }
    
    protected BasicContext _ctx;
    protected MessageBundle _msgs;

    protected JTextField _name;
    protected JLabel _pcount;
    protected int _players;

    protected HashMap<String,JCheckBox> _sboxes =
        new HashMap<String,JCheckBox>();
}
