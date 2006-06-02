//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.shape.Box;

import com.threerings.jme.model.Model;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Track;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a track piece.
 */
public class TrackSprite extends PieceSprite
    implements PieceCodes
{
    @Override // documentation inherited
    public void updated (Piece piece, short tick)
    {
        Track otrack = (Track)_piece, ntrack = (Track)piece;
        super.updated(piece, tick);

        // refresh the geometry when the type changes
        if (ntrack.type != otrack.type) {
            detachAllChildren();
            createGeometry(_ctx);
        }

        // make sure it's lying on the terrain
        setOrientation(piece.orientation);
    }

    @Override // documentation inherited
    public void setOrientation (int orientation)
    {
        super.setOrientation(orientation);
        snapToTerrain();
    }

    @Override // documentation inherited
    protected void createGeometry (BasicContext ctx)
    {
        _ctx = ctx;
        loadModel(ctx, "extras/frontier_town/tracks",
                  MODEL_NAMES[((Track)_piece).type]);
    }

    @Override // documentation inherited
    protected void modelLoaded (BasicContext ctx, Model model)
    {
        // in the game, we can lock the bounds and transforms of tracks
        // because we know they won't move
        super.modelLoaded(ctx, model);
        if (!_editorMode) {
            updateWorldVectors();
            model.lockInstance();
        }
    }
    
    protected BasicContext _ctx;

    /* The models for each type of track. */
    protected static final String[] MODEL_NAMES = { "node", "node", "straight",
        "tee", "cross", "curve" };
}
