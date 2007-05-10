//
// $Id$

/** The color of the water as configured in the board. */
uniform vec4 waterColor;

/** The color of the sky overhead as configured in the board. */
uniform vec4 skyOverheadColor;

/** The light direction in model space. */
uniform vec3 lightVector;

/** The normal map. */
uniform sampler2D normalMap;

/** The interpolated model space eye vector and half vector. */
varying vec3 eyeVector, halfVector;

/** The amount of fog. */
#ifdef ENABLE_FOG
  varying float fogAlpha;
#endif

/**
 * Fragment shader for Bang! water surfaces.
 */
void main ()
{
    vec3 norm = normalize(texture2D(normalMap, gl_TexCoord[0].st).xyz - vec3(0.5, 0.5, 0.5)),
        eye = normalize(eyeVector),
        halfVec = normalize(halfVector);

    // blend the water and sky colors using the Fresnel reflectivity
    // (computed as an approximate) and add the specular highlight
    gl_FragColor =
        mix(waterColor, skyOverheadColor,
            pow(max(dot(norm, eye), 0.0) + 1.0, -8.0)) +
            gl_LightSource[0].specular *
                pow(max(dot(norm, halfVec), 0.0), 32.0);

    // blend between the computed color and the fog color
    #ifdef ENABLE_FOG
      gl_FragColor = mix(gl_Fog.color, gl_FragColor, fogAlpha);
    #endif
}
