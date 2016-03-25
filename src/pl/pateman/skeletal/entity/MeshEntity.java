package pl.pateman.skeletal.entity;

import pl.pateman.skeletal.entity.mesh.AnimationController;
import pl.pateman.skeletal.entity.mesh.MeshFilter;
import pl.pateman.skeletal.entity.mesh.MeshRenderer;
import pl.pateman.skeletal.mesh.Mesh;
import pl.pateman.skeletal.shader.Program;

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
