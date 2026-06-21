//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.backend;

import java.nio.ByteBuffer;

import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;

import com.jmex.bui.BImage;

/**
 * The {@link BImageBacking} implementation on jMonkeyEngine 3.
 *
 * <p> The jME3 {@link com.jme3.texture.Image} handed to {@link #setImage} is a plain pixel
 * container produced by {@link BImage} with straight R,G,B[,A] byte order, i.e.
 * {@link com.jme3.texture.Image.Format#RGB8 RGB8} / {@link com.jme3.texture.Image.Format#RGBA8
 * RGBA8} (which map to GL_RGB/GL_RGBA + GL_UNSIGNED_BYTE, byte-order independent). The pixels
 * become a {@link Texture2D} on an
 * {@code Unshaded} material; rendering issues a textured {@link Quad} geometry through the
 * backend's render manager, honoring the translate and scissor stacks set up by
 * {@link Jme3RenderBackend}.
 */
public class Jme3ImageBacking implements BImageBacking
{
    public Jme3ImageBacking (Jme3RenderBackend backend, BImage image)
    {
        _backend = backend;
        _image = image;
        _material = new Material(backend.getAssetManager(),
                                 "Common/MatDefs/Misc/Unshaded.j3md");
        _material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        _material.getAdditionalRenderState().setDepthTest(false);
        _material.getAdditionalRenderState().setDepthWrite(false);
        // The GUI ortho camera looks toward +Z and a jME3 Quad's front face (+Z normal) faces
        // away from it, so the default Back cull mode would discard every textured UI quad.
        _material.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
    }

    // documentation inherited from interface BImageBacking
    public void setImage (com.jme3.texture.Image image)
    {
        _twidth = image.getWidth();
        _theight = image.getHeight();

        // BImage builds the image as BGR8 (opaque) or ABGR8 (with alpha) -- see BImage; both
        // map directly onto jME3 with no channel reshuffle.
        com.jme3.texture.Image.Format format = image.getFormat();

        // copy the source pixels into a fresh direct buffer for jME3 to own
        ByteBuffer src = image.getData(0);
        src.rewind();
        ByteBuffer dst = BufferUtils.createByteBuffer(src.remaining());
        dst.put(src);
        dst.flip();

        com.jme3.texture.Image jimg = new com.jme3.texture.Image(
            format, _twidth, _theight, dst, com.jme3.texture.image.ColorSpace.sRGB);
        Texture2D texture = new Texture2D(jimg);
        texture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        texture.setMagFilter(Texture.MagFilter.Bilinear);
        _material.setTexture("ColorMap", texture);
    }

    // documentation inherited from interface BImageBacking
    public void setTransparent (boolean transparent)
    {
        _material.getAdditionalRenderState().setBlendMode(
            transparent ? RenderState.BlendMode.Alpha : RenderState.BlendMode.Off);
    }

    // documentation inherited from interface BImageBacking
    public void setTextureCoords (int sx, int sy, int swidth, int sheight)
    {
        _lx = sx / (float)_twidth;
        _ly = sy / (float)_theight;
        _ux = (sx + swidth) / (float)_twidth;
        _uy = (sy + sheight) / (float)_theight;
    }

    // documentation inherited from interface BImageBacking
    public void render (Object renderer, int tx, int ty, int twidth, int theight, float alpha)
    {
        if (_backend.getRenderManager() == null) {
            return;
        }
        // BUI repaints in immediate mode every frame, so this runs many times per frame. Allocating
        // a fresh Quad+Geometry each call would leak the quad's VertexBuffers into jME3's
        // NativeObjectManager on GL upload (the registrations are never released for throwaway
        // geometry), growing the manager's ref map without bound. Reuse one cached quad/geometry and
        // resize it in place; Quad.updateGeometry / Mesh.setBuffer reuse the existing VertexBuffers.
        // Multiply the quad extent by the active (non-uniform) render scale (1f for non-scaled
        // windows, so the legacy path is unchanged); position is scaled below via toScreenX/Y about
        // the pivot.
        float qw = twidth * _backend.getRenderScaleX(), qh = theight * _backend.getRenderScaleY();
        if (_quad == null) {
            _quad = new Quad(qw, qh);
            _geom = new Geometry("bui-image", _quad);
            _geom.setMaterial(_material);
        } else {
            _quad.updateGeometry(qw, qh);
        }
        Quad quad = _quad;
        // jME3's Quad lays out tex coords (0,0)-(1,1); rewrite to our sub-rect. Quad winds
        // bottom-left, bottom-right, top-right, top-left.
        VertexBuffer tc = quad.getBuffer(VertexBuffer.Type.TexCoord);
        BufferUtils.setInBuffer(new Vector2f(_lx, _ly), (java.nio.FloatBuffer)tc.getData(), 0);
        BufferUtils.setInBuffer(new Vector2f(_ux, _ly), (java.nio.FloatBuffer)tc.getData(), 1);
        BufferUtils.setInBuffer(new Vector2f(_ux, _uy), (java.nio.FloatBuffer)tc.getData(), 2);
        BufferUtils.setInBuffer(new Vector2f(_lx, _uy), (java.nio.FloatBuffer)tc.getData(), 3);
        tc.updateData(tc.getData());

        _material.setColor("Color", new ColorRGBA(1f, 1f, 1f, alpha));

        Geometry geom = _geom;
        geom.setLocalTranslation(_backend.toScreenX(_backend.getTranslateX() + tx),
                                 _backend.toScreenY(_backend.getTranslateY() + ty), 0);
        geom.updateGeometricState();
        _backend.getRenderManager().renderGeometry(geom);
    }

    // documentation inherited from interface BImageBacking
    public void acquire ()
    {
        // jME3 uploads the texture lazily on first render; no eager pooling here.
    }

    // documentation inherited from interface BImageBacking
    public void release ()
    {
        // The host's jME3 renderer reclaims GL textures; nothing to do per-image.
    }

    protected Jme3RenderBackend _backend;
    protected BImage _image;
    protected Material _material;

    /** Reused quad/geometry for this backing (see {@link #render}); resized in place to avoid
     * leaking a fresh mesh's VertexBuffers into jME3's NativeObjectManager every frame. */
    protected Quad _quad;
    protected Geometry _geom;
    protected int _twidth, _theight;
    protected float _lx, _ly, _ux = 1f, _uy = 1f;
}
