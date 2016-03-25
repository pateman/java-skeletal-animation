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
}
