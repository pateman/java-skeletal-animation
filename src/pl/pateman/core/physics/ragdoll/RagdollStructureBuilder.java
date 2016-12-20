package pl.pateman.core.physics.ragdoll;

import org.joml.Vector3f;
import pl.pateman.core.mesh.Bone;
import pl.pateman.core.mesh.Mesh;
import pl.pateman.core.mesh.Skeleton;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A builder for ragdolls.
 *
 * Created by pateman.
 */
public final class RagdollStructureBuilder {
    private final RagdollStructure ragdollStructure;
    private final Mesh mesh;
    private final Map<BodyPartType, Part> partMap;
    private final Map<BodyPartType, Set<String>> pivotedBones;
    private final List<Link> linkList;

    public RagdollStructureBuilder(final Mesh mesh) {
        this.ragdollStructure = new RagdollStructure(mesh);
        this.mesh = mesh;
        this.partMap = new HashMap<>();
        this.pivotedBones = new HashMap<>();
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
        }

        final Map<BodyPartType, BodyPart> bodyParts = this.ragdollStructure.getBodyParts();
        for (final Link link : this.linkList) {
            this.ragdollStructure.getBodyLinks().add(new RagdollLink(bodyParts.get(link.partA),
                    bodyParts.get(link.partB), link.limits, link.linkType));
        }

        for (BodyPartType type : bodyParts.keySet()) {
            final Set<String> boneNames = this.pivotedBones.get(type);
            if (boneNames == null) {
                continue;
            }
            this.ragdollStructure.setBodyPartPivotedBones(type, boneNames);
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

    /**
     * Allows to bind bones to a particular body part without considering them as bones that make the part up. Useful
     * for attaching dummy or redundant bones to a part (for example, one could attach finger bones to a lower arm,
     * but the finger bones won't be considered when building the lower arm's physical representation).
     *
     * @param bodyPartType Part of the body to attach the bones to.
     * @param boneNames Bones that should be attached.
     * @return This builder.
     */
    public RagdollStructureBuilder pivotBonesTo(final BodyPartType bodyPartType, final String... boneNames) {
        if (bodyPartType == null || boneNames == null || boneNames.length == 0) {
            throw new IllegalArgumentException();
        }

        Set<String> boneNameSet = this.pivotedBones.get(bodyPartType);
        if (boneNameSet == null) {
            boneNameSet = new HashSet<>();
        }
        boneNameSet.addAll(Arrays.asList(boneNames));
        this.pivotedBones.put(bodyPartType, boneNameSet);

        return this;
    }

    /**
     * Small utility method which returns the names of the bones which haven't been assigned to any body part.
     *
     * @return {@code List<String>}.
     */
    public List<String> getUnassignedBones() {
        final List<String> boneNames = this.mesh.getSkeleton().getBones().
                stream().
                map(Bone::getName).
                collect(Collectors.toList());
        final Set<String> assignedBones = this.partMap.values().
                stream().
                flatMap(part -> part.boneNames.stream()).
                collect(Collectors.toSet());

        boneNames.removeAll(assignedBones);
        return boneNames;
    }

    public final class Part {
        private final BodyPartType type;
        private final Set<String> boneNames;
        private BodyPartCollider collider;
        private final Skeleton skeleton;
        private final RagdollStructureBuilder builder;

        Part(BodyPartType type, Skeleton skeleton, RagdollStructureBuilder builder) {
            this.type = type;
            this.collider = BodyPartCollider.BOX;
            this.skeleton = skeleton;
            this.builder = builder;
            this.boneNames = new LinkedHashSet<>();
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

        public RagdollStructureBuilder endPart() {
            return this.builder;
        }
    }

    public final class Link {
        private final BodyPartType partA;
        private final BodyPartType partB;
        private final RagdollStructureBuilder builder;
        private final float[] limits;
        private RagdollLinkType linkType;

        Link(BodyPartType partA, BodyPartType partB, RagdollStructureBuilder builder) {
            this.partA = partA;
            this.partB = partB;
            this.builder = builder;
            this.limits = new float[11];
            this.linkType = null;
        }

        public Link coneTwist(final float swingSpan1, final float swingSpan2, final float twistSpan) {
            //  Defaults taken from
            //  com.bulletphysics.dynamics.constraintsolver.ConeTwistConstraint.setLimit(float, float, float)
            return this.coneTwist(swingSpan1, swingSpan2, twistSpan, 0.8f, 0.3f, 1.0f);
        }

        public Link coneTwist(final float swingSpan1, final float swingSpan2, final float twistSpan,
                              final float softness, final float biasFactor, final float relaxationFactor) {
            if (this.linkType != null) {
                throw new IllegalStateException("The link is already configured as " + this.linkType);
            }
            this.linkType = RagdollLinkType.CONE_TWIST;

            this.limits[0] = swingSpan1;
            this.limits[1] = swingSpan2;
            this.limits[2] = twistSpan;
            this.limits[3] = softness;
            this.limits[4] = biasFactor;
            this.limits[5] = relaxationFactor;

            return this;
        }

        public Link hinge(final float lowerLimit, final float upperLimit, final Vector3f hingeAxisA,
                          final Vector3f hingeAxisB) {
            //  Defaults taken from
            //  com.bulletphysics.dynamics.constraintsolver.HingeConstraint.setLimit(float, float)
            return this.hinge(lowerLimit, upperLimit, 0.9f, 0.3f, 1.0f, hingeAxisA, hingeAxisB);
        }

        public Link hinge(final float lowerLimit, final float upperLimit, final float softness, final float biasFactor,
                          final float relaxationFactor, final Vector3f hingeAxisA, final Vector3f hingeAxisB) {
            if (hingeAxisA == null || hingeAxisB == null) {
                throw new IllegalArgumentException("Hinge axes need to be provided");
            }
            if (this.linkType != null) {
                throw new IllegalStateException("The link is already configured as " + this.linkType);
            }
            this.linkType = RagdollLinkType.HINGE;

            this.limits[0] = lowerLimit;
            this.limits[1] = upperLimit;
            this.limits[2] = softness;
            this.limits[3] = biasFactor;
            this.limits[4] = relaxationFactor;
            this.limits[5] = hingeAxisA.x;
            this.limits[6] = hingeAxisA.y;
            this.limits[7] = hingeAxisA.z;
            this.limits[8] = hingeAxisB.x;
            this.limits[9] = hingeAxisB.y;
            this.limits[10] = hingeAxisB.z;

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
