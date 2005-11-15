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
        
        center.add(_diffuseColor = new ColorPanel(ctx, "m.diffuse_color"));
        _diffuseColor.addChangeListener(this);
        
        center.add(_ambientColor = new ColorPanel(ctx, "m.ambient_color"));
        _ambientColor.addChangeListener(this);
        
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
        _diffuseColor.setRGB(board.getWaterDiffuseColor());
        _ambientColor.setRGB(board.getWaterAmbientColor());
        _level.setValue(board.getWaterLevel());
    }
    
    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent e)
    {
        _panel.view.setWaterParams(_level.getValue(), _diffuseColor.getRGB(),
            _ambientColor.getRGB());
    }
    
    /** The application context. */
    protected EditorContext _ctx;
    
    /** The containing panel. */
    protected EditorPanel _panel;
    
    /** The water level slider. */
    protected JSlider _level;
    
    /** The diffuse and ambient color panels. */
    public ColorPanel _diffuseColor, _ambientColor;
}
