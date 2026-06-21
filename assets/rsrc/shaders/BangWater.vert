// Bang! water vertex shader: classic OpenGL sphere-map (GL_SPHERE_MAP) texgen.
// The surface normal is the CPU wave-simulated normal pushed into the mesh each frame;
// projecting the reflection of the eye ray into sphere-map space gives the view-dependent
// Fresnel reflection the fork produced with fixed-function EM_SPHERE.
#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldViewMatrix;
uniform mat3 g_NormalMatrix;

attribute vec3 inPosition;
attribute vec3 inNormal;

varying vec2 sphereCoord;

#ifdef USE_FOG
varying float fogDepth;
#endif

void main() {
    // eye-space position and normal
    vec4 viewPos = g_WorldViewMatrix * vec4(inPosition, 1.0);
    vec3 n = normalize(g_NormalMatrix * inNormal);
    vec3 u = normalize(viewPos.xyz);            // eye -> vertex direction

    // reflection vector of the eye ray about the normal
    vec3 r = reflect(u, n);
    // GL_SPHERE_MAP coordinate generation
    float m = 2.0 * sqrt(r.x*r.x + r.y*r.y + (r.z + 1.0)*(r.z + 1.0));
    sphereCoord = vec2(r.x / m + 0.5, r.y / m + 0.5);

    #ifdef USE_FOG
        fogDepth = -viewPos.z;
    #endif

    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);
}
