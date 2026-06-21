//
// $Id$

package com.threerings.jme.tools.model;

/**
 * Read access to {@link ModelMesh}'s protected configuration fields for the j3o
 * converter, which needs the texture/render flags without resolving live GL texture
 * states. Lives in the {@code com.threerings.jme.model} package purely for member
 * access; the converter is the only consumer.
 */
public final class ModelMeshAccess
{
    public static String textureKey (ModelMesh mesh)
    {
        return mesh._textureKey;
    }

    /** The resolved texture file name(s); more than one means "random per instance". */
    public static String[] textures (ModelMesh mesh)
    {
        return mesh._textures;
    }

    public static boolean solid (ModelMesh mesh)
    {
        return mesh._solid;
    }

    public static boolean transparent (ModelMesh mesh)
    {
        return mesh._transparent;
    }

    public static boolean translucent (ModelMesh mesh)
    {
        return mesh._translucent;
    }

    public static boolean additive (ModelMesh mesh)
    {
        return mesh._additive;
    }

    public static boolean emissive (ModelMesh mesh)
    {
        return mesh._emissive;
    }

    public static String emissiveMap (ModelMesh mesh)
    {
        return mesh._emissiveMap;
    }

    public static float alphaThreshold (ModelMesh mesh)
    {
        return mesh._alphaThreshold;
    }

    public static boolean sphereMapped (ModelMesh mesh)
    {
        return mesh._sphereMapped;
    }

    /** The skin's weight groups (bones + interleaved weights per contiguous vertex run),
     * in the same order the deformer walks vertices. */
    public static SkinMesh.WeightGroup[] weightGroups (SkinMesh mesh)
    {
        return mesh._weightGroups;
    }

    /** The inverse of the skin mesh's model-space reference (bind-pose) transform, computed by
     * the fork's {@code setReferenceTransforms()}. The skin's stored vertices live in mesh-local
     * space; {@code inverse(this)} is the mesh-local -> model(root) space transform at bind. */
    public static com.jme.math.Matrix4f invRefTransform (SkinMesh mesh)
    {
        return mesh._invRefTransform;
    }

    private ModelMeshAccess ()
    {
    }
}
