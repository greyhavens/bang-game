//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;

import com.threerings.jme.sprite.Path;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a cow along with an indicator of who owns it.
 */
public class CowSprite extends MobileSprite
{
    public CowSprite ()
    {
        super("extras", "frontier_town/cow");
    }

    @Override // documentation inherited
    public void updated (Piece piece, short tick)
    {
        super.updated(piece, tick);

        // update our colors in the event that our owner changes
        configureOwnerColors();
    }

    @Override // documentation inherited
    protected void createGeometry ()
    {
        super.createGeometry();
        if (_owntex == null) {
            loadTextures(_ctx);
        }

        // this is used to indicate who owns us
        _tlight = _view.getTerrainNode().createHighlight(
                _piece.x, _piece.y, true, true);
        Material omat = RenderUtil.createTextureMaterial(_ctx, _owntex);
        RenderUtil.applyBlendAlpha(omat);
        RenderUtil.applyOverlayZBuf(omat);
        _tlight.setTextures(omat, omat);
        attachHighlight(_tlight);

        // configure our colors
        configureOwnerColors();
    }

    @Override // documentation inherited
    protected String getHelpIdent (int pidx)
    {
        return "cow";
    }

    @Override // documentation inherited
    protected String[] getPreloadSounds ()
    {
        return PRELOAD_SOUNDS;
    }

    @Override // documentation inherited
    protected void moveEnded ()
    {
        super.moveEnded();
        configureOwnerColors();
    }

    @Override // documentation inherited
    public void move (Path path)
    {
        super.move(path);
        _tlight.setCullHint(CullHint.Always);
    }

    /** Sets up our colors according to our owning player. */
    protected void configureOwnerColors ()
    {
        if (_piece.owner < 0 || isMoving()) {
            _tlight.setCullHint(CullHint.Always);
        } else {
            ColorRGBA color = getJPieceColor(_piece.owner);
            _tlight.setColors(color, color);
            _tlight.setCullHint(CullHint.Dynamic);
            updateTileHighlight();
        }
    }

    protected static void loadTextures (BasicContext ctx)
    {
        _owntex = ctx.getTextureCache().getTexture("textures/ustatus/selected.png");
        _owntex.setWrap(WrapMode.Clamp);
    }

    protected static Texture2D _owntex;

    /** Sounds that we preload for mobile units if they exist. */
    protected static final String[] PRELOAD_SOUNDS = {
        "spooked",
        "branded",
    };
}
