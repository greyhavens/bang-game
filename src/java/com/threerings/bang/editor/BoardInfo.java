//
// $Id$

package com.threerings.bang.editor;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.samskivert.swing.VGroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.server.persist.BoardRecord;
import com.threerings.bang.util.BangContext;

/**
 * Displays and allows editing of board metadata.
 */
public class BoardInfo extends JPanel
{
    public BoardInfo (BangContext ctx)
    {
        setLayout(new VGroupLayout(VGroupLayout.NONE, VGroupLayout.STRETCH,
                                   5, VGroupLayout.TOP));
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("editor");

        add(new JLabel(_msgs.get("m.board_name")));
        add(_name = new JTextField());

        add(_pcount = new JLabel());
        updatePlayers(0);

        add(new JLabel(_msgs.get("m.scenarios")));
    }

    /**
     * Reads the supplied board's metadata and configures the UI
     * appropriately.
     */
    public void fromBoard (BoardRecord board)
    {
        _name.setText(board.name);
        updatePlayers(board.players);
    }

    /**
     * Configures the supplied board's metadata with the values from the
     * user interface.
     */
    public void toBoard (BoardRecord board)
    {
        board.name = _name.getText();
        board.players = _players;
    }

    public void updatePlayers (int players)
    {
        _players = players;
        _pcount.setText(_msgs.get("m.players", "" + players));
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;

    protected JTextField _name;
    protected JLabel _pcount;
    protected int _players;
}
