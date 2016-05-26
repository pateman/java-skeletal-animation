package pl.pateman.core.quickhull3d;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by pateman.
 */
public final class QuickHull {
    private final QuickHull3D quickHull3D;

    public QuickHull() {
        this.quickHull3D = new QuickHull3D();
    }

    public Collection<Vector3f> buildHull(final Collection<Vector3f> points) {
        if (points == null || points.isEmpty()) {
            throw new IllegalArgumentException("A valid list of points needs to be provided");
        }

        //  Convert between libraries.
        final Point3d[] arrayOfPoints = new Point3d[points.size()];
        int pointIndex = 0;
        for (Vector3f point : points) {
            arrayOfPoints[pointIndex++] = new Point3d(point.x, point.y, point.z);
        }

        //  Build the hull.
        this.quickHull3D.build(arrayOfPoints, arrayOfPoints.length);

        //  Convert back.
        final Point3d[] hullVertices = this.quickHull3D.getVertices();
        final List<Vector3f> result = new ArrayList<>(hullVertices.length);

        for (Point3d hullVertex : hullVertices) {
            result.add(new Vector3f((float) hullVertex.x, (float) hullVertex.y, (float) hullVertex.z));
        }

        return result;
    }
}
