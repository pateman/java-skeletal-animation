package pl.pateman.my3dsmaxexporterclient.command;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.pateman.my3dsmaxexporterclient.ClientCommand;
import pl.pateman.my3dsmaxexporterclient.CommandContext;
import pl.pateman.my3dsmaxexporterclient.ExporterUtils;
import pl.pateman.skeletal.mesh.Mesh;

import static pl.pateman.my3dsmaxexporterclient.Constants.*;

/**
 * Created by pateman.
 */
public final class BeginNodeCommand implements ClientCommand {
    @Override
    public void execute(final CommandContext context) throws Exception {
        context.mesh = new Mesh();

        context.stateVariables.put(CURRENT_NODE_NAME, ExporterUtils.decodeString(context.commandParameters[1]));
        context.stateVariables.put(CURRENT_NODE_INDEX, context.commandParameters[2]);
        context.stateVariables.put(CURRENT_NODE_PARENT, context.commandParameters[3]);

        context.stateVariables.put(CURRENT_NODE_TRANSFORM, context.commandParameters[2]);
        final Vector3f translation = new Vector3f();
        translation.x = Float.parseFloat(context.commandParameters[4]);
        translation.y = Float.parseFloat(context.commandParameters[5]);
        translation.z = Float.parseFloat(context.commandParameters[6]);
        context.stateVariables.put(CURRENT_NODE_TRANSFORM_TRANSLATION, translation);

        final Quaternionf rotation = new Quaternionf();
        rotation.x = Float.parseFloat(context.commandParameters[7]);
        rotation.y = Float.parseFloat(context.commandParameters[8]);
        rotation.z = Float.parseFloat(context.commandParameters[9]);
        rotation.w = Float.parseFloat(context.commandParameters[10]);
        context.stateVariables.put(CURRENT_NODE_TRANSFORM_ROTATION, rotation);

        final Vector3f scale = new Vector3f();
        scale.x = Float.parseFloat(context.commandParameters[11]);
        scale.y = Float.parseFloat(context.commandParameters[12]);
        scale.z = Float.parseFloat(context.commandParameters[13]);
        context.stateVariables.put(CURRENT_NODE_TRANSFORM_SCALE, scale);
    }

    public static void cleanupContext(final CommandContext context) {
        context.stateVariables.remove(CURRENT_NODE_NAME);
        context.stateVariables.remove(CURRENT_NODE_INDEX);
        context.stateVariables.remove(CURRENT_NODE_PARENT);
        context.stateVariables.remove(CURRENT_NODE_TYPE);
    }
}
