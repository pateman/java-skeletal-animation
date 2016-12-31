package pl.pateman.core.physics.ragdoll;

import pl.pateman.core.mesh.Bone;

/**
 * Defines a connection between two body parts of a ragdoll.
 *
 * Created by pateman.
 */
class RagdollLink {
    private final BodyPart partA;
    private final BodyPart partB;
    private final float[] limits;
    private final RagdollLinkType linkType;
    private final Bone linkBone;

    RagdollLink(BodyPart partA, BodyPart partB, float[] limits, RagdollLinkType linkType, Bone linkBone) {
        this.partA = partA;
        this.partB = partB;
        this.limits = limits;
        this.linkType = linkType;
        this.linkBone = linkBone;
    }

    BodyPart getPartA() {
        return partA;
    }

    BodyPart getPartB() {
        return partB;
    }

    float[] getLimits() {
        return limits;
    }

    RagdollLinkType getLinkType() {
        return linkType;
    }

    Bone getLinkBone() {
        return linkBone;
    }
}
