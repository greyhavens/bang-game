//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Rectangle;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;

import com.threerings.jme.camera.CameraHandler;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Allows the camera's ground point to follow a heavily smoothed version of the
 * board terrain.
 */
public class GameCameraHandler extends CameraHandler
{
    public GameCameraHandler (Camera camera)
    {
        super(camera);
    }
    
    /**
     * Initializes the smoothed ground heightfield.
     */ 
    public void prepareForRound (BangView view, BangObject bangobj)
    {
        Rectangle area = bangobj.board.getPlayableArea();
        _parea.setBounds(area.x * BangBoard.HEIGHTFIELD_SUBDIVISIONS,
            area.y * BangBoard.HEIGHTFIELD_SUBDIVISIONS,
            area.width * BangBoard.HEIGHTFIELD_SUBDIVISIONS + 1,
            area.height * BangBoard.HEIGHTFIELD_SUBDIVISIONS + 1);
        _heightfield = new float[_parea.height][_parea.width];
        TerrainNode tnode = view.view.getTerrainNode();
        for (int ii = 0; ii < _parea.height; ii++) {
            for (int jj = 0; jj < _parea.width; jj++) {
                _heightfield[ii][jj] = computeSmoothedHeight(tnode,
                    _parea.x + jj, _parea.y + ii);
            }
        }
        _minGroundX = area.x * TILE_SIZE;
        _minGroundY = area.y * TILE_SIZE;
        _maxGroundX = _minGroundX + area.width * TILE_SIZE;
        _maxGroundY = _minGroundY + area.height * TILE_SIZE;
    }

    /**
     * Clears out the heightfield.
     */
    public void endRound ()
    {
        _heightfield = null;
    }
    
    /**
     * Sets the z value of the ground point to the smoothed terrain height at
     * the camera's location.  The value will be updated when the camera is
     * panned.
     */
    public void resetGroundPointHeight ()
    {
        Vector3f camloc = _camera.getLocation();
        _groundz = getSmoothedHeight(camloc.x, camloc.y);
    }
    
    @Override // documentation inherited
    public void panCamera (float x, float y)
    {
        _rxdir.mult(x, _temp);
        _temp.scaleAdd(y, _rydir, _temp);
        panCameraAbs(_temp.x, _temp.y);
    }
    
    /**
     * Pans the camera in the x and/or y directions without regard to the
     * direction the camera is facing.
     */
    public void panCameraAbs (float x, float y)
    {
        getGroundPoint(_gpoint);
        float nx = Math.min(Math.max(_gpoint.x + x, _minGroundX), _maxGroundX),
            ny = Math.min(Math.max(_gpoint.y + y, _minGroundY), _maxGroundY);
        _groundz = getSmoothedHeight(nx, ny);
        setLocation(_camera.getLocation().addLocal(nx - _gpoint.x,
            ny - _gpoint.y, _groundz - _gpoint.z));
    }
    
    @Override // documentation inherited
    public Vector3f getGroundPoint ()
    {
        return getGroundPoint(null);
    }
    
    /**
     * Computes the smoothed height at the given location in world coordinates
     * by interpolating between the nearest heightfield values.
     */
    public float getSmoothedHeight (float x, float y)
    {
        if (_heightfield == null) {
            return 0f;
        }
        
        // scale down to sub-tile coordinates
        float stscale = BangBoard.HEIGHTFIELD_SUBDIVISIONS / TILE_SIZE;
        x *= stscale;
        y *= stscale;

        // sample at the four closest points and find the fractional components
        int fx = (int)FastMath.floor(x), cx = (int)FastMath.ceil(x),
            fy = (int)FastMath.floor(y), cy = (int)FastMath.ceil(y);
        float ff = getSmoothedHeight(fx, fy),
            fc = getSmoothedHeight(fx, cy),
            cf = getSmoothedHeight(cx, fy),
            cc = getSmoothedHeight(cx, cy),
            ax = x - fx, ay = y - fy;

        return FastMath.LERP(ax, FastMath.LERP(ay, ff, fc),
            FastMath.LERP(ay, cf, cc));
    }
    
    /**
     * Returns the smoothed height at the specified sub-tile coordinates.
     */
    protected float getSmoothedHeight (int x, int y)
    {
        // clamp to the playable area
        x = Math.min(Math.max(x, _parea.x), _parea.x + _parea.width - 1);
        y = Math.min(Math.max(y, _parea.y), _parea.y + _parea.height - 1);
        return _heightfield[y - _parea.y][x - _parea.x];
    }
    
    /**
     * Computes and returns the smoothed height at the given location in
     * sub-tile coordinates.
     */
    protected float computeSmoothedHeight (TerrainNode tnode, int x, int y)
    {
        float sum = 0;
        for (int ii = -SMOOTH_EXTENT; ii <= +SMOOTH_EXTENT; ii++) {
            for (int jj = -SMOOTH_EXTENT; jj <= +SMOOTH_EXTENT; jj++) {
                sum += tnode.getHeightfieldValue(x + jj, y + ii);
            }
        }
        return sum / SMOOTH_AREA;
    }

    /**
     * Gets the point on the ground to which the camera is pointing, storing
     * the result in the given vector if it's non-null.
     */
    protected Vector3f getGroundPoint(Vector3f result)
    {
        if (result == null) {
            result = new Vector3f();
        }
        float dist = -1f * (_camera.getLocation().z - _groundz) /
            _ground.normal.dot(_camera.getDirection());
        result.scaleAdd(dist, _camera.getDirection(), _camera.getLocation());
        return result;
    }
    
    /** The heightfield containing a the ground terrain for the play area. */
    protected float[][] _heightfield;
    
    /** The playable area of the board in sub-tile coordinates. */
    protected Rectangle _parea = new Rectangle();
    
    /** The limits of the ground point. */
    protected float _minGroundX, _minGroundY, _maxGroundX, _maxGroundY;
    
    /** The z value of the ground plane. */
    protected float _groundz;
    
    /** A temporary ground point value. */
    protected Vector3f _gpoint = new Vector3f();
    
    /** The extent of the smoothing region. */
    protected static final int SMOOTH_EXTENT = 8;
    
    /** The number of points in the smoothing region. */
    protected static final int SMOOTH_AREA =
        (int)FastMath.sqr(SMOOTH_EXTENT*2 + 1);
}
