package pl.pateman.skeletal.mesh;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pateman.
 */
public final class AnimationTrack {
    private final Bone bone;
    private final List<AnimationKeyframe> keyframes;

    public AnimationTrack(Bone bone) {
        this.bone = bone;
        this.keyframes = new ArrayList<>();
    }

    public Bone getBone() {
        return bone;
    }

    public List<AnimationKeyframe> getKeyframes() {
        return keyframes;
    }
}
