package pl.pateman.core.physics.ragdoll;

import pl.pateman.core.mesh.Bone;
import pl.pateman.core.mesh.Mesh;

import java.util.*;

/**
 * Holds the structure of a ragdoll.
 *
 * Created by pateman.
 */
public final class RagdollStructure {
    static final byte CUSTOM_TRANSFORM_TRANSLATION = 1;
    static final byte CUSTOM_TRANSFORM_ROTATION = 2;

    private final Map<BodyPartType, BodyPart> bodyParts;
    private final List<RagdollLink> bodyLinks;
    private final Mesh mesh;

    RagdollStructure(Mesh mesh) {
        this.mesh = mesh;
        if (this.mesh == null) {
            throw new IllegalArgumentException("A valid mesh is required");
        }

        //  Initialize the body parts map.
        BodyPartType[] bodyPartTypes = BodyPartType.values();
        this.bodyParts = new HashMap<>(bodyPartTypes.length);
        for (BodyPartType partType : bodyPartTypes) {
            this.bodyParts.put(partType, new BodyPart(partType));
        }

        this.bodyLinks = new ArrayList<>(this.bodyParts.size());
    }

    Map<BodyPartType, BodyPart> getBodyParts() {
        return bodyParts;
    }

    List<RagdollLink> getBodyLinks() {
        return bodyLinks;
    }

    public List<Bone> getBodyPartBones(final BodyPartType bodyPartType) {
        if (bodyPartType == null) {
            throw new IllegalArgumentException();
        }
        return this.bodyParts.get(bodyPartType).getBones();
    }

    void setBodyPartBones(final BodyPartType bodyPart, final String... boneNames) {
        if (bodyPart == null || boneNames == null || boneNames.length == 0) {
            throw new IllegalArgumentException();
        }

        final BodyPart part = this.bodyParts.get(bodyPart);
        for (String boneName : boneNames) {
            final Bone bone = this.mesh.getSkeleton().getBoneByName(boneName);
            if (bone == null) {
                throw new IllegalArgumentException("Unknown bone '" + boneName + "'");
            }
            part.getBones().add(bone);
        }
    }

    public BodyPartCollider getBodyPartColliderType(final BodyPartType bodyPartType) {
        if (bodyPartType == null) {
            throw new IllegalArgumentException();
        }
        return this.bodyParts.get(bodyPartType).getColliderType();
    }

    void setBodyPartColliderType(final BodyPartType bodyPartType, final BodyPartCollider bodyPartCollider) {
        if (bodyPartType == null || bodyPartCollider == null) {
            throw new IllegalArgumentException();
        }
        this.bodyParts.get(bodyPartType).setColliderType(bodyPartCollider);
    }

    void setBodyPartPivotedBones(final BodyPartType bodyPartType, final Collection<String> boneNames) {
        if (bodyPartType == null || boneNames == null || boneNames.isEmpty()) {
            throw new IllegalArgumentException();
        }

        final BodyPart bodyPart = this.bodyParts.get(bodyPartType);
        for (String boneName : boneNames) {
            final Bone boneByName = this.mesh.getSkeleton().getBoneByName(boneName);
            if (boneByName == null) {
                throw new IllegalArgumentException("Unknown bone '" + boneName + "'");
            }

            bodyPart.getPivotBones().add(boneByName);
        }
    }
}
