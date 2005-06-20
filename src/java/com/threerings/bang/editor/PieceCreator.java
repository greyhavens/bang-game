//
// $Id$

package com.threerings.bang.editor;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.samskivert.swing.CommandButton;
import com.samskivert.swing.VGroupLayout;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BuildingConfig;
import com.threerings.bang.data.piece.BonusMarker;
import com.threerings.bang.data.piece.Building;
import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.data.piece.Prop;
import com.threerings.bang.data.piece.StartMarker;
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

        add(new JLabel(_ctx.xlate("editor", "m.pieces_fixed")));

        // TODO: deal with town selection
        BuildingConfig[] bldgs = BuildingConfig.getTownBuildings(
            BangCodes.FRONTIER_TOWN);
        for (int ii = 0; ii < bldgs.length; ii++) {
            String type = bldgs[ii].type;
            add(createPieceButton(type, Building.getBuilding(type)));
        }

        add(createPieceButton("rocks", new Prop("rock_2x2", 2, 2)));
        add(createPieceButton("cactus", new Prop("cactus2_1x1", 1, 1)));

        add(new JLabel(_ctx.xlate("editor", "m.pieces_marker")));
        add(createPieceButton("start_marker", new StartMarker()));
        add(createPieceButton("bonus_marker", new BonusMarker()));
    }

    protected CommandButton createPieceButton (String type, Piece piece)
    {
        String name = _ctx.xlate("editor", "m.piece_" + type);
        CommandButton button = new CommandButton();
        button.setText(name);
        button.setActionCommand(EditorController.CREATE_PIECE);
        button.setActionArgument(piece);
        button.addActionListener(EditorController.DISPATCHER);
        return button;
    }

    protected BangContext _ctx;
}
