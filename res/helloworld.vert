#version 330

in vec3 Position;
in vec3 Normal;
in vec2 TexCoord;
in vec3 BoneIndices;
in vec3 BoneWeights;

out vec2 textureCoord;
out vec3 fragmentNormal;
out vec3 lightDir;

uniform mat4 projection;
uniform mat4 modelView;
uniform mat4 bones[100];
uniform int useSkinning;

mat4 boneTransform() {
    mat4 ret = BoneWeights.z * bones[int(BoneIndices.z)]
        + BoneWeights.y * bones[int(BoneIndices.y)]
        + BoneWeights.x * bones[int(BoneIndices.x)];

    return ret;
}

void main()
{
    mat4 boneMatrix = (useSkinning > 0) ?
        boneTransform() :
        mat4(
            1., 0., 0., 0.,
            0., 1., 0., 0.,
            0., 0., 1., 0.,
            0., 0., 0., 1.
        );

    fragmentNormal = (modelView * vec4(Normal, 0.0)).xyz;
    fragmentNormal = normalize(fragmentNormal);

    textureCoord = TexCoord;

    gl_Position = projection * modelView * boneMatrix * vec4(Position, 1.0);
}