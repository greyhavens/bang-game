//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.ArrayList;

import java.lang.Exception;

import com.jme.bounding.BoundingBox;
import com.jme.bounding.BoundingSphere;
import com.jme.bounding.BoundingVolume;

import com.jme.math.Vector3f;

import com.jme.scene.Node;

import com.threerings.bang.client.util.ModelAttacher;

import com.threerings.bang.util.BasicContext;

import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.TotemBase;

import com.threerings.jme.model.Model;
import static com.threerings.bang.client.BangMetrics.*;
import static com.threerings.bang.Log.log;

/**
 * Does something extraordinary.
 */
public class TotemBaseSprite extends PropSprite
{
    public TotemBaseSprite ()
    {
        super("indian_post/special/totem_base");
        _totHeight += _config.height * TILE_SIZE / 2f;
    }

    @Override // documentation inherited
    public String getHelpIdent (int pidx)
    {
        return (pidx == _piece.owner ? "own_" : "other_") + _config.type;
    }

    @Override // documentation inherited
    public boolean isHoverable ()
    {
        return true;
    }

    @Override // documentation inherited
    public void updated (Piece piece, short tick)
    {
        super.updated(piece, tick);
        
        TotemBase base = (TotemBase)piece;
        final int size = _totemPieces.size();
        int baseHeight = base.getTotemHeight();

        // This assumes updated is called after any piece has been added
        // or removed from the TotemBase
        if (baseHeight < size) {
            _totHeight -= _totemHeights.remove(size - 1);
            Node totemPiece = _totemPieces.remove(size - 1);
            detachChild(totemPiece);
            totemPiece.detachAllChildren();
        } else if (baseHeight > size) {
            String type = base.getTopPiece();
            final Node totemPiece = new Node(type);
            totemPiece.setLocalTranslation(new Vector3f(0, 0, _totHeight));
            _ctx.loadModel("bonuses", type, new ModelAttacher(totemPiece) {
                public void requestCompleted (Model model) {
                    model.updateGeometricState(0f, true);
                    BoundingVolume bound = model.getWorldBound();
                    float height = bound.getCenter().z;
                    if (bound instanceof BoundingBox) {
                        height += ((BoundingBox)bound).zExtent;
                    } else if (bound instanceof BoundingSphere) {
                        height += ((BoundingSphere)bound).radius;
                    }
                    _totemHeights.add(size, height);
                    _totHeight += height;
                    super.requestCompleted(model);
                }
                public void requestFailed (Exception cause) {
                    _totemHeights.add(size, 0f);
                }
            });
            attachChild(totemPiece);
            _totemPieces.add(totemPiece);
            totemPiece.updateRenderState();
        } 
    }

    @Override // documentation inherited
    protected void createGeometry (BasicContext ctx)
    {
        super.createGeometry(ctx);
        _ctx = ctx;
    }

    protected ArrayList<Node> _totemPieces = new ArrayList<Node>();
    protected ArrayList<Float> _totemHeights = new ArrayList<Float>();
    protected float _totHeight;

    protected BasicContext _ctx;
}
