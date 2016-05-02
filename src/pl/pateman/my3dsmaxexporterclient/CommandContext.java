package pl.pateman.my3dsmaxexporterclient;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.pateman.core.mesh.Mesh;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by pateman.
 */
public final class CommandContext {
    public Mesh mesh;
    public String[] commandParameters;
    public String outputFile;
    public final Map<String, Object> stateVariables;
    public final Map<String, NodeData> nodes;

    public CommandContext() {
        this.stateVariables = new HashMap<>();
        this.nodes = new HashMap<>();
    }

    public static class NodeData {
        public Mesh mesh;
        public String nodeType;
        public Integer index;
        public Integer parentIndex;
        public Vector3f translation;
        public Quaternionf rotation;
        public Vector3f scale;
    }
}
