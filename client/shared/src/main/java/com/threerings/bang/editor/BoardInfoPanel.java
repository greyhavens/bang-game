//
// $Id$

package com.threerings.bang.editor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.samskivert.swing.VGroupLayout;
import com.samskivert.swing.HGroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.game.util.BoardFile;
import com.threerings.bang.util.BasicContext;

/**
 * Displays and allows editing of board metadata.
 */
public class BoardInfoPanel extends JPanel
    implements ItemListener
{
    public BoardInfoPanel (BasicContext ctx, EditorPanel panel)
    {
        _panel = panel;
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
            new VGroupLayout(VGroupLayout.NONE, VGroupLayout.STRETCH, 2, VGroupLayout.TOP));

        // prop visibility combo box
        _props = new JComboBox<ScenarioLabel>();
        _props.addItem(new ScenarioLabel(null));

        String townId = BangCodes.TOWN_IDS[BangCodes.TOWN_IDS.length-1];
        for (ScenarioInfo info : ScenarioInfo.getScenarios(townId, true)) {
            JCheckBox box = new JCheckBox(gmsgs.get(info.getName()));
            spanel.add(box);
            _sboxes.put(info.getIdent(), box);
            _props.addItem(new ScenarioLabel(info.getIdent()));
        }

        add(new JScrollPane(spanel) {
            public Dimension getPreferredSize () {
                Dimension d = super.getPreferredSize();
                d.height = Math.min(d.height, 200);
                return d;
            }
        });
        add(_privateBoard = new JCheckBox(_msgs.get("m.private_board")));
        _privateBoard.setSelected(true);

        // create the prop visibility panel
        JPanel ppanel = new JPanel(new HGroupLayout(HGroupLayout.STRETCH));
        ppanel.add(_plabel = new JLabel(_msgs.get("m.props")),
                HGroupLayout.FIXED);
        ppanel.add(_props);
        _props.addItemListener(this);
        add(ppanel);
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
    public void fromBoard (BoardFile board)
    {
        _name.setText(board.name);
        _props.setSelectedIndex(0);
        updatePlayers(board.players);

        // first turn off all the scenario check boxes
        for (String scid : _sboxes.keySet()) {
            _sboxes.get(scid).setSelected(false);
        }

        // then turn on the ones that are valid for this board
        String[] scids = board.scenarios;
        for (int ii = 0; ii < scids.length; ii++) {
            JCheckBox box = _sboxes.get(scids[ii]);
            if (box != null) {
                box.setSelected(true);
            }
        }
        _privateBoard.setSelected(board.privateBoard);
    }

    /**
     * Configures the supplied board's metadata with the values from the
     * user interface.
     */
    public void toBoard (BoardFile board)
    {
        board.name = _name.getText();
        board.players = _players;
        ArrayList<String> scenids = getSelectedScenarios();
        board.scenarios = scenids.toArray(new String[scenids.size()]);
        board.privateBoard = _privateBoard.isSelected();
    }

    /**
     * Get an ArrayList of selected scenario ids.
     */
    public ArrayList<String> getSelectedScenarios ()
    {
        ArrayList<String> scenids = new ArrayList<String>();
        for (String scid : _sboxes.keySet()) {
            if (_sboxes.get(scid).isSelected()) {
                scenids.add(scid);
            }
        }
        return scenids;
    }

    /**
     * Clears the user interface.
     */
    public void clear ()
    {
        _name.setText("");
        for (Iterator<JCheckBox> it = _sboxes.values().iterator(); it.hasNext(); ) {
            it.next().setSelected(false);
        }
        _privateBoard.setSelected(true);
    }

    /**
     * Get the currently selected prop id.
     */
    public String getPropId ()
    {
        return ((ScenarioLabel)_props.getSelectedItem()).id;
    }

    // inherited from iterface ItemListener
    public void itemStateChanged (ItemEvent ie)
    {
        ScenarioLabel sl = (ScenarioLabel)ie.getItem();
        _plabel.setForeground(sl.getColor());
        _props.setForeground(sl.getColor());
        ((EditorController)_panel.getController()).setViewingProps(sl.id);
    }

    protected class ScenarioLabel {
        String id;

        public ScenarioLabel (String scenarioId)
        {
            id = scenarioId;
            _name = (id == null ? _msgs.get("m.all") :
                    _ctx.xlate("game", "m.scenario_" + id));
        }

        public String toString ()
        {
            return _name;
        }

        public Color getColor ()
        {
            return (id == null) ? Color.BLACK : Color.RED;
        }

        String _name;
    }

    protected BasicContext _ctx;
    protected MessageBundle _msgs;

    protected JTextField _name;
    protected JLabel _pcount, _plabel;
    protected JComboBox<ScenarioLabel> _props;
    protected JCheckBox _privateBoard;
    protected int _players;

    protected EditorPanel _panel;

    protected HashMap<String,JCheckBox> _sboxes = new HashMap<String,JCheckBox>();
}
