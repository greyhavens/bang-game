// Bang! sepia post-process fragment shader.
//
// Full-screen pass run by a jME3 FilterPostProcessor on the main 3D viewport: it
// tints the already-rendered scene (ground, sky dome, buildings, town backdrop --
// everything in the world viewport, but NOT the separate BUI overlay) toward a
// warm sepia, reproducing the old fork's aged-photo look. m_Intensity (0..1) lerps
// between the original color and the classic sepia matrix, so 0 is a pure pass-through.
#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_Texture;
uniform float m_Intensity;

varying vec2 texCoord;

void main() {
    vec4 color = texture2D(m_Texture, texCoord);
    vec3 c = color.rgb;
    vec3 sepia;
    sepia.r = dot(c, vec3(0.393, 0.769, 0.189));
    sepia.g = dot(c, vec3(0.349, 0.686, 0.168));
    sepia.b = dot(c, vec3(0.272, 0.534, 0.131));
    gl_FragColor = vec4(mix(c, sepia, m_Intensity), color.a);
}
