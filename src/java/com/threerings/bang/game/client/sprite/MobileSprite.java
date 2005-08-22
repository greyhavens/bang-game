//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.awt.Point;
import java.util.Iterator;
import java.util.List;

import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.TextureState;

import com.samskivert.util.StringUtil;
import com.threerings.media.util.MathUtil;
import com.threerings.openal.Sound;
import com.threerings.openal.SoundGroup;

import com.threerings.jme.sprite.LinePath;
import com.threerings.jme.sprite.LineSegmentPath;
import com.threerings.jme.sprite.Path;

import com.threerings.bang.client.Model;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays some sort of mobile entity on the board (generally a unit or
 * some other piece that moves around and is interacted with).
 */
public class MobileSprite extends PieceSprite
{
    /**
     * Creates a mobile sprite with the specified model type and name.
     */
    public MobileSprite (String type, String name)
    {
        _type = type;
        _name = name;
    }

    /**
     * Returns true if this sprite supports the specified action
     * animation.
     */
    public boolean hasAction (String action)
    {
        return _model.hasMeshes(action);
    }

    @Override // documentation inherited
    public void pathCompleted ()
    {
        super.pathCompleted();

        // stop our movement sound
        _moveSound.stop();
    }

    @Override // documentation inherited
    protected void createGeometry (BangContext ctx)
    {
        super.createGeometry(ctx);

        if (_shadtex == null) {
            loadTextures(ctx);
        }

        // we display a simple shadow texture on the ground beneath us
        _shadow = RenderUtil.createIcon(_shadtex);
        _shadow.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        float height = _piece.isFlyer() ? -2 * TILE_SIZE : 0;
        height += 0.1f;
        _shadow.setLocalTranslation(new Vector3f(0, 0, height));
        attachChild(_shadow);

        // load our model
        _model = ctx.getModelCache().getModel(_type, _name);

        // start in our rest post
        setAction(getRestPose());
    }

    @Override // documentation inherited
    protected void createSounds (SoundGroup sounds)
    {
        super.createSounds(sounds);

        // load up our movement sounds
        _moveSound = sounds.getSound(
            "rsrc/sounds/" + _type + "/" + _name + "/move.wav");
    }

    @Override // documentation inherited
    protected void moveSprite (BangBoard board, Piece opiece, Piece npiece)
    {
        // no animating when we're in the editor
        if (_editorMode) {
            super.moveSprite(board, opiece, npiece);
            return;
        }

        // TODO: append an additional path if we're currently moving
        if (!isMoving()) {
            Path path = createPath(board, opiece, npiece);
            if (path != null) {
                // start looping our movement sound
                _moveSound.loop(false);
                move(path);
            } else {
                int elev = computeElevation(board, npiece.x, npiece.y);
                setLocation(npiece.x, npiece.y, elev);
            }
        }
    }

    /**
     * Configures the current set of meshes being used by this sprite.
     */
    protected void setAction (String action)
    {
        if (_meshes != null) {
            for (int ii = 0; ii < _meshes.length; ii++) {
                detachChild(_meshes[ii]);
            }
        }
        _meshes = _model.getMeshes(action);
        for (int ii = 0; ii < _meshes.length; ii++) {
            attachChild(_meshes[ii]);
            _meshes[ii].updateRenderState();
        }
        // TODO: get this from the model?
        setAnimationSpeed(20);
    }

    /**
     * Returns the default pose for this model when it is simply resting
     * on the board.
     */
    protected String getRestPose ()
    {
        return "standing";
    }

    /**
     * Creates a path that will be used to move this piece from the
     * specified old position to the new one.
     */
    protected Path createPath (BangBoard board, Piece opiece, Piece npiece)
    {
        List path = null;
        if (board != null) {
            path = board.computePath(opiece, npiece.x, npiece.y);
        }

        if (path != null) {
            if (path.size() < 2) {
                log.warning("Created short path? [opiece=" + opiece.info() +
                            ", npiece=" + npiece.info() +
                            ", path=" + StringUtil.toString(path) + "].");
                return null;
            }
            // create a world coordinate path from the tile
            // coordinates
            Vector3f[] coords = new Vector3f[path.size()];
            float[] durations = new float[path.size()-1];
            int ii = 0, elevation = 0; // TODO: handle elevated paths
            for (Iterator iter = path.iterator(); iter.hasNext(); ii++) {
                Point p = (Point)iter.next();
                coords[ii] = new Vector3f();
                toWorldCoords(p.x, p.y, elevation, coords[ii]);
                if (ii > 0) {
                    durations[ii-1] = 0.2f;
                }
            }
            return new MoveUnitPath(this, coords, durations);

        } else {
            Vector3f start = toWorldCoords(
                opiece.x, opiece.y, 0, new Vector3f());
            Vector3f end = toWorldCoords(npiece.x, npiece.y, 0, new Vector3f());
            float duration = (float)MathUtil.distance(
                opiece.x, opiece.y, npiece.x, npiece.y) * .003f;
            return new LinePath(this, start, end, duration);
        }
    }

    protected static void loadTextures (BangContext ctx)
    {
        _shadtex = RenderUtil.createTexture(
            ctx, ctx.loadImage("textures/ustatus/shadow.png"));
    }

    protected String _type, _name;
    protected Model _model;
    protected Node[] _meshes;
    protected Quad _shadow;
    protected Sound _moveSound;

    protected static TextureState _shadtex;
}
