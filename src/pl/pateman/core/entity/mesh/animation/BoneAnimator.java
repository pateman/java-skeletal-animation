package pl.pateman.core.entity.mesh.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.pateman.core.TempVars;
import pl.pateman.core.Utils;
import pl.pateman.core.mesh.Animation;
import pl.pateman.core.mesh.Bone;
import pl.pateman.core.mesh.BoneManualControlType;

/**
 * Created by pateman.
 */
final class BoneAnimator {
    private float speed;
    private AnimationPlaybackMode playbackMode;
    private final Animation animation;
    private float animTime;

    public BoneAnimator(final Animation animation, AnimationPlaybackMode playbackMode, float speed) {
        this.animation = animation;
        this.playbackMode = playbackMode;
        this.speed = speed;
    }

    private void animateBone(final Bone bone, final float lerpFactor, final BoneAnimator blendFrom,
                             final AnimationChannelBoneMask boneMask) {
        //  If the bone is controlled by the bone mask that an animation channel has passed to this animator,
        //  process it. Otherwise, just process the children.
        if (boneMask.isBoneControlled(bone)) {
            final TempVars tempVars = TempVars.get();

            bone.getOffsetMatrix().identity();
            final Vector3f pos = tempVars.vect3d1.set(bone.getBindPosition());
            final Quaternionf rot = tempVars.quat2.set(bone.getBindRotation());
            //  If the user has requested full control over the bone, use the data that they have provided.
            if (bone.getManualControlType().equals(BoneManualControlType.FULL)) {
                pos.add(bone.getManualControlPosition(), pos);
                rot.mul(bone.getManualControlRotation(), rot);
            } else {
                //  Get the frame of the current animation.
                final Quaternionf frameRot = tempVars.quat1;
                final Vector3f framePos = tempVars.vect3d2;
                BoneAnimatorUtils.getFrame(this, bone, lerpFactor, frameRot, framePos);

                //  If there's an animation that we need to blend from...
                if (blendFrom != null) {
                    final Quaternionf blendFrameRot = tempVars.quat3;
                    final Vector3f blendFramePos = tempVars.vect3d3;

                    //  ...get its frame...
                    BoneAnimatorUtils.getFrame(blendFrom, bone, lerpFactor, blendFrameRot, blendFramePos);

                    //  ...and interpolate between the current animation and the one we're blending from.
                    frameRot.slerp(blendFrameRot, 1.0f - lerpFactor, frameRot);
                    framePos.lerp(blendFramePos, 1.0f - lerpFactor, framePos);
                }

                //  Apply frame transformation to the bind pose.
                pos.add(framePos, pos);
                rot.mul(frameRot, rot);

                //  Finally, if the user has requested to blend their transform with animation, do it now.
                if (bone.getManualControlType().equals(BoneManualControlType.BLEND_WITH_ANIMATION)) {
                    pos.add(bone.getManualControlPosition(), pos);
                    rot.mul(bone.getManualControlRotation(), rot);
                }
            }

            //  Calculate the offset matrix.
            Utils.fromRotationTranslationScale(bone.getOffsetMatrix(), rot, pos, bone.getBindScale());
            if (bone.getParent() != null) {
                bone.getParent().getOffsetMatrix().mul(bone.getOffsetMatrix(), bone.getOffsetMatrix());
            }

            tempVars.release();
        }
        for (Bone child : bone.getChildren()) {
            this.animateBone(child, lerpFactor, blendFrom, boneMask);
        }
    }

    public void stepAnimationTime(float deltaTime) {
        this.animTime = BoneAnimatorUtils.clampAnimationTime(this.animTime + (deltaTime * this.speed),
                this.animation.getLength(), this.playbackMode);
    }

    public void resetAnimator() {
        this.animTime = 0.0f;
    }

    public void animate(final Bone rootBone, final float deltaTime, final float lerpFactor,
                        final BoneAnimator blendFrom, final AnimationChannelBoneMask boneMask) {
        this.animateBone(rootBone, lerpFactor, blendFrom, boneMask);
        this.stepAnimationTime(deltaTime);
    }

    public Animation getAnimation() {
        return animation;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
        this.resetAnimator();
    }

    public float getAnimTime() {
        return animTime;
    }

    public AnimationPlaybackMode getPlaybackMode() {
        return playbackMode;
    }

    public void setPlaybackMode(AnimationPlaybackMode playbackMode) {
        this.playbackMode = playbackMode;
        this.resetAnimator();
    }
}
