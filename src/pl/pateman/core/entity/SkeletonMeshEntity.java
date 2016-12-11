package pl.pateman.core.entity;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
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
    private final Skeleton skeleton;
    private final List<SkeletonJoint> joints;
    private Program meshProgram;
    private final Vector3f boneScaling;

    public SkeletonMeshEntity(Skeleton skeleton) {
        this.skeleton = skeleton;
        this.joints = new ArrayList<>(this.skeleton.getBones().size());
        this.boneScaling = new Vector3f(0.1f, 0.1f, 0.1f);
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
            jointMeshEntity.setTransformation(rotation, translation, this.boneScaling);

            final SkeletonJoint skeletonJoint = new SkeletonJoint(jointMeshEntity, bone);
            this.joints.add(skeletonJoint);
        }
    }

    public Skeleton getSkeleton() {
        return skeleton;
    }

    public Vector3f getBoneScaling() {
        return boneScaling;
    }

    public void setJointColor(final String boneName, final Vector4f newColor) {
        if (boneName == null || boneName.isEmpty() || newColor == null) {
            throw new IllegalArgumentException();
        }

        for (SkeletonJoint joint : this.joints) {
            if (joint.getRelatedBone().getName().equals(boneName)) {
                joint.getJointColor().set(newColor);
                return;
            }
        }
    }

    public void applyAnimation(final List<Matrix4f> animationMatrices) {
        final TempVars tempVars = TempVars.get();
        final Vector3f translation = tempVars.vect3d1;
        final Quaternionf rotation = tempVars.quat1;

        for (int i = 0; i < this.joints.size(); i++) {
            final CubeMeshEntity joint = this.joints.get(i).getCubeMeshEntity();

            this.getQuaternionAndTranslationFromMatrix(animationMatrices.get(i), rotation, translation);
            joint.setTransformation(rotation, translation, this.boneScaling);
        }

        tempVars.release();
    }

    public void drawSkeletonMesh(final CameraEntity camera) {
        final TempVars tempVars = TempVars.get();

        final Matrix4f worldTrans = tempVars.tempMat4x41;
        final Matrix4f modelViewMatrix = tempVars.tempMat4x42;

        for (SkeletonJoint joint : this.joints) {
            final MeshRenderer meshRenderer = joint.getCubeMeshEntity().getMeshRenderer();
            meshRenderer.initializeRendering();

            this.getTransformation().mul(joint.getCubeMeshEntity().getTransformation(), worldTrans);

            //  Prepare the model-view matrix.
            camera.getViewMatrix().mul(worldTrans, modelViewMatrix);

            this.meshProgram.setUniformMatrix4(Utils.MODELVIEW_UNIFORM, Utils.matrix4fToBuffer(modelViewMatrix));
            this.meshProgram.setUniformMatrix4(Utils.PROJECTION_UNIFORM, Utils.matrix4fToBuffer(camera.
                    getProjectionMatrix()));
            this.meshProgram.setUniform3(Utils.CAMERADIRECTION_UNIFORM, camera.getDirection().x,
                    camera.getDirection().y, camera.getDirection().z);
            this.meshProgram.setUniform4(Utils.DIFFUSECOLOR_UNIFORM, joint.getJointColor().x, joint.getJointColor().y,
                    joint.getJointColor().z, joint.getJointColor().w);
            this.meshProgram.setUniform1(Utils.USESKINNING_UNIFORM, 0);
            this.meshProgram.setUniform1(Utils.USETEXTURING_UNIFORM, 0);
            this.meshProgram.setUniform1(Utils.USELIGHTING_UNIFORM, 1);

            meshRenderer.renderMesh();
            meshRenderer.finalizeRendering();
        }

        tempVars.release();
    }

    private class SkeletonJoint {
        private final CubeMeshEntity cubeMeshEntity;
        private final Bone relatedBone;
        private final Vector4f jointColor;

        SkeletonJoint(CubeMeshEntity cubeMeshEntity, Bone relatedBone) {
            this.cubeMeshEntity = cubeMeshEntity;
            this.relatedBone = relatedBone;
            this.jointColor = new Vector4f(0.8f, 0.8f, 0.8f, 1.0f);
        }

        CubeMeshEntity getCubeMeshEntity() {
            return cubeMeshEntity;
        }

        Bone getRelatedBone() {
            return relatedBone;
        }

        Vector4f getJointColor() {
            return jointColor;
        }
    }
}
