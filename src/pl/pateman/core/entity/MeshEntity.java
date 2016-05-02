package pl.pateman.core.entity;

import pl.pateman.core.entity.mesh.animation.AnimationController;
import pl.pateman.core.entity.mesh.MeshFilter;
import pl.pateman.core.entity.mesh.MeshRenderer;
import pl.pateman.core.mesh.Mesh;
import pl.pateman.core.shader.Program;

/**
 * Created by pateman.
 */
public class MeshEntity extends AbstractEntity {
    private Mesh mesh;
    private Program shaderProgram;
    private final MeshFilter meshFilter;
    private MeshRenderer meshRenderer;
    private AnimationController animationController;

    public MeshEntity() {
        this(null);
    }

    public MeshEntity(final String name) {
        super(name);
        this.meshFilter = new MeshFilter();
    }

    public Mesh getMesh() {
        return mesh;
    }

    public void setMesh(Mesh mesh) {
        this.mesh = mesh;
    }

    public Program getShaderProgram() {
        return shaderProgram;
    }

    public void setShaderProgram(Program shaderProgram) {
        this.shaderProgram = shaderProgram;
    }

    public void buildMesh() {
        this.meshFilter.setMeshData(this.mesh);
        this.meshFilter.setShaderProgram(this.shaderProgram);
        this.meshFilter.buildMeshFilter();

        this.animationController = new AnimationController(this.mesh);
        this.meshRenderer = new MeshRenderer(this.meshFilter, this.shaderProgram, this.animationController);
    }

    public MeshRenderer getMeshRenderer() {
        return meshRenderer;
    }

    public AnimationController getAnimationController() {
        return animationController;
    }

    @Override
    public void clearAndDestroy() {
        this.meshFilter.clearAndDestroy();
    }
}
