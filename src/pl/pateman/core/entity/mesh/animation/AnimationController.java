package pl.pateman.core.entity.mesh.animation;

import org.joml.Matrix4f;
import pl.pateman.core.entity.MeshEntity;
import pl.pateman.core.mesh.Mesh;
import pl.pateman.core.physics.ragdoll.Ragdoll;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static pl.pateman.core.entity.mesh.animation.BoneAnimationChannel.DEFAULT_BLENDING_TIME;

/**
 * Created by pateman.
 */
public final class AnimationController {
    private final Mesh mesh;

    private final List<Matrix4f> animationMatrices;
    private final List<BoneAnimationChannel> animationChannels;
    private final Ragdoll ragdoll;
    private final MeshEntity meshEntity;

    public AnimationController(Mesh mesh, MeshEntity meshEntity) {
        this.mesh = mesh;
        this.meshEntity = meshEntity;

        //  Initialize animation matrices.
        final int boneCount = this.mesh.getSkeleton().getBones().size();
        this.animationMatrices = new ArrayList<>(boneCount);
        for (int i = 0; i < boneCount; i++) {
            this.animationMatrices.add(this.mesh.getSkeleton().getBone(i).getOffsetMatrix());
        }

        this.animationChannels = new ArrayList<>();
        this.ragdoll = new Ragdoll(this.mesh, this.meshEntity);
    }

    private void checkChannelNameValid(final String channelName) throws IllegalArgumentException {
        if (channelName == null || channelName.isEmpty()) {
            throw new IllegalArgumentException("Invalid animation channel name");
        }
    }

    private BoneAnimationChannel getChannelByName(final String channelName) {
        for (int i = 0; i < this.animationChannels.size(); i++) {
            final BoneAnimationChannel animationChannel = this.animationChannels.get(i);
            if (animationChannel.getChannelName().equals(channelName)) {
                return animationChannel;
            }
        }
        return null;
    }

    public BoneAnimationChannel addAnimationChannel(final String channelName) {
        this.checkChannelNameValid(channelName);

        BoneAnimationChannel channel = this.getChannelByName(channelName);
        if (channel != null) {
            throw new IllegalStateException("Animation channel '" + channelName + "' already exists");
        }

        channel = new BoneAnimationChannel(channelName, this.mesh);
        this.animationChannels.add(channel);

        return channel;
    }

    public void removeAnimationChannel(final String channelName) {
        this.checkChannelNameValid(channelName);

        final Iterator<BoneAnimationChannel> iterator = this.animationChannels.iterator();
        while (iterator.hasNext()) {
            final BoneAnimationChannel channel = iterator.next();
            if (channel.getChannelName().equals(channelName)) {
                iterator.remove();
                break;
            }
        }
    }

    public BoneAnimationChannel getAnimationChannel(final String channelName) {
        this.checkChannelNameValid(channelName);

        final BoneAnimationChannel channel = this.getChannelByName(channelName);
        if (channel == null) {
            throw new IllegalStateException("Animation channel '" + channelName + "' does not exist");
        }

        return channel;
    }

    public void stepAnimation(float deltaTime) {
        if (!this.ragdoll.isEnabled()) {
            for (int i = 0; i < this.animationChannels.size(); i++) {
                this.animationChannels.get(i).stepAnimation(deltaTime);
            }
            this.ragdoll.alignRagdollToModel();
        } else {
            this.ragdoll.updateRagdoll();
        }
    }

    public void switchToAnimation(final String animation) {
        this.switchToAnimation(animation, DEFAULT_BLENDING_TIME);
    }

    public void switchToAnimation(final String animation, float blendingTime) {
        for (int i = 0; i < this.animationChannels.size(); i++) {
            this.animationChannels.get(i).switchToAnimation(animation, blendingTime);
        }
    }

    public Ragdoll getRagdoll() {
        return ragdoll;
    }

    public List<Matrix4f> getAnimationMatrices() {
        return this.animationMatrices;
    }
}
