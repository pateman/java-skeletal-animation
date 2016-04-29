#version 330

uniform sampler2D texture;
uniform int useTexturing;
uniform int useLighting;

in vec3 fragmentNormal;
in vec2 textureCoord;
out vec4 FragColor;

void main()
{
    FragColor = vec4(0.8, 0.8, 0.8, 1.0);
    if (useLighting > 0) {
        FragColor *= max(dot(fragmentNormal, vec3(0.0, 0.0, 1.0)), 0.0) ;
    }
    if (useTexturing > 0) {
        FragColor *= texture2D(texture, textureCoord);
    }
}
