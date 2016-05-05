package pl.pateman.core.entity.line;

import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Created by pateman.
 */
public class Line {
    private final Vector3f from;
    private final Vector3f to;
    private final Vector4f color;

    public Line() {
        this.from = new Vector3f();
        this.to = new Vector3f();
        this.color = new Vector4f();
    }

    public Line(final Vector3f from, final Vector3f to, final Vector4f color) {
        this();

        if (from == null || to == null || color == null) {
            throw new IllegalArgumentException("None of the values can be NULL");
        }

        this.from.set(from);
        this.to.set(to);
        this.color.set(color);
    }

    public Vector3f getFrom() {
        return from;
    }

    public Vector3f getTo() {
        return to;
    }

    public Vector4f getColor() {
        return color;
    }
}
