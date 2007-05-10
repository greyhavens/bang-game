//
// $Id$

/** The splat terrain textures and alpha maps. */
uniform sampler2D splatTextures[NUM_SPLATS * 2];

/** The scale values for each terrain layer. */
uniform float terrainScales[NUM_SPLATS];

/**
 * Fragment shader for Bang! terrain.
 */
void main ()
{
    // start with black
    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);

    // add the splats, scaled by their alpha values
    ADD_SPLATS

    // modulate by the light color
    gl_FragColor.rgb *= gl_Color.rgb;

    // blend between the computed color and the fog color
    #ifdef ENABLE_FOG
      gl_FragColor = mix(gl_Fog.color * gl_FragColor.a, gl_FragColor,
        exp(gl_Fog.density * gl_FogFragCoord));
    #endif
}
