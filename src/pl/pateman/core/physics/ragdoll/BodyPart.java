package pl.pateman.core.physics.ragdoll;

import org.joml.Matrix4f;
import pl.pateman.core.mesh.Bone;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pateman.
 */
final class BodyPart {
    private final BodyPartType partType;
    private final List<Bone> bones;
    private BodyPartCollider colliderType;
    private Matrix4f customTransform;
    private byte customTransformFlag;
    private final List<Bone> pivotBones;

    BodyPart(BodyPartType partType) {
        this.partType = partType;
        this.bones = new ArrayList<>();
        this.pivotBones = new ArrayList<>();
        this.colliderType = BodyPartCollider.BOX;
    }

    boolean isConfigured() {
        return !this.bones.isEmpty();
    }

    Bone getFirstBone() {
        return this.bones.get(0);
    }

    Bone getLastBone() {
        return this.bones.get(this.bones.size() - 1);
    }

    BodyPartType getPartType() {
        return partType;
    }

    List<Bone> getBones() {
        return bones;
    }

    BodyPartCollider getColliderType() {
        return colliderType;
    }

    void setColliderType(BodyPartCollider colliderType) {
        this.colliderType = colliderType;
    }

    Matrix4f getCustomTransform() {
        return customTransform;
    }

    byte getCustomTransformFlag() {
        return customTransformFlag;
    }

    void setCustomTransform(Matrix4f customTransform, byte customTransformFlag) {
        this.customTransform = customTransform;
        this.customTransformFlag = customTransformFlag;
    }

    List<Bone> getPivotBones() {
        return pivotBones;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BodyPart bodyPart = (BodyPart) o;

        return partType == bodyPart.partType && colliderType == bodyPart.colliderType;
    }

    @Override
    public int hashCode() {
        int result = partType.hashCode();
        result = 31 * result + (colliderType != null ? colliderType.hashCode() : 0);
        return result;
    }
}
