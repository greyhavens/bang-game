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

import com.jme.math.FastMath;

import com.samskivert.swing.VGroupLayout;

import com.threerings.bang.game.data.BangBoard;

/**
 * Allows the user to edit the board's environment configuration.
 */
public class EnvironmentDialog extends JDialog
    implements ChangeListener
{
    public EnvironmentDialog (EditorContext ctx, EditorPanel panel)
    {
        super(ctx.getFrame(), ctx.xlate("editor", "t.environment_dialog"),
            true);
        _ctx = ctx;
        _panel = panel;
    
        JPanel center = new JPanel(new VGroupLayout());
        
        JPanel dpanel = new JPanel();
        dpanel.add(new JLabel(_ctx.xlate("editor", "m.wind_direction")));
        dpanel.add(_direction = new JSlider(-180, +180, 0));
        _direction.addChangeListener(this);
        center.add(dpanel);
        
        JPanel spanel = new JPanel();
        spanel.add(new JLabel(_ctx.xlate("editor", "m.wind_speed")));
        spanel.add(_speed = new JSlider(0, 500, 20));
        _speed.addChangeListener(this);
        center.add(spanel);
        
        center.add(_fogColor = new ColorPanel(ctx, "m.fog_color"));
        _fogColor.addChangeListener(this);
        
        center.add(_fogDensity = new ValuePanel(_ctx.xlate("editor",
            "m.fog_density"), 0, 100, 0));
        _fogDensity.addChangeListener(this);
        
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
        
        setSize(400, 200);
        setResizable(false);
    }
    
    /**
     * Initializes this panel based on the state of the supplied board.
     */
    public void fromBoard (BangBoard board)
    {
        _direction.setValue((int)Math.toDegrees(board.getWindDirection()));
        _speed.setValue((int)board.getWindSpeed());
        _fogColor.setRGB(board.getFogColor());
        _fogDensity.setValue((int)(board.getFogDensity() * 5000));
    }
    
    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent e)
    {
        if (!isShowing()) {
            return; // invoked from fromBoard
        }
        _panel.view.setWindParams(_direction.getValue() * FastMath.DEG_TO_RAD,
            _speed.getValue(), true);
        if (!_direction.getValueIsAdjusting() &&
            !_speed.getValueIsAdjusting()) {
            _panel.view.commitWindEdit();
        }
        _panel.view.setFogParams(_fogColor.getRGB(),
            _fogDensity.getValue() / 5000f, true);
        if (!_fogDensity.getValueIsAdjusting()) {
            _panel.view.commitFogEdit();
        }
    }
    
    /** The application context. */
    protected EditorContext _ctx;
    
    /** The containing panel. */
    protected EditorPanel _panel;
    
    /** The wind direction slider. */
    protected JSlider _direction;
    
    /** The wind speed slider. */
    protected JSlider _speed;
    
    /** The fog color panel. */
    public ColorPanel _fogColor;
    
    /** The fog density slider. */
    protected ValuePanel _fogDensity;
}
