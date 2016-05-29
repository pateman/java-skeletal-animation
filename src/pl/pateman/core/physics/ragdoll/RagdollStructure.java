package pl.pateman.core.physics.ragdoll;

import org.joml.Matrix4f;
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
    }

    Map<BodyPartType, BodyPart> getBodyParts() {
        return bodyParts;
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

    public Matrix4f getBodyPartCustomTransform(final BodyPartType bodyPartType) {
        if (bodyPartType == null) {
            throw new IllegalArgumentException();
        }
        return this.bodyParts.get(bodyPartType).getCustomTransform();
    }

    void setBodyPartCustomTransform(final BodyPartType bodyPartType, final Matrix4f matrix4f, byte transformFlag) {
        if (bodyPartType == null || matrix4f == null) {
            throw new IllegalArgumentException();
        }

        this.bodyParts.get(bodyPartType).setCustomTransform(matrix4f, transformFlag);
    }

}
