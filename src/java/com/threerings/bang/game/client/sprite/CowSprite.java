//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.image.Texture;
import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.SharedMesh;
import com.jme.scene.state.TextureState;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.TerrainNode;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;
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
    public String getHelpIdent (int pidx)
    {
        return "cow";
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
        _owner = new TerrainNode.SharedHighlight("owner", _shadow);
        _owner.setRenderState(_owntex);
        _owner.updateRenderState();
        attachHighlight(_owner);

        // configure our colors
        configureOwnerColors();
    }

    @Override // documentation inherited
    protected String[] getPreloadSounds ()
    {
        return PRELOAD_SOUNDS;
    }

    /** Sets up our colors according to our owning player. */
    protected void configureOwnerColors ()
    {
        if (_piece.owner < 0) {
            _owner.setCullMode(CULL_ALWAYS);
        } else {
            _owner.setDefaultColor(JPIECE_COLORS[_piece.owner + 1]);
            _owner.updateRenderState();
            _owner.setCullMode(CULL_DYNAMIC);
        }
    }

    protected static void loadTextures (BasicContext ctx)
    {
        _owntex = RenderUtil.createTextureState(
            ctx, "textures/ustatus/selected.png");
        _owntex.getTexture().setWrap(Texture.WM_CLAMP_S_CLAMP_T);
    }

    protected SharedMesh _owner;

    protected static TextureState _owntex;

    /** Sounds that we preload for mobile units if they exist. */
    protected static final String[] PRELOAD_SOUNDS = {
        "spooked",
        "branded",
    };
}
