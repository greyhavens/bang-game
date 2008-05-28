//
// $Id$

package com.threerings.bang.editor;

import java.awt.Point;

import java.util.HashSet;

import javax.swing.JPanel;

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
            _edit = new TrackLaid();
            _edit.processPoint(_hoverTile);
            
        } else if (e.getButton() == MouseEvent.BUTTON2) {
            _edit = new TrackRemoved();
            _edit.processPoint(_hoverTile);
        }
    }
    
    @Override // documentation inherited
    public void mouseReleased (MouseEvent e)
    {
        if (_edit == null) {
            return;
        }
        _edit.commit();
        _edit = null;
    }
    
    @Override // documentation inherited
    public void hoverTileChanged (int tx, int ty)
    {
        _hoverTile.setLocation(tx, ty);
        if (_edit != null) {
            _edit.processPoint(_hoverTile);
        }
    }
    
    // documentation inherited
    protected JPanel createOptions ()
    {
        return new JPanel();
    }
    
    /** The superclass of edits to the train tracks. */
    protected abstract class TrackEdit
        implements EditorController.Edit
    {
        /** Processes the specified point for this edit. */
        public abstract void processPoint (Point point);
        
        /** Commits this edit to the undo buffer. */
        public void commit ()
        {
            if (_points.size() > 0) {
                _ctrl.addEdit(this);
            }
        }

        /** Adds the points removed. */
        protected void layTracks ()
        {
            for (Point point : _points) {
                _ctrl.layTrack(point);
            }
        }
        
        /** Removes the points added. */
        protected void removeTracks ()
        {
            for (Point point : _points) {
                _ctrl.removeTrack(point);
            }
        }
        
        /** The affected locations. */
        protected HashSet<Point> _points = new HashSet<Point>();
    }
    
    /** An edit where track was laid. */
    protected class TrackLaid extends TrackEdit
    {
        // documentation inherited
        public void processPoint (Point point)
        {
            if (_ctrl.layTrack(point)) {
                _points.add((Point)point.clone());
            }
        }
        
        // documentation inherited from interface EditorController.Edit
        public void undo ()
        {
            removeTracks();
        }
        
        // documentation inherited from interface EditorController.Edit
        public void redo ()
        {
            layTracks();
        }
    }
    
    /** An edit where track was removed. */
    protected class TrackRemoved extends TrackEdit
    {
        // documentation inherited
        public void processPoint (Point point)
        {
            if (_ctrl.removeTrack(point)) {
                _points.add((Point)point.clone());
            }
        }
        
        // documentation inherited from interface EditorController.Edit
        public void undo ()
        {
            layTracks();
        }
        
        // documentation inherited from interface EditorController.Edit
        public void redo ()
        {
            removeTracks();
        }
    }
    
    /** The location of the mouse pointer in tile coordinates. */
    protected Point _hoverTile = new Point(-1, -1);
    
    /** The current track edit, if any. */
    protected TrackEdit _edit;
}
