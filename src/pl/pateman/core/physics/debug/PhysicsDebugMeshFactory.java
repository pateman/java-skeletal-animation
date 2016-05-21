package pl.pateman.core.physics.debug;

import com.bulletphysics.collision.shapes.*;
import com.google.gson.reflect.TypeToken;
import org.joml.Vector3f;
import pl.pateman.core.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pateman.
 */
final class PhysicsDebugMeshFactory {
    private static final Class<? super List<Vector3f>> VERTICES_LIST_CLASS = new TypeToken<java.util.List<Vector3f>>() {
    }.getRawType();

    private static final javax.vecmath.Vector3f aabbMin = new javax.vecmath.Vector3f(-1e30f, -1e30f, -1e30f);
    private static final javax.vecmath.Vector3f aabbMax = new javax.vecmath.Vector3f(1e30f, 1e30f, 1e30f);

    private PhysicsDebugMeshFactory() {

    }

    static List<Vector3f> getMeshVertices(final CollisionShape collisionShape) {
        //  First of all, check if the given collision shape has its user pointer set. If so, we don't need to generate
        //  the mesh as it had been already generated.
        if (collisionShape.getUserPointer() != null &&
            collisionShape.getUserPointer().getClass().isAssignableFrom(VERTICES_LIST_CLASS)) {
            return (List<Vector3f>) collisionShape.getUserPointer();
        }

        //  Otherwise, check the type of the collision shape and generate the mesh.
        List<Vector3f> result = null;
        if (collisionShape instanceof ConcaveShape) {
            final ArrayListTriangleCallback callback = new ArrayListTriangleCallback();
            ((ConcaveShape) collisionShape).processAllTriangles(callback, aabbMin, aabbMax);

            result = callback.getTriangles();
        } else if (collisionShape instanceof ConvexShape) {
            //  Build the shape hull.
            final ShapeHull shapeHull = new ShapeHull((ConvexShape) collisionShape);
            shapeHull.buildHull(collisionShape.getMargin());

            //  If the hull is valid, process it and fetch triangles.
            final int numOfTriangles = shapeHull.numTriangles();
            if (numOfTriangles > 0) {
                result = new ArrayList<>(numOfTriangles);

                javax.vecmath.Vector3f tmp;
                int index = 0;
                for (int i = 0; i < numOfTriangles; i++) {
                    //  First vertex.
                    tmp = shapeHull.getVertexPointer().get(shapeHull.getIndexPointer().get(index++));
                    result.add(Utils.convert(new Vector3f(), tmp));

                    //  Second vertex.
                    tmp = shapeHull.getVertexPointer().get(shapeHull.getIndexPointer().get(index++));
                    result.add(Utils.convert(new Vector3f(), tmp));

                    //  Third vertex.
                    tmp = shapeHull.getVertexPointer().get(shapeHull.getIndexPointer().get(index++));
                    result.add(Utils.convert(new Vector3f(), tmp));
                }
            }
        }

        collisionShape.setUserPointer(result);
        return result;
    }

    private static class ArrayListTriangleCallback extends TriangleCallback {
        private final List<Vector3f> triangles;

        ArrayListTriangleCallback() {
            this.triangles = new ArrayList<>();
        }

        @Override
        public void processTriangle(javax.vecmath.Vector3f[] triangle, int partId, int triangleIndex) {
            this.triangles.add(Utils.convert(new Vector3f(), triangle[0]));
            this.triangles.add(Utils.convert(new Vector3f(), triangle[1]));
            this.triangles.add(Utils.convert(new Vector3f(), triangle[2]));
        }

        List<Vector3f> getTriangles() {
            return triangles;
        }
    }
}
