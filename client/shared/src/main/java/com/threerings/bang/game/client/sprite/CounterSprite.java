//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Counter;
import com.threerings.bang.game.data.piece.Piece;

import com.threerings.openal.SoundGroup;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a counter prop along with the count value.
 */
public class CounterSprite extends PropSprite
{
    public CounterSprite (String type)
    {
        super(type);
    }

    @Override // documentation inherited
    public boolean hasTooltip ()
    {
        return true;
    }

    @Override // documentation inherited
    public Coloring getColoringType ()
    {
        return Coloring.DYNAMIC;
    }

    @Override // documentation inherited
    public void init (BasicContext ctx, BoardView view, BangBoard board,
                      SoundGroup sounds, Piece piece, short tick)
    {
        super.init(ctx, view, board, sounds, piece, tick);
        updateCount(piece);
    }

    @Override // documentation inherited
    public void updated (Piece piece, short tick)
    {
        super.updated(piece, tick);
        updateCount(piece);
    }

    /**
     * Updates the count hovering over the sprite.
     */
    protected void updateCount (Piece piece)
    {
        // recompute and display our nugget count
        Counter counter = (Counter)piece;
        if (_piece.owner >= 0 && _dcount != counter.count) {
            Vector2f[] tcoords = new Vector2f[4];
            Texture2D tex = RenderUtil.createTextTexture(
                _ctx, BangUI.COUNTER_FONT, getJPieceColor(_piece.owner),
                getDarkerPieceColor(_piece.owner), String.valueOf(counter.count),
                tcoords, null);
            // resize our quad to accommodate the text and set its UVs
            float qrat = TILE_SIZE * 0.8f / tcoords[2].y;
            float qw = qrat * tcoords[2].x, qh = qrat * tcoords[2].y;
            Quad mesh = new Quad(qw, qh);
            mesh.setBuffer(VertexBuffer.Type.TexCoord, 2,
                BufferUtils.createFloatBuffer(tcoords));
            mesh.updateBound();
            _counter.setMesh(mesh);
            // jME3 Quad is corner-anchored ((0,0)..(w,h)); the fork Quad was
            // center-anchored (-w/2..+w/2). Recenter on the billboard pivot so
            // the number floats centered above the claim rather than off-pivot
            // (up-and-right) and sunk into the claim rock mesh (BUG-22).
            _counter.setLocalTranslation(-qw / 2f, -qh / 2f, 0f);
            _mat.setTexture("ColorMap", tex);
            _dcount = counter.count;
            _counter.setCullHint(CullHint.Dynamic);
        }
    }

    @Override // documentation inherited
    protected void createGeometry ()
    {
        super.createGeometry();

        // create a billboard to display this mine's current count
        _counter = new Geometry("counter", new Quad(25, 25));
        _counter.setLocalTranslation(-25 / 2f, -25 / 2f, 0f);
        _mat = RenderUtil.createTextureMaterial(_ctx, (Texture2D)null);
        RenderUtil.applyBlendAlpha(_mat);
        RenderUtil.applyOverlayZBuf(_mat);
        _counter.setMaterial(_mat);
        RenderUtil.setOverlay(_counter);
        Node bbn = new Node("cbillboard");
        bbn.addControl(new BillboardControl());
        bbn.attachChild(_counter);
        bbn.setLocalTranslation(new Vector3f(
                    0, 0, (_config.height + 0.5f) * TILE_SIZE));
        attachChild(bbn);
        _counter.setCullHint(CullHint.Always);
    }

    @Override // documentation inherited
    protected String getHelpIdent (int pidx)
    {
        return (pidx == _piece.owner ? "own_" : "other_") + _config.type;
    }

    protected Geometry _counter;
    protected Material _mat;
    protected int _dcount = -1;
}
