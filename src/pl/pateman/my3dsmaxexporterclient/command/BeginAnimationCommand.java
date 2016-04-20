package pl.pateman.my3dsmaxexporterclient.command;

import pl.pateman.my3dsmaxexporterclient.ClientCommand;
import pl.pateman.my3dsmaxexporterclient.CommandContext;
import pl.pateman.my3dsmaxexporterclient.Constants;
import pl.pateman.my3dsmaxexporterclient.ExporterUtils;
import pl.pateman.skeletal.mesh.Animation;

/**
 * Created by pateman.
 */
public class BeginAnimationCommand implements ClientCommand {

    @Override
    public void execute(CommandContext context) throws Exception {
        final Animation animation = new Animation(ExporterUtils.decodeString(context.commandParameters[1]),
                Float.parseFloat(context.commandParameters[4]));
        context.stateVariables.put(Constants.CURRENT_ANIMATION, animation);
    }
}
