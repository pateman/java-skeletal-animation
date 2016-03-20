#version 330

in vec3 Position;
in vec3 Normal;
in vec2 TexCoord;

out vec2 textureCoord;
out vec3 fragmentNormal;
out vec3 lightDir;

uniform mat4 projection;
uniform mat4 modelView;


void main()
{
    fragmentNormal = (modelView * vec4(Normal, 0.0)).xyz;
    fragmentNormal = normalize(fragmentNormal);

    textureCoord = TexCoord;

    gl_Position = projection * modelView * vec4(Position, 1.0);
}