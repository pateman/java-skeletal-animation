package pl.pateman.core.entity;

import org.joml.Vector3f;
import pl.pateman.core.mesh.Mesh;

import static pl.pateman.core.Utils.HALF_PI;
import static pl.pateman.core.Utils.TWO_PI;

/**
 * Created by pateman.
 */
public class SphereMeshEntity extends MeshEntity {
    private float radius;
    private int rings;
    private int sectors;

    public SphereMeshEntity() {
        this(null, 1.0f, 16, 16);
    }

    public SphereMeshEntity(final String name, float radius, int rings, int sectors) {
        super(name);

        this.radius = radius;
        this.rings = Math.max(2, rings);
        this.sectors = Math.max(2, sectors);
    }

    public float getRadius() {
        return radius;
    }

    public int getRings() {
        return rings;
    }

    public int getSectors() {
        return sectors;
    }

    @Override
    public void buildMesh() {
        final Mesh mesh = new Mesh();

        final float R = 1.0f / (float) (this.rings - 1);
        final float S = 1.0f / (float) (this.sectors - 1);

        int r, s, curRow, nextRow;
        float PIrR, sinPIrR;
        for (r = 0; r < this.rings; r++) {
            PIrR = (float) Math.PI * r * R;
            sinPIrR = (float) Math.sin(PIrR);
            curRow = r * this.sectors;
            nextRow = (r + 1) * this.sectors;

            for (s = 0; s < this.sectors; s++) {
                float x = (float) Math.cos(TWO_PI * s * S) * sinPIrR;
                float y = (float) Math.sin(-HALF_PI + PIrR);
                float z = (float) Math.sin(TWO_PI * s * S) * sinPIrR;

                mesh.getVertices().add(new Vector3f(x * this.radius, y * this.radius, z * this.radius));
                mesh.getNormals().add(new Vector3f(x, y, z));

                mesh.getTriangles().add(curRow + s);
                mesh.getTriangles().add(nextRow + s);
                mesh.getTriangles().add(nextRow + (s + 1));

                mesh.getTriangles().add(curRow + s);
                mesh.getTriangles().add(nextRow + (s + 1));
                mesh.getTriangles().add(curRow + (s + 1));
            }
        }

        this.setMesh(mesh);
        super.buildMesh();
    }
}
