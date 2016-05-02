package pl.pateman.core.entity;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20;
import pl.pateman.core.TempVars;
import pl.pateman.core.Utils;
import pl.pateman.core.entity.mesh.MeshRenderer;
import pl.pateman.core.mesh.Bone;
import pl.pateman.core.mesh.Skeleton;
import pl.pateman.core.shader.Program;
import pl.pateman.core.shader.Shader;

import java.io.IOException;
import java.util.ArrayList;
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
            final CubeMeshEntity jointMeshEntity = new CubeMeshEntity(bone.getName(), 0.05f);
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
        final TempVars tempVars = TempVars.get();
        final Vector3f translation = tempVars.vect3d1;
        final Quaternionf rotation = tempVars.quat1;

        for (int i = 0; i < this.joints.size(); i++) {
            final CubeMeshEntity joint = this.joints.get(i);

            this.getQuaternionAndTranslationFromMatrix(animationMatrices.get(i), rotation, translation);
            joint.setTransformation(rotation, translation, BONE_SCALING);
        }

        tempVars.release();
    }

    public void drawSkeletonMesh(final CameraEntity camera) {
        final TempVars tempVars = TempVars.get();

        final Matrix4f worldTrans = tempVars.tempMat4x41;
        final Matrix4f modelViewMatrix = tempVars.tempMat4x42;

        for (CubeMeshEntity joint : this.joints) {
            final MeshRenderer meshRenderer = joint.getMeshRenderer();
            meshRenderer.initializeRendering();

            this.getTransformation().mul(joint.getTransformation(), worldTrans);

            //  Prepare the model-view matrix.
            camera.getViewMatrix().mul(worldTrans, modelViewMatrix);

            this.meshProgram.setUniformMatrix4(Utils.MODELVIEW_UNIFORM, Utils.matrix4fToBuffer(modelViewMatrix));
            this.meshProgram.setUniformMatrix4(Utils.PROJECTION_UNIFORM, Utils.matrix4fToBuffer(camera.
                    getProjectionMatrix()));
            this.meshProgram.setUniform3(Utils.CAMERADIRECTION_UNIFORM, camera.getDirection().x,
                    camera.getDirection().y, camera.getDirection().z);
            this.meshProgram.setUniform4(Utils.DIFFUSECOLOR_UNIFORM, 0.8f, 0.8f, 0.8f, 1.0f);
            this.meshProgram.setUniform1(Utils.USESKINNING_UNIFORM, 0);
            this.meshProgram.setUniform1(Utils.USETEXTURING_UNIFORM, 0);
            this.meshProgram.setUniform1(Utils.USELIGHTING_UNIFORM, 1);

            meshRenderer.renderMesh();
            meshRenderer.finalizeRendering();
        }

        tempVars.release();
    }

}
