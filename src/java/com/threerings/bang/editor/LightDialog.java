//
// $Id$

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
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
 * Allows the user to edit the board's light configuration.
 */
public class LightDialog extends JDialog
    implements ChangeListener
{
    public LightDialog (EditorContext ctx, EditorPanel panel)
    {
        super(ctx.getFrame(), ctx.xlate("editor", "t.light_dialog"), true);
        _ctx = ctx;
        _panel = panel;
    
        JPanel center = new JPanel(new VGroupLayout());
        
        JPanel apanel = new JPanel();
        apanel.add(new JLabel(_ctx.xlate("editor", "m.light_azimuth")));
        apanel.add(_azimuth = new JSlider(-180, +180, 0));
        _azimuth.addChangeListener(this);
        center.add(apanel);
        
        JPanel epanel = new JPanel();
        epanel.add(new JLabel(_ctx.xlate("editor", "m.light_elevation")));
        epanel.add(_elevation = new JSlider(0, 90, 45));
        _elevation.addChangeListener(this);
        center.add(epanel);
        
        JPanel dcpanel = new JPanel();
        dcpanel.add(new JLabel(_ctx.xlate("editor", "m.light_diffuse_color")));
        dcpanel.add(_diffuseColor = new JButton("    "));
        _diffuseColor.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent e) {
                Color chosen = JColorChooser.showDialog(LightDialog.this,
                    _ctx.xlate("editor", "m.light_diffuse_color"),
                    _diffuseColor.getBackground());
                if (chosen != null) {
                    _diffuseColor.setBackground(chosen);
                    stateChanged(null);
                }
            }
        });
        center.add(dcpanel);
        
        JPanel acpanel = new JPanel();
        acpanel.add(new JLabel(_ctx.xlate("editor", "m.light_ambient_color")));
        acpanel.add(_ambientColor = new JButton("    "));
        _ambientColor.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent e) {
                Color chosen = JColorChooser.showDialog(LightDialog.this,
                    _ctx.xlate("editor", "m.light_ambient_color"),
                    _ambientColor.getBackground());
                if (chosen != null) {
                    _ambientColor.setBackground(chosen);
                    stateChanged(null);
                }
            }
        });
        center.add(acpanel);
        
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
        _azimuth.setValue((int)Math.toDegrees(board.getLightAzimuth()));
        _elevation.setValue((int)Math.toDegrees(board.getLightElevation()));
        _diffuseColor.setBackground(new Color(board.getLightDiffuseColor()));
        _ambientColor.setBackground(new Color(board.getLightAmbientColor()));
    }
    
    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent e)
    {
        _panel.view.setLightParams(_azimuth.getValue() * FastMath.DEG_TO_RAD,
            _elevation.getValue() * FastMath.DEG_TO_RAD,
            _diffuseColor.getBackground().getRGB(),
            _ambientColor.getBackground().getRGB());
    }
    
    /** The application context. */
    protected EditorContext _ctx;
    
    /** The containing panel. */
    protected EditorPanel _panel;
    
    /** The azimuth and elevation sliders. */
    protected JSlider _azimuth, _elevation;
    
    /** The diffuse and ambient color buttons. */
    protected JButton _diffuseColor, _ambientColor;
}
