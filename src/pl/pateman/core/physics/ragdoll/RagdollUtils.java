package pl.pateman.core.physics.ragdoll;

import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.constraintsolver.ConeTwistConstraint;
import com.bulletphysics.dynamics.constraintsolver.Generic6DofConstraint;
import com.bulletphysics.dynamics.constraintsolver.HingeConstraint;
import com.bulletphysics.dynamics.constraintsolver.TypedConstraint;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.MatrixUtil;
import com.bulletphysics.linearmath.Transform;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.pateman.core.TempVars;
import pl.pateman.core.Utils;
import pl.pateman.core.mesh.Bone;
import pl.pateman.core.mesh.Mesh;

import java.util.*;

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
    static Matrix4f getMatrixForBone(final Matrix4f out, final Bone bone) {
        final TempVars vars = TempVars.get();

        final Vector3f bonePos = bone.getWorldBindMatrix().getTranslation(vars.vect3d2);
        final Quaternionf boneRot = bone.getWorldBindMatrix().getNormalizedRotation(vars.quat1);

        Utils.fromRotationTranslationScale(out, boneRot, bonePos, Utils.IDENTITY_VECTOR);
        vars.release();
        return out;
    }

    static Matrix4f getOffsetMatrixForBone(final Matrix4f out, final Bone bone) {
        final TempVars vars = TempVars.get();

        final Vector3f bonePos = bone.getOffsetMatrix().getTranslation(vars.vect3d2);
        final Quaternionf boneRot = bone.getOffsetMatrix().getNormalizedRotation(vars.quat1);

        Utils.fromRotationTranslationScale(out, boneRot, bonePos, Utils.IDENTITY_VECTOR);
        vars.release();
        return out;
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

    /**
     * Returns the extents of the given rigid body.
     *
     * @param rigidBody Rigid body.
     * @param outVector Rigid body's extents.
     * @return {@code Vector3f}.
     */
    static Vector3f getRigidBodyExtents(final RigidBody rigidBody, final Vector3f outVector) {
        final TempVars vars = TempVars.get();

        rigidBody.getCenterOfMassTransform(vars.vecmathTransform);
        rigidBody.getCollisionShape().getAabb(vars.vecmathTransform, vars.vecmathVect3d1, vars.vecmathVect3d2);

        final SimpleAABB simpleAABB = new SimpleAABB();
        Utils.convert(simpleAABB.min, vars.vecmathVect3d1);
        Utils.convert(simpleAABB.max, vars.vecmathVect3d2);
        simpleAABB.getExtents(outVector);

        vars.release();
        return outVector;
    }

    static TypedConstraint createConstraint(final Bone parent, final RigidBody rbA,
                                            final RigidBody rbB, final Vector3f pivotA, final Vector3f pivotB,
                                            final RagdollLinkType constraintType, final List<Float> limits) {
        final TempVars vars = TempVars.get();

        final Vector3f hingePos = parent.getWorldBindMatrix().getTranslation(vars.vect3d1);

        final Matrix4f worldA = vars.tempMat4x41;
        final Matrix4f worldB = vars.tempMat4x42;
        Utils.transformToMatrix(worldA, rbA.getCenterOfMassTransform(vars.vecmathTransform));
        Utils.transformToMatrix(worldB, rbB.getCenterOfMassTransform(vars.vecmathTransform));

        worldA.invert();
        worldB.invert();

        final Vector3f offA = vars.vect3d2;
        final Vector3f offB = vars.vect3d3;
        worldA.transformPosition(hingePos, offA);
        worldB.transformPosition(hingePos, offB);

        final Transform transA = vars.vecmathTransform;
        final Transform transB = vars.vecmathTransform2;
        transA.setIdentity();
        transB.setIdentity();
        Utils.convert(transA.origin, offA);
        Utils.convert(transB.origin, offB);
        MatrixUtil.setEulerZYX(transA.basis, pivotA.x, pivotA.y, pivotA.z);
        MatrixUtil.setEulerZYX(transB.basis, pivotB.x, pivotB.y, pivotB.z);

        TypedConstraint constraint = null;
        switch (constraintType) {
            case CONE_TWIST:
                constraint = new ConeTwistConstraint(rbA, rbB, transA, transB);
                if (limits.size() > 3) {
                    ((ConeTwistConstraint) constraint).setLimit(limits.get(0), limits.get(1), limits.get(2),
                            limits.get(3), limits.get(4), limits.get(5));
                } else {
                    ((ConeTwistConstraint) constraint).setLimit(limits.get(0), limits.get(1), limits.get(2));
                }
                break;
            case HINGE:
                constraint = new HingeConstraint(rbA, rbB, transA, transB);
                if (limits.size() > 2) {
                    ((HingeConstraint) constraint).setLimit(limits.get(0), limits.get(1), limits.get(2), limits.get(3),
                            limits.get(4));
                } else {
                    ((HingeConstraint) constraint).setLimit(limits.get(0), limits.get(1));
                }
                break;
            case GENERIC:
                constraint = new Generic6DofConstraint(rbA, rbB, transA, transB, true);

                //  Lower limit.
                vars.vecmathVect3d1.set(limits.get(0), limits.get(1), limits.get(2));
                //  Upper limit.
                vars.vecmathVect3d2.set(limits.get(3), limits.get(4), limits.get(5));

                ((Generic6DofConstraint) constraint).setAngularLowerLimit(vars.vecmathVect3d1);
                ((Generic6DofConstraint) constraint).setAngularUpperLimit(vars.vecmathVect3d2);

                vars.vecmathVect3d1.set(0.0f, 0.0f, 0.0f);
                ((Generic6DofConstraint) constraint).setLinearLowerLimit(vars.vecmathVect3d1);
                ((Generic6DofConstraint) constraint).setLinearUpperLimit(vars.vecmathVect3d1);
                break;
        }

        vars.release();
        return constraint;
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
