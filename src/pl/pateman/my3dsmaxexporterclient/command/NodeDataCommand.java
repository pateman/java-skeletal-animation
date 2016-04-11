package pl.pateman.my3dsmaxexporterclient.command;

import pl.pateman.my3dsmaxexporterclient.ClientCommand;
import pl.pateman.my3dsmaxexporterclient.CommandContext;

import static pl.pateman.my3dsmaxexporterclient.Constants.*;

/**
 * Created by pateman.
 */
public final class NodeDataCommand implements ClientCommand {


    @Override
    public void execute(final CommandContext context) throws Exception {
        final String cmd = context.commandParameters[0];

        switch (cmd) {
            case NAME:
                context.stateVariables.put(CURRENT_NODE_NAME, context.commandParameters[1]);
                break;
            case INDEX:
                context.stateVariables.put(CURRENT_NODE_INDEX, context.commandParameters[1]);
                break;
            case PARENT:
                context.stateVariables.put(CURRENT_NODE_PARENT, context.commandParameters[1]);
                break;
            case TYPE:
                context.stateVariables.put(CURRENT_NODE_TYPE, context.commandParameters[1]);
                break;
        }
    }

    public static void cleanupContext(final CommandContext context) {
        context.stateVariables.remove(CURRENT_NODE_NAME);
        context.stateVariables.remove(CURRENT_NODE_INDEX);
        context.stateVariables.remove(CURRENT_NODE_PARENT);
        context.stateVariables.remove(CURRENT_NODE_TYPE);
    }
}
