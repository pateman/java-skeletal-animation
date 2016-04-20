package pl.pateman.my3dsmaxexporterclient.command;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.pateman.my3dsmaxexporterclient.ClientCommand;
import pl.pateman.my3dsmaxexporterclient.CommandContext;
import pl.pateman.my3dsmaxexporterclient.Constants;
import pl.pateman.skeletal.mesh.Animation;
import pl.pateman.skeletal.mesh.AnimationKeyframe;
import pl.pateman.skeletal.mesh.AnimationTrack;
import pl.pateman.skeletal.mesh.Bone;

import static pl.pateman.my3dsmaxexporterclient.Constants.CURRENT_ANIMATION;
import static pl.pateman.my3dsmaxexporterclient.Constants.CURRENT_ANIMATION_TRACK;

/**
 * Created by pateman.
 */
public class AnimationDataCommand implements ClientCommand {

    @Override
    public void execute(CommandContext context) throws Exception {
        switch (context.commandParameters[0]) {
            case Constants.BEGIN_TRACK:
                final Bone keyframe = context.mesh.getSkeleton().getBoneByIndex(Integer.parseInt(
                        context.commandParameters[1]));
                context.stateVariables.put(CURRENT_ANIMATION_TRACK, new AnimationTrack(keyframe));
                break;
            case Constants.FINISH_TRACK:
                final AnimationTrack track = (AnimationTrack) context.stateVariables.remove(CURRENT_ANIMATION_TRACK);
                ((Animation) context.stateVariables.get(CURRENT_ANIMATION)).getTracks().add(track);
                break;
            case Constants.KEYFRAME:
                final float time = Float.parseFloat(context.commandParameters[1]);
                final AnimationKeyframe animationKeyframe = new AnimationKeyframe(time, new Vector3f(),
                        new Quaternionf());

                animationKeyframe.getTranslation().x = Float.parseFloat(context.commandParameters[4]);
                animationKeyframe.getTranslation().y = Float.parseFloat(context.commandParameters[5]);
                animationKeyframe.getTranslation().z = Float.parseFloat(context.commandParameters[6]);
                animationKeyframe.getRotation().x = Float.parseFloat(context.commandParameters[7]);
                animationKeyframe.getRotation().y = Float.parseFloat(context.commandParameters[8]);
                animationKeyframe.getRotation().z = Float.parseFloat(context.commandParameters[9]);
                animationKeyframe.getRotation().w = Float.parseFloat(context.commandParameters[10]);

                final AnimationTrack animTrack = (AnimationTrack) context.stateVariables.get(CURRENT_ANIMATION_TRACK);
                animTrack.getKeyframes().add(animationKeyframe);

                break;
        }
    }
}
