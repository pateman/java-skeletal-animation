package pl.pateman.core.physics.ragdoll;

import org.joml.Vector3f;

/**
 * Defines a connection between two body parts of a ragdoll.
 *
 * Created by pateman.
 */
class RagdollLink {
    private final BodyPart partA;
    private final BodyPart partB;
    private final Vector3f minLimit;
    private final Vector3f maxLimit;

    RagdollLink(BodyPart partA, BodyPart partB, Vector3f minLimit, Vector3f maxLimit) {
        this.partA = partA;
        this.partB = partB;
        this.minLimit = minLimit;
        this.maxLimit = maxLimit;
    }

    public BodyPart getPartA() {
        return partA;
    }

    public BodyPart getPartB() {
        return partB;
    }

    public Vector3f getMinLimit() {
        return minLimit;
    }

    public Vector3f getMaxLimit() {
        return maxLimit;
    }
}
