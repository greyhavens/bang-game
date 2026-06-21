//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.Plane;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.BillboardControl;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.BufferUtils;

import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.IconConfig;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.NuggetEffect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.client.BangMetrics.*;

import static com.threerings.bang.Log.log;

/**
 * A target display for Pieces that are Targetable.
 */
public class PieceTarget extends Node
    implements Targetable
{
    public PieceTarget (Piece piece, BasicContext ctx)
    {
        super("piece_target");
        _ctx = ctx;
        _piece = piece;
        createGeometry();
    }

    // documentation inherited from Targetable
    public void setTargeted (BangObject bangobj, TargetMode mode, Unit attacker)
    {
        boolean addModifiers = false;
        if (_pendingTick == -1) {
            addModifiers = true;
            setQuadColor(_tgtquad, DEFAULT_COLOR);
            switch (mode) {
            case NONE:
                _tgtquad.setCullHint(CullHint.Always);
                addModifiers = false;
                break;
            case SURE_SHOT:
                displayTextureQuad(_tgtquad, _crosstex[0]);
                break;
            case MAYBE:
                displayTextureQuad(_tgtquad, _crosstex[1]);
                break;
            case KILL_SHOT:
                displayTextureQuad(_tgtquad, _crosstex[5]);
                break;
            }
        }
        if (!addModifiers) {
            for (int ii = 0; ii < _modquad.length; ii++) {
                _modquad[ii].setCullHint(CullHint.Always);
            }
            return;
        }
        int diff = attacker.computeDamageDiff(bangobj, _piece);
        if (diff > 0) {
            displayTextureQuad(_modquad[ModIcon.ARROW_UP.idx()],
                    _modtex[ModIcon.ARROW_UP.ordinal()]);
        } else if (diff < 0) {
            displayTextureQuad(_modquad[ModIcon.ARROW_DOWN.idx()],
                    _modtex[ModIcon.ARROW_DOWN.ordinal()]);
        }
        if (_piece instanceof Unit) {
            Unit unit = (Unit)_piece;
            if (unit.getConfig().rank == UnitConfig.Rank.BIGSHOT) {
                displayTextureQuad(_modquad[ModIcon.STAR.idx()],
                        _modtex[ModIcon.STAR.ordinal()]);
            }
            if (NuggetEffect.NUGGET_BONUS.equals(unit.holding)) {
                displayTextureQuad(_modquad[ModIcon.NUGGET.idx()],
                        _modtex[ModIcon.NUGGET.ordinal()]);
            }
        }
    }

    // documentation inherited from Targetable
    public void setPendingShot (boolean pending)
    {
        if (pending) {
            if (_pendingTick == -1) {
                setQuadColor(_tgtquad, ColorRGBA.Red);
            }
            _pendingTick = _tick;
        } else {
            _pendingTick = -1;
        }
        _tgtquad.setCullHint(pending ? CullHint.Dynamic : CullHint.Always);
        for (int ii = 0; ii < _modquad.length; ii++) {
            _modquad[ii].setCullHint(CullHint.Always);
        }
    }

    // documentation inherited from Targetable
    public void setPossibleShot (boolean possible)
    {
        if (_pendingTick == -1) {
            ColorRGBA color = (possible ? POSSIBLE_COLOR : DEFAULT_COLOR);
            setQuadColor(_tgtquad, color);
            for (Geometry quad : _modquad) {
                setQuadColor(quad, color);
            }
        }
    }

    // documentation inherited from Targetable
    public void configureAttacker (int pidx, int delta)
    {
        // sanity check
        if (_attackers == 0 && delta < 0) {
            log.warning("Requested to decrement attackers but we have none!", "sprite", this,
                        "pidx", pidx, "delta", delta);
            Thread.dumpStack();
            return;
        }

        _attackers += delta;

        if (_attackers > 0) {
            displayTextureQuad(_ptquad, _crosstex[Math.min(_attackers, 3)+1],
                    getJPieceColor(pidx));
        } else {
            _ptquad.setCullHint(CullHint.Always);
        }
    }

    /**
     * Called to update the target.
     */
    public void updated (Piece piece, short tick)
    {
        _tick = tick;
        _piece = (Piece)piece.clone();

        // clear our pending shot once we've been ticked (or if we die)
        if (!piece.isAlive() || (_pendingTick != -1 && tick > _pendingTick)) {
            setPendingShot(false);
        }
    }

    @Override // documentation inherited from Spatial
    public int collideWith (Collidable other, CollisionResults results)
    {
        // after picking, remove the result if it exceeds the radius of the reticle (about 3/8ths
        // the size of the texture). jME3: fork findPick(Ray, PickResults) -> collideWith.
        int onum = results.size();
        int added = super.collideWith(other, results);
        if (added > 0 && other instanceof Ray) {
            Ray ray = (Ray)other;
            // find the billboard plane using the translation and the camera direction
            Vector3f cdir = _ctx.getCameraHandler().getCamera().getDirection(),
                trans = _tgtquad.getWorldTranslation(), isect = new Vector3f();
            Plane cplane = new Plane(cdir, cdir.dot(trans));
            if (!ray.intersectsWherePlane(cplane, isect) ||
                isect.distance(trans) < 3*TILE_SIZE/8) {
                return added;
            }
            // the pick lies outside the reticle radius; drop these results
            while (results.size() > onum) {
                results.clear();
            }
            return 0;
        }
        return added;
    }

    /**
     * Create the geometry.
     */
    protected void createGeometry ()
    {
        loadTextures(_ctx);

        // we'll use this to keep a few things rotated toward the camera (BillboardControl)
        Node bbn = new Node("billboard");
        bbn.addControl(new BillboardControl());
        setCullHint(CullHint.Never);
        bbn.setCullHint(CullHint.Never);
        bbn.setLocalTranslation(new Vector3f(0, 0, TILE_SIZE/3));
        attachChild(bbn);

        // this icon is displayed when we're highlighted as a potential target.
        // jME3's Quad is corner-anchored (spans (0,0)-(w,h)) whereas the fork's
        // jME1 Quad was center-anchored, so shift each quad by (-w/2,-h/2) to
        // re-center it on its intended anchor point (matching the fork's layout).
        _tgtquad = IconConfig.createIcon(crossMaterial(_ctx, 0));
        _tgtquad.setLocalTranslation(new Vector3f(-TILE_SIZE/2f, -TILE_SIZE/2f, 0));
        bbn.attachChild(_tgtquad);
        _tgtquad.setCullHint(CullHint.Always);

        // these icons are displayed when there are modifiers for a potential target
        _modquad = new Geometry[MOD_COORDS.length];
        for (int ii = 0; ii < _modquad.length; ii++) {
            _modquad[ii] = IconConfig.createIcon(
                    modMaterial(_ctx, 0), TILE_SIZE/4f, TILE_SIZE/4f);
            _modquad[ii].setLocalTranslation(MOD_COORDS[ii].add(
                    -TILE_SIZE/8f, -TILE_SIZE/8f, 0));
            bbn.attachChild(_modquad[ii]);
            _modquad[ii].setCullHint(CullHint.Always);
        }

        // this icon is displayed when we have pending shots aimed at us
        _ptquad = IconConfig.createIcon(crossMaterial(_ctx, 2));
        _ptquad.setLocalTranslation(new Vector3f(-TILE_SIZE/2f, 0, 0));
        _ptquad.getMesh().setBuffer(VertexBuffer.Type.TexCoord, 2,
                BufferUtils.createFloatBuffer(PTARG_COORDS));
        bbn.attachChild(_ptquad);
        _ptquad.setCullHint(CullHint.Always);
    }

    /** Sets the colour param on a quad geometry's material. */
    protected static void setQuadColor (Geometry quad, ColorRGBA color)
    {
        if (quad.getMaterial() != null) {
            quad.getMaterial().setColor("Color", new ColorRGBA(color));
        }
    }

    /**
     * Helper function to update a quad with a texture and display it.
     */
    protected void displayTextureQuad (Geometry quad, com.jme3.texture.Texture2D tex)
    {
        displayTextureQuad(quad, tex, null);
    }

    /**
     * Helper function to update a quad with a texture and color then display it.
     */
    protected void displayTextureQuad (
            Geometry quad, com.jme3.texture.Texture2D tex, ColorRGBA color)
    {
        if (quad.getMaterial() != null) {
            quad.getMaterial().setTexture("ColorMap", tex);
        }
        quad.setCullHint(CullHint.Dynamic);
        if (color != null) {
            setQuadColor(quad, color);
        }
    }

    /** Returns a fresh textured Material for the given crosshair texture index. */
    protected static Material crossMaterial (BasicContext ctx, int idx)
    {
        Material mat = RenderUtil.createTextureMaterial(ctx, _crosstex[idx]);
        RenderUtil.applyBlendAlpha(mat);
        return mat;
    }

    /** Returns a fresh textured Material for the given modifier texture index. */
    protected static Material modMaterial (BasicContext ctx, int idx)
    {
        Material mat = RenderUtil.createTextureMaterial(ctx, _modtex[idx]);
        RenderUtil.applyBlendAlpha(mat);
        return mat;
    }

    protected static void loadTextures (BasicContext ctx)
    {
        if (_crosstex == null) {
            _crosstex = new com.jme3.texture.Texture2D[CROSS_TEXS.length];
            for (int ii = 0; ii < CROSS_TEXS.length; ii++) {
                _crosstex[ii] = ctx.getTextureCache().getTexture(
                    "textures/ustatus/crosshairs" + CROSS_TEXS[ii] + ".png");
                _crosstex[ii].setWrap(WrapMode.Clamp);
            }
        }

        if (_modtex == null) {
            ModIcon[] values = ModIcon.values();
            _modtex = new com.jme3.texture.Texture2D[values.length];
            for (ModIcon icon : values) {
                int idx = icon.ordinal();
                _modtex[idx] = ctx.getTextureCache().getTexture(icon.png());
                _modtex[idx].setWrap(WrapMode.Clamp);
            }
        }
    }

    /** Used when displaying bonus or penalty modifiers. */
    protected enum ModIcon {
        ARROW_UP (2, "arrow_up"),
        ARROW_DOWN (3, "arrow_down"),
        CANNOT (2, "cannot"),
        STAR (1, "star"),
        NUGGET (0, "nugget");

        ModIcon (int idx, String png) {
            _idx = idx;
            _png = png;
        }

        public int idx () {
            return _idx;
        }

        public String png () {
            return "/textures/ustatus/icon_" + _png + ".png";
        }

        protected final int _idx;
        protected final String _png;
    }

    protected BasicContext _ctx;

    /** Reference to the piece we are attached to. */
    protected Piece _piece;

    protected short _tick;

    protected Geometry _tgtquad, _ptquad;
    protected Geometry[] _modquad;

    protected short _pendingTick = -1;
    protected int _attackers;

    protected static com.jme3.texture.Texture2D[] _crosstex;
    protected static com.jme3.texture.Texture2D[] _modtex;

    protected static final String[] CROSS_TEXS = {
        "", "_q", "_1", "_2", "_n", "_skull" };

    // jME3 cutover (BUG-20): jME3's com.jme3.scene.shape.Quad winds its vertices
    // bottom-left, bottom-right, top-right, top-left with the texture's U axis running
    // along the quad's local +X (same winding RenderUtil.createTextTexture maps to for the
    // BUG-4 sideways-numbers fix). The fork's literal {(0,2),(0,0),(2,0),(2,2)} assigned U to
    // the quad's local +Y, rotating the attacker-count number 90 deg onto its side. Map
    // U->local X and V->local Y (v0 BL -> (0,0) ... v3 TL -> (0,2)) so the number reads upright.
    protected static final Vector2f[] PTARG_COORDS = {
        new Vector2f(0, 0),  // v0 bottom-left
        new Vector2f(2, 0),  // v1 bottom-right
        new Vector2f(2, 2),  // v2 top-right
        new Vector2f(0, 2),  // v3 top-left
    };

    protected static final float MOD_OFFSET = 3f * TILE_SIZE / 8f;
    protected static final Vector3f[] MOD_COORDS = {
        new Vector3f(-MOD_OFFSET,  MOD_OFFSET, 0f),
        new Vector3f(-MOD_OFFSET, -MOD_OFFSET, 0f),
        new Vector3f( MOD_OFFSET,  MOD_OFFSET, 0f),
        new Vector3f( MOD_OFFSET, -MOD_OFFSET, 0f),
    };

    protected static final ColorRGBA DEFAULT_COLOR =
        new ColorRGBA(230f/255f, 165f/255f, 20f/255f, 0.85f);
    protected static final ColorRGBA POSSIBLE_COLOR = ColorRGBA.White;
}
