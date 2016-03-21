package pl.pateman.skeletal.mesh;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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

    private final Matrix4f localMatrix;
    private final Matrix4f worldMatrix;
    private final Matrix4f inverseBindposeMatrix;

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
        this.bindScale = new Vector3f();

        this.localMatrix = new Matrix4f();
        this.worldMatrix = new Matrix4f();
        this.inverseBindposeMatrix = new Matrix4f();
    }

    public void addVertexWeight(int vertexId, float weight) {
        this.vertexWeights.put(vertexId, weight);
    }

    void calculateBoneMatrices() {
        this.localMatrix.rotation(this.bindRotation).setTranslation(this.bindPosition);
        if (this.parent == null) {
            this.worldMatrix.set(this.localMatrix);
        } else {
            this.parent.getWorldMatrix().mul(this.localMatrix, this.worldMatrix);
        }
        this.worldMatrix.invert(this.inverseBindposeMatrix);
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

    public Matrix4f getLocalMatrix() {
        return new Matrix4f(localMatrix);
    }

    public Matrix4f getWorldMatrix() {
        return new Matrix4f(worldMatrix);
    }

    public Matrix4f getInverseBindposeMatrix() {
        return new Matrix4f(inverseBindposeMatrix);
    }
}
