package pl.pateman.core.mesh;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Created by pateman.
 */
public final class AnimationKeyframe {
    private final float time;
    private final Vector3f translation;
    private final Quaternionf rotation;

    public AnimationKeyframe(float time, Vector3f translation, Quaternionf rotation) {
        this.time = time;
        this.translation = translation;
        this.rotation = rotation;
    }

    public float getTime() {
        return time;
    }

    public Vector3f getTranslation() {
        return translation;
    }

    public Quaternionf getRotation() {
        return rotation;
    }
}
