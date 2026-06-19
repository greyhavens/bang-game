//
// $Id$

package com.threerings.bang.game.client;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.material.RenderState.TestFunction;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Dome;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.BufferUtils;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

/**
 * Used to display the sky.
 *
 * <h3>jME3 cutover (Phase 2, cluster 3)</h3>
 *
 * Rebuilt off the fork render-state pipeline (migration map §2.3/§2.4):
 * <ul>
 *   <li>The dome is a jME3 {@link Dome} {@link Mesh} wrapped in a {@link Geometry} (was a fork
 *   {@code Dome}/{@code SharedMesh} node), with an {@code Unshaded} gradient {@link Material}.</li>
 *   <li>The cloud plane was a fork {@code Disk} (no jME3 equivalent — migration map §2.3 REBUILD);
 *   it is rebuilt here as a small custom radial-fan {@link Mesh} with the same per-vertex alpha
 *   falloff.</li>
 *   <li>Sky/cloud render states (light-off, depth-always-no-write, blend) move to the
 *   {@code Unshaded} material's render state + the {@code Sky} render bucket.</li>
 *   <li>The fork animated the cloud texture by transforming the {@code Texture} object; jME3
 *   textures carry no transform, so cloud scrolling rewrites the cloud mesh's texture-coordinate
 *   buffer each frame.</li>
 * </ul>
 *
 * <p>Phase-4 fidelity note: cloud scroll and gradient mapping reproduce the fork behaviour at the
 * mesh/UV level; exact visual parity (dome shading, cloud edge fade) is a Phase-4 review item.
 */
public class SkyNode extends Node
{
    public SkyNode (BasicContext ctx)
    {
        super("skynode");
        _ctx = ctx;

        setQueueBucket(Bucket.Sky);
        setCullHint(CullHint.Never);

        // create the dome geometry
        Dome dmesh = new Dome(new Vector3f(), DOME_PLANES, DOME_RADIAL_SAMPLES, DOME_RADIUS, true);
        _dome = new Geometry("dome", dmesh);
        Quaternion rot = new Quaternion();
        rot.fromAngleNormalAxis(FastMath.HALF_PI, Vector3f.UNIT_X);
        _dome.setLocalRotation(rot);
        _dome.setLocalTranslation(new Vector3f(0f, 0f, -10f));
        _dmat = new Material(ctx.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        _dmat.getAdditionalRenderState().setDepthTest(true);
        _dmat.getAdditionalRenderState().setDepthWrite(false);
        // Depth func LessOrEqual (NOT Always): jME3 renders the Sky bucket AFTER the Opaque bucket
        // (RenderManager.renderViewPortQueues: Opaque then Sky), so a depth-func of Always made the
        // dome paint OVER the already-drawn buildings, slicing their tops off at the dome's horizon.
        // The fork used CF_ALWAYS, but jME1 drew the sky FIRST so it sat behind geometry; under
        // jME3's reversed order we instead let the closer opaque geometry occlude the sky (LEQUAL),
        // which is the same visual result (sky fills only the empty background).
        _dmat.getAdditionalRenderState().setDepthFunc(TestFunction.LessOrEqual);
        // The fork dome carried no CullState, so both faces drew; jME3's Unshaded material
        // defaults to back-face culling, which (combined with the inside-view winding/rotation)
        // culled the dome away and left the viewport clear-colour filling the upper view. Match
        // the fork by drawing both faces.
        _dmat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        _dome.setMaterial(_dmat);
        _dome.setModelBound(new BoundingBox());
        attachChild(_dome);

        // create the cloud plane geometry, which fades out towards the edge
        _cmesh = createCloudMesh();
        _clouds = new Geometry("clouds", _cmesh);
        _clouds.setLocalTranslation(new Vector3f(0f, 0f, CLOUD_HEIGHT));
        _cmat = RenderUtil.createTextureMaterial(ctx, "textures/environ/clouds.png");
        _cmat.setBoolean("VertexColor", true);
        RenderUtil.applyBlendAlpha(_cmat);
        RenderUtil.applyOverlayZBuf(_cmat);
        // fork cloud Disk carried no CullState; draw both faces so the plane shows from below
        _cmat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        Texture2D ctex = (Texture2D)_cmat.getTextureParam("ColorMap").getTextureValue();
        ctex.setWrap(WrapMode.Repeat);
        _clouds.setMaterial(_cmat);
        _clouds.setModelBound(new BoundingBox());
        _clouds.setCullHint(CullHint.Never);
        // The cloud fan is a translucent (per-vertex alpha + alpha-blended texture) overlay; it must
        // composite OVER the gradient dome. The node sits in Bucket.Sky so the opaque dome is drawn
        // after the Opaque bucket and is occluded by buildings (BUG-21 fix). jME3's Sky bucket,
        // however, does no transparency sorting/compositing and draws sky geometry in scene order
        // with no guarantee the alpha-blended clouds land on top of the dome -- in practice the
        // clouds were lost. Override the cloud Geometry's bucket to Transparent so it is drawn in
        // the alpha-composited transparent pass (after Opaque and Sky), painting the clouds over the
        // dome exactly as the fork's alpha-blended cloud Disk did. The dome stays in Bucket.Sky.
        _clouds.setQueueBucket(Bucket.Transparent);
        attachChild(_clouds);
    }

    /**
     * Initializes the sky geometry using data from the given board and saves the board reference
     * for later updates.
     */
    public void createBoardSky (BangBoard board)
    {
        _board = board;
        refreshGradient();
    }

    /**
     * Releases the resources created by this node.
     */
    public void cleanup ()
    {
        // jME3 reclaims the gradient texture when the material/geometry is released.
    }

    /**
     * Updates the gradient texture according to the board parameters.
     */
    public void refreshGradient ()
    {
        _dmat.setTexture("ColorMap", createGradientTexture());
    }

    @Override // documentation inherited
    public void updateLogicalState (float tpf)
    {
        super.updateLogicalState(tpf);

        // match the position of the camera
        setLocalTranslation(_ctx.getCameraHandler().getCamera().getLocation());

        if (_board == null) {
            return;
        }

        // scroll the clouds according to the wind velocity (rewriting the cloud UVs, since jME3
        // textures carry no transform)
        float wdir = _board.getWindDirection(), wspeed = _board.getWindSpeed();
        _cloudU += tpf * wspeed * FastMath.cos(wdir) * 0.001f;
        _cloudV += tpf * wspeed * FastMath.sin(wdir) * 0.001f;
        applyCloudScroll();
    }

    /**
     * Creates the cloud plane mesh: a radial fan of {@link #CLOUD_RADIAL_SAMPLES} sectors and
     * {@link #CLOUD_SHELL_SAMPLES} shells, with per-vertex alpha that fades out toward the edge
     * (the fork {@code Disk} behaviour).
     */
    protected Mesh createCloudMesh ()
    {
        int radial = CLOUD_RADIAL_SAMPLES, shells = CLOUD_SHELL_SAMPLES;
        int rings = shells - 1;
        int vcount = 1 + rings * radial;

        Vector3f[] verts = new Vector3f[vcount];
        Vector3f[] uvs = new Vector3f[vcount]; // store base UVs (xy) for scrolling
        float[] colors = new float[vcount * 4];

        // center vertex
        verts[0] = new Vector3f();
        uvs[0] = new Vector3f(0.5f, 0.5f, 0f);
        colors[0] = colors[1] = colors[2] = colors[3] = 1f;

        int vi = 1;
        for (int ii = 0; ii < radial; ii++) {
            float ang = FastMath.TWO_PI * ii / radial;
            float cos = FastMath.cos(ang), sin = FastMath.sin(ang);
            for (int jj = 0; jj < rings; jj++) {
                float d = (float)(jj + 1) / rings;
                float r = d * CLOUD_RADIUS;
                verts[vi] = new Vector3f(cos * r, sin * r, 0f);
                uvs[vi] = new Vector3f(0.5f + cos * d * 0.5f, 0.5f + sin * d * 0.5f, 0f);
                float a = (d < 0.5f) ? 1f : 1f - (d - 0.5f) / 0.5f;
                colors[vi*4] = 1f; colors[vi*4+1] = 1f; colors[vi*4+2] = 1f; colors[vi*4+3] = a;
                vi++;
            }
        }

        // build triangle indices (fan from center to first ring, then quads between rings)
        java.util.ArrayList<Integer> idx = new java.util.ArrayList<Integer>();
        for (int ii = 0; ii < radial; ii++) {
            int next = (ii + 1) % radial;
            int base = 1 + ii * rings, nbase = 1 + next * rings;
            // center triangle
            idx.add(0); idx.add(base); idx.add(nbase);
            for (int jj = 0; jj < rings - 1; jj++) {
                int a = base + jj, b = base + jj + 1;
                int c = nbase + jj, d = nbase + jj + 1;
                idx.add(a); idx.add(b); idx.add(d);
                idx.add(a); idx.add(d); idx.add(c);
            }
        }

        _cloudBaseUV = new float[vcount * 2];
        float[] uvflat = new float[vcount * 2];
        for (int ii = 0; ii < vcount; ii++) {
            _cloudBaseUV[ii*2] = uvs[ii].x * CLOUD_TEXTURE_SCALE;
            _cloudBaseUV[ii*2+1] = uvs[ii].y * CLOUD_TEXTURE_SCALE;
            uvflat[ii*2] = _cloudBaseUV[ii*2];
            uvflat[ii*2+1] = _cloudBaseUV[ii*2+1];
        }
        int[] indices = new int[idx.size()];
        for (int ii = 0; ii < indices.length; ii++) {
            indices[ii] = idx.get(ii);
        }

        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(verts));
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, uvflat);
        mesh.setBuffer(VertexBuffer.Type.Color, 4, colors);
        mesh.setBuffer(VertexBuffer.Type.Index, 3, indices);
        mesh.updateBound();
        mesh.updateCounts();
        return mesh;
    }

    /**
     * Re-applies the accumulated cloud scroll offset to the cloud mesh's texture coordinates.
     */
    protected void applyCloudScroll ()
    {
        VertexBuffer tc = _cmesh.getBuffer(VertexBuffer.Type.TexCoord);
        FloatBuffer fb = (FloatBuffer)tc.getData();
        fb.rewind();
        for (int ii = 0; ii < _cloudBaseUV.length; ii += 2) {
            fb.put(_cloudBaseUV[ii] + _cloudU);
            fb.put(_cloudBaseUV[ii+1] + _cloudV);
        }
        tc.updateData(fb);
    }

    /**
     * Creates and returns the gradient texture that fades from the horizon color to the overhead
     * color.
     */
    protected Texture2D createGradientTexture ()
    {
        int size = GRADIENT_TEXTURE_SIZE;
        ByteBuffer pbuf = ByteBuffer.allocateDirect(size * 3);
        ColorRGBA hcolor = RenderUtil.createColorRGBA(
            _board.getSkyHorizonColor()),
                ocolor = RenderUtil.createColorRGBA(
            _board.getSkyOverheadColor()),
                tcolor = new ColorRGBA();
        float falloff = _board.getSkyFalloff();

        for (int i = 0; i < size; i++) {
            float s = i / (size-1f),
                a = FastMath.exp(-falloff * s);
            tcolor.interpolateLocal(ocolor, hcolor, a);

            pbuf.put((byte)(tcolor.r * 255));
            pbuf.put((byte)(tcolor.g * 255));
            pbuf.put((byte)(tcolor.b * 255));
        }
        pbuf.rewind();

        Image img = new Image(Image.Format.RGB8, 1, size, pbuf,
            com.jme3.texture.image.ColorSpace.sRGB);
        Texture2D texture = new Texture2D(img);
        texture.setMagFilter(MagFilter.Bilinear);
        texture.setWrap(WrapMode.Clamp);
        return texture;
    }

    /** The application context. */
    protected BasicContext _ctx;

    /** The dome geometry and its gradient material. */
    protected Geometry _dome;
    protected Material _dmat;

    /** The cloud plane geometry, mesh, and material. */
    protected Geometry _clouds;
    protected Mesh _cmesh;
    protected Material _cmat;

    /** Base (un-scrolled) cloud texture coordinates. */
    protected float[] _cloudBaseUV;

    /** Accumulated cloud scroll offset. */
    protected float _cloudU, _cloudV;

    /** The current board object. */
    protected BangBoard _board;

    /** The number of vertical samples in the sky dome. */
    protected static final int DOME_PLANES = 16;

    /** The number of radial samples for the sky dome. */
    protected static final int DOME_RADIAL_SAMPLES = 32;

    /** The radius of the sky dome. */
    protected static final float DOME_RADIUS = 1000f;

    /** The size of the one-dimensional gradient texture. */
    protected static final int GRADIENT_TEXTURE_SIZE = 1024;

    /** The number of rings in the cloud plane. */
    protected static final int CLOUD_SHELL_SAMPLES = 16;

    /** The number of radial samples in the cloud plane. */
    protected static final int CLOUD_RADIAL_SAMPLES = 16;

    /**
     * The radius of the cloud plane. Matches the fork exactly (10000, with a 500 cloud height).
     * <p>Under the jME3 board camera the cloud disk's visible low-elevation rim sits at slant
     * distance &radic;(10000&sup2;+500&sup2;) &asymp; 10012, just beyond a 10000 far-clip, so the
     * on-screen portion of the disk was being clipped away and NO clouds drew. The fix is NOT to
     * shrink the disk (its screen position relative to the fixed-radius gradient dome changes when
     * scaled, which visibly regressed the look) but to push the board camera far plane out far
     * enough that the full-size disk is never far-clipped (see {@code JmeApp.FRUSTUM_FAR}); that is
     * the look-correct, fork-matching result.
     */
    protected static final float CLOUD_RADIUS = 10000f;

    /** The height of the cloud plane (matches the fork). */
    protected static final float CLOUD_HEIGHT = 500f;

    /** The texture scale (tiling factor) of the cloud texture. */
    protected static final float CLOUD_TEXTURE_SCALE = 10f;
}
