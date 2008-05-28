//
// $Id$

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jme.math.FastMath;

import com.samskivert.swing.VGroupLayout;

import com.threerings.util.MessageBundle;

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
        
        _lpanels = new LightPanel[BangBoard.NUM_LIGHTS];
        for (int i = 0; i < _lpanels.length; i++) {
            center.add(_lpanels[i] = new LightPanel(i));
        }
        
        center.add(_shadow = new ValuePanel(_ctx.xlate("editor",
            "m.shadow_intensity"), 0, 100, 100));
        _shadow.addChangeListener(this);
        
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
        
        setSize(425, 450);
        setResizable(false);
    }
    
    /**
     * Initializes this panel based on the state of the supplied board.
     */
    public void fromBoard (BangBoard board)
    {
        for (int i = 0; i < _lpanels.length; i++) {
            _lpanels[i].fromBoard(board);
        }
        _shadow.setValue((int)(board.getShadowIntensity() * 100f));
    }
    
    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent e)
    {
        if (!isShowing()) {
            return; // invoked from fromBoard
        }
        _panel.view.setShadowIntensity(_shadow.getValue() / 100f, true);
        if (!_shadow.getValueIsAdjusting()) {
            _panel.view.commitShadowIntensityEdit();
        }
    }
    
    /** Controls the parameters for a single light. */
    protected class LightPanel extends JPanel
        implements ChangeListener
    {
        public int idx;
        public ValuePanel azimuth, elevation;
        public ColorPanel diffuseColor, ambientColor;
        
        public LightPanel (int idx)
        {
            super(new VGroupLayout());
            this.idx = idx;
            
            setBorder(BorderFactory.createTitledBorder(null,
                _ctx.xlate("editor", MessageBundle.tcompose("m.light_name",
                    new Integer(idx)))));
            
            add(azimuth = new ValuePanel(
                _ctx.xlate("editor", "m.light_azimuth"), -180, +180, 0));
            azimuth.addChangeListener(this);
            
            add(elevation = new ValuePanel(
                _ctx.xlate("editor", "m.light_elevation"), -90, 90, 45));
            elevation.addChangeListener(this);
        
            add(diffuseColor = new ColorPanel(_ctx, "m.diffuse_color"));
            diffuseColor.addChangeListener(this);
            
            add(ambientColor = new ColorPanel(_ctx, "m.ambient_color"));
            ambientColor.addChangeListener(this);
        }
        
        public void fromBoard (BangBoard board)
        {
            diffuseColor.setRGB(board.getLightDiffuseColor(idx));
            ambientColor.setRGB(board.getLightAmbientColor(idx));
            azimuth.setValue(
                (int)Math.toDegrees(board.getLightAzimuth(idx)));
            elevation.setValue(
                (int)Math.toDegrees(board.getLightElevation(idx)));   
        }
        
        public void stateChanged (ChangeEvent e)
        {
            if (!isShowing()) {
                return; // invoked from fromBoard
            }
            _panel.view.setLightParams(idx,
                azimuth.getValue() * FastMath.DEG_TO_RAD,
                elevation.getValue() * FastMath.DEG_TO_RAD,
                diffuseColor.getRGB(), ambientColor.getRGB(), true);
            if (!azimuth.getValueIsAdjusting() &&
                !elevation.getValueIsAdjusting()) {
                _panel.view.commitLightEdit(idx);
            }
        }
    }
    
    /** The application context. */
    protected EditorContext _ctx;
    
    /** The containing panel. */
    protected EditorPanel _panel;
    
    /** The panels for each light. */
    protected LightPanel[] _lpanels;
    
    /** The shadow intensity slider. */
    protected ValuePanel _shadow;
}
