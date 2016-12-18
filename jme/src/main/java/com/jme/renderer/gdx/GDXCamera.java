/*
 * Copyright (c) 2003-2006 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.jme.renderer.gdx;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import com.jme.math.Matrix4f;
import com.jme.renderer.AbstractCamera;

public class GDXCamera extends AbstractCamera {

    private static final long serialVersionUID = 1L;

    public GDXCamera() {}

    /**
     * Constructor instantiates a new <code>GDXCamera</code> object. The
     * width and height are provided, which cooresponds to either the
     * width and height of the rendering window, or the resolution of the
     * fullscreen display.
     * @param width the width/resolution of the display.
     * @param height the height/resolution of the display.
     */
    public GDXCamera(int width, int height, Object parent) {
        super();
        this.width = width;
        this.height = height;
        this.parent = parent;
        parentClass = parent.getClass();
        onFrustumChange();
        onViewPortChange();
        onFrameChange();
    }

    /**
     * @return the width/resolution of the display.
     */
    @Override
    public int getHeight() {
        return height;
    }

    /**
     * @return the height/resolution of the display.
     */
    @Override
    public int getWidth() {
        return width;
    }

    /**
     * <code>resize</code> resizes this cameras view with the given width/height.
     * This is similar to constructing a new camera, but reusing the same
     * Object.
     * @param width int
     * @param height int
     */
    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        onViewPortChange();
    }

    /**
     * <code>onFrustumChange</code> updates the frustum when needed. It calls
     * super to set the new frustum values then sets the OpenGL frustum.
     * @see com.jme.renderer.Camera#onFrustumChange()
     */
    @Override
    public void onFrustumChange() {
        super.onFrustumChange();

        // set projection matrix
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        if ( !isParallelProjection() )
        {
            GL11.glFrustum(
                frustumLeft,
                frustumRight,
                frustumBottom,
                frustumTop,
                frustumNear,
                frustumFar);
        }
        else
        {
            GL11.glOrtho(
                frustumLeft,
                frustumRight,
                frustumTop,
                frustumBottom,
                frustumNear,
                frustumFar);
        }
        if ( projection != null )
        {
            tmp_FloatBuffer.rewind();
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, tmp_FloatBuffer);
            tmp_FloatBuffer.rewind();
            projection.readFloatBuffer( tmp_FloatBuffer );
        }

    }

    /**
     * <code>onViewportChange</code> updates the viewport when needed. It
     * calculates the viewport coordinates and then calls OpenGL's viewport.
     * @see com.jme.renderer.Camera#onViewPortChange()
     */
    @Override
    public void onViewPortChange() {
        // set view port
        int x = (int) (viewPortLeft * width);
        int y = (int) (viewPortBottom * height);
        int w = (int) ((viewPortRight - viewPortLeft) * width);
        int h = (int) ((viewPortTop - viewPortBottom) * height);
        GL11.glViewport(x, y, w, h);
    }

    /**
     * <code>onFrameChange</code> updates the view frame when needed. It calls
     * super to update the data and then uses GLU's lookat function to set the
     * OpenGL frame.
     * @see com.jme.renderer.Camera#onFrameChange()
     */
    @Override
    public void onFrameChange() {
        super.onFrameChange();

        if (parentClass == GDXTextureRenderer.class)
            ((GDXTextureRenderer)parent).activate();

        // set view matrix
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GLU.gluLookAt(
            location.x,
            location.y,
            location.z,
            location.x + direction.x,
            location.y + direction.y,
            location.z + direction.z,
            up.x,
            up.y,
            up.z);

        if ( modelView != null )
        {
            tmp_FloatBuffer.rewind();
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, tmp_FloatBuffer);
            tmp_FloatBuffer.rewind();
            modelView.readFloatBuffer( tmp_FloatBuffer );
        }

        if (parentClass == GDXTextureRenderer.class)
            ((GDXTextureRenderer)parent).deactivate();
    }

    private static final FloatBuffer tmp_FloatBuffer = BufferUtils.createFloatBuffer(16);
    private Matrix4f projection;

    @Override
    public Matrix4f getProjectionMatrix() {
        if ( projection == null )
        {
            projection = new Matrix4f();
            onFrustumChange();
        }
        return projection;
    }

    private Matrix4f modelView;

    @Override
    public Matrix4f getModelViewMatrix() {
        if ( modelView == null )
        {
            modelView = new Matrix4f();
            onFrameChange();
        }
        return modelView;
    }
}
