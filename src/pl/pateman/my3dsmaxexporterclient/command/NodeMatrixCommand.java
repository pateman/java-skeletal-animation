package pl.pateman.my3dsmaxexporterclient.command;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.pateman.my3dsmaxexporterclient.ClientCommand;
import pl.pateman.my3dsmaxexporterclient.CommandContext;

import static pl.pateman.my3dsmaxexporterclient.Constants.*;

/**
 * Created by pateman.
 */
public final class NodeMatrixCommand implements ClientCommand {
    @Override
    public void execute(CommandContext context) throws Exception {
        final String cmd = context.commandParameters[0];

        switch (cmd) {
            case BEGIN_TRANSFORM:
                context.stateVariables.put(CURRENT_NODE_TRANSFORM, context.commandParameters[1]);
                break;
            case TRANSLATION:
                final Vector3f translation = new Vector3f();
                translation.x = Float.parseFloat(context.commandParameters[1]);
                translation.y = Float.parseFloat(context.commandParameters[2]);
                translation.z = Float.parseFloat(context.commandParameters[3]);

                context.stateVariables.put(CURRENT_NODE_TRANSFORM_TRANSLATION, translation);
                break;
            case ROTATION:
                final Quaternionf rotation = new Quaternionf();
                rotation.x = Float.parseFloat(context.commandParameters[1]);
                rotation.y = Float.parseFloat(context.commandParameters[2]);
                rotation.z = Float.parseFloat(context.commandParameters[3]);
                rotation.w = Float.parseFloat(context.commandParameters[4]);

                context.stateVariables.put(CURRENT_NODE_TRANSFORM_ROTATION, rotation);
                break;
            case SCALE:
                final Vector3f scale = new Vector3f();
                scale.x = Float.parseFloat(context.commandParameters[1]);
                scale.y = Float.parseFloat(context.commandParameters[2]);
                scale.z = Float.parseFloat(context.commandParameters[3]);

                context.stateVariables.put(CURRENT_NODE_TRANSFORM_SCALE, scale);
                break;
        }
    }
}
