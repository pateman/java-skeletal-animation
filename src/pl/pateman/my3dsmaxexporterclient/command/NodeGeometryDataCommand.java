package pl.pateman.my3dsmaxexporterclient.command;

import org.joml.Vector2f;
import org.joml.Vector3f;
import pl.pateman.my3dsmaxexporterclient.ClientCommand;
import pl.pateman.my3dsmaxexporterclient.CommandContext;
import pl.pateman.my3dsmaxexporterclient.ExporterUtils;
import pl.pateman.core.mesh.Bone;

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

                bone.getBindPosition().x = Float.parseFloat(context.commandParameters[4]);
                bone.getBindPosition().y = Float.parseFloat(context.commandParameters[5]);
                bone.getBindPosition().z = Float.parseFloat(context.commandParameters[6]);
                bone.getBindRotation().x = Float.parseFloat(context.commandParameters[7]);
                bone.getBindRotation().y = Float.parseFloat(context.commandParameters[8]);
                bone.getBindRotation().z = Float.parseFloat(context.commandParameters[9]);
                bone.getBindRotation().w = Float.parseFloat(context.commandParameters[10]);
                bone.getBindScale().x = Float.parseFloat(context.commandParameters[11]);
                bone.getBindScale().y = Float.parseFloat(context.commandParameters[12]);
                bone.getBindScale().z = Float.parseFloat(context.commandParameters[13]);

                context.mesh.getSkeleton().getBones().add(bone);
                break;
            case SKIN:
                final int vertexIndex = Integer.parseInt(context.commandParameters[1]);
                final int bone0 = Integer.parseInt(context.commandParameters[2]);
                final float weight0 = Float.parseFloat(context.commandParameters[3]);
                final int bone1 = Integer.parseInt(context.commandParameters[4]);
                final float weight1 = Float.parseFloat(context.commandParameters[5]);
                final int bone2 = Integer.parseInt(context.commandParameters[6]);
                final float weight2 = Float.parseFloat(context.commandParameters[7]);
                final int bone3 = Integer.parseInt(context.commandParameters[8]);
                final float weight3 = Float.parseFloat(context.commandParameters[9]);

                Bone skeletonBone;
                if (bone0 != -1) {
                    skeletonBone = context.mesh.getSkeleton().getBoneByIndex(bone0);
                    skeletonBone.addVertexWeight(vertexIndex, weight0);
                }
                if (bone1 != -1) {
                    skeletonBone = context.mesh.getSkeleton().getBoneByIndex(bone1);
                    skeletonBone.addVertexWeight(vertexIndex, weight1);
                }
                if (bone2 != -1) {
                    skeletonBone = context.mesh.getSkeleton().getBoneByIndex(bone2);
                    skeletonBone.addVertexWeight(vertexIndex, weight2);
                }
                if (bone3 != -1) {
                    skeletonBone = context.mesh.getSkeleton().getBoneByIndex(bone3);
                    skeletonBone.addVertexWeight(vertexIndex, weight3);
                }

                break;
        }
    }
}
