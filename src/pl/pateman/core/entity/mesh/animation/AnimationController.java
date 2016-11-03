package pl.pateman.core.entity.mesh.animation;

import org.joml.Matrix4f;
import pl.pateman.core.entity.MeshEntity;
import pl.pateman.core.mesh.Bone;
import pl.pateman.core.mesh.Mesh;
import pl.pateman.core.physics.ragdoll.Ragdoll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pl.pateman.core.entity.mesh.animation.BoneAnimationChannel.DEFAULT_BLENDING_TIME;

/**
 * Created by pateman.
 */
public final class AnimationController {
    private final Mesh mesh;

    private final List<Matrix4f> animationMatrices;
    private final Map<String, BoneAnimationChannel> animationChannels;
    private final Ragdoll ragdoll;
    private final MeshEntity meshEntity;

    public AnimationController(Mesh mesh, MeshEntity meshEntity) {
        this.mesh = mesh;
        this.meshEntity = meshEntity;

        //  Initialize animation matrices.
        this.animationMatrices = new ArrayList<>(this.mesh.getSkeleton().getBones().size());
        for (Bone bone : this.mesh.getSkeleton().getBones()) {
            this.animationMatrices.add(bone.getOffsetMatrix());
        }

        this.animationChannels = new HashMap<>();
        this.ragdoll = new Ragdoll(this.mesh, this.meshEntity);
    }

    private void checkChannelNameValid(final String channelName) throws IllegalArgumentException {
        if (channelName == null || channelName.isEmpty()) {
            throw new IllegalArgumentException("Invalid animation channel name");
        }
    }

    public BoneAnimationChannel addAnimationChannel(final String channelName) {
        this.checkChannelNameValid(channelName);

        BoneAnimationChannel channel = this.animationChannels.get(channelName);
        if (channel != null) {
            throw new IllegalStateException("Animation channel '" + channelName + "' already exists");
        }

        channel = new BoneAnimationChannel(channelName, this.mesh);
        this.animationChannels.put(channelName, channel);

        return channel;
    }

    public void removeAnimationChannel(final String channelName) {
        this.checkChannelNameValid(channelName);
        this.animationChannels.remove(channelName);
    }

    public BoneAnimationChannel getAnimationChannel(final String channelName) {
        this.checkChannelNameValid(channelName);

        final BoneAnimationChannel channel = this.animationChannels.get(channelName);
        if (channel == null) {
            throw new IllegalStateException("Animation channel '" + channelName + "' does not exist");
        }

        return channel;
    }

    public void stepAnimation(float deltaTime) {
        for (BoneAnimationChannel channel : this.animationChannels.values()) {
            channel.stepAnimation(deltaTime);
        }
        this.ragdoll.updateRagdoll();
    }

    public void switchToAnimation(final String animation) {
        this.switchToAnimation(animation, DEFAULT_BLENDING_TIME);
    }

    public void switchToAnimation(final String animation, float blendingTime) {
        for (BoneAnimationChannel channel : this.animationChannels.values()) {
            channel.switchToAnimation(animation, blendingTime);
        }
    }

    public Ragdoll getRagdoll() {
        return ragdoll;
    }

    public List<Matrix4f> getAnimationMatrices() {
        return this.animationMatrices;
    }
}
