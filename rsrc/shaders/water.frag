//
// $Id$

/** The color of the water as configured in the board. */
uniform vec4 waterColor;

/** The color of the sky overhead as configured in the board. */
uniform vec4 skyOverheadColor;

/** The interpolated eye space normal and eye vector. */
varying vec3 normal, eyeVector;

/**
 * Fragment shader for Bang! water surfaces.
 */
void main ()
{
    vec3 norm = normalize(normal), eye = normalize(eyeVector),
        halfVec = normalize(gl_LightSource[0].position + eye);
    
    // blend the water and sky colors using the Fresnel reflectivity
    // (computed as an approximate) and add the specular highlight
    gl_FragColor =
        mix(waterColor, skyOverheadColor,
            pow(max(dot(norm, eye), 0) + 1, -8)) +
            gl_LightSource[0].specular * pow(max(dot(norm, halfVec), 0), 32);
}
