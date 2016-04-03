package pl.pateman.skeletal.entity.mesh.animation;

import pl.pateman.skeletal.mesh.Animation;
import pl.pateman.skeletal.mesh.Bone;
import pl.pateman.skeletal.mesh.Mesh;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by pateman.
 */
public final class BoneAnimationChannel {
    public static final float DEFAULT_ANIMATION_SPEED = 1.0f;
    public static final float DEFAULT_BLENDING_TIME = 0.5f;
    public static final AnimationPlaybackMode DEFAULT_ANIMATION_PLAYBACK_MODE = AnimationPlaybackMode.LOOP;

    private final AnimationChannelBoneMask controlledBones;
    private final String channelName;
    private final Mesh mesh;

    private final Map<String, Animation> animationMap;
    private final Map<Animation, BoneAnimator> boneAnimatorMap;

    private BoneAnimator currentAnimation;
    private BoneAnimator blendingAnimation;
    private float animBlendAmount;
    private float animBlendRate;

    BoneAnimationChannel(final String name, final Mesh mesh) {
        this.channelName = name;
        this.mesh = mesh;

        this.controlledBones = new AnimationChannelBoneMask(this.mesh.getSkeleton().getBones().size());

        //  Initialize the animation map and for each animation, create an animator.
        this.animationMap = new HashMap<>(this.mesh.getAnimations().size());
        this.boneAnimatorMap = new HashMap<>(this.mesh.getAnimations().size());
        for (Animation animation : this.mesh.getAnimations()) {
            this.animationMap.put(animation.getName(), animation);

            final BoneAnimator boneAnimator = new BoneAnimator(animation, DEFAULT_ANIMATION_PLAYBACK_MODE,
                    DEFAULT_ANIMATION_SPEED);
            this.boneAnimatorMap.put(animation, boneAnimator);
        }
    }

    private void checkIfAnimationIsSet() throws IllegalStateException {
        if (this.currentAnimation == null) {
            throw new IllegalStateException("No animation is currently set.");
        }
    }

    private Bone checkBoneValid(final String boneName) throws IllegalArgumentException {
        final Bone bone = this.mesh.getSkeleton().getBoneByName(boneName);
        if (bone == null) {
            throw new IllegalArgumentException("Bone '" + boneName + "' does not exist");
        }
        return bone;
    }

    private void processBonesTree(final Bone bone, final boolean isControlled) {
        this.controlledBones.setBoneControlled(bone, isControlled);

        for (Bone childBone : bone.getChildren()) {
            this.processBonesTree(childBone, isControlled);
        }
    }

    public void switchToAnimation(final String animation) {
        this.switchToAnimation(animation, DEFAULT_BLENDING_TIME);
    }

    public void switchToAnimation(final String animation, float blendingTime) {
        final Animation destAnim = this.animationMap.get(animation);
        if (destAnim == null) {
            throw new IllegalArgumentException("Unknown animation " + animation);
        }

        //  If an animation is currently set, we need to blend from it.
        this.blendingAnimation = null;
        if (this.currentAnimation != null) {
            blendingTime = Math.min(blendingTime, this.currentAnimation.getAnimation().getLength() /
                    this.currentAnimation.getSpeed());

            this.blendingAnimation = this.currentAnimation;
            this.animBlendAmount = 0.0f;
            this.animBlendRate = 1.0f / blendingTime;
        }

        this.currentAnimation = this.boneAnimatorMap.get(destAnim);
        this.currentAnimation.resetAnimator();
    }

    public void stepAnimation(float deltaTime) {
        if (this.currentAnimation == null) {
            return;
        }

        //  If we have an animation to blend from, compute the blending first. Also check if we haven't already fully
        //  blended the animation.
        if (this.animBlendAmount < 1.0f) {
            this.animBlendAmount += deltaTime * this.animBlendRate;
            if (this.animBlendAmount > 1.0f) {
                this.animBlendAmount = 1.0f;
                this.blendingAnimation = null;
            }
        }

        this.currentAnimation.animate(this.mesh.getSkeleton().getRootBone(), deltaTime, this.animBlendAmount,
                this.blendingAnimation, this.controlledBones);
        if (this.blendingAnimation != null) {
            this.blendingAnimation.stepAnimationTime(deltaTime);
        }
    }

    public void addAllBones() {
        for (Bone bone : this.mesh.getSkeleton().getBones()) {
            this.controlledBones.setBoneControlled(bone, true);
        }
    }

    public void removeAllBones() {
        for (Bone bone : this.mesh.getSkeleton().getBones()) {
            this.controlledBones.setBoneControlled(bone, false);
        }
    }

    public void addBone(final String boneName) {
        final Bone bone = this.checkBoneValid(boneName);
        this.controlledBones.setBoneControlled(bone, true);
    }

    public void removeBone(final String boneName) {
        final Bone bone = this.checkBoneValid(boneName);
        this.controlledBones.setBoneControlled(bone, false);
    }

    public void addBonesTree(final String rootBoneName) {
        final Bone rootBone = this.checkBoneValid(rootBoneName);
        this.processBonesTree(rootBone, true);
    }

    public void removeBonesTree(final String rootBoneName) {
        final Bone rootBone = this.checkBoneValid(rootBoneName);
        this.processBonesTree(rootBone, false);
    }

    public String getChannelName() {
        return channelName;
    }

    public Animation getCurrentAnimation() {
        this.checkIfAnimationIsSet();
        return this.currentAnimation.getAnimation();
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
