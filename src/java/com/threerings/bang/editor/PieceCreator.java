//
// $Id$

package com.threerings.bang.editor;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.samskivert.swing.CommandButton;
import com.samskivert.swing.VGroupLayout;

import com.threerings.toybox.util.ToyBoxContext;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.piece.*;

/**
 * Provides buttons for adding pieces of various types to the board.
 */
public class PieceCreator extends JPanel
{
    public PieceCreator (ToyBoxContext ctx)
    {
        setLayout(new VGroupLayout());
        _ctx = ctx;

        add(new JLabel(_ctx.xlate(BangCodes.BANG_MSGS, "m.pieces_fixed")));
        add(createPieceButton("building", new Building()));

//         add(new JLabel(_ctx.xlate(BangCodes.BANG_MSGS, "m.pieces_player")));
//         add(createPieceButton("artillery", new Artillery()));
//         add(createPieceButton("bee", new Bee()));
//         add(createPieceButton("beetle", new Beetle()));
//         add(createPieceButton("caterpillar", new Caterpillar()));
//         add(createPieceButton("termite", new Termite()));
    }

    protected CommandButton createPieceButton (String type, Piece piece)
    {
        String name = _ctx.xlate(BangCodes.BANG_MSGS, "m.piece_" + type);
        CommandButton button = new CommandButton();
        button.setText(name);
        button.setActionCommand(EditorController.CREATE_PIECE);
        button.setActionArgument(piece);
        button.addActionListener(EditorController.DISPATCHER);
        return button;
    }

    protected ToyBoxContext _ctx;
}
