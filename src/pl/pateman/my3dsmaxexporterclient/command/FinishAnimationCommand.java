package pl.pateman.my3dsmaxexporterclient.command;

import pl.pateman.my3dsmaxexporterclient.ClientCommand;
import pl.pateman.my3dsmaxexporterclient.CommandContext;
import pl.pateman.core.mesh.Animation;

import static pl.pateman.my3dsmaxexporterclient.Constants.CURRENT_ANIMATION;

/**
 * Created by pateman.
 */
public class FinishAnimationCommand implements ClientCommand {
    @Override
    public void execute(CommandContext context) throws Exception {
        final Animation animation = (Animation) context.stateVariables.remove(CURRENT_ANIMATION);
        context.mesh.getAnimations().add(animation);
    }
}
