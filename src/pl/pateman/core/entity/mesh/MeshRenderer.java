package pl.pateman.core.entity.mesh;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL15;
import pl.pateman.core.entity.mesh.animation.AnimationController;
import pl.pateman.core.shader.Program;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.glBindBuffer;

/**
 * Created by pateman.
 */
public final class MeshRenderer {
    private final MeshFilter meshFilter;
    private final Program shaderProgram;
    private final AnimationController animationController;

    public MeshRenderer(MeshFilter meshFilter, Program shaderProgram, AnimationController animationController) {
        this.meshFilter = meshFilter;
        this.shaderProgram = shaderProgram;
        this.animationController = animationController;
    }

    public void initializeRendering() {
        if (this.meshFilter == null || this.shaderProgram == null) {
            throw new IllegalStateException("Valid MeshFilter and shader program are required to bind for rendering");
        }

        this.shaderProgram.bind();
        this.meshFilter.bind();
    }

    public void renderMesh() {
        for (MeshFilter.MeshFace face : this.meshFilter.getFaces()) {
            glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, face.getHandle());
            glDrawElements(GL_TRIANGLES, face.getIndices().size(), GL_UNSIGNED_INT, 0);
            glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
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
}
