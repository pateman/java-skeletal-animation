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
    private final Map<String, RagdollBody> partRigidBodies;
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
        this.partRigidBodies = new TreeMap<>();
        this.boneMatrices = new TreeMap<>();
    }

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

    private RigidBody createRigidBody(final RagdollStructure.Part part, final CollisionShape collisionShape) {
        final TempVars vars = TempVars.get();

        vars.tempMat4x41.identity();

        //  Create entity data as well. We don't really need a valid identifier, so a random value will do just
        //  fine.
        final EntityData entityData = new EntityData(System.currentTimeMillis() + this.random.nextInt() +
                part.getName().hashCode(),"RagdollBoxCollider-" + part.getName(), null);
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

    private void addBodyToSimulation(final RagdollStructure.Part part, final RigidBody rigidBody,
                                     final Matrix4f transform, final Vector3f initialPivot) {
        final TempVars vars = TempVars.get();

        transform.getUnnormalizedRotation(vars.quat1);

        Utils.matrixToTransform(vars.vecmathTransform, transform);
        rigidBody.setCenterOfMassTransform(vars.vecmathTransform);
        rigidBody.getMotionState().setWorldTransform(vars.vecmathTransform);

        final List<Bone> allBones = new LinkedList<>(part.getColliderBones());
        allBones.addAll(part.getAttachedBones());

        this.partRigidBodies.put(part.getName(), new RagdollBody(rigidBody, allBones, vars.quat1, initialPivot));
        this.dynamicsWorld.addRigidBody(rigidBody);

        vars.release();
    }

    private void createConstraint(final RagdollStructure.Link ragdollLink) {
        final TempVars vars = TempVars.get();

        //  Get the rigid bodies.
        final RigidBody rbA = this.partRigidBodies.get(ragdollLink.getPartA()).getRigidBody();
        final RigidBody rbB = this.partRigidBodies.get(ragdollLink.getPartB()).getRigidBody();

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

    public void alignRagdollToModel() {
        final TempVars vars = TempVars.get();

        this.entity.getTransformation().invert(vars.tempMat4x42);

        for (final Map.Entry<String, RagdollBody> entry : this.partRigidBodies.entrySet()) {
            final RagdollStructure.Part part = this.ragdollStructure.getPart(entry.getKey());

            this.computeTransform(part.getParentBone(), part.getBone(), part.getOffsetRotation(), false, vars.tempMat4x41);
            vars.tempMat4x42.mul(vars.tempMat4x41, vars.tempMat4x41);
            Utils.matrixToTransform(vars.vecmathTransform, vars.tempMat4x41);
            entry.getValue().getRigidBody().setCenterOfMassTransform(vars.vecmathTransform);
        }

        vars.release();
    }

    private void computeTransform(final Bone parent, final Bone bone, final Quaternionf rot, boolean useBindMatrix,
                                  final Matrix4f outMatrix) {
        final TempVars vars = TempVars.get();

        final Matrix4f parentMat = useBindMatrix ? RagdollUtils.getMatrixForBone(vars.tempMat4x41, parent) :
                RagdollUtils.getOffsetMatrixForBone(vars.tempMat4x41, parent);
        final Matrix4f boneMat = useBindMatrix ? RagdollUtils.getMatrixForBone(vars.tempMat4x42, bone) :
                RagdollUtils.getOffsetMatrixForBone(vars.tempMat4x42, bone);

        final Vector3f parentPos = parentMat.getTranslation(vars.vect3d1);
        final Vector3f bonePos = boneMat.getTranslation(vars.vect3d2);

        final Quaternionf parentRot = parentMat.getNormalizedRotation(vars.quat1);
        final Quaternionf q = parentRot.mul(rot, vars.quat2);

        final Vector3f p = parentPos.add(bonePos, vars.vect3d3).mul(0.5f);
        Utils.fromRotationTranslationScale(outMatrix, q, p, Utils.IDENTITY_VECTOR);

        vars.release();
    }

    private void createBodyPart(final RagdollStructure.Part bodyPart) {
        final TempVars vars = TempVars.get();

        this.computeTransform(bodyPart.getParentBone(), bodyPart.getBone(), bodyPart.getOffsetRotation(), true, vars.tempMat4x41);
        final Vector3f p = vars.tempMat4x41.getTranslation(vars.vect3d1);

        final CollisionShape collisionShape = this.createColliderForBodyPart(bodyPart);
        final RigidBody rigidBody = this.createRigidBody(bodyPart, collisionShape);

        this.addBodyToSimulation(bodyPart, rigidBody, vars.tempMat4x41, p);

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

        this.mesh.getSkeleton().getBones().forEach(bone -> this.boneMatrices.put(bone.getIndex(),
                bone.getOffsetMatrix()));

        this.partRigidBodies.values().forEach(RagdollBody::initializeBody);

        this.setEnabled(false);
    }

    public void updateRagdoll() {
        final TempVars vars = TempVars.get();

        final Matrix4f invEntityTM = this.entity.getTransformation().invert(vars.tempMat4x41);

        //  Transform each assigned bone.
        for (final RagdollBody ragdollBody : this.partRigidBodies.values()) {
            for (final Bone bone : ragdollBody.getAssignedBones()) {
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

        this.partRigidBodies.values().forEach(rb -> {
            if (this.enabled && !rb.getRigidBody().isActive()) {
                rb.getRigidBody().activate();
            }
            if (!this.enabled && rb.getRigidBody().isActive()) {
                rb.getRigidBody().setActivationState(CollisionObject.WANTS_DEACTIVATION);
            }
        });
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
