//
// $Id$

package com.threerings.bang.editor;

import javax.swing.JPanel;

import com.jme.input.KeyInput;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.scene.Controller;

import com.jmex.bui.event.KeyEvent;
import com.jmex.bui.event.KeyListener;
import com.jmex.bui.event.MouseEvent;

/**
 * Allows the user to move the camera around the board.
 */
public class CameraDolly extends EditorTool
    implements KeyListener
{
    /** The name of this tool. */
    public static final String NAME = "camera_dolly";
    
    public CameraDolly (EditorContext ctx, EditorPanel panel)
    {
        super(ctx, panel);
        _panel.view.addListener(this);
        
        ctx.getRootNode().addController(new Controller() {
            public void update (float time) {
                updateCamera(time);
            }
        });
    }
    
    // documentation inherited
    public String getName ()
    {
        return NAME;
    }
    
    @Override // documentation inherited
    public void activate ()
    {
        _active = true;
    }
    
    @Override // documentation inherited
    public void deactivate ()
    {
        _active = false;
    }
    
    @Override // documentation inherited
    public void mousePressed (MouseEvent e)
    {
        if (!_active) {
            return;
        }
        
        _lastX = e.getX();
        _lastY = e.getY();
        _lastButton = e.getButton();
    }
    
    @Override // documentation inherited
    public void mouseDragged (MouseEvent e)
    {
        if (!_active) {
            return;
        }
        
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
        if (!_active) {
            return;
        }
        
        Position pos = getCameraPosition();
        pos.distance = Math.min(Math.max(pos.distance + e.getDelta() * 10 *
            LINEAR_SCALE, MIN_DISTANCE), MAX_DISTANCE);
        setCameraPosition(pos);
    }
    
    // documentation inherited from interface KeyListener
    public void keyPressed (KeyEvent e)
    {
        int code = e.getKeyCode();
        switch (code) {
             case KeyInput.KEY_Q: _dvel = +LINEAR_SPEED; _dcode = code; break;
             case KeyInput.KEY_W: _evel = +ANGULAR_SPEED; _ecode = code; break;
             case KeyInput.KEY_E: _dvel = -LINEAR_SPEED; _dcode = code; break;
             case KeyInput.KEY_A: _avel = -ANGULAR_SPEED; _acode = code; break;
             case KeyInput.KEY_S: _evel = -ANGULAR_SPEED; _ecode = code; break;
             case KeyInput.KEY_D: _avel = +ANGULAR_SPEED; _acode = code; break;
        }
    }
    
    // documentation inherited from interface KeyListener
    public void keyReleased (KeyEvent e)
    {
        int code = e.getKeyCode();
        switch (code) {
             case KeyInput.KEY_Q: _dvel = (_dcode == code) ? 0f : _dvel; break;
             case KeyInput.KEY_W: _evel = (_ecode == code) ? 0f : _evel; break;
             case KeyInput.KEY_E: _dvel = (_dcode == code) ? 0f : _dvel; break;
             case KeyInput.KEY_A: _avel = (_acode == code) ? 0f : _avel; break;
             case KeyInput.KEY_S: _evel = (_ecode == code) ? 0f : _evel; break;
             case KeyInput.KEY_D: _avel = (_acode == code) ? 0f : _avel; break;
        }
    }
    
    // documentation inherited
    protected JPanel createOptions ()
    {
        return new JPanel();
    }
    
    /**
     * Updates the position of the camera since the elapsed time.
     */
    protected void updateCamera (float time)
    {
        Position pos = getCameraPosition();
        pos.azimuth += _avel * time;
        pos.elevation = Math.min(Math.max(pos.elevation + _evel*time,
            MIN_ELEVATION), MAX_ELEVATION);
        pos.distance = Math.min(Math.max(pos.distance + _dvel*time,
            MIN_DISTANCE), MAX_DISTANCE);
        setCameraPosition(pos);
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
 
    /** Whether or not the tool is currently active. */
    protected boolean _active;
    
    /** Camera azimuth, elevation, and distance velocities. */
    protected float _avel, _evel, _dvel;
    
    /** The last keys pressed for azimuth, elevation, and distance. */
    protected int _acode, _ecode, _dcode;
    
    /** The angular scale (radians per pixel). */
    protected static final float ANGULAR_SCALE = FastMath.PI / 1000;
    
    /** The angular speed (radians per second). */
    protected static final float ANGULAR_SPEED = FastMath.PI / 2;
    
    /** The linear scale (world units per pixel). */
    protected static final float LINEAR_SCALE = 1.0f; 
    
    /** The linear speed (world units per second). */
    protected static final float LINEAR_SPEED = 300f;
    
    /** The minimum distance from the target. */
    protected static final float MIN_DISTANCE = 50.0f;
    
    /** The maximum distance from the target. */
    protected static final float MAX_DISTANCE = 500.0f;
    
    /** The minimum elevation. */
    protected static final float MIN_ELEVATION = FastMath.PI / 16.0f; 
    
    /** The maximum elevation. */
    protected static final float MAX_ELEVATION = FastMath.PI * 7.0f / 16.0f;
}
