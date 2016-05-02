package pl.pateman.core.entity.mesh.animation;

import pl.pateman.core.mesh.Bone;

import java.util.BitSet;

/**
 * Created by pateman.
 */
final class AnimationChannelBoneMask {
    private final BitSet boneMask;

    public AnimationChannelBoneMask(int numberOfBones) {
        this.boneMask = new BitSet(numberOfBones);
    }

    public boolean isBoneControlled(final Bone bone) {
        return this.boneMask.get(bone.getIndex());
    }

    public void setBoneControlled(final Bone bone, final boolean isControlled) {
        this.boneMask.set(bone.getIndex(), isControlled);
    }
}
