//
// $Id$

/** The interpolated eye space normal and eye vector. */
varying vec3 normal, eyeVector;

/**
 * Vertex shader for Bang! water surfaces.
 */
void main ()
{
    // compute the normal in the normal way
    normal = gl_NormalMatrix * gl_Normal;
    
    // the eye vector points from the vertex to the eye space origin
    eyeVector = -vec3(gl_ModelViewMatrix * gl_Vertex);
    
    // the position is computed using the fixed function transform
    gl_Position = ftransform();
}
