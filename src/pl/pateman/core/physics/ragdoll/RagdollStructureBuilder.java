package pl.pateman.core.physics.ragdoll;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.pateman.core.Utils;
import pl.pateman.core.mesh.Bone;
import pl.pateman.core.mesh.Mesh;
import pl.pateman.core.mesh.Skeleton;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A builder for ragdolls.
 *
 * Created by pateman.
 */
public final class RagdollStructureBuilder {
    private final RagdollStructure ragdollStructure;
    private final Mesh mesh;
    private final Map<BodyPartType, Part> partMap;

    public RagdollStructureBuilder(final Mesh mesh) {
        this.ragdollStructure = new RagdollStructure(mesh);
        this.mesh = mesh;
        this.partMap = new HashMap<>();
    }

    /**
     * Builds the ragdoll structure.
     *
     * @return {@code RagdollStructure}
     */
    public RagdollStructure build() {
        //  Convert internal structures.
        for (Map.Entry<BodyPartType, Part> entry : this.partMap.entrySet()) {
            final BodyPartType partType = entry.getKey();
            final Part part = entry.getValue();

            this.ragdollStructure.setBodyPartBones(partType, part.boneNames.toArray(
                    new String[part.boneNames.size()]));
            this.ragdollStructure.setBodyPartColliderType(partType, part.collider);

            //  Handle custom transformation.
            final Matrix4f customTransform = new Matrix4f();
            Utils.fromRotationTranslationScale(customTransform, part.customRotation, part.customTranslation,
                    Utils.IDENTITY_VECTOR);
            this.ragdollStructure.setBodyPartCustomTransform(partType, customTransform, part.customTransformFlag);
        }

        return this.ragdollStructure;
    }

    /**
     * Starts building a new body part. If the given body part already exists, it will be used instead of creating
     * a new one to allow modifications.
     *
     * When you add bones to the body part, make sure that the first and the last bone determine the part's length
     * and shape, otherwise you might get wrong results. For example, if you want to create a body part which resembles
     * an arm by using the following bones:
     *
     * 'Elbow', 'Fingertip', 'Hand', 'Shoulder'
     *
     * The order in which you add the bones should be either:
     *
     * 'Shoulder', 'Hand', 'Elbow', 'Fingertip'
     *
     * or:
     *
     * 'Fingertip', 'Hand', 'Elbow', 'Shoulder'
     *
     * This way, the bones 'Shoulder' and 'Fingertip' will determine the part's length and shape. The order of bones
     * between the first and the last bone DOES NOT matter.
     *
     * @param type Body part type.
     * @return Body part builder.
     */
    public Part startPart(final BodyPartType type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }

        Part part = this.partMap.get(type);
        if (part == null) {
            part = new Part(type, this.mesh.getSkeleton(), this);
            this.partMap.put(type, part);
        }

        return part;
    }

    public final class Part {
        private final BodyPartType type;
        private final Set<String> boneNames;
        private BodyPartCollider collider;
        private final Skeleton skeleton;
        private final RagdollStructureBuilder builder;
        private final Quaternionf customRotation;
        private final Vector3f customTranslation;
        private byte customTransformFlag;

        Part(BodyPartType type, Skeleton skeleton, RagdollStructureBuilder builder) {
            this.type = type;
            this.collider = BodyPartCollider.BOX;
            this.skeleton = skeleton;
            this.builder = builder;
            this.boneNames = new LinkedHashSet<>();
            this.customRotation = new Quaternionf();
            this.customTranslation = new Vector3f();
        }

        public Part addBone(final String boneName) {
            if (boneName == null || boneName.isEmpty()) {
                throw new IllegalArgumentException();
            }
            this.boneNames.add(boneName);

            return this;
        }

        public Part addBones(final String... bones) {
            if (bones == null || bones.length == 0) {
                throw new IllegalArgumentException();
            }

            for (String bone : bones) {
                this.addBone(bone);
            }

            return this;
        }

        private void addBonesFromRoot(final Bone rootBone) {
            this.boneNames.add(rootBone.getName());
            for (Bone bone : rootBone.getChildren()) {
                this.addBonesFromRoot(bone);
            }
        }

        public Part addBonesFromRoot(final String rootBoneName) {
            final Bone bone = this.skeleton.getBoneByName(rootBoneName);
            if (bone == null) {
                throw new IllegalArgumentException("Unknown bone '" + rootBoneName + "'");
            }

            this.addBonesFromRoot(bone);
            return this;
        }

        public Part setTranslation(final Vector3f translation) {
            if (translation == null) {
                throw new IllegalArgumentException();
            }
            this.customTransformFlag |= RagdollStructure.CUSTOM_TRANSFORM_TRANSLATION;
            this.customTranslation.set(translation);
            return this;
        }

        public Part setRotation(final Quaternionf rotation) {
            if (rotation == null) {
                throw new IllegalArgumentException();
            }

            this.customTransformFlag |= RagdollStructure.CUSTOM_TRANSFORM_ROTATION;
            this.customRotation.set(rotation);
            return this;
        }

        public RagdollStructureBuilder endPart() {
            return this.builder;
        }
    }
}
