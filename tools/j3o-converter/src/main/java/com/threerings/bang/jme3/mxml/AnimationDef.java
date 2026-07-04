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

import java.util.ArrayList;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 * The neutral parsed representation of a Bang! Howdy animation <code>.mxml</code> (Epic 4
 * Phase 2a): sampled per-frame TRS keyframes for named bones/nodes, as {@link AnimationParser}
 * populates it straight from the XML. Ported from the fork's
 * <code>com.threerings.jme.tools.AnimationDef</code> onto jME3 math types; the jME3
 * <code>.j3o</code> and glTF writers (Phase 2b) consume it.
 *
 * <p>Deliberately NOT ported from the fork original (the fork-scene-typed tail):
 * <code>createAnimation</code> (built the fork's live <code>Model.Animation</code>),
 * <code>FrameDef.addTransformTargets</code>/<code>getTransforms</code>/<code>getTransform</code>
 * (resolved names against fork <code>Spatial</code>s), and <code>filterTransforms</code>
 * (drove the dropped <code>TransformNode</code> mesh-merge preprocessing — see the
 * {@link ModelDef} javadoc). All the parsed data — frame rate, frames, and each frame's
 * per-node transforms — is retained.
 */
public class AnimationDef
{
    /** The rate of the animation in frames per second. */
    public int frameRate;

    /** A single frame of the animation. */
    public static class FrameDef
    {
        /** Transform for affected nodes. */
        public ArrayList<TransformDef> transforms = new ArrayList<TransformDef>();

        public void addTransform (TransformDef transform)
        {
            transforms.add(transform);
        }
    }

    /** A transform for a single node. */
    public static class TransformDef
    {
        /** The name of the affected node. */
        public String name;

        /** The transformation parameters. */
        public float[] translation;
        public float[] rotation;
        public float[] scale;

        /** Returns the translation as a jME3 vector. */
        public Vector3f getTranslation ()
        {
            return new Vector3f(translation[0], translation[1], translation[2]);
        }

        /** Returns the rotation as a jME3 quaternion. */
        public Quaternion getRotation ()
        {
            return new Quaternion(rotation[0], rotation[1], rotation[2], rotation[3]);
        }

        /** Returns the scale as a jME3 vector. */
        public Vector3f getScale ()
        {
            return new Vector3f(scale[0], scale[1], scale[2]);
        }
    }

    /** The individual frames of the animation. */
    public ArrayList<FrameDef> frames = new ArrayList<FrameDef>();

    public void addFrame (FrameDef frame)
    {
        frames.add(frame);
    }
}
