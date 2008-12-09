//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.math.Quaternion;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;

import com.samskivert.util.HashIntMap;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.SafeMarker;

import com.threerings.openal.SoundGroup;

/**
 * Sprite for the safe markers.
 */
public class SafeMarkerSprite extends MarkerSprite
{
    public SafeMarkerSprite (int type)
    {
        super(type);
    }

    @Override // documentation inherited
    public void init (BasicContext ctx, BoardView view, BangBoard board,
            SoundGroup sounds, Piece piece, short tick)
    {
        super.init(ctx, view, board, sounds, piece, tick);
        int type = ((Marker)_piece).getType();
        boolean emissive = (TextureState.getNumberOfFixedUnits() >= 2);
        TextureState[] texstates = _highlightStates.get(type);
        if (texstates == null) {
            texstates = new TextureState[DIRECTIONS.length * HIGHLIGHT_TEXS.length];
            for (int ii = 0; ii < HIGHLIGHT_TEXS.length; ii++) {
                String root = (String)SPRITES[type*2+1];
                Texture btex = ctx.getTextureCache().getTexture(
                    root + HIGHLIGHT_TEXS[ii] + ".png");
                int idx = ii * DIRECTIONS.length;
                texstates[idx] = ctx.getRenderer().createTextureState();
                if (emissive) {
                    Texture etex = ctx.getTextureCache().getTexture(
                        root + EMISSIVE_TEXS[ii] + ".png");
                    etex.setApply(Texture.AM_BLEND);
                    etex.setBlendColor(ColorRGBA.white);
                    texstates[idx].setTexture(etex, 0);
                    texstates[idx].setTexture(btex, 1);
                } else {
                    texstates[idx].setTexture(btex);
                }
                RenderUtil.ensureLoaded(texstates[idx]); // load before cloning
            }
            
            // create the three other rotations of the texture
            for (int ii = 1; ii < DIRECTIONS.length; ii++) {
                for (int jj = 0; jj < HIGHLIGHT_TEXS.length; jj++) {
                    int idx = jj * DIRECTIONS.length + ii;
                    texstates[idx] = createRotatedState(
                        texstates[jj * DIRECTIONS.length], ii * FastMath.HALF_PI);
                }
            }
            _highlightStates.put(type, texstates);
        }
        _tlight = view.getTerrainNode().createHighlight(
                piece.x, piece.y, false, (byte)1);
        if (emissive) {
            _tlight.setLightCombineMode(LightState.REPLACE);
            _tlight.setTextureBuffer(0, _tlight.getTextureBuffer(0, 0), 1);
            _tlight.setHasNormals(true);
        }
        setOrientation(piece.orientation);
        attachHighlight(_tlight);
    }

    @Override // documenatation inherited
    public void setOrientation (int orientation)
    {
        setOnOff(orientation, ((SafeMarker)_piece).isOn());
    }

    @Override // documentation inherited
    public void updated (Piece piece, short tick)
    {
        super.updated(piece, tick);
        setOnOff(_piece.orientation, ((SafeMarker)_piece).isOn());
    }

    /**
     * Set the on/off texture for a highlight marker.
     */
    public void setOnOff (int orientation, boolean on)
    {
        if (_tlight != null) {
            if (!on) {
                orientation += DIRECTIONS.length;
            }
            _tlight.setRenderState(_highlightStates.get(
                        ((Marker)_piece).getType())[orientation]);
            _tlight.updateRenderState();
        }
    }

    /**
     * Creates and returns a rotated "clone" of the given texture state.
     */
    protected TextureState createRotatedState (TextureState ostate, float angle)
    {
        TextureState nstate = _ctx.getRenderer().createTextureState();
        for (int ii = 0, nn = ostate.getNumberOfSetTextures(); ii < nn; ii++) {
            Texture ntex = ostate.getTexture(ii).createSimpleClone();
            ntex.setRotation(new Quaternion().fromAngleNormalAxis(angle, Vector3f.UNIT_Z));
            nstate.setTexture(ntex, ii);
        }
        return nstate;
    }
    
    protected static final String[] HIGHLIGHT_TEXS = {
        "_on", "_off"
    };

    protected static final String[] EMISSIVE_TEXS = {
        "_on_emis", "_off_emis"
    };
    
    protected static HashIntMap<TextureState[]> _highlightStates =
        new HashIntMap<TextureState[]>();
}
