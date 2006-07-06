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
import com.jme.scene.Spatial;

import com.threerings.bang.client.util.ResultAttacher;

import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.TotemBase;
import com.threerings.bang.game.data.piece.Unit;

import com.threerings.jme.model.Model;

import com.threerings.media.image.Colorization;

import static com.threerings.bang.client.BangMetrics.*;
import static com.threerings.bang.Log.log;

/**
 * Does something extraordinary.
 */
public class TotemBaseSprite extends PropSprite
    implements Targetable
{
    public TotemBaseSprite ()
    {
        super("indian_post/special/totem_base");
        _totHeight += _config.height * TILE_SIZE;
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

    // documentation inherited from Targetable
    public void setTargeted (TargetMode mode, Unit attacker)
    {
        _target.setTargeted(mode, attacker);
    }

    // documentation inherited from Targetable
    public void setPendingShot (boolean pending)
    {
        _target.setPendingShot(pending);
    }

    // documentation inherited from Targetable
    public void configureAttacker ( int pidx, int delta)
    {
        _target.configureAttacker(pidx, delta);
    }

    @Override // documentation inherited
    public void updated (Piece piece, short tick)
    {
        super.updated(piece, tick);
        
        TotemBase base = (TotemBase)piece;
        final int size = _totemPieces.size();
        int baseHeight = base.numPieces();

        // This assumes updated is called after any piece has been added
        // or removed from the TotemBase
        if (baseHeight < size) {
            _totHeight -= _totemHeights.remove(size - 1);
            Node totemPiece = _totemPieces.remove(size - 1);
            float height = (size > 1) ? 
                _totHeight - _totemHeights.get(size - 2) : 0f;
            setLocalTranslation(_baseTrans.add(new Vector3f(0, 0, height)));
            detachChild(totemPiece);
            adjustTotemPieces();

        } else if (baseHeight > size) {
            String type = base.getTopPiece();
            int owner = base.getTopOwner();
            Colorization[] zations = new Colorization[] {
                _ctx.getAvatarLogic().getColorPository().getColorization(
                        "unit", PIECE_COLOR_IDS[owner + 1]) };
            final Node totemPiece = new Node(type);
            _totemPieces.add(totemPiece);
            _ctx.getModelCache().getModel("bonuses", type, zations,
                    new ResultAttacher<Model>(totemPiece) {
                public void requestCompleted (Model model) {
                    // calculate the height of the model
                    model.updateGeometricState(0f, true);
                    BoundingVolume bound = model.getWorldBound();
                    float height = bound.getCenter().z;
                    if (bound instanceof BoundingBox) {
                        height += ((BoundingBox)bound).zExtent;
                    } else if (bound instanceof BoundingSphere) {
                        height += ((BoundingSphere)bound).radius;
                    }
                    // translate the base node to the new height
                    setLocalTranslation(_baseTrans.add(
                            new Vector3f(0, 0, _totHeight)));
                    _totemHeights.add(size, height);
                    _totHeight += height;
                    adjustTotemPieces();
                    super.requestCompleted(model);
                }
                public void requestFailed (Exception cause) {
                    _totemHeights.add(size, 0f);
                }
            });
            attachChild(totemPiece);
            totemPiece.updateRenderState();
        } 
        updateRenderState();

        _target.updated(piece, tick);
    }

    /**
     * Adjusts the translations of all the totem pieces to compensate for
     * the rising node translation.
     */
    protected void adjustTotemPieces ()
    {
        float height = 0f;
        for (int ii = _totemPieces.size() - 1; ii >= 0; ii--) {
            Node piece = _totemPieces.get(ii);
            log.info("Adjust translation of piece " + ii + " to height: " +
                    height);
            piece.setLocalTranslation(new Vector3f(0, 0, height));
            if (ii > 0) {
                height -= _totemHeights.get(ii - 1);
            }
        }
    }

    @Override // documentation inherited
    protected void createGeometry ()
    {
        super.createGeometry();
        
        _tlight = _view.getTerrainNode().createHighlight(
                        _piece.x, _piece.y, false, false);
        attachHighlight(_status = new PieceStatus(_ctx, _tlight));
        updateStatus();
        _target = new PieceTarget(_piece, _ctx);
        attachChild(_target);
        _baseTrans = getLocalTranslation();
    }

    protected ArrayList<Node> _totemPieces = new ArrayList<Node>();
    protected ArrayList<Float> _totemHeights = new ArrayList<Float>();
    protected float _totHeight;

    protected PieceTarget _target;

    protected Vector3f _baseTrans;
}
