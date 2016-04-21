package pl.pateman.skeletal.json;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.pateman.skeletal.MeshImporter;
import pl.pateman.skeletal.TempVars;
import pl.pateman.skeletal.Utils;
import pl.pateman.skeletal.entity.MeshEntity;
import pl.pateman.skeletal.mesh.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pateman.
 */
public final class JSONImporter implements MeshImporter {
    private final Gson gson;
    private final Map<Integer, Bone> boneMap;

    public JSONImporter() {
        this.gson = new GsonBuilder().
                registerTypeAdapter(Bone.class, new BoneDeserializer()).
                registerTypeAdapter(AnimationTrack.class, new AnimationTrackDeserializer()).
                create();
        this.boneMap = new HashMap<>(MeshSkinningInfo.MAX_BONES);
    }

    @Override
    public MeshEntity load(String meshFileResource) throws IOException {
        final Map<String, JSONSceneData> importResult = this.gson.fromJson(Utils.readResource(meshFileResource),
                new TypeToken<Map<String, JSONSceneData>>(){}.getType());

        //  After import, clear the bones map.
        this.boneMap.clear();

        if (importResult.isEmpty()) {
            return null;
        }

        final JSONSceneData sceneData = importResult.entrySet().iterator().next().getValue();

        //  Now that the skeleton is fully processed, calculate the bind matrices and arrange the bones.
        final Mesh mesh = sceneData.getMesh();
        mesh.getSkeleton().calculateBindMatrices();
        mesh.getSkeleton().arrangeBones();
        mesh.createBoneTracks();

        //  Create the pallete skinning buffer for passing animation matrices to the shader.
        TempVars.initializeStorageForSkinning(mesh.getSkeleton().getBones().size());

        //  Prepare data from transformation.
        final TempVars vars = TempVars.get();

        final Vector3f translation = vars.vect3d1;
        final Quaternionf rotation = vars.quat1;
        final Vector3f scale = vars.vect3d2.set(1.0f, 1.0f, 1.0f);
        if (sceneData.getTranslation() != null) {
            translation.set(sceneData.getTranslation());
        }
        if (sceneData.getRotation() != null) {
            rotation.set(sceneData.getRotation());
        }
        if (sceneData.getScale() != null) {
            scale.set(sceneData.getScale());
        }

        //  Create the entity.
        final MeshEntity meshEntity = new MeshEntity();
        meshEntity.setMesh(mesh);
        meshEntity.setTransformation(rotation, translation, scale);
        meshEntity.forceTransformationUpdate();

        return meshEntity;
    }

    private class BoneDeserializer implements JsonDeserializer<Bone> {
        private final Type vector3fType;
        private final Type quaternionfType;
        private final Type weightsMapType;

        public BoneDeserializer() {
            this.vector3fType = new TypeToken<Vector3f>(){}.getType();
            this.quaternionfType = new TypeToken<Quaternionf>(){}.getType();
            this.weightsMapType = new TypeToken<Map<Integer, Float>>(){ }.getType();
        }

        @Override
        public Bone deserialize(JsonElement jsonElement, Type type,
                                JsonDeserializationContext context) throws JsonParseException {
            final JsonObject jsonObject = jsonElement.getAsJsonObject();

            final String boneName = jsonObject.get("name").getAsString();
            final int boneIndex = jsonObject.get("index").getAsInt();
            final int parent = jsonObject.get("parent").getAsInt();

            final Bone bone = new Bone(boneName, boneIndex);
            if (parent != -1) {
                final Bone parentBone = JSONImporter.this.boneMap.get(parent);
                bone.setParent(parentBone);
                parentBone.getChildren().add(bone);
            }

            bone.getBindPosition().set((Vector3f) context.deserialize(jsonObject.get("bindTranslation"),
                    this.vector3fType));
            bone.getBindRotation().set((Quaternionf) context.deserialize(jsonObject.get("bindRotation"),
                    this.quaternionfType));
            bone.getBindScale().set((Vector3f) context.deserialize(jsonObject.get("bindScale"), this.vector3fType));

            final Map<Integer, Float> weights = context.deserialize(jsonObject.get("weights"), this.weightsMapType);
            bone.getVertexWeights().putAll(weights);

            JSONImporter.this.boneMap.put(boneIndex, bone);

            return bone;
        }
    }

    private class AnimationTrackDeserializer implements JsonDeserializer<AnimationTrack> {
        private final Type keyframesListType;

        public AnimationTrackDeserializer() {
            this.keyframesListType = new TypeToken<List<AnimationKeyframe>>(){}.getType();;
        }

        @Override
        public AnimationTrack deserialize(JsonElement jsonElement, Type type,
                                          JsonDeserializationContext context) throws JsonParseException {
            final JsonObject jsonObject = jsonElement.getAsJsonObject();
            final int trackBoneIndex = jsonObject.get("bone").getAsInt();

            final AnimationTrack animationTrack = new AnimationTrack(JSONImporter.this.boneMap.get(trackBoneIndex));

            final List<AnimationKeyframe> keyframes = context.deserialize(jsonObject.get("keyframes"),
                    this.keyframesListType);
            animationTrack.getKeyframes().addAll(keyframes);

            return animationTrack;
        }
    }
}
