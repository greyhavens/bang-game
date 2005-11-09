//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.shape.Box;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.data.BangBoard;
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
    public void updated (BangBoard board, Piece piece, short tick)
    {
        Track otrack = (Track)_piece, ntrack = (Track)piece;
        super.updated(board, piece, tick);
        
        // refresh the geometry when the type changes
        if (ntrack.type != otrack.type) {
            detachAllChildren();
            createGeometry(null);
        }
    }
    
    @Override // documentation inherited
    protected void createGeometry (BasicContext ctx)
    {
        Track track = (Track)_piece;
        if (track.type == Track.SINGLETON || track.type == Track.TERMINAL) {
            attachCenter();
        }
        if (track.type == Track.TERMINAL || track.type == Track.STRAIGHT ||
            track.type == Track.X_JUNCTION || track.type == Track.TURN) {
            attachLeg(NORTH);
        }
        if (track.type == Track.STRAIGHT || track.type == Track.T_JUNCTION ||
            track.type == Track.X_JUNCTION) {
            attachLeg(SOUTH);
        }
        if (track.type == Track.T_JUNCTION || track.type == Track.X_JUNCTION ||
            track.type == Track.TURN) {
            attachLeg(EAST);
        }
        if (track.type == Track.T_JUNCTION || track.type == Track.X_JUNCTION) {
            attachLeg(WEST);
        }
        updateRenderState();
    }
    
    /**
     * Attaches a big cube to indicate that this piece is a source.
     */
    protected void attachCenter ()
    {
        Box box = new Box("box",
            new Vector3f(-TILE_SIZE/4, -TILE_SIZE/4, -TILE_SIZE/16),
            new Vector3f(TILE_SIZE/4, TILE_SIZE/4, TILE_SIZE/2));
        attachChild(box);
    }
    
    /**
     * Attaches a track leg in the specified direction.
     */
    protected void attachLeg(int dir)
    {
        Box box = new Box("box",
            new Vector3f(-TILE_SIZE/8, -TILE_SIZE/8, -TILE_SIZE/16),
            new Vector3f(TILE_SIZE/8, TILE_SIZE/2, TILE_SIZE/16));
        Quaternion rot = new Quaternion();
        rot.fromAngleAxis(ROTATIONS[dir], Vector3f.UNIT_Z);
        box.setLocalRotation(rot);
        attachChild(box);
    }
}
