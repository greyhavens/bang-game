//
// $Id$

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangBoard;

/**
 * Allows the user to change various properties of the board, such as its size
 * and elevation scale.
 */
public class BoardPropertiesDialog extends JDialog
    implements ChangeListener
{
    public BoardPropertiesDialog (EditorContext ctx, EditorPanel panel)
    {
        super(ctx.getFrame(), ctx.xlate("editor", "t.board_props_dialog"),
            true);
        _ctx = ctx;
        _panel = panel;
    
        JPanel center = new JPanel(new VGroupLayout());
        getContentPane().add(center, BorderLayout.CENTER);
        
        JPanel spanel = GroupLayout.makeHBox();
        spanel.setBorder(BorderFactory.createTitledBorder(null,
            _ctx.xlate("editor", "m.board_size")));
        spanel.add(_dimensions = new DimensionsPanel(ctx));
        JButton change = new JButton(_ctx.xlate("editor", "b.resize"));
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
        spanel.add(change);
        center.add(spanel);
        
        JPanel epanel = new JPanel();
        epanel.add(new JLabel(_ctx.xlate("editor", "m.elevation_scale")));
        epanel.add(_elevationScale = new JSlider(1, 127, 64));
        _elevationScale.addChangeListener(this);
        center.add(epanel);
        
        center.add(_gridColor = new ColorPanel(ctx, "m.grid_color"));
        _gridColor.addChangeListener(this);
        
        JPanel buttons = new JPanel();
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
        _elevationScale.setValue(128 - board.getElevationUnitsPerTile());
        _gridColor.setRGB(board.getGridColor());
    }
    
    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent e)
    {
        if (!isShowing()) {
            return; // invoked from fromBoard
        } else if (e.getSource() == _gridColor) {
            _panel.view.setGridColor(_gridColor.getRGB(), true);
            return;
        }
        _panel.view.setElevationUnitsPerTile(128 - _elevationScale.getValue(),
            true);
        if (!_elevationScale.getValueIsAdjusting()) {
            _panel.view.commitElevationUnitsEdit();
        }
    }
    
    /** The application context. */
    protected EditorContext _ctx;
    
    /** The containing panel. */
    protected EditorPanel _panel;
    
    /** The dimensions panel. */
    protected DimensionsPanel _dimensions;
    
    /** The elevation scale slider. */
    protected JSlider _elevationScale;
    
    /** The grid color panel. */
    public ColorPanel _gridColor;
}
