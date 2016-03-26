package pl.pateman.skeletal.ogrexml;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.XppDriver;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import pl.pateman.skeletal.TempVars;
import pl.pateman.skeletal.Utils;
import pl.pateman.skeletal.mesh.*;

import java.io.IOException;

/**
 * Created by pateman on 2016-03-17.
 */
public final class OgreXMLImporter {
    private XStream xStream;

    public OgreXMLImporter() {
        this.xStream = new XStream(new XppDriver(new NoNameCoder()));
        this.xStream.ignoreUnknownElements();
        this.xStream.processAnnotations(OgreXMLMesh.class);
        this.xStream.processAnnotations(OgreXMLSkeleton.class);
    }

    private Quaternionf quaternionFromRotation(final OgreXMLSkeleton.Rotation rotation) {
        return new Quaternionf().set(new AxisAngle4f(rotation.angle, rotation.axis.x, rotation.axis.y, rotation.axis.z).
                normalize());
    }

    public Mesh load(final String meshFileResource) throws IOException {
        final Mesh mesh = new Mesh();

        //  Load the mesh first.
        final String meshFileContent = Utils.readResource(meshFileResource);
        final OgreXMLMesh ogreXMLMesh = (OgreXMLMesh) this.xStream.fromXML(meshFileContent);

        final OgreXMLSubmesh submesh = ogreXMLMesh.submeshes.isEmpty() ? null : ogreXMLMesh.submeshes.get(0);
        if (submesh == null) {
            throw new IOException("No submeshes in the file");
        }

        //  Iterate over the available vertex buffers and assign the correct values.
        for (OgreXMLSubmesh.VertexBuffer vertexBuffer : submesh.vertexBuffers) {
            //  Determine what kind of vertex buffer it is.
            for (OgreXMLSubmesh.Vertex vertex : vertexBuffer.vertexes) {
                if (vertexBuffer.positions) {
                    mesh.getVertices().add(new Vector3f(vertex.position.x, vertex.position.y, vertex.position.z));
                } else if (vertexBuffer.normals) {
                    mesh.getNormals().add(new Vector3f(vertex.normal.x, vertex.normal.y, vertex.normal.z));
                } else if (vertexBuffer.textureCoords) {
                    mesh.getTexcoords().add(new Vector2f(vertex.texcoord.u, vertex.texcoord.v));
                }
            }
        }

        //  Assign triangles.
        for (OgreXMLSubmesh.Face face : submesh.faces) {
            mesh.getTriangles().add(face.v1);
            mesh.getTriangles().add(face.v2);
            mesh.getTriangles().add(face.v3);
        }

        //  If there's a skeleton file linked in the mesh, load it as well.
        if (ogreXMLMesh.skeletonResource != null) {
            final String skeletonFileContent = Utils.readResource(ogreXMLMesh.skeletonResource + ".xml");
            final OgreXMLSkeleton skeleton = (OgreXMLSkeleton) this.xStream.fromXML(skeletonFileContent);

            //  Add bones.
            for (OgreXMLSkeleton.Bone bone : skeleton.boneList) {
                final Bone skeletonBone = new Bone(bone.name, bone.id);
                if (bone.position != null) {
                    skeletonBone.getBindPosition().set(bone.position.x, bone.position.y, bone.position.z);
                }
                if (bone.rotation != null) {
                    skeletonBone.getBindRotation().set(this.quaternionFromRotation(bone.rotation));
                }
                if (bone.scale != null) {
                    skeletonBone.getBindScale().set(bone.scale.x, bone.scale.y, bone.scale.z);
                }

                //  For each bone, add vertex assignments.
                for (OgreXMLSubmesh.VertexBoneAssignment vertexBoneAssignment : submesh.boneAssignments) {
                    if (vertexBoneAssignment.bone != skeletonBone.getIndex()) {
                        continue;
                    }

                    skeletonBone.addVertexWeight(vertexBoneAssignment.vertex, vertexBoneAssignment.weight);
                }

                //  Add the bone to the skeleton.
                mesh.getSkeleton().getBones().add(skeletonBone);
            }

            //  Build the hierarchy. Check if there's only one root.
            if (skeleton.boneList.size() - skeleton.hierarchyInfo.size() > 1) {
                throw new IOException("Multiple roots not supported");
            }

            //  Check if the number of bones is acceptable.
            if (skeleton.boneList.size() > MeshSkinningInfo.MAX_BONES) {
                throw new IOException("Too many bones in the skeleton");
            }

            for (OgreXMLSkeleton.BoneHierarchyInfo boneHierarchyInfo : skeleton.hierarchyInfo) {
                final Bone bone = mesh.getSkeleton().getBoneByName(boneHierarchyInfo.boneName);
                final Bone parent = mesh.getSkeleton().getBoneByName(boneHierarchyInfo.parentBoneName);

                if (bone == null) {
                    throw new IOException("Missing bone " + boneHierarchyInfo.boneName);
                }
                if (parent == null) {
                    throw new IOException("Missing parent bone " + boneHierarchyInfo.parentBoneName);
                }

                if (bone.getParent() != null) {
                    throw new IOException("Bone " + bone.getName() + " has multiple parents");
                }
                bone.setParent(parent);
                parent.getChildren().add(bone);
            }

            //  Process animations.
            for (OgreXMLSkeleton.Animation animation : skeleton.animations) {
                final Animation anim = new Animation(animation.name, animation.length);

                //  Create animation tracks.
                for (OgreXMLSkeleton.Track track : animation.tracks) {
                    final Bone trackBone = mesh.getSkeleton().getBoneByName(track.boneName);
                    if (trackBone == null) {
                        throw new IOException("Missing animation bone " + track.boneName);
                    }

                    //  Load keyframes for the animation track.
                    final AnimationTrack animationTrack = new AnimationTrack(trackBone);
                    for (OgreXMLSkeleton.Keyframe keyframe : track.keyframes) {
                        final AnimationKeyframe animationKeyframe = new AnimationKeyframe(keyframe.time,
                                new Vector3f(keyframe.translation.x, keyframe.translation.y, keyframe.translation.z),
                                this.quaternionFromRotation(keyframe.rotation));
                        animationTrack.getKeyframes().add(animationKeyframe);
                    }

                    anim.setFrameCount(Math.max(anim.getFrameCount(), animationTrack.getKeyframes().size()));
                    anim.getTracks().add(animationTrack);
                }

                mesh.getAnimations().add(anim);
            }

            //  Now that the skeleton is fully processed, calculate the bind matrices and arrange the bones.
            mesh.getSkeleton().calculateBindMatrices();
            mesh.getSkeleton().arrangeBones();

            //  Create the pallete skinning buffer for passing animation matrices to the shader.
            TempVars.initializeStorageForSkinning(mesh.getSkeleton().getBones().size());
        }

        return mesh;
    }
}
