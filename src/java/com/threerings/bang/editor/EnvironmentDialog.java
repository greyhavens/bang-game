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
        
        setSize(350, 200);
        setResizable(false);
    }
    
    /**
     * Initializes this panel based on the state of the supplied board.
     */
    public void fromBoard (BangBoard board)
    {
        _direction.setValue((int)Math.toDegrees(board.getWindDirection()));
        _speed.setValue((int)board.getWindSpeed());
    }
    
    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent e)
    {
        _panel.view.setWindParams(_direction.getValue() * FastMath.DEG_TO_RAD,
            _speed.getValue());
    }
    
    /** The application context. */
    protected EditorContext _ctx;
    
    /** The containing panel. */
    protected EditorPanel _panel;
    
    /** The wind direction slider. */
    protected JSlider _direction;
    
    /** The wind speed slider. */
    protected JSlider _speed;
}
