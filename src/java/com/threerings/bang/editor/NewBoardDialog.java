//
// $Id$

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import com.threerings.bang.game.data.BangBoard;

/**
 * Allows the user to create a new board.
 */
public class NewBoardDialog extends JDialog
{
    public NewBoardDialog (EditorContext ctx, EditorPanel panel)
    {
        super(ctx.getFrame(), ctx.xlate("editor", "t.new_board_dialog"),
            true);
        _ctx = ctx;
        _panel = panel;
    
        getContentPane().add(_dimensions = new DimensionsPanel(ctx),
            BorderLayout.CENTER);
        
        JPanel buttons = new JPanel();
        JButton create = new JButton(_ctx.xlate("editor", "b.create_board"));
        create.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent ae) {
                _panel.view.createNewBoard(_dimensions.getWidthValue(),
                    _dimensions.getHeightValue());
                _ctx.displayStatus(_ctx.xlate("editor", "m.created_board"));
                _ctx.setWindowTitle(_ctx.xlate("editor", "m.editor_title"));  
                setVisible(false);
            }
        });
        buttons.add(create);
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
