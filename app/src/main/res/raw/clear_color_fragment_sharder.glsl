#extension GL_OES_EGL_image_external : require
precision mediump float;
uniform samplerExternalOES texture;
varying vec2 v_TexCoordinate;

void main () {
    vec4 color = texture2D(texture, v_TexCoordinate);
    if (color.r < 0.15 && color.g < 0.15 && color.b < 0.15) {
        color = vec4(0.0, 0.0, 0.0, 0.0);
    }
    gl_FragColor = color;
}