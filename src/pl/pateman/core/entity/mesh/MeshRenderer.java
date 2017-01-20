package pl.pateman.core.entity.mesh;

import org.joml.Matrix4f;
import pl.pateman.core.entity.mesh.animation.AnimationController;
import pl.pateman.core.shader.Program;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * Created by pateman.
 */
public final class MeshRenderer {
    private final MeshFilter meshFilter;
    private final Program shaderProgram;
    private final AnimationController animationController;
    private final List<Matrix4f> rendererBoneMatrices;

    public MeshRenderer(MeshFilter meshFilter, Program shaderProgram, AnimationController animationController) {
        this.meshFilter = meshFilter;
        this.shaderProgram = shaderProgram;
        this.animationController = animationController;

        //  Initialize the renderer's list of bone matrices. This list will be passed to the shader for skinning.
        final int numberOfBones = this.meshFilter.getMeshData().getSkeleton().getBones().size();
        this.rendererBoneMatrices = new ArrayList<>(numberOfBones);
        for (int i = 0; i < numberOfBones; i++) {
            this.rendererBoneMatrices.add(new Matrix4f());
        }
    }

    public void initializeRendering() {
        if (this.meshFilter == null || this.shaderProgram == null) {
            throw new IllegalStateException("Valid MeshFilter and shader program are required to bind for rendering");
        }

        this.shaderProgram.bind();
        this.meshFilter.bind();
    }

    public void renderMesh() {
        this.meshFilter.getEbo().bind();
        glDrawElements(GL_TRIANGLES, this.meshFilter.getMeshData().getTriangles().size(), GL_UNSIGNED_INT, 0);
        this.meshFilter.getEbo().unbind();
    }

    public void finalizeRendering() {
        if (this.meshFilter == null || this.shaderProgram == null) {
            throw new IllegalStateException("Valid MeshFilter and shader program are required to bind for rendering");
        }

        this.meshFilter.unbind();
        this.shaderProgram.unbind();
    }

    public List<Matrix4f> getBoneMatrices() {
        return this.animationController.getAnimationMatrices();
    }

    public List<Matrix4f> getRendererBoneMatrices() {
        return rendererBoneMatrices;
    }
}
