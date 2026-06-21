// Bang! water fragment shader: sample the Fresnel sphere map at the sphere-mapped
// reflection coordinate. The sphere map's own alpha already encodes the water/sky
// Fresnel blend; we scale it by the overall surface Alpha.
#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_SphereMap;
uniform float m_Alpha;

#ifdef USE_FOG
uniform vec4  m_FogColor;
uniform float m_FogDensity;
varying float fogDepth;
#endif

varying vec2 sphereCoord;

void main() {
    vec4 color = texture2D(m_SphereMap, sphereCoord);
    color.a *= m_Alpha;

    #ifdef USE_FOG
        float fogFactor = clamp(exp(-m_FogDensity * fogDepth), 0.0, 1.0);
        color.rgb = mix(m_FogColor.rgb, color.rgb, fogFactor);
    #endif

    gl_FragColor = color;
}
