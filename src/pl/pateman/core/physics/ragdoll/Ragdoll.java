package pl.pateman.core.physics.ragdoll;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.constraintsolver.TypedConstraint;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.pateman.core.TempVars;
import pl.pateman.core.Utils;
import pl.pateman.core.entity.AbstractEntity;
import pl.pateman.core.entity.EntityData;
import pl.pateman.core.mesh.Bone;
import pl.pateman.core.mesh.Mesh;

import java.util.*;

/**
 * Created by pateman.
 */
public final class Ragdoll {
    private final Mesh mesh;
    private final Random random;
    private DiscreteDynamicsWorld dynamicsWorld;
    private boolean enabled;
    private RagdollStructure ragdollStructure;
    private final List<RagdollBody> partRigidBodies;
    private final Map<Integer, Matrix4f> boneMatrices;
    private final AbstractEntity entity;

    public Ragdoll(Mesh mesh, AbstractEntity entity) {
        if (mesh == null || entity == null) {
            throw new IllegalArgumentException("Valid mesh and owner entity are required");
        }
        this.entity = entity;
        this.mesh = mesh;
        this.enabled = false;
        this.random = new Random();
        this.partRigidBodies = new ArrayList<>();
        this.boneMatrices = new TreeMap<>();
    }

    /**
     * Creates a collision shape for the given ragdoll part.
     *
     * @param part Ragdoll part to create a collision shape from.
     * @return {@code CollisionShape}.
     */
    private CollisionShape createColliderForBodyPart(final RagdollStructure.Part part) {
        //  Start by computing AABBs for each bone that the body part consists of. When creating the AABBs, compute
        //  one big AABB which encloses all of them.
        final RagdollUtils.SimpleAABB result = new RagdollUtils.SimpleAABB();
        final RagdollUtils.SimpleAABB boneAABB = new RagdollUtils.SimpleAABB();
        for (final Bone bone : part.getColliderBones()) {
            //  If calculateAABBForBone() returns true, it means that the bone influences at least one vertex.
            if (RagdollUtils.calculateAABBForBone(boneAABB, bone, this.mesh)) {
                result.merge(boneAABB);
            }

            //  Don't forget to reset the bone's AABB so we get correct results.
            boneAABB.reset();
        }

        if (result.isUndefined()) {
            throw new IllegalStateException("The body part '" + part.getName() +
                    "' consists of bones that do not influence any vertices");
        }

        final TempVars vars = TempVars.get();

        CollisionShape resultShape;

        //  Check what kind of a collider we're dealing with.
        if (part.getColliderType().equals(BodyPartCollider.BOX)) {
            //  Using the AABB that we've just computed, create the part's rigid body.
            final Vector3f halfExtents = result.getExtents(vars.vect3d1);
            resultShape = new BoxShape(Utils.convert(vars.vecmathVect3d1, halfExtents));
        } else {
            throw new UnsupportedOperationException("Unsupported collider type");
        }

        vars.release();
        return resultShape;
    }

    /**
     * Creates a rigid body.
     *
     * @param part Ragdoll part.
     * @param collisionShape Collision shape.
     * @return {@code RigidBody}.
     */
    private RigidBody createRigidBody(final RagdollStructure.Part part, final CollisionShape collisionShape) {
        final TempVars vars = TempVars.get();

        vars.tempMat4x41.identity();

        //  Create entity data as well. We don't really need a valid identifier, so a random value will do just
        //  fine.
        final EntityData entityData = new EntityData((long) part.getName().hashCode(),
                "RagdollBoxCollider-" + part.getName(), null);
        final RigidBody rigidBody = RagdollUtils.createRigidBody(part.getColliderBones().size(), collisionShape,
                vars.tempMat4x41);
        rigidBody.setUserPointer(entityData);
        rigidBody.setDamping(part.getPhysicalProperties().getLinearDamping(), part.getPhysicalProperties().
                getAngularDamping());
        rigidBody.setDeactivationTime(part.getPhysicalProperties().getDeactivationTime());
        rigidBody.setSleepingThresholds(part.getPhysicalProperties().getLinearSleepingThreshold(), part.
                getPhysicalProperties().getAngularSleepingThreshold());

        vars.release();
        return rigidBody;
    }

    /**
     * Adds the given rigid body to the simulation.
     *
     * @param part Ragdoll part.
     * @param rigidBody Rigid body.
     * @param transform The rigid body's initial transformation matrix.
     */
    private void addBodyToSimulation(final RagdollStructure.Part part, final RigidBody rigidBody,
                                     final Matrix4f transform) {
        final TempVars vars = TempVars.get();

        transform.getUnnormalizedRotation(vars.quat1);

        Utils.matrixToTransform(vars.vecmathTransform, transform);
        rigidBody.setCenterOfMassTransform(vars.vecmathTransform);
        rigidBody.getMotionState().setWorldTransform(vars.vecmathTransform);

        final List<Bone> allBones = new LinkedList<>(part.getColliderBones());
        allBones.addAll(part.getAttachedBones());

        this.partRigidBodies.add(new RagdollBody(part.getName(), rigidBody, allBones, vars.quat1));
        this.dynamicsWorld.addRigidBody(rigidBody);

        vars.release();
    }

    private RagdollBody getBodyByPartName(final String partName) {
        for (int i = 0; i < this.partRigidBodies.size(); i++) {
            final RagdollBody ragdollBody = this.partRigidBodies.get(i);
            if (ragdollBody.getPartName().equals(partName)) {
                return ragdollBody;
            }
        }
        return null;
    }

    /**
     * Creates a constraint between two rigid bodies.
     *
     * @param ragdollLink Constraint information.
     */
    private void createConstraint(final RagdollStructure.Link ragdollLink) {
        final TempVars vars = TempVars.get();

        //  Get the rigid bodies.
        final RigidBody rbA = this.getBodyByPartName(ragdollLink.getPartA()).getRigidBody();
        final RigidBody rbB = this.getBodyByPartName(ragdollLink.getPartB()).getRigidBody();

        //  Convert pivots to vectors.
        final Vector3f pivotA = Utils.floatsToVec3f(ragdollLink.getPivotA(), vars.vect3d1);
        final Vector3f pivotB = Utils.floatsToVec3f(ragdollLink.getPivotB(), vars.vect3d2);

        //  Create the constraint.
        TypedConstraint constraint;
        if (!ragdollLink.isFlipAWithB()) {
            constraint = RagdollUtils.createConstraint(ragdollLink.getLinkBone(), rbA, rbB, pivotA, pivotB,
                    ragdollLink.getConstraintType(), ragdollLink.getLimits());
        } else {
            constraint = RagdollUtils.createConstraint(ragdollLink.getLinkBone(), rbB, rbA, pivotB, pivotA,
                    ragdollLink.getConstraintType(), ragdollLink.getLimits());
        }
        this.dynamicsWorld.addConstraint(constraint, true);

        System.out.println("Created constraint " + ragdollLink.getPartA() + "->" + ragdollLink.getPartB());
        vars.release();
    }

    /**
     * Computes the transformation of a rigid body by using the parent and the ending bone. When {@code useBindMatrix}
     * is set to {@code true}, the bones' world bind matrices are used, otherwise their offset matrices.
     *
     * @param parent Parent bone.
     * @param bone Ending bone.
     * @param rot Rotation offset.
     * @param useBindMatrix Whether the world bind matrix should be used or not.
     * @param outMatrix Computed transformation.
     */
    private void computeTransform(final Bone parent, final Bone bone, final Quaternionf rot, boolean useBindMatrix,
                                  final Matrix4f outMatrix) {
        final TempVars vars = TempVars.get();

        final Matrix4f parentMat = useBindMatrix ? RagdollUtils.getWorldBindMatrixForBone(vars.tempMat4x41, parent) :
                RagdollUtils.getOffsetMatrixForBone(vars.tempMat4x41, parent);
        final Matrix4f boneMat = useBindMatrix ? RagdollUtils.getWorldBindMatrixForBone(vars.tempMat4x42, bone) :
                RagdollUtils.getOffsetMatrixForBone(vars.tempMat4x42, bone);

        final Vector3f parentPos = parentMat.getTranslation(vars.vect3d1);
        final Vector3f bonePos = boneMat.getTranslation(vars.vect3d2);

        //  Multiply the parent's rotation by the given offset.
        final Quaternionf parentRot = parentMat.getNormalizedRotation(vars.quat1);
        final Quaternionf q = parentRot.mul(rot, vars.quat2);

        //  Finally, compute the translation by finding the middle point between the bones.
        final Vector3f p = parentPos.add(bonePos, vars.vect3d3).mul(0.5f);
        Utils.fromRotationTranslationScale(outMatrix, q, p, Utils.IDENTITY_VECTOR);

        vars.release();
    }

    /**
     * Creates the given ragdoll part.
     *
     * @param bodyPart Ragdoll part to create.
     */
    private void createBodyPart(final RagdollStructure.Part bodyPart) {
        final TempVars vars = TempVars.get();

        //  Compute the initial transformation of the rigid body.
        this.computeTransform(bodyPart.getParentBone(), bodyPart.getBone(), bodyPart.getOffsetRotation(),
                true, vars.tempMat4x41);

        //  Create the rigid body.
        final CollisionShape collisionShape = this.createColliderForBodyPart(bodyPart);
        final RigidBody rigidBody = this.createRigidBody(bodyPart, collisionShape);

        //  Add it to the simulation.
        this.addBodyToSimulation(bodyPart, rigidBody, vars.tempMat4x41);

        vars.release();
    }

    /**
     * Aligns the ragdoll to the entity.
     */
    public void alignRagdollToModel() {
        final TempVars vars = TempVars.get();

        this.entity.getTransformation().invert(vars.tempMat4x42);

        //  Iterate through the rigid bodies...
        for (int i = 0; i < this.partRigidBodies.size(); i++) {
            final RagdollBody ragdollBody = this.partRigidBodies.get(i);

            final RagdollStructure.Part part = this.ragdollStructure.getPart(ragdollBody.getPartName());

            //  Compute the new transformation using the bones' offset matrix.
            this.computeTransform(part.getParentBone(), part.getBone(), part.getOffsetRotation(),
                    false, vars.tempMat4x41);

            //  Multiply the computed transformation by the inverse transformation matrix of the entity to position
            //  everything correctly.
            vars.tempMat4x42.mul(vars.tempMat4x41, vars.tempMat4x41);

            //  Set the rigid body's transform.
            Utils.matrixToTransform(vars.vecmathTransform, vars.tempMat4x41);
            ragdollBody.getRigidBody().setCenterOfMassTransform(vars.vecmathTransform);
        }

        vars.release();
    }

    public void buildRagdoll() {
        if (this.dynamicsWorld == null) {
            throw new IllegalStateException("A valid physics world needs to be assigned");
        }
        if (this.ragdollStructure == null) {
            throw new IllegalStateException("A valid ragdoll structure needs to be assigned");
        }

        //  Construct ragdoll parts.
        for (final String partName : this.ragdollStructure.getPartNames()) {
            final RagdollStructure.Part part = this.ragdollStructure.getPart(partName);
            this.createBodyPart(part);
        }

        //  Construct constraints.
        for (final String linkName : this.ragdollStructure.getLinkNames()) {
            final RagdollStructure.Link link = this.ragdollStructure.getLink(linkName);
            this.createConstraint(link);
        }

        //  Compose an ordered map of bone offset matrices.
        this.mesh.getSkeleton().getBones().forEach(bone -> this.boneMatrices.put(bone.getIndex(),
                bone.getOffsetMatrix()));

        //  Initialize the created ragdoll bodies.
        for (int i = 0; i < this.partRigidBodies.size(); i++) {
            this.partRigidBodies.get(i).initializeBody();
        }

        this.setEnabled(false);
    }

    public void updateRagdoll() {
        final TempVars vars = TempVars.get();

        final Matrix4f invEntityTM = this.entity.getTransformation().invert(vars.tempMat4x41);

        //  Transform each assigned bone.
        for (int i = 0; i < this.partRigidBodies.size(); i++) {
            final RagdollBody ragdollBody = this.partRigidBodies.get(i);

            for (int j = 0; j < ragdollBody.getAssignedBones().size(); j++) {
                final Bone bone = ragdollBody.getAssignedBones().get(j);

                ragdollBody.getTransformedBone(bone, vars.vect3d1, vars.quat1, vars.vect3d2);

                final Matrix4f boneMatrix = this.boneMatrices.get(bone.getIndex());
                Utils.fromRotationTranslationScale(boneMatrix, vars.quat1, vars.vect3d1, vars.vect3d2);

                //  Multiply the bone matrix by the inverse transformation of the entity in order to orient it
                //  correctly.
                invEntityTM.mul(boneMatrix, boneMatrix);
            }
        }

        vars.release();
    }

    AbstractEntity getEntity() {
        return this.entity;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        for (int i = 0; i < this.partRigidBodies.size(); i++) {
            final RagdollBody rb = this.partRigidBodies.get(i);

            if (this.enabled && !rb.getRigidBody().isActive()) {
                rb.getRigidBody().activate();
            }
            if (!this.enabled && rb.getRigidBody().isActive()) {
                rb.getRigidBody().setActivationState(CollisionObject.WANTS_DEACTIVATION);
            }
        }
    }

    public void setDynamicsWorld(DiscreteDynamicsWorld dynamicsWorld) {
        this.dynamicsWorld = dynamicsWorld;
    }

    public void setRagdollStructure(RagdollStructure ragdollStructure) {
        this.ragdollStructure = ragdollStructure;
    }

    Map<Integer, Matrix4f> getBoneMatrices() {
        return this.boneMatrices;
    }
}
