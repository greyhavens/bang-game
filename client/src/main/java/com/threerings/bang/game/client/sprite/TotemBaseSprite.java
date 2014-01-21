//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.ArrayList;

import com.jme.bounding.BoundingBox;
import com.jme.bounding.BoundingSphere;
import com.jme.bounding.BoundingVolume;
import com.jme.math.Vector3f;
import com.jme.scene.Node;

import com.threerings.jme.sprite.Sprite;
import com.threerings.jme.model.Model;

import com.threerings.bang.client.util.ResultAttacher;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.TotemBase;

import com.threerings.media.image.Colorization;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays the totem base for the Totem Building scenario.
 */
public class TotemBaseSprite extends TargetablePropSprite
{
    public TotemBaseSprite ()
    {
        super("indian_post/special/totem_base");
        _totHeight += _config.height * TILE_SIZE;
    }

    @Override // documentation inherited
    public void updated (Piece piece, short tick)
    {
        super.updated(piece, tick);
        showPieces(piece);
    }

    /**
     * Returns a reference to the piece at the top of the totem.
     */
    public Sprite getTopPiece ()
    {
        int size = _totemPieces.size();
        return (size > 0) ? _totemPieces.get(size - 1) : null;
    }

    @Override // documentation inherited
    protected String getHelpIdent (int pidx)
    {
        return "totem_base";
    }

    @Override // documentation inherited
    protected void createGeometry ()
    {
        super.createGeometry();
        _baseTrans = getLocalTranslation();
        showPieces(_piece);
    }

    protected void showPieces (Piece piece)
    {
        TotemBase base = (TotemBase)piece;
        final int size = _totemPieces.size();
        int baseHeight = base.numPieces();

        if (_powner != piece.owner) {
            updateColorizations();
            _powner = piece.owner;
        }

        if (baseHeight < size) {
            for (int ii = size - 1; ii >= baseHeight; ii--) {
                _totHeight -= _totemHeights.remove(ii);
                Node totemPiece = _totemPieces.remove(ii);
                float height = (ii > 0) ?
                    _totHeight - _totemHeights.get(ii - 1) : 0f;
                setLocalTranslation(_baseTrans.add(new Vector3f(0, 0, height)));
                detachChild(totemPiece);
            }
            adjustTotemPieces();

        } else if (baseHeight > size) {
            for (int ii = size; ii < baseHeight; ii++) {
                final String type = base.getType(ii).bonus();
                int owner = base.getOwner(ii);
                final int idx = ii;
                Colorization[] zations = new Colorization[] {
                    _ctx.getAvatarLogic().getColorPository().getColorization(
                            "unit", colorLookup[owner + 1] + 1) };
                Sprite totemPiece = new Sprite();
                _totemPieces.add(totemPiece);
                _totemHeights.add(0f);
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
                        _totemHeights.set(idx, height);
                        _totHeight += height;
                        adjustTotemPieces();
                        super.requestCompleted(model);
                    }
                });
                attachChild(totemPiece);
                totemPiece.updateRenderState();
            }
        }
        updateRenderState();
    }

    /**
     * Adjusts the translations of all the totem pieces to compensate for the
     * rising node translation.
     */
    protected void adjustTotemPieces ()
    {
        float height = 0f;
        for (int ii = _totemPieces.size() - 1; ii >= 0; ii--) {
            Node piece = _totemPieces.get(ii);
            piece.setLocalTranslation(new Vector3f(0, 0, height));
            if (ii > 0) {
                height -= _totemHeights.get(ii - 1);
            }
        }
    }

    protected ArrayList<Sprite> _totemPieces = new ArrayList<Sprite>();
    protected ArrayList<Float> _totemHeights = new ArrayList<Float>();
    protected float _totHeight;
    protected Vector3f _baseTrans;
}
