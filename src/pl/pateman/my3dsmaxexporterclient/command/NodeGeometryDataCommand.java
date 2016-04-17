package pl.pateman.my3dsmaxexporterclient.command;

import org.joml.Vector2f;
import org.joml.Vector3f;
import pl.pateman.my3dsmaxexporterclient.ClientCommand;
import pl.pateman.my3dsmaxexporterclient.CommandContext;
import pl.pateman.my3dsmaxexporterclient.ExporterUtils;
import pl.pateman.skeletal.mesh.Bone;

import static pl.pateman.my3dsmaxexporterclient.Constants.*;

/**
 * Created by pateman.
 */
public final class NodeGeometryDataCommand implements ClientCommand {

    @Override
    public void execute(final CommandContext context) throws Exception {
        final String cmd = context.commandParameters[0];

        switch (cmd) {
            case VERTEX:
                final Vector3f vertex = new Vector3f();
                vertex.x = Float.parseFloat(context.commandParameters[1]);
                vertex.y = Float.parseFloat(context.commandParameters[2]);
                vertex.z = Float.parseFloat(context.commandParameters[3]);

                context.mesh.getVertices().add(vertex);
                break;
            case NORMAL:
                final Vector3f normal = new Vector3f();
                normal.x = Float.parseFloat(context.commandParameters[1]);
                normal.y = Float.parseFloat(context.commandParameters[2]);
                normal.z = Float.parseFloat(context.commandParameters[3]);

                context.mesh.getNormals().add(normal);
                break;
            case FACE:
                context.mesh.getTriangles().add(Integer.parseInt(context.commandParameters[1]));
                context.mesh.getTriangles().add(Integer.parseInt(context.commandParameters[2]));
                context.mesh.getTriangles().add(Integer.parseInt(context.commandParameters[3]));
                break;
            case TEXCOORD:
                final Vector2f texcoord = new Vector2f();
                texcoord.x = Float.parseFloat(context.commandParameters[1]);
                texcoord.y = Float.parseFloat(context.commandParameters[2]);

                context.mesh.getTexcoords().add(texcoord);
                break;
            case BONE:
                final Bone bone = new Bone(ExporterUtils.decodeString(context.commandParameters[1]),
                        Integer.parseInt(context.commandParameters[2]));
                final int parentIndex = Integer.parseInt(context.commandParameters[3]);

                if (parentIndex != -1) {
                    bone.setParent(context.mesh.getSkeleton().getBoneByIndex(parentIndex));
                }

                context.mesh.getSkeleton().getBones().add(bone);
                break;
        }
    }
}
