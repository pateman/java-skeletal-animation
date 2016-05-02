package pl.pateman.core.mesh;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pateman.
 */
public final class MeshSkinningInfo {
    public static final int MAX_BONES_PER_VERTEX = 3;
    public static final int MAX_BONES = 60;

    private final List<Vector3f> boneIndices;
    private final List<Vector3f> boneWeights;

    public MeshSkinningInfo() {
        this.boneIndices = new ArrayList<>();
        this.boneWeights = new ArrayList<>();
    }

    public boolean hasSkinningInfo() {
        return !this.boneIndices.isEmpty() && !this.boneWeights.isEmpty();
    }

    public List<Vector3f> getBoneIndices() {
        return boneIndices;
    }

    public List<Vector3f> getBoneWeights() {
        return boneWeights;
    }
}
