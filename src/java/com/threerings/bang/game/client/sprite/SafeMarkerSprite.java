//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.math.Vector3f;
import com.jme.math.Quaternion;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;

import com.samskivert.util.HashIntMap;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.SafeMarker;

import com.threerings.openal.SoundGroup;

import static com.threerings.bang.client.BangMetrics.*;

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
        TextureState[] texstates = _highlightStates.get(type);
        if (texstates == null) {
            texstates = new TextureState[
                DIRECTIONS.length * HIGHLIGHT_TEXS.length / 2];
            Texture[] ntex = new Texture[HIGHLIGHT_TEXS.length],
                      rtex = new Texture[HIGHLIGHT_TEXS.length];
            for (int ii = 0; ii < HIGHLIGHT_TEXS.length; ii++) {
                int idx = ii / 2 * DIRECTIONS.length;
                TextureState tstate = RenderUtil.createTextureState(ctx,
                        (String)SPRITES[type*2+1] +
                        HIGHLIGHT_TEXS[ii] + ".png");
                ntex[ii] = tstate.getTexture();
                if (ii % 2 == 0) {
                    texstates[idx] = DisplaySystem.getDisplaySystem().
                        getRenderer().createTextureState();
                    ntex[ii].setApply(Texture.AM_BLEND);
                    ntex[ii].setBlendColor(ColorRGBA.white);
                }
                //texstates[idx].setTexture(ntex[ii], ii % 2);
                texstates[idx] = tstate;
            }
            Vector3f up = new Vector3f(0f, 0f, 1f);
            // create the 4 rotations of the texture
            for (int ii = 1; ii < DIRECTIONS.length; ii++) {
                for (int jj = 0; jj < ntex.length; jj++) {
                    rtex[jj] = ntex[jj].createSimpleClone();
                    Quaternion rot = new Quaternion();
                    rtex[jj].setRotation(rot.fromAngleNormalAxis(
                                (float)(ii * Math.PI / 2), up));
                    int idx = jj / 2 * DIRECTIONS.length + ii;
                    if (jj % 2 == 0) {
                        texstates[idx] = DisplaySystem.getDisplaySystem().
                            getRenderer().createTextureState();
                    }
                    texstates[idx].setTexture(rtex[jj], jj % 2);
                }
            }
            _highlightStates.put(type, texstates);
        }
        _tlight = view.getTerrainNode().createHighlight(
                piece.x, piece.y, false, (byte)1);
        _tlight.setLightCombineMode(LightState.OFF);
        _tlight.setTextureBuffer(0, _tlight.getTextureBuffer(0, 0), 1);
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

    protected static final String[] HIGHLIGHT_TEXS = {
        "_on_emis", "_on", "_off_emis", "_off"
    };

    protected static HashIntMap<TextureState[]> _highlightStates =
        new HashIntMap<TextureState[]>();
}
