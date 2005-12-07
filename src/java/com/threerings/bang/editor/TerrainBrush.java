//
// $Id$

package com.threerings.bang.editor;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jme.math.Vector3f;

import com.jmex.bui.event.MouseEvent;

import com.samskivert.swing.HGroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.threerings.bang.game.client.TerrainNode;

/**
 * Allows the user to paint heightfield vertices with different types of
 * terrain.
 */
public class TerrainBrush extends EditorTool
{
    /** The name of this tool. */
    public static final String NAME = "terrain_brush";
    
    public TerrainBrush (EditorContext ctx, EditorPanel panel)
    {
        super(ctx, panel);
        _cursor = panel.view.getTerrainNode().createCursor();
        _cursor.radius = DEFAULT_CURSOR_RADIUS;
    }
    
    // documentation inherited
    public String getName ()
    {
        return NAME;
    }
    
    @Override // documentation inherited
    public void activate ()
    {
        super.activate();
        _panel.view.getNode().attachChild(_cursor);
    }
    
    @Override // documentation inherited
    public void deactivate ()
    {
        super.deactivate();
        _panel.view.getNode().detachChild(_cursor);
    }
    
    @Override // documentation inherited
    public void mousePressed (MouseEvent e)
    {
        _panel.view.paintTerrain(_cursor.x, _cursor.y, _cursor.radius,
            _tbopts.selector.getSelectedTerrain());
    }
    
    @Override // documentation inherited
    public void mouseMoved (MouseEvent e)
    {
        Vector3f ground = _panel.view.getGroundIntersect(e, true, null);
        _cursor.setPosition(ground.x, ground.y);
    }
    
    @Override // documentation inherited
    public void mouseDragged (MouseEvent e)
    {
        mouseMoved(e);
        _panel.view.paintTerrain(_cursor.x, _cursor.y, _cursor.radius,
            _tbopts.selector.getSelectedTerrain());
    }
    
    @Override // documentation inherited
    public void mouseWheeled (MouseEvent e)
    {
        _cursor.setRadius(clamp(_cursor.radius + e.getDelta(),
            MIN_CURSOR_RADIUS, MAX_CURSOR_RADIUS));
        _tbopts.sizer.setValue((int)_cursor.radius);
    }
    
    // documentation inherited
    protected JPanel createOptions ()
    {
        return (_tbopts = new TerrainBrushOptions());
    }
    
    /**
     * Clamps v between a and b.
     */
    protected static float clamp (float v, float a, float b)
    {
        return Math.min(Math.max(v, a), b);
    }
    
    /** The options for this panel. */
    protected class TerrainBrushOptions extends JPanel
        implements ChangeListener
    {
        public TerrainSelector selector;
        public JSlider sizer;
        
        public TerrainBrushOptions ()
        {
            super(new VGroupLayout(VGroupLayout.NONE, VGroupLayout.TOP));
            setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            
            add(selector = new TerrainSelector(_ctx));
            
            JPanel spanel = new JPanel();
            spanel.add(new JLabel(_msgs.get("m.brush_size")));
            spanel.add(sizer = new JSlider(MIN_CURSOR_RADIUS,
                MAX_CURSOR_RADIUS, DEFAULT_CURSOR_RADIUS));
            sizer.addChangeListener(this);
            sizer.setPreferredSize(new Dimension(70,
                sizer.getPreferredSize().height));
            add(spanel);
        }
        
        public void stateChanged (ChangeEvent e)
        {
            _cursor.setRadius(sizer.getValue());
        }
    }
    
    /** The terrain cursor. */
    protected TerrainNode.Cursor _cursor;
    
    /** The casted options panel. */
    protected TerrainBrushOptions _tbopts;
    
    /** The minimum cursor radius. */
    protected static final int MIN_CURSOR_RADIUS = 1;
    
    /** The default cursor radius. */
    protected static final int DEFAULT_CURSOR_RADIUS = 5;
    
    /** The maximum cursor radius. */
    protected static final int MAX_CURSOR_RADIUS = 50;
}
