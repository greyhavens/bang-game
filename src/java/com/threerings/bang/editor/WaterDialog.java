//
// $Id$

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.VGroupLayout;

import com.threerings.bang.game.data.BangBoard;

/**
 * Allows the user to edit the board's water configuration.
 */
public class WaterDialog extends JDialog
    implements ChangeListener
{
    public WaterDialog (EditorContext ctx, EditorPanel panel)
    {
        super(ctx.getFrame(), ctx.xlate("editor", "t.water_dialog"), true);
        _ctx = ctx;
        _panel = panel;
    
        JPanel center = new JPanel(new VGroupLayout());
        center.add(_enable = new JCheckBox(_ctx.xlate("editor",
            "m.water_enable"), false));
        _enable.addChangeListener(this);
        
        JPanel lpanel = new JPanel();
        lpanel.add(new JLabel(_ctx.xlate("editor", "m.water_level")));
        lpanel.add(_level = new JSlider(-128, +127, 0));
        _level.addChangeListener(this);
        _level.setEnabled(false);
        center.add(lpanel);
        
        getContentPane().add(center, BorderLayout.CENTER);
        
        JPanel buttons = new JPanel();
        JButton dismiss = new JButton(_ctx.xlate("editor", "b.dismiss"));
        dismiss.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent e) {
                setVisible(false);
            }
        });
        buttons.add(dismiss);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        
        setSize(300, 200);
        setResizable(false);
    }
    
    /**
     * Initializes this panel based on the state of the supplied board.
     */
    public void fromBoard (BangBoard board)
    {
        int level = board.getWaterLevel();
        boolean enable = (level != -128);
        _enable.setSelected(enable);
        _level.setEnabled(enable);
        _level.setValue(enable ? level : 0);
    }
    
    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent e)
    {
        boolean enable = _enable.isSelected();
        _panel.view.setWaterLevel(enable ? _level.getValue() : -128);
        _level.setEnabled(enable);
    }
    
    /** The application context. */
    protected EditorContext _ctx;
    
    /** The containing panel. */
    protected EditorPanel _panel;
    
    /** The enable/disable water checkbox. */
    protected JCheckBox _enable;
    
    /** The water level slider. */
    protected JSlider _level;
}
