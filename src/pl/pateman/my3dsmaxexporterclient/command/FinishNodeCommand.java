package pl.pateman.my3dsmaxexporterclient.command;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.pateman.my3dsmaxexporterclient.ClientCommand;
import pl.pateman.my3dsmaxexporterclient.CommandContext;

import static pl.pateman.my3dsmaxexporterclient.Constants.*;

/**
 * Created by pateman.
 */
public final class FinishNodeCommand implements ClientCommand {
    @Override
    public void execute(final CommandContext context) throws Exception {
        final CommandContext.NodeData nodeData = new CommandContext.NodeData();
        nodeData.mesh = context.mesh;
        nodeData.index = Integer.parseInt((String) context.stateVariables.get(CURRENT_NODE_INDEX));
        nodeData.parentIndex = Integer.parseInt((String) context.stateVariables.get(CURRENT_NODE_PARENT));
        nodeData.nodeType = (String) context.stateVariables.get(CURRENT_NODE_TYPE);

        //  Read transformation.
        final String currentTransform = (String) context.stateVariables.get(CURRENT_NODE_TRANSFORM);
        if (currentTransform != null) {
            final int transformNodeIndex = Integer.parseInt(currentTransform);
            if (transformNodeIndex == nodeData.index) {
                nodeData.translation = (Vector3f) context.stateVariables.get(CURRENT_NODE_TRANSFORM_TRANSLATION);
                nodeData.rotation = (Quaternionf) context.stateVariables.get(CURRENT_NODE_TRANSFORM_ROTATION);
                nodeData.scale = (Vector3f) context.stateVariables.get(CURRENT_NODE_TRANSFORM_SCALE);
            }
        }

        final String name = (String) context.stateVariables.get(CURRENT_NODE_NAME);
        context.nodes.put(name, nodeData);
        context.mesh = null;

        //  Cleanup the context after storing the node in the map.
        NodeDataCommand.cleanupContext(context);
    }
}
