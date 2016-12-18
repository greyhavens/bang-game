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

package com.jme.math;

import java.io.IOException;
import java.io.Serializable;

import com.jme.renderer.Camera;

import com.jme.util.export.InputCapsule;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.OutputCapsule;
import com.jme.util.export.Savable;

/**
 * <code>Frustum</code> defines a pyramidal frustum for purposes of generating
 * points randomly within portions of the view volume or its boundaries.
 * 
 * @author Andrzej Kapolka
 * @version $Id$
 */

public class Frustum implements Serializable, Savable {
    private static final long serialVersionUID = 1L;
    
    private Vector3f location, leftVec, upVec, direction;
    private float near, far, left, right, top, bottom;
    private boolean solid;
    
    /**
     * Constructor creates a new <code>Frustum</code> with the default camera
     * parameters.
     */
    public Frustum() {
        location = new Vector3f();
        leftVec = new Vector3f(Vector3f.UNIT_X);
        upVec = new Vector3f(Vector3f.UNIT_Y);
        direction = new Vector3f(Vector3f.UNIT_Z);
        near = 1f;
        far = 2f;
        left = -0.5f;
        right = 0.5f;
        top = 0.5f;
        bottom = -0.5f;
    }
    
    public void setNear(float near) {
        this.near = near;
    }
    
    public float getNear() {
        return near;
    }
    
    public void setFar(float far) {
        this.far = far;
    }
    
    public float getFar() {
        return far;
    }
    
    /**
     * <code>setSolid</code> sets whether the frustum is considered to be solid
     * when generating random points, or whether the points should be generated
     * only on the boundaries of the frustum.
     * 
     * @param solid
     *            whether or not the frustum is solid.
     */
    public void setSolid(boolean solid) {
        this.solid = solid;
    }

    /**
     * <code>isSolid</code> checks whether the frustum is considered to be
     * solid when calling {@link #random}.
     * 
     * @return whether or not the frustum is solid.
     */
    public boolean isSolid() {
        return solid;
    }
    
    /**
     * Copies the parameters of this frustum from the specified camera.
     *
     * @param scale a scale factor to apply to the camera parameters 
     * @param keepNearFar if true, preserve the currently configured near and
     * far plane distances rather than copying them from the camera
     */
    public void setFromCamera(Camera camera, Vector3f scale, boolean keepNearFar) {
        camera.getLocation().mult(scale, location);
        leftVec.set(camera.getLeft());
        upVec.set(camera.getUp());
        direction.set(camera.getDirection());
        
        float dscale = scaleAlongVector(direction, scale),
            lscale = scaleAlongVector(leftVec, scale),
            uscale = scaleAlongVector(upVec, scale);
        left = camera.getFrustumLeft() * lscale;
        right = camera.getFrustumRight() * lscale;
        top = camera.getFrustumTop() * uscale;
        bottom = camera.getFrustumBottom() * uscale;
        if (keepNearFar) {
            float nscale = near / (camera.getFrustumNear() * dscale);
            left *= nscale;
            right *= nscale;
            top *= nscale;
            bottom *= nscale;
        } else {
            near = camera.getFrustumNear() * dscale;
            far = camera.getFrustumFar() * dscale;
        }
    }
    
    /**
     * Computes the greatest pseudo-distance from the given point to any of the
     * planes of the frustum.  If the distance is less than or equal to
     * zero, the point lies within the frustum.
     */
    public float pseudoDistance(Vector3f pt) {
        float depth = direction.dot(pt) - direction.dot(location),
            ldist = leftVec.dot(pt) - leftVec.dot(location),
            udist = upVec.dot(pt) - upVec.dot(location),
            scale = depth / near;
        return Math.max(Math.max(near - depth, depth - far),
            Math.max(Math.max(ldist + left * scale, -right * scale - ldist),
            Math.max(udist - top * scale, bottom * scale - udist)));
    }
    
    /**
     * 
     * <code>random</code> returns a random point within the frustum (or on its
     * boundaries, depending on whether the frustum is configured to be solid).
     * This method uses the formulae for frustum (side) surface area and volume
     * given at <a href="http://mathworld.wolfram.com/PyramidalFrustum.html>
     * MathWorld</a>.  
     * 
     * @return a random point within the frustum or on its boundaries.
     */
    public Vector3f random() {
        float depth;
        boolean perim = false;
        if (solid) {
            // compute a random depth according to the volume distribution
            float n3 = FastMath.pow(near, 3f), f3 = FastMath.pow(far, 3f);
            depth = FastMath.pow(
                FastMath.nextRandomFloat() * (f3 - n3) + n3, 1/3f);
        } else {
            // compute a random depth according to the surface area
            // distribution
            float nwidth = right - left, nheight = top - bottom,
                narea = nwidth * nheight, n2 = near*near, f2 = far*far,
                farea = narea * (f2 / n2),
                nperim = 2 * nwidth + 2 * nheight,
                fperim = nperim * far / near,
                l2 = FastMath.sqr((right + top) / 2),
                s = far * FastMath.sqrt(1f + l2 / n2) - FastMath.sqrt(n2 + l2),
                sarea = 0.5f * (nperim + fperim) * s,
                tarea = narea + sarea + farea,
                r = FastMath.nextRandomFloat();
            if (r < narea / tarea) {
                depth = near;
            } else if (r > 1f - farea / tarea) {
                depth = far;
            } else {
                r = (r * tarea - narea) / sarea;
                depth = FastMath.sqrt(r * (f2 - n2) + n2);
                perim = true;
            }
        }
        
        // choose a random point on the slice or the perimeter
        Vector3f pt = new Vector3f(direction);
        pt.scaleAdd(depth, location);
        float dscale = depth / near,
            dleft = -left * dscale, dtop = top * dscale,
            dwidth = (right - left) * dscale,
            dheight = (top - bottom) * dscale,
            s = dleft - FastMath.nextRandomFloat() * dwidth,
            t = dtop - FastMath.nextRandomFloat() * dheight;
        if (perim) {
            float r = FastMath.nextRandomFloat();
            if (r < 0.5f) {
                s = (r < 0.25f) ? dleft : (dleft - dwidth);
            } else {
                t = (r < 0.75f) ? dtop : (dtop - dheight);
            }
        }
        pt.scaleAdd(s, leftVec, pt);
        pt.scaleAdd(t, upVec, pt);
        
        return pt;
    }
    
    @Override
	public void write(JMEExporter e) throws IOException {
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(solid, "solid", false);
        capsule.write(location, "location", Vector3f.ZERO);
        capsule.write(leftVec, "leftVec", Vector3f.UNIT_X);
        capsule.write(upVec, "upVec", Vector3f.UNIT_Y);
        capsule.write(direction, "direction", Vector3f.UNIT_Z);
        capsule.write(near, "near", 1f);
        capsule.write(far, "far", 2f);
        capsule.write(left, "left", -0.5f);
        capsule.write(right, "right", 0.5f);
        capsule.write(top, "top", 0.5f);
        capsule.write(bottom, "bottom", -0.5f);
    }

    @Override
	public void read(JMEImporter e) throws IOException {
        InputCapsule capsule = e.getCapsule(this);
        solid = capsule.readBoolean("solid", false);
        location = (Vector3f) capsule.readSavable("location", new Vector3f(
                Vector3f.ZERO));
        leftVec = (Vector3f) capsule.readSavable("leftVec", new Vector3f(
                Vector3f.UNIT_X));
        upVec = (Vector3f) capsule.readSavable("upVec", new Vector3f(
                Vector3f.UNIT_Y));
        direction = (Vector3f) capsule.readSavable("direction", new Vector3f(
                Vector3f.UNIT_Z));
        near = capsule.readFloat("near", 1f);
        far = capsule.readFloat("far", 2f);
        left = capsule.readFloat("left", -0.5f);
        right = capsule.readFloat("right", 0.5f);
        top = capsule.readFloat("top", 0.5f);
        bottom = capsule.readFloat("bottom", -0.5f);
    }

    @Override
	public Class<? extends Frustum> getClassTag() {
        return this.getClass();
    }
    
    /**
     * Given a unit vector and a scale, returns the amount of scale along the
     * vector.
     */
    protected static float scaleAlongVector (Vector3f vec, Vector3f scale)
    {
        return FastMath.sqrt(FastMath.sqr(vec.x * scale.x) +
            FastMath.sqr(vec.y * scale.y) + FastMath.sqr(vec.z * scale.z));
    }
}
