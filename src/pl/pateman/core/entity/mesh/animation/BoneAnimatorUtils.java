package pl.pateman.core.entity.mesh.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.pateman.core.mesh.AnimationKeyframe;
import pl.pateman.core.mesh.AnimationTrack;
import pl.pateman.core.mesh.Bone;

/**
 * Created by pateman.
 */
final class BoneAnimatorUtils {
    private BoneAnimatorUtils() {

    }

    public static float clampAnimationTime(final float animationTime, final float animationDuration,
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
                animTime = Math.min(animationTime, animationDuration);
                break;
        }

        if (animTime < 0.0f) {
            animTime = -animTime;
        }

        return animTime;
    }

    public static void getFrame(final BoneAnimator animator, final Bone bone, final float lerpFactor,
                                 final Quaternionf outRotation, final Vector3f outTranslation) {
        //  Basing on the animation's current time, calculate the frames for interpolation.
//        final AnimationTrack track = animator.getAnimation().getTracks().get(bone.getIndex());
        final AnimationTrack track = animator.getAnimation().getTrackForBone(bone);
        final int lastFrame = track.getKeyframes().size() - 1;
        int startFrame = 0;
        int endFrame;
        if (animator.getAnimTime() >= 0.0f && lastFrame != 0) {
            //  We're on the last frame.
            if (animator.getAnimTime() >= track.getKeyframes().get(lastFrame).getTime()) {
                startFrame = endFrame = lastFrame;
            } else {
                //  Any frame between the start and the end.
                for (int i = 0; i < lastFrame && track.getKeyframes().get(i).getTime() < animator.getAnimTime(); i++) {
                    startFrame = i;
                }
                endFrame = startFrame + 1;
            }
        } else {
            //  Just in case.
            startFrame = endFrame = 0;
        }

        //  Get the respective keyframes.
        final AnimationKeyframe keyframe = track.getKeyframes().get(startFrame);
        final AnimationKeyframe keyframe1 = track.getKeyframes().get(endFrame);

        //  Interpolate between the start and the end frame and set the results in the output parameters.
        keyframe.getRotation().slerp(keyframe1.getRotation(), lerpFactor, outRotation);
        keyframe.getTranslation().lerp(keyframe1.getTranslation(), lerpFactor, outTranslation);
    }
}
