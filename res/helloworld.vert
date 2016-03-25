#version 330

in vec3 Position;
in vec3 Normal;
in vec2 TexCoord;
in vec3 BoneIndices;
in vec3 BoneWeights;

out vec2 textureCoord;
out vec3 fragmentNormal;

uniform mat4 projection;
uniform mat4 modelView;
uniform mat4 bones[60];
uniform int useSkinning;

void boneTransform(inout vec4 position) {
    mat4 ret;

    if (BoneWeights.x != 0.0f && useSkinning > 0) {
        ret = mat4(0.0f);

        ret += bones[int(BoneIndices.x)] * BoneWeights.x;
        ret += bones[int(BoneIndices.y)] * BoneWeights.y;
        ret += bones[int(BoneIndices.z)] * BoneWeights.z;
    } else {
        ret = mat4(
              1., 0., 0., 0.,
              0., 1., 0., 0.,
              0., 0., 1., 0.,
              0., 0., 0., 1.
        );
    }

    position = ret * position;
}

void main()
{
    vec4 modelSpacePos = vec4(Position, 1.0);

    fragmentNormal = (modelView * vec4(Normal, 0.0)).xyz;
    fragmentNormal = normalize(fragmentNormal);

    textureCoord = TexCoord;

    boneTransform(modelSpacePos);
    gl_Position = projection * modelView * modelSpacePos;
}