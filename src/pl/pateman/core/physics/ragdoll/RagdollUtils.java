package pl.pateman.core.physics.ragdoll;

import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.DefaultMotionState;
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
        final Quaternionf boneRot = bone.getWorldBindMatrix().getNormalizedRotation(vars.quat1);

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
    private static Collection<Vector3f> getInfluencingVerticesForBone(final Bone bone, final Mesh mesh) {
        final Set<Map.Entry<Integer, Float>> vertexWeights = bone.getVertexWeights().entrySet();
        final Set<Vector3f> vertices = new HashSet<>(vertexWeights.size());

        for (Map.Entry<Integer, Float> vertexWeight : vertexWeights) {
            if (vertexWeight.getValue() != 0.0f) {
                vertices.add(mesh.getVertices().get(vertexWeight.getKey()));
            }
        }

        return vertices;
    }

    /**
     * Calculates an AABB of the given bone.
     *
     * @param out Result.
     * @param bone Bone to calculate an AABB of.
     * @param mesh Mesh which the bone belongs to.
     * @return {@code true} if the bone influences at least one vertex (meaning that an AABB can be created),
     * {@code false} otherwise.
     */
    static boolean calculateAABBForBone(final RagdollUtils.SimpleAABB out, final Bone bone, final Mesh mesh) {
        //  Get all vertices that are influenced by the given bone.
        final Collection<Vector3f> influencingVerticesForBone = RagdollUtils.getInfluencingVerticesForBone(bone, mesh);
        //  Calculate an AABB of the influencing vertices.
        for (Vector3f boneVertex : influencingVerticesForBone) {
            out.expand(boneVertex);
        }
        return !influencingVerticesForBone.isEmpty();
    }

    /**
     * Creates a new rigid body.
     *
     * @param mass Mass.
     * @param collisionShape Collision shape.
     * @param initialTransform Initial transformation matrix of the rigid body.
     * @return {@code RigidBody}.
     */
    static RigidBody createRigidBody(float mass, final CollisionShape collisionShape, final Matrix4f initialTransform) {
        final TempVars vars = TempVars.get();

        //  Calculate the local inertia and convert the initial transformation matrix to JBullet.
        collisionShape.calculateLocalInertia(mass, vars.vecmathVect3d1);
        Utils.matrixToTransform(vars.vecmathTransform, initialTransform);

        final RigidBody body = new RigidBody(mass, new DefaultMotionState(vars.vecmathTransform), collisionShape,
                vars.vecmathVect3d1);
        vars.release();
        return body;
    }

    static class SimpleAABB {
        private final Vector3f min;
        private final Vector3f max;

        SimpleAABB() {
            this.min = new Vector3f();
            this.max = new Vector3f();
            this.reset();
        }

        void reset() {
            this.min.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
            this.max.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        }

        boolean isUndefined() {
            return Float.isInfinite(this.min.x) && Float.isInfinite(this.min.y) && Float.isInfinite(this.min.z) &&
                    Float.isInfinite(this.max.x) && Float.isInfinite(this.max.y) && Float.isInfinite(this.max.z);
        }

        void expand(final Vector3f point) {
            this.min.x = Math.min(this.min.x, point.x);
            this.min.y = Math.min(this.min.y, point.y);
            this.min.z = Math.min(this.min.z, point.z);

            this.max.x = Math.max(this.max.x, point.x);
            this.max.y = Math.max(this.max.y, point.y);
            this.max.z = Math.max(this.max.z, point.z);
        }

        void merge(final SimpleAABB anotherAABB) {
            this.min.x = Math.min(this.min.x, anotherAABB.min.x);
            this.min.y = Math.min(this.min.y, anotherAABB.min.y);
            this.min.z = Math.min(this.min.z, anotherAABB.min.z);

            this.max.x = Math.max(this.max.x, anotherAABB.max.x);
            this.max.y = Math.max(this.max.y, anotherAABB.max.y);
            this.max.z = Math.max(this.max.z, anotherAABB.max.z);
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
}
