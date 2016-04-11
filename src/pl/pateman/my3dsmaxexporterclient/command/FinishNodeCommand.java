package pl.pateman.my3dsmaxexporterclient.command;

import pl.pateman.my3dsmaxexporterclient.ClientCommand;
import pl.pateman.my3dsmaxexporterclient.CommandContext;

/**
 * Created by pateman.
 */
public final class FinishNodeCommand implements ClientCommand {
    @Override
    public void execute(final CommandContext context) throws Exception {
        final CommandContext.NodeData nodeData = new CommandContext.NodeData();
        nodeData.mesh = context.mesh;
        nodeData.index = Integer.parseInt((String) context.stateVariables.get(NodeDataCommand.CURRENT_NODE_INDEX));
        nodeData.parentIndex = Integer.parseInt((String) context.stateVariables.get(NodeDataCommand.CURRENT_NODE_PARENT));
        nodeData.nodeType = (String) context.stateVariables.get(NodeDataCommand.CURRENT_NODE_TYPE);

        final String name = (String) context.stateVariables.get(NodeDataCommand.CURRENT_NODE_NAME);
        context.nodes.put(name, nodeData);
        context.mesh = null;
    }
}
