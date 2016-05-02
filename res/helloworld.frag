#version 330

uniform sampler2D texture;
uniform int useTexturing;
uniform int useLighting;
uniform vec3 cameraDirection;
uniform vec4 diffuseColor;

in vec3 fragmentNormal;
in vec2 textureCoord;
out vec4 FragColor;

void main()
{
    FragColor = diffuseColor;
    if (useLighting > 0) {
        FragColor *= max(dot(fragmentNormal, cameraDirection), 0.0);
    }
    if (useTexturing > 0) {
        FragColor *= texture2D(texture, textureCoord);
    }
}
