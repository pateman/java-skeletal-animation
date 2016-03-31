package pl.pateman.skeletal.entity.mesh;

import org.joml.Matrix4f;
import pl.pateman.skeletal.mesh.Animation;
import pl.pateman.skeletal.mesh.Bone;
import pl.pateman.skeletal.mesh.Mesh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pateman.
 */
public final class AnimationController {
    private static final float DEFAULT_LERP_FACTOR = 0.005f;
    private static final float DEFAULT_ANIMATION_SPEED = 1.0f;
    private static final AnimationPlaybackMode DEFAULT_ANIMATION_PLAYBACK_MODE = AnimationPlaybackMode.LOOP;

    private final Mesh mesh;
    private final Map<String, Animation> animationMap;
    private final Map<Animation, BoneAnimator> boneAnimatorMap;

    private BoneAnimator currentAnimation;
    private final List<Matrix4f> animationMatrices;

    public AnimationController(Mesh mesh) {
        this.mesh = mesh;

        //  Initialize the animation map and for each animation, create an animator.
        this.animationMap = new HashMap<>(this.mesh.getAnimations().size());
        this.boneAnimatorMap = new HashMap<>(this.mesh.getAnimations().size());
        for (Animation animation : this.mesh.getAnimations()) {
            this.animationMap.put(animation.getName(), animation);

            final BoneAnimator boneAnimator = new BoneAnimator(animation, DEFAULT_ANIMATION_PLAYBACK_MODE,
                    DEFAULT_ANIMATION_SPEED, DEFAULT_LERP_FACTOR);
            this.boneAnimatorMap.put(animation, boneAnimator);
        }

        //  Initialize animation matrices.
        this.animationMatrices = new ArrayList<>(this.mesh.getSkeleton().getBones().size());
        for (Bone bone : this.mesh.getSkeleton().getBones()) {
            this.animationMatrices.add(bone.getOffsetMatrix());
        }
    }

    private void checkIfAnimationIsSet() throws IllegalStateException {
        if (this.currentAnimation == null) {
            throw new IllegalStateException("No animation is currently set.");
        }
    }

    public void switchToAnimation(final String animation) {
        final Animation destAnim = this.animationMap.get(animation);
        if (destAnim == null) {
            throw new IllegalArgumentException("Unknown animation " + animation);
        }

        this.currentAnimation = this.boneAnimatorMap.get(destAnim);
        this.currentAnimation.resetAnimator();
    }

    public void stepAnimation(float deltaTime) {
        if (this.currentAnimation == null) {
            return;
        }

        this.currentAnimation.animate(this.mesh.getSkeleton().getRootBone(), deltaTime);
    }

    public List<Matrix4f> getAnimationMatrices() {
        return this.animationMatrices;
    }

    public Animation getCurrentAnimation() {
        this.checkIfAnimationIsSet();
        return this.currentAnimation.getAnimation();
    }

    public float getLerpFactor() {
        this.checkIfAnimationIsSet();
        return this.currentAnimation.getLerpFactor();
    }

    public void setLerpFactor(float lerpFactor) {
        this.checkIfAnimationIsSet();

        this.currentAnimation.setLerpFactor(lerpFactor);
    }

    public float getSpeed() {
        this.checkIfAnimationIsSet();
        return this.currentAnimation.getSpeed();
    }

    public void setSpeed(float speed) {
        this.checkIfAnimationIsSet();
        this.currentAnimation.setSpeed(speed);
    }

    public AnimationPlaybackMode getPlaybackMode() {
        this.checkIfAnimationIsSet();
        return this.currentAnimation.getPlaybackMode();
    }

    public void setPlaybackMode(AnimationPlaybackMode playbackMode) {
        this.checkIfAnimationIsSet();
        this.currentAnimation.setPlaybackMode(playbackMode);
    }
}
