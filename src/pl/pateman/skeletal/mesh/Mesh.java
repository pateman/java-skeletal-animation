package pl.pateman.skeletal.mesh;

import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pateman.
 */
public class Mesh {
    private final List<Vector3f> vertices;
    private final List<Vector3f> normals;
    private final List<Vector2f> texcoords;
    private final List<Integer> triangles;
    private final Skeleton skeleton;

    public Mesh() {
        this.vertices = new ArrayList<>();
        this.normals = new ArrayList<>();
        this.texcoords = new ArrayList<>();
        this.triangles = new ArrayList<>();
        this.skeleton = new Skeleton();
    }

    public List<Vector3f> getVertices() {
        return vertices;
    }

    public List<Vector3f> getNormals() {
        return normals;
    }

    public List<Vector2f> getTexcoords() {
        return texcoords;
    }

    public List<Integer> getTriangles() {
        return triangles;
    }

    public Skeleton getSkeleton() {
        return skeleton;
    }
}
