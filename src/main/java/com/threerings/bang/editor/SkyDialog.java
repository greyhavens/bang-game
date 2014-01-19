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
 * Allows the user to edit the board's sky configuration.
 */
public class SkyDialog extends JDialog
    implements ChangeListener
{
    public SkyDialog (EditorContext ctx, EditorPanel panel)
    {
        super(ctx.getFrame(), ctx.xlate("editor", "t.sky_dialog"), true);
        _ctx = ctx;
        _panel = panel;
    
        JPanel center = new JPanel(new VGroupLayout());
        
        center.add(_horizonColor = new ColorPanel(ctx, "m.horizon_color"));
        _horizonColor.addChangeListener(this);
        
        center.add(_overheadColor = new ColorPanel(ctx, "m.overhead_color"));
        _overheadColor.addChangeListener(this);
        
        JPanel fpanel = new JPanel();
        fpanel.add(new JLabel(_ctx.xlate("editor", "m.falloff")));
        fpanel.add(_falloff = new JSlider(0, 100, 10));
        _falloff.addChangeListener(this);
        center.add(fpanel);
        
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
        _horizonColor.setRGB(board.getSkyHorizonColor());
        _overheadColor.setRGB(board.getSkyOverheadColor());
        _falloff.setValue((int)board.getSkyFalloff());
    }
    
    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent e)
    {
        if (!isShowing()) {
            return; // invoked from fromBoard
        }
        _panel.view.setSkyParams(_horizonColor.getRGB(),
            _overheadColor.getRGB(), _falloff.getValue(), true);
        if (!_falloff.getValueIsAdjusting()) {
            _panel.view.commitSkyEdit();
        }
    }
    
    /** The application context. */
    protected EditorContext _ctx;
    
    /** The containing panel. */
    protected EditorPanel _panel;
    
    /** The horizon and overhead color panels. */
    public ColorPanel _horizonColor, _overheadColor;
    
    /** The falloff slider. */
    protected JSlider _falloff;
}
