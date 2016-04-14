package pl.pateman.skeletal.json;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.pateman.skeletal.mesh.Mesh;

/**
 * Created by pateman.
 */
class JSONSceneData {
    enum NodeType {
        Mesh,
        Bone
    }

    private Mesh mesh;
    private NodeType nodeType;
    private int index;
    private int parentIndex;
    private Vector3f translation;
    private Quaternionf rotation;
    private Vector3f scale;

    public Mesh getMesh() {
        return mesh;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public int getIndex() {
        return index;
    }

    public int getParentIndex() {
        return parentIndex;
    }

    public Vector3f getTranslation() {
        return translation;
    }

    public Quaternionf getRotation() {
        return rotation;
    }

    public Vector3f getScale() {
        return scale;
    }
}
