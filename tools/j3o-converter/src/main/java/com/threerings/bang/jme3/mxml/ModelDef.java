//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/nenya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.bang.jme3.mxml;

import java.nio.FloatBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import com.samskivert.util.PropertiesUtil;
import com.samskivert.util.StringUtil;

/**
 * The neutral parsed representation of a Bang! Howdy <code>.mxml</code> model (Epic 4
 * Phase 2a): the data structure {@code ModelParser} populates straight from the XML, ported
 * from the fork's <code>com.threerings.jme.tools.ModelDef</code> onto jME3 math types
 * (<code>com.jme3.math.*</code>). The jME3 <code>.j3o</code> and glTF writers (Phase 2b)
 * consume this representation.
 *
 * <p>Deliberately NOT ported from the fork original: the fork-scene-typed tail that built a
 * live fork scene graph from the parsed data — <code>getSpatial</code>/<code>createSpatial</code>/
 * <code>setTransform</code>/<code>resolveReferences</code> on the spatial defs,
 * <code>createModel</code>/<code>createController</code>/<code>pruneUnusedNodes</code>,
 * <code>SkinVertex.getBones</code>, and the animation-driven mesh-merge machinery
 * (<code>TransformNode</code>, <code>createTransformTree</code>, <code>mergeSpatials</code>),
 * which extended the fork's <code>com.jme.scene.Node</code> to decide (via
 * <code>updateGeometricState</code> across every animation frame) which compatible meshes kept a
 * constant relative transform and could be merged. The pure-data halves of that machinery
 * ({@link SpatialDef#canMerge}, {@link SpatialDef#merge}, {@link #epsilonEquals}) are kept, so
 * Phase 2b can reimplement the merge decision on jME3 scene types if the optimization is wanted —
 * merging is a compile-time mesh-count optimization, not a correctness requirement.
 */
public class ModelDef
{
    /** The base class of nodes in the model. */
    public abstract static class SpatialDef
    {
        /** The node's name. */
        public String name;

        /** The name of the node's parent. */
        public String parent;

        /** The node's transformation. */
        public float[] translation;
        public float[] rotation;
        public float[] scale;

        /**
         * Stores the names of all bones referenced by this spatial in the supplied set.
         */
        public void getBoneNames (HashSet<String> bones)
        {
            // nothing by default
        }

        /** Checks whether it's possible (disregarding issues of transformation) to merge
         * the specified spatial into this one. */
        public abstract boolean canMerge (Properties props, SpatialDef other);

        /** Merges another spatial into this one.
         *
         * @param xform the matrix taking the other spatial's coordinate space into this one's
         * (in the fork this came from the dropped <code>TransformNode</code> preprocessing). */
        public abstract void merge (SpatialDef other, Matrix4f xform);
    }

    /** A rigid triangle mesh. */
    public static class TriMeshDef extends SpatialDef
    {
        /** The geometry offset transform. */
        public float[] offsetTranslation;
        public float[] offsetRotation;
        public float[] offsetScale;

        /** Whether or not the mesh allows back face culling. */
        public boolean solid;

        /** The texture of the mesh, if any. */
        public String texture;

        /** Whether or not the mesh is (partially) transparent. */
        public boolean transparent;

        /** The vertices of the mesh. */
        public HashArrayList<Vertex> vertices = new HashArrayList<Vertex>();

        /** The triangle indices. */
        public ArrayList<Integer> indices = new ArrayList<Integer>();

        /** Whether or not any of the vertices have texture coordinates. */
        public boolean tcoords;

        public void addVertex (Vertex vertex)
        {
            int idx = vertices.indexOf(vertex);
            if (idx != -1) {
                indices.add(idx);
            } else {
                indices.add(vertices.size());
                vertices.add(vertex);
            }
            tcoords = tcoords || vertex.tcoords != null;
        }

        // documentation inherited
        public boolean canMerge (Properties props, SpatialDef other)
        {
            if (getClass() != other.getClass()) {
                return false; // require exact class match
            }
            TriMeshDef omesh = (TriMeshDef)other;
            return solid == omesh.solid && transparent == omesh.transparent &&
                Objects.equals(texture, omesh.texture) &&
                PropertiesUtil.getSubProperties(props, name).equals(
                    PropertiesUtil.getSubProperties(props, omesh.name));
        }

        // documentation inherited
        public void merge (SpatialDef other, Matrix4f xform)
        {
            TriMeshDef omesh = (TriMeshDef)other;

            // prepend the inverse of the offset transformation
            xform = getOffsetTransform().invertLocal().multLocal(xform);

            // and append the other's offset
            xform.multLocal(omesh.getOffsetTransform());

            // extract the rotation to transform normals (fork-exact: see toRotationQuat)
            Quaternion xrot = toRotationQuat(xform);

            // transform the vertices and add them in
            for (Vertex vertex : omesh.vertices) {
                vertex.transform(xform, xrot);
            }
            for (int idx : omesh.indices) {
                addVertex(omesh.vertices.get(idx));
            }
        }

        /** Gets the matrix representing the offset transform. */
        public Matrix4f getOffsetTransform ()
        {
            Vector3f otrans = new Vector3f(), oscale = new Vector3f(1f, 1f, 1f);
            Quaternion orot = new Quaternion();
            if (offsetTranslation != null) {
                otrans.set(offsetTranslation[0], offsetTranslation[1], offsetTranslation[2]);
            }
            if (offsetRotation != null) {
                orot.set(offsetRotation[0], offsetRotation[1], offsetRotation[2],
                    offsetRotation[3]);
            }
            if (offsetScale != null) {
                oscale.set(offsetScale[0], offsetScale[1], offsetScale[2]);
            }
            return setTransform(otrans, orot, oscale, new Matrix4f());
        }

        /** Returns {@link #texture} reduced to a plain filename (the exporter sometimes wrote
         * full DOS paths), or <code>null</code> if the mesh has no texture. This is the
         * normalization the fork applied in its <code>configureMesh</code>. */
        public String getTextureFilename ()
        {
            int sidx = (texture == null) ? -1 :
                Math.max(texture.lastIndexOf('/'), texture.lastIndexOf('\\'));
            return (sidx == -1) ? texture : texture.substring(sidx + 1);
        }

        /** Reorders the vertices to optimize for vertex cache utilization.  Uses the algorithm
         * described in Tom Forsyth's article
         * <a href="http://home.comcast.net/~tom_forsyth/papers/fast_vert_cache_opt.html">
         * Linear-Speed Vertex Cache Optimization</a>. In the fork pipeline this ran after
         * parsing/merging and before the mesh buffers were built; the Phase 2b writers should
         * call it at the same point.
         */
        public void optimizeVertexOrder ()
        {
            // start by compiling a list of triangles cross-linked with the vertices they use
            // (we use a linked hash set to ensure consistent iteration order for serialization)
            LinkedHashSet<Triangle> triangles = new LinkedHashSet<Triangle>();
            for (int ii = 0, nn = indices.size(); ii < nn; ii += 3) {
                Vertex[] tverts = new Vertex[] {
                    vertices.get(indices.get(ii)),
                    vertices.get(indices.get(ii + 1)),
                    vertices.get(indices.get(ii + 2))
                };
                Triangle triangle = new Triangle(tverts);
                for (Vertex tvert : tverts) {
                    if (tvert.triangles == null) {
                        tvert.triangles = new ArrayList<Triangle>();
                    }
                    tvert.triangles.add(triangle);
                }
                triangles.add(triangle);
            }

            // init the scores
            for (Vertex vertex : vertices) {
                vertex.updateScore(Integer.MAX_VALUE);
            }

            // clear the vertices and indices to prepare for readdition
            vertices.clear();
            indices.clear();

            // while there are triangles remaining, keep adding the one with the best score
            // (as determined by its LRU cache position and number of remaining triangles)
            HashArrayList<Vertex> vcache = new HashArrayList<Vertex>();
            while (!triangles.isEmpty()) {
                // first look for triangles in the cache
                Triangle bestTriangle = null;
                float bestScore = -1f;
                for (Vertex vertex : vcache) {
                    for (Triangle triangle : vertex.triangles) {
                        float score = triangle.getScore();
                        if (score > bestScore) {
                            bestTriangle = triangle;
                            bestScore = score;
                        }
                    }
                }

                // if that didn't work, scan the full list
                if (bestTriangle == null) {
                    for (Triangle triangle : triangles) {
                        float score = triangle.getScore();
                        if (score > bestScore) {
                            bestTriangle = triangle;
                            bestScore = score;
                        }
                    }
                }

                // add and update the vertices from the best triangle
                triangles.remove(bestTriangle);
                for (Vertex vertex : bestTriangle.vertices) {
                    addVertex(vertex);
                    vertex.triangles.remove(bestTriangle);
                    vcache.remove(vertex);
                    vcache.add(0, vertex);
                }

                // update the scores of the vertices in the cache
                for (int ii = 0, nn = vcache.size(); ii < nn; ii++) {
                    vcache.get(ii).updateScore(ii);
                }

                // trim the excess (if any) from the end of the cache
                while (vcache.size() > 64) {
                    vcache.remove(vcache.size() - 1);
                }
            }
        }
    }

    /** A triangle mesh that deforms according to bone positions. */
    public static class SkinMeshDef extends TriMeshDef
    {
        @Override
        public void getBoneNames (HashSet<String> bones)
        {
            for (Vertex vertex : vertices) {
                bones.addAll(((SkinVertex)vertex).boneWeights.keySet());
            }
        }

        /**
         * Divides the vertices up by weight group (the set of bones influencing them) and
         * reorders the vertex list so that each group's vertices are contiguous, remapping
         * {@link #indices} accordingly. This is the pure-data half of the fork's
         * <code>SkinMeshDef.configureMesh</code>; in the fork pipeline it ran after
         * {@link #optimizeVertexOrder}.
         *
         * <p>Note (fork-verbatim behavior): the returned groups' {@link WeightGroupDef#indices}
         * hold the vertices' <em>pre-reorder</em> positions — after this call each group's
         * vertices simply appear consecutively in {@link #vertices}, in group iteration order,
         * so consumers (like the fork) need only the groups' sizes and interleaved weights.
         */
        public HashMap<Set<String>, WeightGroupDef> groupVertices ()
        {
            // divide the vertices up by weight groups
            HashMap<Set<String>, WeightGroupDef> groups =
                new HashMap<Set<String>, WeightGroupDef>();
            for (int ii = 0, nn = vertices.size(); ii < nn; ii++) {
                SkinVertex svertex = (SkinVertex)vertices.get(ii);
                Set<String> bones = svertex.boneWeights.keySet();
                WeightGroupDef group = groups.get(bones);
                if (group == null) {
                    groups.put(bones, group = new WeightGroupDef());
                }
                group.indices.add(ii);
                for (String bone : bones) {
                    group.weights.add(svertex.boneWeights.get(bone).weight);
                }
            }

            // reorder the vertices by group
            ArrayList<Vertex> overts = vertices;
            vertices = new HashArrayList<Vertex>();
            int[] imap = new int[overts.size()];
            for (Map.Entry<Set<String>, WeightGroupDef> entry : groups.entrySet()) {
                for (int idx : entry.getValue().indices) {
                    imap[idx] = vertices.size();
                    vertices.add(overts.get(idx));
                }
            }
            for (int ii = 0, nn = indices.size(); ii < nn; ii++) {
                indices.set(ii, imap[indices.get(ii)]);
            }

            return groups;
        }
    }

    /** A generic node. */
    public static class NodeDef extends SpatialDef
    {
        // documentation inherited
        public boolean canMerge (Properties props, SpatialDef other)
        {
            return false;
        }

        // documentation inherited
        public void merge (SpatialDef other, Matrix4f xform)
        {
            throw new UnsupportedOperationException();
        }
    }

    /** Represents a triangle for processing purposes. */
    public static class Triangle
    {
        public Vertex[] vertices;

        public Triangle (Vertex[] vertices)
        {
            this.vertices = vertices;
        }

        public float getScore ()
        {
            return vertices[0].score + vertices[1].score + vertices[2].score;
        }
    }

    /** A basic vertex. */
    public static class Vertex
    {
        public float[] location;
        public float[] normal;
        public float[] tcoords;

        public ArrayList<Triangle> triangles;
        public float score;

        public void updateScore (int cacheIdx)
        {
            float pscore;
            if (cacheIdx > 63) {
                pscore = 0f; // outside the cache
            } else if (cacheIdx < 3) {
                pscore = 0.75f; // the three most recent vertices
            } else {
                pscore = FastMath.pow((63 - cacheIdx) / 60f, 1.5f);
            }
            score = pscore + 2f * FastMath.pow(triangles.size(), -0.5f);
        }

        public void transform (Matrix4f xform, Quaternion xrot)
        {
            Vector3f xvec = new Vector3f(location[0], location[1], location[2]);
            xform.mult(xvec, xvec);
            location[0] = xvec.x;
            location[1] = xvec.y;
            location[2] = xvec.z;

            xvec.set(normal[0], normal[1], normal[2]);
            xrot.mult(xvec, xvec);
            normal[0] = xvec.x;
            normal[1] = xvec.y;
            normal[2] = xvec.z;
        }

        public void setInBuffers (
            FloatBuffer vbuf, FloatBuffer nbuf, FloatBuffer tbuf)
        {
            vbuf.put(location);
            nbuf.put(normal);

            if (tbuf != null) {
                if (tcoords != null) {
                    tbuf.put(tcoords);
                } else {
                    tbuf.put(0f);
                    tbuf.put(0f);
                }
            }
        }

        public String toString ()
        {
            return StringUtil.toString(location);
        }

        @Override
        public int hashCode ()
        {
            return Arrays.hashCode(location) ^ Arrays.hashCode(normal) ^ Arrays.hashCode(tcoords);
        }

        @Override
        public boolean equals (Object obj)
        {
            Vertex overt = (Vertex)obj;
            return Arrays.equals(location, overt.location) &&
                Arrays.equals(normal, overt.normal) &&
                Arrays.equals(tcoords, overt.tcoords);
        }
    }

    /** A vertex influenced by a number of bones. */
    public static class SkinVertex extends Vertex
    {
        /** The bones influencing the vertex, mapped by name. */
        public HashMap<String, BoneWeight> boneWeights = new HashMap<String, BoneWeight>();

        public void addBoneWeight (BoneWeight weight)
        {
            if (weight.weight == 0f) {
                return;
            }
            BoneWeight bweight = boneWeights.get(weight.bone);
            if (bweight != null) {
                bweight.weight += weight.weight;
            } else {
                boneWeights.put(weight.bone, weight);
            }
        }

        /** Returns the weight of the named bone. */
        public float getWeight (String bone)
        {
            BoneWeight bweight = boneWeights.get(bone);
            return (bweight == null) ? 0f : bweight.weight;
        }
    }

    /** The influence of a bone on a vertex. */
    public static class BoneWeight
    {
        /** The name of the influencing bone. */
        public String bone;

        /** The amount of influence. */
        public float weight;
    }

    /** A group of vertices influenced by the same bone. */
    public static class WeightGroupDef
    {
        /** The indices of the affected vertex. */
        public ArrayList<Integer> indices = new ArrayList<Integer>();

        /** The interleaved vertex weights. */
        public ArrayList<Float> weights = new ArrayList<Float>();
    }

    /** The meshes and bones comprising the model. */
    public ArrayList<SpatialDef> spatials = new ArrayList<SpatialDef>();

    public void addSpatial (SpatialDef spatial)
    {
        // put nodes before meshes so that bones are updated before skin
        spatials.add(spatial instanceof NodeDef ?  0 : spatials.size(),
            spatial);
    }

    /**
     * Returns the names of all bones referenced by the model's meshes.
     */
    public HashSet<String> getBoneNames ()
    {
        HashSet<String> bones = new HashSet<String>();
        for (SpatialDef spatial : spatials) {
            spatial.getBoneNames(bones);
        }
        return bones;
    }

    /** Determines whether a pair of matrices are "close enough" to equal. */
    public static boolean epsilonEquals (Matrix4f m1, Matrix4f m2)
    {
        for (int ii = 0; ii < 4; ii++) {
            for (int jj = 0; jj < 4; jj++) {
                if (FastMath.abs(m1.get(ii, jj) - m2.get(ii, jj)) > 0.0001f) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Sets a matrix to the transform defined by the given translation, rotation, and scale
     * values (the fork's <code>JmeUtil.setTransform</code>, ported to jME3's
     * {@link Matrix4f} — same row-major field layout, so the column scaling is identical).
     *
     * <p>The quaternion&rarr;rotation-matrix conversion is a <b>bit-exact transliteration of
     * the fork's</b> <code>Quaternion.toRotationMatrix(Matrix4f)</code> rather than a call to
     * jME3's <code>setRotationQuaternion</code>, for two reasons: (1) jME3's version
     * <em>preserves the target matrix's existing scale</em>, so reusing a result matrix would
     * compound its previous scale into the new one; and (2) jME3 normalises with
     * <code>2/norm</code> and halved products where the fork uses <code>invSqrt(norm)</code>
     * and <code>1&nbsp;-&nbsp;2*(..)</code> — mathematically close but not bitwise equal, and
     * the Phase 2b mesh-merge decisions compare these matrices against a 1e-4 epsilon, where
     * ULP differences flipped real corpus cases (the frontier_town train models). Keeping the
     * fork's exact float sequence keeps the merge decisions identical to the oracle's.
     */
    public static Matrix4f setTransform (
        Vector3f translation, Quaternion rotation, Vector3f scale, Matrix4f result)
    {
        // fork Quaternion.toRotationMatrix(Matrix4f), verbatim float sequence
        float x = rotation.getX(), y = rotation.getY(), z = rotation.getZ(),
            w = rotation.getW();
        float norm = w * w + x * x + y * y + z * z;
        if (norm == 0f) {
            // a zero/degenerate rotation quaternion (e.g. "0,0,0,0" from hand-editing) would make
            // invSqrt(0)=Infinity and 0*Infinity=NaN, silently baking NaN geometry (the fork does
            // this too); reject it with the values instead. Valid unit-ish quaternions are
            // untouched — this guard runs before any arithmetic on the result.
            throw new IllegalArgumentException("degenerate rotation quaternion (" +
                x + ", " + y + ", " + z + ", " + w + "): zero norm");
        }
        if (norm != 1.0f) {
            norm = (float)(1.0 / Math.sqrt(norm)); // fork FastMath.invSqrt
        }
        float xx = x * x * norm;
        float xy = x * y * norm;
        float xz = x * z * norm;
        float xw = x * w * norm;
        float yy = y * y * norm;
        float yz = y * z * norm;
        float yw = y * w * norm;
        float zz = z * z * norm;
        float zw = z * w * norm;

        result.m00 = 1 - 2 * (yy + zz);
        result.m01 =     2 * (xy - zw);
        result.m02 =     2 * (xz + yw);
        result.m10 =     2 * (xy + zw);
        result.m11 = 1 - 2 * (xx + zz);
        result.m12 =     2 * (yz - xw);
        result.m20 =     2 * (xz - yw);
        result.m21 =     2 * (yz + xw);
        result.m22 = 1 - 2 * (xx + yy);

        result.m03 = translation.x;
        result.m13 = translation.y;
        result.m23 = translation.z;
        result.m30 = 0f;
        result.m31 = 0f;
        result.m32 = 0f;
        result.m33 = 1f;

        result.m00 *= scale.x;
        result.m01 *= scale.y;
        result.m02 *= scale.z;

        result.m10 *= scale.x;
        result.m11 *= scale.y;
        result.m12 *= scale.z;

        result.m20 *= scale.x;
        result.m21 *= scale.y;
        result.m22 *= scale.z;

        return result;
    }

    /**
     * Extracts the rotation quaternion from a (possibly scaled) matrix using the <b>fork's</b>
     * <code>Matrix4f.toRotationQuat</code>/<code>Quaternion.fromRotationMatrix</code> trace
     * method verbatim. jME3's version normalises the matrix columns first, so on the scaled
     * matrices {@link TriMeshDef#merge} feeds it the two produce different quaternions — and
     * merged-mesh <em>normals</em> (rotated by this quaternion; magnitude is washed out by the
     * shader's normalise, direction is not) shade visibly differently from the oracle bake
     * (bonuses/ghost_town/plague rendered at meanDiff 0.004 vs 0.000 with the fork method).
     */
    public static Quaternion toRotationQuat (Matrix4f m)
    {
        float t = m.m00 + m.m11 + m.m22 + 1;
        float x, y, z, w;
        if (t > 1e-5) {
            float s = 2 * FastMath.sqrt(t);
            w = 0.25f * s;
            x = (m.m21 - m.m12) / s;
            y = (m.m02 - m.m20) / s;
            z = (m.m10 - m.m01) / s;
        } else if ((m.m00 > m.m11) && (m.m00 > m.m22)) {
            float s = FastMath.sqrt(1.0f + m.m00 - m.m11 - m.m22) * 2;
            x = 0.25f * s;
            y = (m.m01 + m.m10) / s;
            z = (m.m02 + m.m20) / s;
            w = (m.m21 - m.m12) / s;
        } else if (m.m11 > m.m22) {
            float s = FastMath.sqrt(1.0f + m.m11 - m.m00 - m.m22) * 2;
            x = (m.m01 + m.m10) / s;
            y = 0.25f * s;
            z = (m.m12 + m.m21) / s;
            w = (m.m02 - m.m20) / s;
        } else {
            float s = FastMath.sqrt(1.0f + m.m22 - m.m00 - m.m11) * 2;
            x = (m.m02 + m.m20) / s;
            y = (m.m21 + m.m12) / s;
            z = 0.25f * s;
            w = (m.m10 - m.m01) / s;
        }
        return new Quaternion(x, y, z, w);
    }

    /** Converts a boxed Float list to an unboxed float array. */
    public static float[] toArray (ArrayList<Float> list)
    {
        float[] array = new float[list.size()];
        for (int ii = 0, nn = list.size(); ii < nn; ii++) {
            array[ii] = list.get(ii);
        }
        return array;
    }

    /** Accelerates {@link ArrayList#indexOf}, {@link ArrayList#contains}, and
     * {@link ArrayList#remove} using an internal hash map (assumes that all elements of the list
     * are unique and non-null). */
    protected static class HashArrayList<E> extends ArrayList<E>
    {
        @Override
        public boolean add (E element)
        {
            add(size(), element);
            return true;
        }

        @Override
        public void add (int idx, E element)
        {
            super.add(idx, element);
            remapFrom(idx);
        }

        @Override
        public E remove (int idx)
        {
            E element = super.remove(idx);
            _indices.remove(element);
            remapFrom(idx);
            return element;
        }

        @Override
        public void clear ()
        {
            super.clear();
            _indices.clear();
        }

        @Override
        public int indexOf (Object obj)
        {
            Integer idx = _indices.get(obj);
            return (idx == null ? -1 : idx);
        }

        @Override
        public boolean contains (Object obj)
        {
            return _indices.containsKey(obj);
        }

        @Override
        public boolean remove (Object obj)
        {
            Integer idx = _indices.remove(obj);
            if (idx != null) {
                super.remove(idx);
                return true;
            } else {
                return false;
            }
        }

        protected void remapFrom (int idx)
        {
            for (int ii = idx, nn = size(); ii < nn; ii++) {
                _indices.put(get(ii), ii);
            }
        }

        /** Maps elements to their indices in the list. */
        protected HashMap<Object, Integer> _indices = new HashMap<Object, Integer>();
    }
}
