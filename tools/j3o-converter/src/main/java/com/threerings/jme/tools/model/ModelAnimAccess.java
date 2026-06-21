//
// $Id$

package com.threerings.jme.tools.model;

import com.jme.math.Quaternion;
import com.jme.math.Vector3f;

/**
 * Read access to {@link Model.Transform}'s protected TRS fields for the jME3 model
 * loader, which unpacks the fork's per-frame keyframe transforms into jME3
 * {@code TransformTrack}s without going through the live animation runtime. Lives in the
 * {@code com.threerings.jme.model} package purely for member access.
 */
public final class ModelAnimAccess
{
    public static Vector3f translation (Model.Transform xform)
    {
        return xform._translation;
    }

    public static Quaternion rotation (Model.Transform xform)
    {
        return xform._rotation;
    }

    public static Vector3f scale (Model.Transform xform)
    {
        return xform._scale;
    }

    private ModelAnimAccess ()
    {
    }
}
