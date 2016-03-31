package pl.pateman.skeletal.entity.mesh;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.pateman.skeletal.TempVars;
import pl.pateman.skeletal.Utils;
import pl.pateman.skeletal.mesh.Animation;
import pl.pateman.skeletal.mesh.AnimationKeyframe;
import pl.pateman.skeletal.mesh.AnimationTrack;
import pl.pateman.skeletal.mesh.Bone;

/**
 * Created by pateman.
 */
final class BoneAnimator {
    private float lerpFactor;
    private float speed;
    private AnimationPlaybackMode playbackMode;
    private final Animation animation;
    private float animTime;

    public BoneAnimator(final Animation animation, AnimationPlaybackMode playbackMode, float speed, float lerpFactor) {
        this.animation = animation;
        this.playbackMode = playbackMode;
        this.speed = speed;
        this.lerpFactor = lerpFactor;
    }

    private static float clampAnimationTime(final float animationTime, final float animationDuration,
                                           final AnimationPlaybackMode playbackMode) {
        if (animationTime == 0.0f) {
            return 0.0f;
        }

        float animTime = animationTime;
        switch (playbackMode) {
            case LOOP:
                animTime = animationTime % animationDuration;
                break;
            case ONCE:
                animTime = animationTime > animationDuration ? animationDuration : animationTime;
                break;
        }

        if (animTime < 0.0f) {
            animTime = -animTime;
        }

        return animTime;
    }

    private void animateBone(final Bone bone) {
        final TempVars tempVars = TempVars.get();

        //  Basing on the animation's current time, calculate the frames for interpolation.
        final AnimationTrack track = this.animation.getTracks().get(bone.getIndex());
        final int lastFrame = track.getKeyframes().size() - 1;
        int startFrame = 0;
        int endFrame = 1;
        if (this.animTime >= 0.0f && lastFrame != 0) {
            //  We're on the last frame.
            if (this.animTime >= track.getKeyframes().get(lastFrame).getTime()) {
                startFrame = endFrame = lastFrame;
            } else {
                //  Any frame between the start and the end.
                for (int i = 0; i < lastFrame && track.getKeyframes().get(i).getTime() < this.animTime; i++) {
                    startFrame = i;
                    endFrame = i + 1;
                }
            }
        } else {
            //  Just in case.
            startFrame = endFrame = 0;
        }

        bone.getOffsetMatrix().identity();
        final Vector3f pos = tempVars.vect3d1.set(bone.getBindPosition());

        final AnimationKeyframe keyframe = track.getKeyframes().get(startFrame);
        final AnimationKeyframe keyframe1 = track.getKeyframes().get(endFrame);

        final Quaternionf frameRot = keyframe.getRotation().slerp(keyframe1.getRotation(), this.lerpFactor,
                tempVars.quat1);
        final Vector3f framePos = keyframe.getTranslation().lerp(keyframe1.getTranslation(), this.lerpFactor,
                tempVars.vect3d2);

        pos.add(framePos);
        final Quaternionf rot = bone.getBindRotation().mul(frameRot, tempVars.quat2);

        Utils.fromRotationTranslationScale(bone.getOffsetMatrix(), rot, pos, bone.getBindScale());
        if (bone.getParent() != null) {
            bone.getParent().getOffsetMatrix().mul(bone.getOffsetMatrix(), bone.getOffsetMatrix());
        }

        tempVars.release();
        for (Bone child : bone.getChildren()) {
            this.animateBone(child);
        }
    }

    public void resetAnimator() {
        this.animTime = 0.0f;
    }

    public void animate(final Bone rootBone, final float deltaTime) {
        this.animateBone(rootBone);

        this.animTime += deltaTime * this.speed;
        this.animTime = BoneAnimator.clampAnimationTime(this.animTime, this.animation.getLength(), this.playbackMode);
    }

    public Animation getAnimation() {
        return animation;
    }

    public float getLerpFactor() {
        return lerpFactor;
    }

    public void setLerpFactor(float lerpFactor) {
        this.lerpFactor = lerpFactor;
        this.resetAnimator();
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
        this.resetAnimator();
    }

    public AnimationPlaybackMode getPlaybackMode() {
        return playbackMode;
    }

    public void setPlaybackMode(AnimationPlaybackMode playbackMode) {
        this.playbackMode = playbackMode;
        this.resetAnimator();
    }
}
