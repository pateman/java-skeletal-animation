package pl.pateman.skeletal.mesh;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pateman.
 */
public class Bone {
    private final String name;
    private final int index;

    private final Vector3f bindPosition;
    private final Quaternionf bindRotation;
    private final Vector3f bindScale;

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
    }

    public void addVertexWeight(int vertexId, float weight) {
        this.vertexWeights.put(vertexId, weight);
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
}
