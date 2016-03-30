package pl.pateman.skeletal.entity.mesh;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.pateman.skeletal.TempVars;
import pl.pateman.skeletal.Utils;
import pl.pateman.skeletal.mesh.*;

import java.util.*;

/**
 * Created by pateman.
 */
public final class AnimationController {
    private static final float DEFAULT_LERP_FACTOR = 0.005f;
    private static final float DEFAULT_ANIMATION_SPEED = 1.0f;
    private static final AnimationPlaybackMode DEFAULT_ANIMATION_PLAYBACK_MODE = AnimationPlaybackMode.LOOP;

    private final Mesh mesh;
    private final Map<String, Animation> animationMap;

    private float lerpFactor;
    private float speed;
    private AnimationPlaybackMode playbackMode;

    private Animation currentAnimation;
    private float animTime;
    private final List<Matrix4f> animationMatrices;

    public AnimationController(Mesh mesh) {
        this.mesh = mesh;

        this.animationMap = new HashMap<>(this.mesh.getAnimations().size());
        for (Animation animation : this.mesh.getAnimations()) {
            this.animationMap.put(animation.getName(), animation);
        }

        //  Initialize animation matrices.
        this.animationMatrices = new ArrayList<>(this.mesh.getSkeleton().getBones().size());
        for (Bone bone : this.mesh.getSkeleton().getBones()) {
            this.animationMatrices.add(bone.getOffsetMatrix());
        }

        //  Set defaults.
        this.lerpFactor = DEFAULT_LERP_FACTOR;
        this.speed = DEFAULT_ANIMATION_SPEED;
        this.playbackMode = DEFAULT_ANIMATION_PLAYBACK_MODE;
    }

    private void animateBone(final Bone bone) {
        final TempVars tempVars = TempVars.get();

        //  Basing on the animation's current time, calculate the frames for interpolation.
        final AnimationTrack track = this.currentAnimation.getTracks().get(bone.getIndex());
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

    public void switchToAnimation(final String animation) {
        final Animation destAnim = this.animationMap.get(animation);
        if (destAnim == null) {
            throw new IllegalArgumentException("Unknown animation " + animation);
        }

        this.currentAnimation = destAnim;
        this.animTime = 0.0f;
    }

    public void stepAnimation(float deltaTime) {
        if (this.currentAnimation == null) {
            return;
        }

        this.animateBone(this.mesh.getSkeleton().getRootBone());

        this.animTime += deltaTime * this.speed;
        this.animTime = Utils.clampAnimationTime(this.animTime, this.currentAnimation.getLength(), this.playbackMode);
    }

    public List<Matrix4f> getAnimationMatrices() {
        return this.animationMatrices;
    }

    public Animation getCurrentAnimation() {
        return currentAnimation;
    }

    public float getLerpFactor() {
        return lerpFactor;
    }

    public void setLerpFactor(float lerpFactor) {
        this.lerpFactor = lerpFactor;
        this.animTime = 0.0f;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
        this.animTime = 0.0f;
    }

    public AnimationPlaybackMode getPlaybackMode() {
        return playbackMode;
    }

    public void setPlaybackMode(AnimationPlaybackMode playbackMode) {
        this.playbackMode = playbackMode;
        this.animTime = 0.0f;
    }
}
