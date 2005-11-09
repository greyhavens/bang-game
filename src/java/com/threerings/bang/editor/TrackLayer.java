//
// $Id$

package com.threerings.bang.editor;

import java.awt.Point;

import javax.swing.JPanel;

import com.jme.math.Vector3f;

import com.jmex.bui.event.MouseEvent;

/**
 * Allows the user to lay train tracks across the terrain.
 */
public class TrackLayer extends EditorTool
{
    /** The name of this tool. */
    public static final String NAME = "track_layer";
    
    public TrackLayer (EditorContext ctx, EditorPanel panel)
    {
        super(ctx, panel);
    }
    
    // documentation inherited
    public String getName ()
    {
        return NAME;
    }
    
    @Override // documentation inherited
    public void mousePressed (MouseEvent e)
    {
        if (e.getButton() == MouseEvent.BUTTON1) {
            _laying = true;
            EditorController.postAction(_panel, EditorController.LAY_TRACK,
                _hoverTile);
            
        } else if (e.getButton() == MouseEvent.BUTTON2) {
            _removing = true;
            EditorController.postAction(_panel, EditorController.REMOVE_TRACK,
                _hoverTile);
        }
    }
    
    @Override // documentation inherited
    public void mouseReleased (MouseEvent e)
    {
        _laying = _removing = false;
    }
    
    @Override // documentation inherited
    public void hoverTileChanged (int tx, int ty)
    {
        _hoverTile.setLocation(tx, ty);
        
        if (_laying) {
            EditorController.postAction(_panel, EditorController.LAY_TRACK,
                _hoverTile);
                
        } else if (_removing) {
            EditorController.postAction(_panel, EditorController.REMOVE_TRACK,
                _hoverTile);
        }
    }
    
    // documentation inherited
    protected JPanel createOptions ()
    {
        return new JPanel();
    }
    
    /** The location of the mouse pointer in tile coordinates. */
    protected Point _hoverTile = new Point(-1, -1);
    
    /** Whether or not we are currently laying or removing track. */
    protected boolean _laying, _removing;
}
