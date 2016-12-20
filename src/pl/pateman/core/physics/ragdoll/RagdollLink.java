package pl.pateman.core.physics.ragdoll;

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

    RagdollLink(BodyPart partA, BodyPart partB, float[] limits, RagdollLinkType linkType) {
        this.partA = partA;
        this.partB = partB;
        this.limits = limits;
        this.linkType = linkType;
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
}
