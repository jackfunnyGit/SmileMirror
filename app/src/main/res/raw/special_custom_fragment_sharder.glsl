#extension GL_OES_EGL_image_external : require
precision mediump float;
uniform samplerExternalOES texture;
varying vec2 v_TexCoordinate;

void main () {
    vec4 color = texture2D(texture, v_TexCoordinate);

    float alpha = (color.r + color.g + color.b) * 1.8 / 4.0;
//    float alpha = (color.r + color.g + color.b) / 3.0;
    vec4 resultColor;
    if (alpha != 0.0) {
        resultColor = vec4(color.rgb / alpha, alpha);
    } else {
        resultColor = vec4(0.0);
    }

    gl_FragColor = vec4(resultColor.rgb * resultColor.a, resultColor.a);
}