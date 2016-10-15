package pl.pateman.core.entity;

import pl.pateman.core.Utils;
import pl.pateman.core.mesh.Mesh;

import java.util.Arrays;

/**
 * Created by pateman.
 */
public class CubeMeshEntity extends MeshEntity {
    private float cubeSize;

    public CubeMeshEntity() {
        this(null, 1.0f);
    }

    public CubeMeshEntity(final String name, float cubeSize) {
        super(name);
        this.cubeSize = cubeSize;
    }

    public float getCubeSize() {
        return cubeSize;
    }

    @Override
    public void buildMesh() {
        final Mesh mesh = new Mesh();
        mesh.getVertices().addAll(Utils.arrayToVector3fList(
                -cubeSize, -cubeSize, -cubeSize,
                cubeSize, -cubeSize, -cubeSize,
                cubeSize, cubeSize, -cubeSize,
                -cubeSize, cubeSize, -cubeSize,

                cubeSize, -cubeSize, -cubeSize,
                cubeSize, -cubeSize, cubeSize,
                cubeSize, cubeSize, cubeSize,
                cubeSize, cubeSize, -cubeSize,

                cubeSize, -cubeSize, cubeSize,
                -cubeSize, -cubeSize, cubeSize,
                -cubeSize, cubeSize, cubeSize,
                cubeSize, cubeSize, cubeSize,

                -cubeSize, -cubeSize, cubeSize,
                -cubeSize, -cubeSize, -cubeSize,
                -cubeSize, cubeSize, -cubeSize,
                -cubeSize, cubeSize, cubeSize,

                cubeSize, cubeSize, -cubeSize,
                cubeSize, cubeSize, cubeSize,
                -cubeSize, cubeSize, cubeSize,
                -cubeSize, cubeSize, -cubeSize,

                -cubeSize, -cubeSize, -cubeSize,
                -cubeSize, -cubeSize, cubeSize,
                cubeSize, -cubeSize, cubeSize,
                cubeSize, -cubeSize, -cubeSize
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
