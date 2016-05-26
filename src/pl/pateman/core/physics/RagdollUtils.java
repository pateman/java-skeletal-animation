package pl.pateman.core.physics;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.pateman.core.TempVars;
import pl.pateman.core.Utils;
import pl.pateman.core.mesh.Bone;
import pl.pateman.core.mesh.Mesh;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by pateman.
 */
final class RagdollUtils {
    private RagdollUtils() {

    }

    /**
     * Returns the bone's world bind matrix with its scaling removed.
     *
     * @param out Final matrix.
     * @param bone Bone.
     */
    static void getMatrixForBone(final Matrix4f out, final Bone bone) {
        final TempVars vars = TempVars.get();

        final Vector3f bonePos = bone.getWorldBindMatrix().getTranslation(vars.vect3d2);
        final Quaternionf boneRot = vars.quat1.set(bone.getWorldBindMatrix().getRotation(vars.axisAngle4f1));

        Utils.fromRotationTranslationScale(out, boneRot, bonePos, Utils.IDENTITY_VECTOR);
        vars.release();
    }

    /**
     * Returns a {@code Collection} of all unique vertices that are influenced by the given bone.
     *
     * @param bone Bone.
     * @param mesh Mesh that holds the vertices.
     * @return {@code Collection}.
     */
    static Collection<Vector3f> getInfluencingVerticesForBone(final Bone bone, final Mesh mesh) {
        final Set<Map.Entry<Integer, Float>> vertexWeights = bone.getVertexWeights().entrySet();
        final Set<Vector3f> vertices = new HashSet<>(vertexWeights.size());

        for (Map.Entry<Integer, Float> vertexWeight : vertexWeights) {
            if (vertexWeight.getValue() != 0.0f) {
                vertices.add(mesh.getVertices().get(vertexWeight.getKey()));
            }
        }

        return vertices;
    }

    static class SimpleAABB {
        private final Vector3f min;
        private final Vector3f max;

        SimpleAABB() {
            this.min = new Vector3f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
            this.max = new Vector3f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        }

        void expand(final Vector3f point) {
            this.min.x = Math.min(this.min.x, point.x);
            this.min.y = Math.min(this.min.y, point.y);
            this.min.z = Math.min(this.min.z, point.z);

            this.max.x = Math.max(this.max.x, point.x);
            this.max.y = Math.max(this.max.y, point.y);
            this.max.z = Math.max(this.max.z, point.z);
        }

        Vector3f getExtents(final Vector3f out) {
            final TempVars tempVars = TempVars.get();
            final Vector3f center = this.max.add(this.min, tempVars.vect3d1);
            center.mul(0.5f);

            this.max.sub(center, out);
            tempVars.release();

            return out;
        }
    }

    enum RagdollBodyPartCollider {
        BOX,
        CAPSULE,
        SPHERE
    }
}
