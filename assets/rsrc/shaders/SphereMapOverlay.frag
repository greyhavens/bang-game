// Sphere-map overlay fragment shader: sample the sphere map at the sphere-mapped reflection
// coordinate, tint it, and scale by the ramped Intensity. The material's additive blend
// (RenderUtil.applyAddAlpha: SRC_ALPHA / ONE) accumulates this over the lit unit, matching the
// fork's additive EM_SPHERE overlay pass. The output alpha carries the intensity into the
// additive SRC_ALPHA factor so Intensity ramps the contribution smoothly in and out.
#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_SphereMap;
uniform vec4  m_TintColor;
uniform float m_Intensity;

varying vec2 sphereCoord;

void main() {
    // the sphere map is opaque RGB (textures/environ/spheremap.png); the additive contribution is
    // gated entirely by the ramped Intensity (the fork ramped the overlay material's alpha), so the
    // SRC_ALPHA additive factor comes from Intensity, not the texture.
    vec3 rgb = texture2D(m_SphereMap, sphereCoord).rgb * m_TintColor.rgb;
    float a = m_TintColor.a * m_Intensity;
    gl_FragColor = vec4(rgb, a);
}
