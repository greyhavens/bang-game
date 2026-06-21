//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.jme3.math.Vector3f;

import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

import com.threerings.jme.sprite.Path;

import com.threerings.bang.util.BasicContext;

import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.client.WendigoHandler;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PointList;

import com.threerings.openal.SoundGroup;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays the wendigo.
 */
public class WendigoSprite extends MobileSprite
{
    public boolean claw;

    public WendigoSprite (boolean claw)
    {
        super("extras", "indian_post/wendigo" + (claw ? "_claw" : ""));
        this.claw = claw;
    }

    public void init (BasicContext ctx, BoardView view, BangBoard board,
            SoundGroup sounds, Piece piece, short tick)
    {
        super.init(ctx, view, board, sounds, piece, tick);
        // start fully transparent; the fade controller ramps the diffuse alpha. jME3: the tint
        // rides the SpriteMaterialState (no fixed-function alpha/material state on the node).
        _fadeState = new SpriteMaterialState();
        _fadeState.getAmbient().set(ColorRGBA.White);
        _fadeState.getDiffuse().set(ColorRGBA.White);
        _fadeState.getDiffuse().a = 0f;
        _fadeState.apply(this);
    }

    @Override // documentation inherited
    public Shadow getShadowType ()
    {
        return Shadow.NONE;
    }

    @Override // documentation inherited
    public void centerWorldCoords (Vector3f coords)
    {
        // Since the wendigo takes up a 2x2 tile space, center it to the far
        // corner of it's occupying tile
        coords.x += TILE_SIZE;
        coords.y += TILE_SIZE;
    }

    /**
     * Perform a move with an associate penderId for the WendigoHandler.
     */
    public void move (BangBoard board, int ox, int oy, int nx, int ny,
            float speed, WendigoHandler handler, int penderId)
    {
        PointList path = new PointList();
        path.add(new Point(ox, oy));
        path.add(new Point(nx, ny));
        _handler = handler;
        _penderId = penderId;
        move(board, path, speed);
    }

    @Override // documentation inherited
    protected Path createPath (BangBoard board, List<Point> path, float speed)
    {
        BangObject bangobj = _handler.getBangObject();
        ArrayList<Piece> colliders = new ArrayList<Piece>();
        Point start = path.get(0);
        Point end = path.get(1);
        boolean onX = (start.x == end.x);

        // Get the pieces we'll be colliding with
        for (Piece p : bangobj.pieces) {
            if (p instanceof Unit && p.isAlive()) {
                int pidx = (onX ? p.x : p.y);
                if (pidx == start.x || pidx == start.x + 1) {
                    colliders.add(p);
                }
            }
        }

        ArrayList<Vector3f> coords = new ArrayList<Vector3f>();
        ArrayList<Float> durations = new ArrayList<Float>();
        Vector3f temp = new Vector3f();
        toWorldCoords(start.x, start.y, _piece.computeElevation(
                    board, start.x, start.y), temp);
        coords.add(temp);

        // Average the elevation over a window of 3 tiles, if colliding with
        // a unit, use the unit elevation, otherwise use the terrain elevation
        int lastx = start.x, lasty = start.y;
        int stepx = 0, stepy = 0, offx = 0, offy = 0;
        int inc = 0, pieces = 0, psum = 0, tsum = 0;
        if (!onX) {
            stepx = (end.x > start.x) ? 1 : -1;
            offy = 1;
        } else {
            stepy = (end.y > start.y) ? 1 : -1;
            offx = 1;
        }
        for (int xx = start.x + stepx, yy = start.y + stepy;
                !(xx == end.x + stepx && yy == end.y + stepy);
                xx += stepx, yy += stepy) {
            tsum += _piece.computeElevation(board, xx, yy);
            inc++;
            // check if we collide with any units at this coordinate
            for (Iterator<Piece> iter = colliders.iterator();
                    iter.hasNext(); ) {
                Piece p = iter.next();
                if ((p.x == xx && p.y == yy) ||
                        (p.x == xx + offx && p.y == yy + offy)) {
                    psum += p.computeElevation(board, p.x, p.y);
                    pieces++;
                    iter.remove();
                }
            }

            // when we've sumed 3 tiles, calculate the average
            if (inc == 3) {
                int elev;
                if (pieces > 0) {
                    elev = psum / pieces;
                } else {
                    elev = tsum / inc;
                }
                int x = xx - stepx, y = yy - stepy;
                temp = new Vector3f();
                toWorldCoords(x, y, elev, temp);
                coords.add(temp);
                int dist = Math.abs(lastx - x + lasty - y);
                durations.add(dist / speed);
                lastx = x;
                lasty = y;
                pieces = 0;
                psum = 0;
                tsum = 0;
                inc = 0;
            }
        }

        temp = new Vector3f();
        toWorldCoords(end.x, end.y, _piece.computeElevation(
                    board, end.x, end.y), temp);
        coords.add(temp);
        int dist = Math.abs(lastx - end.x + lasty - end.y);
        durations.add(dist / speed);
        float[] durs = new float[durations.size()];
        for (int ii = 0; ii < durs.length; ii++) {
            durs[ii] = durations.get(ii);
        }
        return new WendigoPath(
                this, coords.toArray(new Vector3f[coords.size()]), durs);
    }

    @Override // documentation inherited
    public void pathCompleted ()
    {
        super.pathCompleted();
        _view.removeSprite(WendigoSprite.this);
        _handler.pathCompleted(_penderId);
    }

    @Override // documentation inherited
    protected String getHelpIdent (int pidx)
    {
        return "wendigo";
    }

    @Override // documentation inherited
    protected Path createPath (
            Vector3f[] coords, float[] durations, String action)
    {
        return new WendigoPath(this, coords, durations);
    }

    /**
     * Fades in/out the wendigo.
     */
    public void fade (final boolean in, final float duration)
    {
        final SpriteMaterialState mstate = _fadeState;
        mstate.getAmbient().set(ColorRGBA.White);
        mstate.getDiffuse().set(ColorRGBA.White);
        mstate.getDiffuse().a = (in ? 0f : 1f);
        mstate.apply(this);
        _view.getNode().addControl(new AbstractControl() {
            protected void controlUpdate (float time) {
                _elapsed = Math.min(_elapsed + time, duration);
                float alpha = _elapsed / duration;
                if (!in) {
                    alpha = 1f - alpha;
                }
                mstate.getDiffuse().a = alpha;
                mstate.apply(WendigoSprite.this);
                if (_elapsed >= duration) {
                    _view.getNode().removeControl(this);
                }
            }
            protected void controlRender (RenderManager rm, ViewPort vp) {}
            protected float _elapsed;
        });
    }

    protected WendigoHandler _handler;

    protected int _penderId;

    /** Tracks the wendigo's fade tint. */
    protected SpriteMaterialState _fadeState;
}
