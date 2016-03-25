package pl.pateman.skeletal.mesh;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.pateman.skeletal.Utils;

import java.util.*;

/**
 * Created by pateman.
 */
public class Bone {
    private final String name;
    private final int index;

    private final Vector3f bindPosition;
    private final Quaternionf bindRotation;
    private final Vector3f bindScale;

    private final Matrix4f localBindMatrix;
    private final Matrix4f worldBindMatrix;
    private final Matrix4f inverseBindMatrix;
    private final Matrix4f offsetMatrix;

    private Bone parent;
    private final List<Bone> children;
    private final Map<Integer, Float> vertexWeights;

    public Bone(String name, int index) {
        if (name == null) {
            throw new IllegalArgumentException("Bone name cannot be null");
        }
        this.name = name;
        this.index = index;

        this.children = new ArrayList<>();
        this.vertexWeights = new HashMap<>();

        this.bindPosition = new Vector3f();
        this.bindRotation = new Quaternionf();
        this.bindScale = new Vector3f(1.0f, 1.0f, 1.0f);

        this.localBindMatrix = new Matrix4f();
        this.worldBindMatrix = new Matrix4f();
        this.inverseBindMatrix = new Matrix4f();
        this.offsetMatrix = new Matrix4f();
    }

    public void addVertexWeight(int vertexId, float weight) {
        this.vertexWeights.put(vertexId, weight);
    }

    void calculateBindMatrices() {
        this.localBindMatrix.set(Utils.fromRotationTranslationScale(this.bindRotation, this.bindPosition,
                this.bindScale));
        if (this.parent == null) {
            this.worldBindMatrix.set(this.localBindMatrix);
        } else {
            this.parent.getWorldBindMatrix().mul(this.localBindMatrix, this.worldBindMatrix);
        }
        this.worldBindMatrix.invert(this.inverseBindMatrix);
    }

    Map<Integer, Float> getVertexWeights() {
        return vertexWeights;
    }

    public float getWeight(int vertexId) {
        final Float weight = this.vertexWeights.get(vertexId);
        return weight == null ? Float.NaN : weight;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public Bone getParent() {
        return parent;
    }

    public void setParent(Bone parent) {
        this.parent = parent;
    }

    public List<Bone> getChildren() {
        return children;
    }

    public Vector3f getBindPosition() {
        return bindPosition;
    }

    public Quaternionf getBindRotation() {
        return bindRotation;
    }

    public Vector3f getBindScale() {
        return bindScale;
    }

    public Matrix4f getLocalBindMatrix() {
        return localBindMatrix;
    }

    public Matrix4f getWorldBindMatrix() {
        return worldBindMatrix;
    }

    public Matrix4f getInverseBindMatrix() {
        return inverseBindMatrix;
    }

    public Matrix4f getOffsetMatrix() {
        return offsetMatrix;
    }

    @Override
    public String toString() {
        return "Bone{" +
                "name='" + name + '\'' +
                ", index=" + index +
                '}';
    }
}
