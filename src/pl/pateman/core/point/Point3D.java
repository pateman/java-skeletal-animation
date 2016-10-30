package pl.pateman.core.point;

import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Defines a single point in the 3D space.
 *
 * Created by pateman.
 */
public final class Point3D {
    private final Vector3f position;
    private final Vector4f color;
    private float thickness;

    public Point3D() {
        this.position = new Vector3f();
        this.color = new Vector4f(1.0f);
        this.thickness = 1.0f;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector4f getColor() {
        return color;
    }

    public float getThickness() {
        return thickness;
    }

    public void setThickness(float thickness) {
        this.thickness = Math.max(1.0f, thickness);
    }
}
