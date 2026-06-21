// Bang! terrain splat fragment shader.
//
// Each terrain layer is one Geometry sharing the block mesh. The base layer is
// opaque (no AlphaMap); each subsequent splat layer multiplies its diffuse by a
// per-pixel alpha sampled from the A8 alpha map (the faithful multi-texture splat
// the fork did with fixed-function texture combine). Vertex color carries the
// baked terrain shadow / high-noon modulation.
#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_ColorMap;

#ifdef HAS_ALPHAMAP
uniform sampler2D m_AlphaMap;
#endif

#ifdef USE_FOG
uniform vec4  m_FogColor;
uniform float m_FogDensity;
varying float fogDepth;
#endif

varying vec2 texCoord;
varying vec2 alphaCoord;

#ifdef HAS_VERTEXCOLOR
varying vec4 vertColor;
#endif

void main() {
    vec4 color = texture2D(m_ColorMap, texCoord);

    #ifdef HAS_VERTEXCOLOR
        color.rgb *= vertColor.rgb;
    #endif

    #ifdef HAS_ALPHAMAP
        // the splat coverage is an Alpha8 texture: jME3 exposes its single channel
        // in the alpha component (.r reads 0 for an alpha-only format), so sample .a.
        color.a = texture2D(m_AlphaMap, alphaCoord).a;
    #endif

    #ifdef USE_FOG
        float fogFactor = clamp(exp(-m_FogDensity * fogDepth), 0.0, 1.0);
        color.rgb = mix(m_FogColor.rgb, color.rgb, fogFactor);
    #endif

    gl_FragColor = color;
}
