//
// $Id$

package com.threerings.bang.editor;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.samskivert.swing.CommandButton;
import com.samskivert.swing.VGroupLayout;
import com.samskivert.util.StringUtil;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PropConfig;
import com.threerings.bang.game.data.piece.BonusMarker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Prop;
import com.threerings.bang.game.data.piece.StartMarker;
import com.threerings.bang.util.BangContext;

/**
 * Provides buttons for adding pieces of various types to the board.
 */
public class PieceCreator extends JPanel
{
    public PieceCreator (BangContext ctx)
    {
        setLayout(new VGroupLayout());
        _ctx = ctx;

        add(new JLabel(_ctx.xlate("editor", "m.pieces_props")));

        // TODO: deal with town selection
        PropConfig[] props = PropConfig.getTownProps(
            BangCodes.FRONTIER_TOWN);
        for (int ii = 0; ii < props.length; ii++) {
            String type = props[ii].type;
            add(createPieceButton(type, Prop.getProp(type)));
        }

        add(new JLabel(_ctx.xlate("editor", "m.pieces_marker")));
        add(createPieceButton("start_marker", new StartMarker()));
        add(createPieceButton("bonus_marker", new BonusMarker()));
    }

    protected CommandButton createPieceButton (String type, Piece piece)
    {
        String key = StringUtil.replace(type, "/", "_");
        String name = _ctx.xlate("editor", "m.piece_" + key);
        CommandButton button = new CommandButton();
        button.setText(name);
        button.setActionCommand(EditorController.CREATE_PIECE);
        button.setActionArgument(piece);
        button.addActionListener(EditorController.DISPATCHER);
        return button;
    }

    protected BangContext _ctx;
}
