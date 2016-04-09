package pl.pateman.skeletal.mesh;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pateman.
 */
public final class Animation {
    private final String name;
    private final float length;
    private final List<AnimationTrack> tracks;
    private int frameCount;

    public Animation(String name, float length) {
        this.name = name;
        this.length = length;
        this.tracks = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public float getLength() {
        return length;
    }

    public List<AnimationTrack> getTracks() {
        return tracks;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public void setFrameCount(int frameCount) {
        this.frameCount = frameCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Animation animation = (Animation) o;

        if (Float.compare(animation.length, length) != 0) return false;
        if (frameCount != animation.frameCount) return false;
        return name != null ? name.equals(animation.name) : animation.name == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (length != +0.0f ? Float.floatToIntBits(length) : 0);
        result = 31 * result + frameCount;
        return result;
    }
}
