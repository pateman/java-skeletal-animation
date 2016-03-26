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
    private final Mesh mesh;
    private final Map<String, Animation> animationMap;

    private float lerpFactor;

    private Animation currentAnimation;
    private int currentFrame;
    private float currentLerp;
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

        this.lerpFactor = DEFAULT_LERP_FACTOR;
    }

    private void animateBone(final Bone bone) {
        final TempVars tempVars = TempVars.get();

        bone.getOffsetMatrix().identity();
        final AnimationTrack track = this.currentAnimation.getTracks().get(bone.getIndex());

        final Vector3f pos = tempVars.vect3d1.set(bone.getBindPosition());

        final AnimationKeyframe keyframe = track.getKeyframes().get(this.currentFrame % track.getKeyframes().size());
        final AnimationKeyframe keyframe1 = track.getKeyframes().get((this.currentFrame + 1) % track.getKeyframes().size());

        final Quaternionf frameRot = keyframe.getRotation().slerp(keyframe1.getRotation(), this.currentLerp,
                tempVars.quat1);
        final Vector3f framePos = keyframe.getTranslation().lerp(keyframe1.getTranslation(), this.currentLerp,
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

    private void animate() {
        while (this.currentLerp > 1.0f) {
            this.currentLerp -= 1.0f;
        }

        this.currentFrame++;
        if (this.currentFrame > this.currentAnimation.getTracks().get(0).getKeyframes().size()) {
            this.currentFrame = 0;
        }

        this.animateBone(this.mesh.getSkeleton().getRootBone());
    }

    public Animation getAnimationByName(final String animation) {
        return this.animationMap.get(animation);
    }

    public void switchToAnimation(final String animation) {
        final Animation destAnim = this.animationMap.get(animation);
        if (destAnim == null) {
            throw new IllegalArgumentException("Unknown animation " + animation);
        }

        this.currentAnimation = destAnim;
        this.currentFrame = 0;
        this.currentLerp = 0.0f;
    }

    public void stepAnimation(float deltaTime) {
        if (this.currentAnimation == null) {
            return;
        }

        this.currentLerp += DEFAULT_LERP_FACTOR * deltaTime;
        this.animate();
    }

    public List<Matrix4f> getAnimationMatrices() {
        return this.animationMatrices;
    }

    public Animation getCurrentAnimation() {
        return currentAnimation;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public float getLerpFactor() {
        return lerpFactor;
    }

    public void setLerpFactor(float lerpFactor) {
        this.lerpFactor = lerpFactor;
    }
}
