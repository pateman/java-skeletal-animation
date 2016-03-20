package pl.pateman.skeletal.entity;

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

        this.meshRenderer = new MeshRenderer(this.meshFilter, this.shaderProgram);
    }

    public MeshRenderer getMeshRenderer() {
        return meshRenderer;
    }

    @Override
    public void clearAndDestroy() {
        this.meshFilter.clearAndDestroy();
    }
}
