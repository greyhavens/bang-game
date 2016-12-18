//
// $Id$

/** The light direction in model space. */
uniform vec3 lightVector;

/** The interpolated model space eye vector and half vector. */
varying vec3 eyeVector, halfVector;

/** The amount of fog. */
#ifdef ENABLE_FOG
  varying float fogAlpha;
#endif

/**
 * Vertex shader for Bang! water surfaces.
 */
void main ()
{
    // the eye vector points from the vertex to the eye space origin
    eyeVector = normalize(gl_ModelViewMatrixInverse[3].xyz - gl_Vertex.xyz);

    // the half vector is the sum of the light vector and the eye vector
    halfVector = normalize(lightVector + eyeVector);

    // compute the fog value using the eye space z coordinate
    #ifdef ENABLE_FOG
      fogAlpha = exp(gl_Fog.density * dot(gl_ModelViewMatrixTranspose[2], gl_Vertex));
    #endif

    // copy the texture coordinates from attribute to varying
    gl_TexCoord[0] = gl_MultiTexCoord0;

    // compute the position using the fixed function transform
    gl_Position = ftransform();
}
