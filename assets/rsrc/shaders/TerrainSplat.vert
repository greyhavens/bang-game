// Bang! terrain splat vertex shader.
#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldViewMatrix;

attribute vec3 inPosition;
attribute vec2 inTexCoord;   // ground-texture (tiled) coords
attribute vec2 inTexCoord2;  // alpha-map (0..1 block) coords
attribute vec4 inColor;      // per-vertex shadow / light modulation

varying vec2 texCoord;
varying vec2 alphaCoord;

#ifdef HAS_VERTEXCOLOR
varying vec4 vertColor;
#endif

#ifdef USE_FOG
varying float fogDepth;
#endif

void main() {
    texCoord   = inTexCoord;
    alphaCoord = inTexCoord2;

    #ifdef HAS_VERTEXCOLOR
        vertColor = inColor;
    #endif

    #ifdef USE_FOG
        vec4 viewPos = g_WorldViewMatrix * vec4(inPosition, 1.0);
        fogDepth = -viewPos.z;
    #endif

    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);
}
