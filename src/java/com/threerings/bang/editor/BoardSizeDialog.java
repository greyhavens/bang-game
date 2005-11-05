//
// $Id$

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jme.math.FastMath;

import com.samskivert.swing.VGroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangBoard;

/**
 * Allows the user to change the board size.
 */
public class BoardSizeDialog extends JDialog
{
    public BoardSizeDialog (EditorContext ctx, EditorPanel panel)
    {
        super(ctx.getFrame(), ctx.xlate("editor", "t.board_size_dialog"),
            true);
        _ctx = ctx;
        _panel = panel;
    
        getContentPane().add(_dimensions = new DimensionsPanel(ctx),
            BorderLayout.CENTER);
        
        JPanel buttons = new JPanel();
        JButton change = new JButton(_ctx.xlate("editor", "b.change_size"));
        change.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent ae) {
                try {
                    _panel.view.changeBoardSize(_dimensions.getWidthValue(),
                        _dimensions.getHeightValue());
                    _ctx.displayStatus(_ctx.xlate("editor", "m.size_changed"));
                    
                } catch (Exception e) {
                    _ctx.displayStatus(_ctx.xlate("editor",
                        MessageBundle.tcompose("m.size_change_error", e)));
                }
                setVisible(false);
            }
        });
        buttons.add(change);
        JButton dismiss = new JButton(_ctx.xlate("editor", "b.dismiss"));
        dismiss.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent e) {
                setVisible(false);
            }
        });
        buttons.add(dismiss);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        
        setSize(350, 250);
        setResizable(false);
    }
    
    /**
     * Initializes this panel based on the state of the supplied board.
     */
    public void fromBoard (BangBoard board)
    {
        _dimensions.setValues(board.getWidth(), board.getHeight());
    }
    
    /** The application context. */
    protected EditorContext _ctx;
    
    /** The containing panel. */
    protected EditorPanel _panel;
    
    /** The dimensions panel. */
    protected DimensionsPanel _dimensions;
}
