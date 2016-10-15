package pl.pateman.core.physics.ragdoll;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.pateman.core.Utils;
import pl.pateman.core.mesh.Bone;
import pl.pateman.core.mesh.Mesh;
import pl.pateman.core.mesh.Skeleton;

import java.util.*;

/**
 * A builder for ragdolls.
 *
 * Created by pateman.
 */
public final class RagdollStructureBuilder {
    private final RagdollStructure ragdollStructure;
    private final Mesh mesh;
    private final Map<BodyPartType, Part> partMap;
    private final List<Link> linkList;

    public RagdollStructureBuilder(final Mesh mesh) {
        this.ragdollStructure = new RagdollStructure(mesh);
        this.mesh = mesh;
        this.partMap = new HashMap<>();
        this.linkList = new ArrayList<>();
    }

    /**
     * Builds the ragdoll structure.
     *
     * @return {@code RagdollStructure}
     */
    public RagdollStructure build() {
        //  Convert internal structures.
        for (final Map.Entry<BodyPartType, Part> entry : this.partMap.entrySet()) {
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

        final Map<BodyPartType, BodyPart> bodyParts = this.ragdollStructure.getBodyParts();
        for (final Link link : this.linkList) {
            this.ragdollStructure.getBodyLinks().add(new RagdollLink(bodyParts.get(link.partA),
                    bodyParts.get(link.partB), link.minLimit, link.maxLimit));
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

    /**
     * Starts building a link between two body parts. If the given body link already exists, it will be used instead of
     * creating a new one to allow modifications.
     *
     * @param partA Body part type of the first part to link.
     * @param partB Body part type of the second part to link.
     * @return Body part link builder.
     */
    public Link startLink(final BodyPartType partA, final BodyPartType partB) {
        if (partA == null || partB == null) {
            throw new IllegalArgumentException();
        }

        if (!this.partMap.containsKey(partA) || !this.partMap.containsKey(partB)) {
            throw new IllegalStateException("One of parts is not configured, hence cannot be linked");
        }

        final Link newLink = new Link(partA, partB, this);
        final int indexOf = this.linkList.indexOf(newLink);
        if (indexOf != -1) {
            return this.linkList.get(indexOf);
        }

        this.linkList.add(newLink);
        return newLink;
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

    public final class Link {
        private final BodyPartType partA;
        private final BodyPartType partB;
        private final RagdollStructureBuilder builder;
        private final Vector3f minLimit;
        private final Vector3f maxLimit;

        Link(BodyPartType partA, BodyPartType partB, RagdollStructureBuilder builder) {
            this.partA = partA;
            this.partB = partB;
            this.builder = builder;
            this.minLimit = new Vector3f(-Utils.EPSILON);
            this.maxLimit = new Vector3f(Utils.EPSILON);
        }

        public Link setMinLimit(final float x, final float y, final float z) {
            this.minLimit.set(x, y, z);
            return this;
        }

        public Link setMaxLimit(final float x, final float y, final float z) {
            this.maxLimit.set(x, y, z);
            return this;
        }

        public RagdollStructureBuilder endLink() {
            return this.builder;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Link link = (Link) o;

            return partA == link.partA && partB == link.partB;
        }

        @Override
        public int hashCode() {
            int result = partA.hashCode();
            result = 31 * result + partB.hashCode();
            return result;
        }
    }
}
