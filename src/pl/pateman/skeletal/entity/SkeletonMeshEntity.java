package pl.pateman.skeletal.entity;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20;
import pl.pateman.skeletal.TempVars;
import pl.pateman.skeletal.Utils;
import pl.pateman.skeletal.entity.mesh.MeshRenderer;
import pl.pateman.skeletal.mesh.Bone;
import pl.pateman.skeletal.mesh.Mesh;
import pl.pateman.skeletal.mesh.Skeleton;
import pl.pateman.skeletal.shader.Program;
import pl.pateman.skeletal.shader.Shader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by pateman.
 */
public class SkeletonMeshEntity extends AbstractEntity {
    public static final Vector3f BONE_SCALING = new Vector3f(0.1f, 0.1f, 0.1f);

    private final Skeleton skeleton;
    private final List<CubeMeshEntity> joints;
    private Program meshProgram;

    public SkeletonMeshEntity(Skeleton skeleton) {
        this.skeleton = skeleton;
        this.joints = new ArrayList<>(this.skeleton.getBones().size());
        this.buildJointMeshes();
    }

    private void getQuaternionAndTranslationFromMatrix(final Matrix4f matrix4f, final Quaternionf quaternionf,
                                                       final Vector3f vector3f) {
        matrix4f.getTranslation(vector3f);
        quaternionf.setFromUnnormalized(matrix4f);
    }

    private void buildJointMeshes() {
        //  Start with creating the shaders.
        final Shader vertexShader = new Shader(GL20.GL_VERTEX_SHADER);
        final Shader fragmentShader = new Shader(GL20.GL_FRAGMENT_SHADER);

        try {
            //  Load shaders.
            vertexShader.load("helloworld.vert");
            fragmentShader.load("helloworld.frag");

            if (!vertexShader.compile()) {
                throw new IllegalStateException(vertexShader.getInfoLog());
            }
            if (!fragmentShader.compile()) {
                throw new IllegalStateException(vertexShader.getInfoLog());
            }

            //  Create the program.
            this.meshProgram = new Program();
            this.meshProgram.attachShader(vertexShader);
            this.meshProgram.attachShader(fragmentShader);
            if (!this.meshProgram.link(false)) {
                throw new IllegalStateException(this.meshProgram.getInfoLog());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final Vector3f translation = new Vector3f();
        final Quaternionf rotation = new Quaternionf();
        for (Bone bone : this.skeleton.getBones()) {
            final CubeMeshEntity jointMeshEntity = new CubeMeshEntity();
            jointMeshEntity.setShaderProgram(this.meshProgram);
            jointMeshEntity.buildMesh();

            this.getQuaternionAndTranslationFromMatrix(bone.getWorldBindMatrix(), rotation, translation);
            jointMeshEntity.setTransformation(rotation, translation, BONE_SCALING);

            this.joints.add(jointMeshEntity);
        }
    }

    public Skeleton getSkeleton() {
        return skeleton;
    }

    public void applyAnimation(final List<Matrix4f> animationMatrices) {
        final Vector3f translation = new Vector3f();
        final Quaternionf rotation = new Quaternionf();

        for (int i = 0; i < this.joints.size(); i++) {
            final CubeMeshEntity joint = this.joints.get(i);

            this.getQuaternionAndTranslationFromMatrix(animationMatrices.get(i), rotation, translation);
            joint.setTransformation(rotation, translation, BONE_SCALING);
        }
    }

    public void drawSkeletonMesh(final Matrix4f viewMatrix, final Matrix4f projectionMatrix) {
        final TempVars tempVars = TempVars.get();

        final Matrix4f worldTrans = tempVars.tempMat4x41;
        final Matrix4f modelViewMatrix = tempVars.tempMat4x42;

        for (CubeMeshEntity joint : this.joints) {
            final MeshRenderer meshRenderer = joint.getMeshRenderer();
            meshRenderer.initializeRendering();

            this.getTransformation().mul(joint.getTransformation(), worldTrans);

            //  Prepare the model-view matrix.
            viewMatrix.mul(worldTrans, modelViewMatrix);

            this.meshProgram.setUniformMatrix4(Utils.MODELVIEW_UNIFORM, Utils.matrix4fToBuffer(modelViewMatrix));
            this.meshProgram.setUniformMatrix4(Utils.PROJECTION_UNIFORM, Utils.matrix4fToBuffer(projectionMatrix));
            this.meshProgram.setUniform1(Utils.USESKINNING_UNIFORM, 0);
            this.meshProgram.setUniform1(Utils.USETEXTURING_UNIFORM, 0);

            meshRenderer.renderMesh();
            meshRenderer.finalizeRendering();
        }

        tempVars.release();
    }

    private class CubeMeshEntity extends MeshEntity {
        @Override
        public void buildMesh() {
            final Mesh mesh = new Mesh();
            mesh.getVertices().addAll(Utils.arrayToVector3fList(
                    -0.05f, -0.05f, -0.05f,
                    0.05f, -0.05f, -0.05f,
                    0.05f, 0.05f, -0.05f,
                    -0.05f, 0.05f, -0.05f,

                    0.05f, -0.05f, -0.05f,
                    0.05f,   -0.05f,  0.05f,
                    0.05f,   0.05f,   0.05f,
                    0.05f,   0.05f,   -0.05f,

                    0.05f,   -0.05f,  0.05f,
                    -0.05f,  -0.05f,  0.05f,
                    -0.05f,  0.05f,   0.05f,
                    0.05f,   0.05f,   0.05f,

                    -0.05f,  -0.05f,  0.05f,
                    -0.05f,  -0.05f,  -0.05f,
                    -0.05f,  0.05f,   -0.05f,
                    -0.05f,  0.05f,   0.05f,

                    0.05f,   0.05f,   -0.05f,
                    0.05f,   0.05f,   0.05f,
                    -0.05f,  0.05f,   0.05f,
                    -0.05f,  0.05f,   -0.05f,

                    -0.05f,  -0.05f,  -0.05f,
                    -0.05f,  -0.05f,  0.05f,
                    0.05f,   -0.05f,  0.05f,
                    0.05f,   -0.05f,  -0.05f
            ));
            mesh.getTriangles().addAll(Arrays.asList(2, 1, 0, 3, 2, 0));
            mesh.getTriangles().addAll(Arrays.asList(6, 5, 4, 7, 6, 4));
            mesh.getTriangles().addAll(Arrays.asList(10, 9, 8, 11, 10, 8));
            mesh.getTriangles().addAll(Arrays.asList(14, 13, 12, 15, 14, 12));
            mesh.getTriangles().addAll(Arrays.asList(18, 17, 16, 19, 18, 16));
            mesh.getTriangles().addAll(Arrays.asList(22, 21, 20, 23, 22, 20));

            mesh.getNormals().addAll(Utils.arrayToVector3fList(
                    0.0f, 0.0f, -1.0f,
                    0.0f, 0.0f, -1.0f,
                    0.0f, 0.0f, -1.0f,
                    0.0f, 0.0f, -1.0f,

                    1.0f, 0.0f, 0.0f,
                    1.0f, 0.0f, 0.0f,
                    1.0f, 0.0f, 0.0f,
                    1.0f, 0.0f, 0.0f,

                    0.0f, 0.0f, 1.0f,
                    0.0f, 0.0f, 1.0f,
                    0.0f, 0.0f, 1.0f,
                    0.0f, 0.0f, 1.0f,

                    -1.0f, 0.0f, 0.0f,
                    -1.0f, 0.0f, 0.0f,
                    -1.0f, 0.0f, 0.0f,
                    -1.0f, 0.0f, 0.0f,

                    0.0f, 1.0f, 0.0f,
                    0.0f, 1.0f, 0.0f,
                    0.0f, 1.0f, 0.0f,
                    0.0f, 1.0f, 0.0f,

                    0.0f, -1.0f, 0.0f,
                    0.0f, -1.0f, 0.0f,
                    0.0f, -1.0f, 0.0f,
                    0.0f, -1.0f, 0.0f
            ));

            this.setMesh(mesh);
            super.buildMesh();
        }
    }
}
