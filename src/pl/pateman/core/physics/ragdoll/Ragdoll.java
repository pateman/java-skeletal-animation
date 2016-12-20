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
    private final Map<Integer, RagdollBody> partRigidBodies;
    private final Map<Integer, Matrix4f> boneMatrices;
    private final AbstractEntity entity;

    public Ragdoll(Mesh mesh, AbstractEntity entity) {
        if (mesh == null || entity == null) {
            throw new IllegalArgumentException();
        }
        this.entity = entity;
        this.mesh = mesh;
        this.enabled = false;
        this.random = new Random();
        this.partRigidBodies = new TreeMap<>();

        this.boneMatrices = new TreeMap<>();
    }

    private CollisionShape createColliderForBodyPart(final BodyPart bodyPart) {
        //  Start by computing AABBs for each bone that the body part consists of. When creating the AABBs, compute
        //  one big AABB which encloses all of them.
        final RagdollUtils.SimpleAABB result = new RagdollUtils.SimpleAABB();
        final RagdollUtils.SimpleAABB boneAABB = new RagdollUtils.SimpleAABB();
        for (Bone bodyPartBone : bodyPart.getBones()) {
            //  If calculateAABBForBone() returns true, it means that the bone influences at least one vertex.
            if (RagdollUtils.calculateAABBForBone(boneAABB, bodyPartBone, this.mesh)) {
                result.merge(boneAABB);
            }

            //  Don't forget to reset the bone's AABB so we get correct results.
            boneAABB.reset();
        }

        if (result.isUndefined()) {
            throw new IllegalStateException("The body part '" + bodyPart.getPartType() +
                    "' consists of bones that do not influence any vertices");
        }

        final TempVars vars = TempVars.get();

        CollisionShape resultShape;

        //  Check what kind of a collider we're dealing with.
        if (bodyPart.getColliderType().equals(BodyPartCollider.BOX)) {
            //  Using the AABB that we've just computed, create the part's rigid body.
            final Vector3f halfExtents = result.getExtents(vars.vect3d1);
            resultShape = new BoxShape(Utils.convert(vars.vecmathVect3d1, halfExtents));
        } else {
            throw new UnsupportedOperationException("Unsupported collider type");
        }

        vars.release();
        return resultShape;
    }

    private RigidBody createRigidBody(final BodyPartType bodyPartType, final CollisionShape collisionShape) {
        final TempVars vars = TempVars.get();

        final BodyPart bodyPart = this.ragdollStructure.getBodyParts().get(bodyPartType);
        vars.tempMat4x41.identity();

        //  Create entity data as well. We don't really need a valid identifier, so a random value will do just
        //  fine.
        final Bone firstBone = bodyPart.getFirstBone();
        final Bone lastBone = bodyPart.getLastBone();

        final EntityData entityData = new EntityData(System.currentTimeMillis() + this.random.nextInt() +
                firstBone.getIndex() + lastBone.getIndex(),"RagdollBoxCollider-" + firstBone.getName() + "-" +
                lastBone.getName(), null);
        final RigidBody rigidBody = RagdollUtils.createRigidBody(1.0f, collisionShape, vars.tempMat4x41);
        rigidBody.setUserPointer(entityData);
        rigidBody.setDamping(0.05f, 0.85f);
        rigidBody.setDeactivationTime(0.8f);
        rigidBody.setSleepingThresholds(1.6f, 2.5f);

        vars.release();
        return rigidBody;
    }

    private void addBodyToSimulation(final BodyPartType bodyPartType, final RigidBody rigidBody,
                                     final Matrix4f transform, final Vector3f initialPivot) {
        final TempVars vars = TempVars.get();

        transform.getUnnormalizedRotation(vars.quat1);

        Utils.matrixToTransform(vars.vecmathTransform, transform);
        rigidBody.setCenterOfMassTransform(vars.vecmathTransform);
        rigidBody.getMotionState().setWorldTransform(vars.vecmathTransform);

        final BodyPart bodyPart = this.ragdollStructure.getBodyParts().get(bodyPartType);
        final List<Bone> allBones = new ArrayList<>(bodyPart.getBones());
        allBones.addAll(bodyPart.getPivotBones());

        this.partRigidBodies.put(bodyPart.getFirstBone().getIndex(), new RagdollBody(rigidBody, allBones, vars.quat1,
                initialPivot));
        this.dynamicsWorld.addRigidBody(rigidBody);

        vars.release();
    }

    private void configureChestBody(final RigidBody chestRigidBody) {
        final TempVars vars = TempVars.get();

        final BodyPart bodyPart = this.ragdollStructure.getBodyParts().get(BodyPartType.CHEST);

        //  Prepare bone matrices for calculating the rigid body's transformation. At the end of the operations:
        //  a)  vars.vect3d1 - will hold the rigid body's translation
        //  b)  vars.quat1 - will hold the rigid body's rotation.
        RagdollUtils.getMatrixForBone(vars.tempMat4x41, this.ragdollStructure.getBodyParts().get(BodyPartType.HEAD).getLastBone());
        RagdollUtils.getMatrixForBone(vars.tempMat4x42, bodyPart.getLastBone());
        vars.tempMat4x41.getTranslation(vars.vect3d1);
        vars.tempMat4x42.getTranslation(vars.vect3d2);

        //  Compute the rotation between the two bones.
        Utils.rotationBetweenVectors(vars.quat1, vars.vect3d1, vars.vect3d2);

        //  Similar story with the translation.
        vars.vect3d1.add(vars.vect3d2).mul(0.5f);

        //  Finally, create the rigid body's transformation matrix.
        Utils.fromRotationTranslationScale(vars.tempMat4x41, vars.quat1, vars.vect3d1, Utils.IDENTITY_VECTOR);

        this.addBodyToSimulation(BodyPartType.CHEST, chestRigidBody, vars.tempMat4x41, Utils.ZERO_VECTOR);
        vars.release();
    }

    private void configureBody(final BodyPartType bodyPartType, final CollisionShape collisionShape,
                               final BodyPartType relativeTo, PivotCallback pivotCallback) {
        final TempVars vars = TempVars.get();

        final Bone relativeToFirstBone = this.ragdollStructure.getBodyParts().get(relativeTo).getFirstBone();
        final int relativeToFirstBoneIdx = relativeToFirstBone.getIndex();
        final RigidBody rigidBodyA = this.partRigidBodies.get(relativeToFirstBoneIdx).getRigidBody();
        final RigidBody rigidBodyB = this.createRigidBody(bodyPartType, collisionShape);

        RagdollUtils.getRigidBodyExtents(rigidBodyA, vars.vect3d1);
        RagdollUtils.getRigidBodyExtents(rigidBodyB, vars.vect3d2);

        pivotCallback.setPivot(vars.vect3d1, vars.vect3d2, vars.vect3d3);
        RagdollUtils.getMatrixForBone(vars.tempMat4x41,
            this.ragdollStructure.getBodyParts().get(bodyPartType).getFirstBone());
        vars.tempMat4x41.getUnnormalizedRotation(vars.quat1);

        Utils.transformToMatrix(vars.tempMat4x42, rigidBodyA.getCenterOfMassTransform(vars.vecmathTransform));
        vars.tempMat4x42.transformPosition(vars.vect3d3, vars.vect3d4);

        Utils.fromRotationTranslationScale(vars.tempMat4x41, vars.quat1, vars.vect3d4, Utils.IDENTITY_VECTOR);

        vars.vecmathTransform.setIdentity();
        vars.vecmathTransform2.setIdentity();
        Utils.convert(vars.vecmathTransform.origin, vars.vect3d3);
        Utils.convert(vars.vecmathTransform2.origin, Utils.ZERO_VECTOR);

        for (RagdollLink ragdollLink : this.ragdollStructure.getBodyLinks()) {
            if (ragdollLink.getPartA().getPartType().equals(relativeTo) &&
                ragdollLink.getPartB().getPartType().equals(bodyPartType)) {

                TypedConstraint constraint;
                if (ragdollLink.getLinkType().equals(RagdollLinkType.CONE_TWIST)) {
                    constraint = RagdollUtils.createConeTwistConstraint(rigidBodyA, rigidBodyB, vars.vecmathTransform,
                            vars.vecmathTransform2, ragdollLink.getLimits());
                } else {
                    constraint = RagdollUtils.createHingeConstraint(rigidBodyA, rigidBodyB, vars.vecmathTransform,
                            vars.vecmathTransform2, ragdollLink.getLimits());
                }
                this.dynamicsWorld.addConstraint(constraint, true);

                break;
            }
        }
        this.addBodyToSimulation(bodyPartType, rigidBodyB, vars.tempMat4x41, vars.vect3d3);

        vars.release();
    }

    private void alignBodyPart(final BodyPartType bodyPartType, final Quaternionf inverseEntityRotation,
                               final Matrix4f parentTM, final Matrix4f outBodyTM) {
        final TempVars vars = TempVars.get();

        final BodyPart bodyPart = this.ragdollStructure.getBodyParts().get(bodyPartType);
        final RagdollBody ragdollBody = this.partRigidBodies.get(bodyPart.getFirstBone().getIndex());
        vars.vect3d1.set(ragdollBody.getInitialTranslation());
        parentTM.transformPosition(vars.vect3d1, vars.vect3d1);

        bodyPart.getFirstBone().getOffsetMatrix().getUnnormalizedRotation(vars.quat1);
        vars.quat1.mul(bodyPart.getFirstBone().getInverseBindMatrix().getUnnormalizedRotation(vars.quat3));
        inverseEntityRotation.mul(vars.quat1, vars.quat1);

        Utils.fromRotationTranslationScale(vars.tempMat4x41, vars.quat1, vars.vect3d1, Utils.IDENTITY_VECTOR);
        Utils.matrixToTransform(vars.vecmathTransform, vars.tempMat4x41);
        ragdollBody.getRigidBody().setCenterOfMassTransform(vars.vecmathTransform);

        if (outBodyTM != null) {
            outBodyTM.set(vars.tempMat4x41);
        }

        vars.release();
    }

    public void alignRagdollToModel() {
        final TempVars vars = TempVars.get();

        final Quaternionf invEntityRot = this.entity.getTransformation().getUnnormalizedRotation(vars.quat2).invert();

        //  Start by transforming the chest.
        final BodyPart bodyPart = this.ragdollStructure.getBodyParts().get(BodyPartType.CHEST);

        final Matrix4f lastHeadBoneMat = this.ragdollStructure.getBodyParts().get(BodyPartType.HEAD).getLastBone().
                getOffsetMatrix();
        final Matrix4f lastChestBoneMat = bodyPart.getLastBone().getOffsetMatrix();

        lastHeadBoneMat.getTranslation(vars.vect3d1);
        lastChestBoneMat.getTranslation(vars.vect3d2);

        Utils.rotationBetweenVectors(vars.quat1, vars.vect3d1, vars.vect3d2);
        invEntityRot.mul(vars.quat1, vars.quat1);

        this.entity.getTransformation().transformPosition(vars.vect3d1);
        this.entity.getTransformation().transformPosition(vars.vect3d2);
        vars.vect3d1.add(vars.vect3d2).mul(0.5f);

        Utils.fromRotationTranslationScale(vars.tempMat4x41, vars.quat1, vars.vect3d1, Utils.IDENTITY_VECTOR);
        Utils.matrixToTransform(vars.vecmathTransform, vars.tempMat4x41);
        this.partRigidBodies.get(bodyPart.getFirstBone().getIndex()).getRigidBody().
                setCenterOfMassTransform(vars.vecmathTransform);

        //  Transform the other body parts.
        this.alignBodyPart(BodyPartType.HEAD, invEntityRot, vars.tempMat4x41, null);
        this.alignBodyPart(BodyPartType.LEFT_UPPER_ARM, invEntityRot, vars.tempMat4x41, vars.tempMat4x42);
        this.alignBodyPart(BodyPartType.LEFT_LOWER_ARM, invEntityRot, vars.tempMat4x42, null);
        this.alignBodyPart(BodyPartType.RIGHT_UPPER_ARM, invEntityRot, vars.tempMat4x41, vars.tempMat4x42);
        this.alignBodyPart(BodyPartType.RIGHT_LOWER_ARM, invEntityRot, vars.tempMat4x42, null);
        this.alignBodyPart(BodyPartType.LEFT_UPPER_LEG, invEntityRot, vars.tempMat4x41, vars.tempMat4x42);
        this.alignBodyPart(BodyPartType.LEFT_LOWER_LEG, invEntityRot, vars.tempMat4x42, null);
        this.alignBodyPart(BodyPartType.RIGHT_UPPER_LEG, invEntityRot, vars.tempMat4x41, vars.tempMat4x42);
        this.alignBodyPart(BodyPartType.RIGHT_LOWER_LEG, invEntityRot, vars.tempMat4x42, null);

        vars.release();
    }

    public void buildRagdoll() {
        if (this.dynamicsWorld == null) {
            throw new IllegalStateException("A valid physics world needs to be assigned");
        }
        if (this.ragdollStructure == null) {
            throw new IllegalStateException("A valid ragdoll structure needs to be assigned");
        }

        final Map<BodyPartType, BodyPart> bodyParts = this.ragdollStructure.getBodyParts();
        final Map<BodyPartType, CollisionShape> colliders = new HashMap<>(bodyParts.size());

        //  Create colliders for body parts.
        for (Map.Entry<BodyPartType, BodyPart> partEntry : bodyParts.entrySet()) {
            final BodyPart bodyPart = partEntry.getValue();
            if (bodyPart.isConfigured()) {
                colliders.put(partEntry.getKey(), this.createColliderForBodyPart(bodyPart));
            } else {
                throw new IllegalStateException("Ragdoll part '" + partEntry.getKey() + "' is not configured");
            }
        }

        //  Start by creating a rigid body for the chest - we treat the chest as, sort of, the main body part of
        //  the ragdoll.
        final RigidBody chestRB = this.createRigidBody(BodyPartType.CHEST, colliders.get(BodyPartType.CHEST));
        this.configureChestBody(chestRB);

        this.configureBody(BodyPartType.HEAD, colliders.get(BodyPartType.HEAD), BodyPartType.CHEST,
                (rbAExtents, rbBExtents, outPivot) -> outPivot.set(0.0f, rbAExtents.y + rbBExtents.y, 0.0f));
        this.configureBody(BodyPartType.LEFT_UPPER_ARM, colliders.get(BodyPartType.LEFT_UPPER_ARM), BodyPartType.CHEST,
                (rbAExtents, rbBExtents, outPivot) -> outPivot.set(rbAExtents.x + rbBExtents.x, rbAExtents.y - rbBExtents.y, 0.0f));
        this.configureBody(BodyPartType.LEFT_LOWER_ARM, colliders.get(BodyPartType.LEFT_LOWER_ARM), BodyPartType.LEFT_UPPER_ARM,
                (rbAExtents, rbBExtents, outPivot) -> outPivot.set(0.0f, -rbAExtents.y - rbBExtents.y, 0.0f));
        this.configureBody(BodyPartType.RIGHT_UPPER_ARM, colliders.get(BodyPartType.RIGHT_UPPER_ARM), BodyPartType.CHEST,
                (rbAExtents, rbBExtents, outPivot) -> outPivot.set(-rbAExtents.x - rbBExtents.x, rbAExtents.y - rbBExtents.y, 0.0f));
        this.configureBody(BodyPartType.RIGHT_LOWER_ARM, colliders.get(BodyPartType.RIGHT_LOWER_ARM), BodyPartType.RIGHT_UPPER_ARM,
                (rbAExtents, rbBExtents, outPivot) -> outPivot.set(0.0f, -rbAExtents.y - rbBExtents.y, 0.0f));
        this.configureBody(BodyPartType.LEFT_UPPER_LEG, colliders.get(BodyPartType.LEFT_UPPER_LEG), BodyPartType.CHEST,
                (rbAExtents, rbBExtents, outPivot) -> outPivot.set(rbAExtents.x, -rbAExtents.y - rbBExtents.y, 0.0f));
        this.configureBody(BodyPartType.LEFT_LOWER_LEG, colliders.get(BodyPartType.LEFT_LOWER_LEG), BodyPartType.LEFT_UPPER_LEG,
                (rbAExtents, rbBExtents, outPivot) -> outPivot.set(0.0f, -rbAExtents.y - rbBExtents.y, 0.0f));
        this.configureBody(BodyPartType.RIGHT_UPPER_LEG, colliders.get(BodyPartType.RIGHT_UPPER_LEG), BodyPartType.CHEST,
                (rbAExtents, rbBExtents, outPivot) -> outPivot.set(-rbAExtents.x, -rbAExtents.y - rbBExtents.y, 0.0f));
        this.configureBody(BodyPartType.RIGHT_LOWER_LEG, colliders.get(BodyPartType.RIGHT_LOWER_LEG), BodyPartType.RIGHT_UPPER_LEG,
                (rbAExtents, rbBExtents, outPivot) -> outPivot.set(0.0f, -rbAExtents.y - rbBExtents.y, 0.0f));

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

        final int activationState = this.enabled ? CollisionObject.ACTIVE_TAG : CollisionObject.DISABLE_SIMULATION;
//        if (this.enabled) {
//            this.alignRagdollToModel();
//        }
        this.partRigidBodies.values().forEach(rb -> {

            final TempVars vars = TempVars.get();
            rb.getRigidBody().getCenterOfMassTransform(vars.vecmathTransform);
            Utils.convert(vars.vecmathVect3d1, Utils.ZERO_VECTOR);

            rb.getRigidBody().setInterpolationWorldTransform(vars.vecmathTransform);
            rb.getRigidBody().setInterpolationAngularVelocity(vars.vecmathVect3d1);
            rb.getRigidBody().setInterpolationLinearVelocity(vars.vecmathVect3d1);

            rb.getRigidBody().forceActivationState(activationState);
            vars.release();
        });
    }

    public void setDynamicsWorld(DiscreteDynamicsWorld dynamicsWorld) {
        this.dynamicsWorld = dynamicsWorld;
    }

    RagdollStructure getRagdollStructure() {
        return ragdollStructure;
    }

    public void setRagdollStructure(RagdollStructure ragdollStructure) {
        this.ragdollStructure = ragdollStructure;
    }

    Map<Integer, Matrix4f> getBoneMatrices() {
        return this.boneMatrices;
    }

    @FunctionalInterface
    private interface PivotCallback {
        void setPivot(final Vector3f rbAExtents, final Vector3f rbBExtents, final Vector3f outPivot);
    }
}
