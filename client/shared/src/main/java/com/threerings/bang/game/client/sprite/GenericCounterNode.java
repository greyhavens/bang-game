//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.ColorRGBA;
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

import com.threerings.bang.game.data.piece.CounterInterface;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a counter prop along with the count value.
 *
 * <h3>jME3 cutover (Phase 2, cluster 1)</h3>
 *
 * Fork {@code BillboardNode} -> {@link BillboardControl} on a {@link Node}; the count {@code Quad}
 * node -> a {@link Quad} mesh in a {@link Geometry}; the fork {@code TextureState} -> a textured
 * {@link Material} (the text texture is regenerated on count change). The quad is rebuilt rather
 * than fork-{@code resize}d so its UVs match the text texture.
 */
public class GenericCounterNode extends Node
{
    /**
     * Creates the geometry
     */
    public void createGeometry (CounterInterface counter, BasicContext ctx)
    {
        // create a billboard to display this mine's current count
        _ctx = ctx;
        _quad = new Geometry("counter", new Quad(25, 25));
        _quad.setLocalTranslation(-25 / 2f, -25 / 2f, 0f);
        _mat = RenderUtil.createTextureMaterial(_ctx, (Texture2D)null);
        RenderUtil.applyBlendAlpha(_mat);
        RenderUtil.applyOverlayZBuf(_mat);
        _quad.setMaterial(_mat);
        RenderUtil.setOverlay(_quad);
        updateCount(counter);
        Node bbn = new Node("cbillboard");
        bbn.addControl(new BillboardControl());
        bbn.attachChild(_quad);
        bbn.setLocalTranslation(new Vector3f(
                    0, 0, (int)((1.0 + 0.5) * TILE_SIZE)));
        attachChild(bbn);
        _quad.setCullHint(CullHint.Always);
    }

    /**
     * Updates the count hovering over the sprite.
     */
    public void updateCount (CounterInterface counter)
    {
        // recompute and display our count
        if (_dcount != counter.getCount()) {
            Vector2f[] tcoords = new Vector2f[4];
            Texture2D tex = RenderUtil.createTextTexture(
                _ctx, BangUI.COUNTER_FONT, ColorRGBA.Gray,
                ColorRGBA.DarkGray, String.valueOf(counter.getCount()),
                tcoords, null);
            // resize our quad to accommodate the text and set its UVs
            float qrat = TILE_SIZE * 0.8f / tcoords[2].y;
            float qw = qrat * tcoords[2].x, qh = qrat * tcoords[2].y;
            Quad mesh = new Quad(qw, qh);
            mesh.setBuffer(VertexBuffer.Type.TexCoord, 2,
                BufferUtils.createFloatBuffer(tcoords));
            mesh.updateBound();
            _quad.setMesh(mesh);
            // jME3 Quad is corner-anchored ((0,0)..(w,h)); the fork Quad was
            // center-anchored (-w/2..+w/2). Recenter on the billboard pivot so
            // the number stays centered (BUG-22).
            _quad.setLocalTranslation(-qw / 2f, -qh / 2f, 0f);
            _mat.setTexture("ColorMap", tex);
            _dcount = counter.getCount();
            _quad.setCullHint(CullHint.Dynamic);
        }
    }

    protected BasicContext _ctx;
    protected Geometry _quad;
    protected Material _mat;
    protected int _dcount = -1;
}
