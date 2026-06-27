// Bang! warm "aged-photo" post-process fragment shader.
//
// Full-screen pass run by a jME3 FilterPostProcessor on the main 3D viewport: it warms the
// already-rendered scene (ground, sky dome, buildings, town backdrop -- everything in the world
// viewport, but NOT the separate BUI overlay) toward the original fork's golden, sun-baked desert
// look. m_Intensity (0..1) lerps between the original color and the graded color, so 0 is a pure
// pass-through.
//
// Unlike a classic desaturating sepia (which collapses every hue to one brown and was too heavy),
// this keeps most of the scene's chroma -- the red/blue signage stays colored -- and applies a
// luma-split warm tint: the shadows/darks get a strong amber bias and the highlights only a mild
// one, matching the fork's measured balance (warm cream sky ~ (222,221,191), warm brown ground ~
// (119,100,64) -- blue strongly suppressed in the darks, only slightly up high).
#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_Texture;
uniform float m_Intensity;

varying vec2 texCoord;

void main() {
    vec4 color = texture2D(m_Texture, texCoord);
    vec3 c = color.rgb;

    float luma = dot(c, vec3(0.299, 0.587, 0.114));

    // keep most of the colour (only a gentle pull toward grey) so signage/teams stay readable
    vec3 graded = mix(c, vec3(luma), 0.22);

    // warm split-tone: strong amber in the shadows, warm cream in the highlights (the fork's bright
    // sky reads warm cream, not blue, so the highlight tint pulls blue down hard too)
    vec3 warmShadow = vec3(1.24, 1.03, 0.60);
    vec3 warmHigh   = vec3(1.16, 1.05, 0.74);
    graded *= mix(warmShadow, warmHigh, luma);

    gl_FragColor = vec4(mix(c, graded, m_Intensity), color.a);
}
