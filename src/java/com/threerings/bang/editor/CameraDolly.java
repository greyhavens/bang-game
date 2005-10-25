//
// $Id$

package com.threerings.bang.editor;

import javax.swing.JPanel;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;

import com.jmex.bui.event.MouseEvent;

/**
 * Allows the user to move the camera around the board.
 */
public class CameraDolly extends EditorTool
{
    /** The name of this tool. */
    public static final String NAME = "camera_dolly";
    
    public CameraDolly (EditorContext ctx, EditorPanel panel)
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
        _lastX = e.getX();
        _lastY = e.getY();
        _lastButton = e.getButton();
    }
    
    @Override // documentation inherited
    public void mouseDragged (MouseEvent e)
    {
        int dx = _lastX - e.getX(), dy =  _lastY - e.getY();
        Position pos = getCameraPosition();
        switch(_lastButton) {
            case MouseEvent.BUTTON1: // left rotates
                pos.azimuth += dx*ANGULAR_SCALE;
                pos.elevation = Math.min(Math.max(pos.elevation +
                    dy*ANGULAR_SCALE, MIN_ELEVATION), MAX_ELEVATION);
                break;
                
            case MouseEvent.BUTTON2: // right "zooms"
                pos.distance = Math.min(Math.max(pos.distance +
                    dy * LINEAR_SCALE, MIN_DISTANCE), MAX_DISTANCE);
                break;
        }
        setCameraPosition(pos);
        
        _lastX = e.getX();
        _lastY = e.getY();
    }
    
    @Override // documentation inherited
    public void mouseWheeled (MouseEvent e)
    {
        Position pos = getCameraPosition();
        pos.distance = Math.min(Math.max(pos.distance + e.getDelta() * 10 *
            LINEAR_SCALE, MIN_DISTANCE), MAX_DISTANCE);
        setCameraPosition(pos);
    }
    
    // documentation inherited
    protected JPanel createOptions ()
    {
        return new JPanel();
    }
    
    /**
     * Gets the azimuth, elevation, and distance from the camera.
     */
    protected Position getCameraPosition ()
    {
        // find out what we're looking at if it's not already set
        if (_target == null) {
            _target = _panel.view.getTerrainNode().getWorldBound().getCenter();
            _target.z = 0.0f;
        }
        
        // use the vector from target to camera to determine position
        Vector3f vec = _ctx.getCamera().getLocation().subtract(_target);
        float distance = vec.length();
        vec.normalizeLocal();
        return new Position(FastMath.atan2(vec.y, vec.x), FastMath.asin(vec.z),
            distance);
    }
    
    /**
     * Sets the camera's position based on the current parameters.
     */
    protected void setCameraPosition (Position pos)
    {
        // determine the vector from target to camera
        float cosel = FastMath.cos(pos.elevation);
        Vector3f vec = new Vector3f(FastMath.cos(pos.azimuth)*cosel,
            FastMath.sin(pos.azimuth)*cosel, FastMath.sin(pos.elevation));
        Vector3f loc = vec.mult(pos.distance).addLocal(_target),
            dir = vec.negate(),
            left = Vector3f.UNIT_Z.cross(dir).normalize(),
            up = dir.cross(left);
            
        _ctx.getCamera().setFrame(loc, left, up, dir);
    }
    
    /** Represents a camera position in spherical coordinates about the
     * target. */
    protected static class Position
    {
        public float azimuth, elevation, distance;
        
        public Position (float azimuth, float elevation, float distance)
        {
            this.azimuth = azimuth;
            this.elevation = elevation;
            this.distance = distance;
        }
    }
    
    /** The last mouse coordinates and button pressed. */
    protected int _lastX, _lastY, _lastButton;
    
    /** The point at which the camera is looking. */
    protected Vector3f _target;
 
    /** The angular scale (radians per pixel). */
    protected static final float ANGULAR_SCALE = FastMath.PI / 1000;
       
    /** The linear scale (world units per pixel). */
    protected static final float LINEAR_SCALE = 1.0f; 
    
    /** The minimum distance from the target. */
    protected static final float MIN_DISTANCE = 50.0f;
    
    /** The maximum distance from the target. */
    protected static final float MAX_DISTANCE = 300.0f;
    
    /** The minimum elevation. */
    protected static final float MIN_ELEVATION = FastMath.PI / 16.0f; 
    
    /** The maximum elevation. */
    protected static final float MAX_ELEVATION = FastMath.PI * 7.0f / 16.0f;
}
