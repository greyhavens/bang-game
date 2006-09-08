//
// $Id$

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
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
        
        JPanel lpanel = new JPanel();
        lpanel.add(new JLabel(_ctx.xlate("editor", "m.water_level")));
        lpanel.add(_level = new JSlider(-128, +127, 0));
        _level.addChangeListener(this);
        center.add(lpanel);
        
        JPanel apanel = new JPanel();
        apanel.add(new JLabel(_ctx.xlate("editor", "m.wave_amplitude")));
        apanel.add(_amplitude = new JSlider(0, 500, 25));
        _amplitude.addChangeListener(this);
        center.add(apanel);
        
        center.add(_color = new ColorPanel(ctx, "m.color"));
        _color.addChangeListener(this);
        
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
        
        setSize(350, 250);
        setResizable(false);
    }
    
    /**
     * Initializes this panel based on the state of the supplied board.
     */
    public void fromBoard (BangBoard board)
    {
        _color.setRGB(board.getWaterColor());
        _level.setValue(board.getWaterLevel());
        _amplitude.setValue((int)board.getWaterAmplitude());
    }
    
    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent e)
    {
        if (!isShowing()) {
            return; // invoked from fromBoard
        }
        _panel.view.setWaterParams(_level.getValue(), _color.getRGB(),
            _amplitude.getValue(), true);
        if (!_level.getValueIsAdjusting() &&
            !_amplitude.getValueIsAdjusting()) {
            _panel.view.commitWaterEdit();
            _panel.view.refreshBoard();
        }
    }
    
    /** The application context. */
    protected EditorContext _ctx;
    
    /** The containing panel. */
    protected EditorPanel _panel;
    
    /** The water level slider. */
    protected JSlider _level;
    
    /** The wave amplitude slider. */
    protected JSlider _amplitude;
    
    /** The color panel. */
    public ColorPanel _color;
}
