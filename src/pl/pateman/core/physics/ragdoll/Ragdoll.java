package pl.pateman.core.physics.ragdoll;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.constraintsolver.Generic6DofConstraint;
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
                                     final Matrix4f transform) {
        final TempVars vars = TempVars.get();

        transform.getUnnormalizedRotation(vars.quat1);

        Utils.matrixToTransform(vars.vecmathTransform, transform);
        rigidBody.setCenterOfMassTransform(vars.vecmathTransform);
        rigidBody.getMotionState().setWorldTransform(vars.vecmathTransform);

        final BodyPart bodyPart = this.ragdollStructure.getBodyParts().get(bodyPartType);
        final List<Bone> allBones = new ArrayList<>(bodyPart.getBones());
        allBones.addAll(bodyPart.getPivotBones());

        this.partRigidBodies.put(bodyPart.getFirstBone().getIndex(), new RagdollBody(rigidBody, allBones, vars.quat1));
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

        //  If the user has set a custom rotation, use it. Otherwise, compute the rotation.
        if ((bodyPart.getCustomTransformFlag() & RagdollStructure.CUSTOM_TRANSFORM_ROTATION) == RagdollStructure.CUSTOM_TRANSFORM_ROTATION) {
            bodyPart.getCustomTransform().getUnnormalizedRotation(vars.quat1);
        } else {
            Utils.rotationBetweenVectors(vars.quat1, vars.vect3d1, vars.vect3d2);
        }
        //  Similar story with the translation.
        if ((bodyPart.getCustomTransformFlag() & RagdollStructure.CUSTOM_TRANSFORM_TRANSLATION) == RagdollStructure.CUSTOM_TRANSFORM_TRANSLATION) {
            bodyPart.getCustomTransform().getTranslation(vars.vect3d1);
        } else {
            vars.vect3d1.add(vars.vect3d2).mul(0.5f);
        }

        //  Finally, create the rigid body's transformation matrix.
        Utils.fromRotationTranslationScale(vars.tempMat4x41, vars.quat1, vars.vect3d1, Utils.IDENTITY_VECTOR);

        this.addBodyToSimulation(BodyPartType.CHEST, chestRigidBody, vars.tempMat4x41);
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

        final Generic6DofConstraint generic6DofConstraint = new Generic6DofConstraint(rigidBodyA, rigidBodyB,
                vars.vecmathTransform, vars.vecmathTransform2, true);
        for (RagdollLink ragdollLink : this.ragdollStructure.getBodyLinks()) {
            if (ragdollLink.getPartA().getPartType().equals(relativeTo) && ragdollLink.getPartB().getPartType().equals(bodyPartType)) {
                Utils.convert(vars.vecmathVect3d1, ragdollLink.getMinLimit());
                Utils.convert(vars.vecmathVect3d2, ragdollLink.getMaxLimit());
                generic6DofConstraint.setAngularLowerLimit(vars.vecmathVect3d1);
                generic6DofConstraint.setAngularUpperLimit(vars.vecmathVect3d2);
                break;
            }
        }

        this.dynamicsWorld.addConstraint(generic6DofConstraint, true);
        this.addBodyToSimulation(bodyPartType, rigidBodyB, vars.tempMat4x41);

        vars.release();
    }

    private void createConstraintForLink(RagdollLink ragdollLink) {
        final RigidBody rigidBodyA = this.partRigidBodies.get(ragdollLink.getPartA().getFirstBone().getIndex()).getRigidBody();
        final RigidBody rigidBodyB = this.partRigidBodies.get(ragdollLink.getPartB().getFirstBone().getIndex()).getRigidBody();

        if (rigidBodyA == null || rigidBodyB == null) {
            throw new IllegalStateException("One of the body parts does not have a collider");
        }

        final TempVars vars = TempVars.get();

        Utils.convert(vars.vect3d1, rigidBodyA.getCenterOfMassTransform(vars.vecmathTransform).origin);
        Utils.convert(vars.vect3d2, rigidBodyB.getCenterOfMassTransform(vars.vecmathTransform).origin);
        vars.vect3d2.sub(vars.vect3d1, vars.vect3d3);

        vars.vecmathTransform.setIdentity();
        vars.vecmathTransform2.setIdentity();
        Utils.convert(vars.vecmathTransform.origin, vars.vect3d3);
        Utils.convert(vars.vecmathTransform2.origin, Utils.ZERO_VECTOR);

        final Generic6DofConstraint constraint = new Generic6DofConstraint(rigidBodyA, rigidBodyB,
                vars.vecmathTransform, vars.vecmathTransform2, true);
        constraint.setAngularUpperLimit(Utils.convert(vars.vecmathVect3d1, ragdollLink.getMaxLimit()));
        constraint.setAngularLowerLimit(Utils.convert(vars.vecmathVect3d1, ragdollLink.getMinLimit()));
        this.dynamicsWorld.addConstraint(constraint, true);

        vars.release();
    }

    private void alignRagdollToModel() {
        final TempVars vars = TempVars.get();

        for (final Map.Entry<Integer, RagdollBody> entry : this.partRigidBodies.entrySet()) {
            final Bone attachBone = this.mesh.getSkeleton().getBone(entry.getKey());

            entry.getValue().getRigidBody().getCenterOfMassTransform(vars.vecmathTransform);
            Utils.convert(vars.quat3, vars.vecmathTransform.getRotation(vars.vecmathQuat));

            RagdollUtils.getBoneOffsetComponents(attachBone, vars.vect3d1, vars.quat1);
            entry.getValue().getInitialBoneRotation(attachBone, vars.quat2);

            this.entity.getRotation().mul(vars.quat1, vars.quat1).mul(vars.quat2.invert(), vars.quat1).mul(vars.quat3,
                    vars.quat1);
            this.entity.getTranslation().add(this.entity.getRotation().transform(vars.vect3d1), vars.vect3d1).
                    mul(this.entity.getScale(), vars.vect3d1);

            Utils.fromRotationTranslationScale(vars.tempMat4x41, vars.quat1, vars.vect3d1, Utils.IDENTITY_VECTOR);
            Utils.matrixToTransform(vars.vecmathTransform, vars.tempMat4x41);
            entry.getValue().getRigidBody().setCenterOfMassTransform(vars.vecmathTransform);
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
        this.configureBody(BodyPartType.RIGHT_UPPER_ARM, colliders.get(BodyPartType.RIGHT_UPPER_ARM), BodyPartType.CHEST,
                (rbAExtents, rbBExtents, outPivot) -> outPivot.set(-rbAExtents.x - rbBExtents.x, rbAExtents.y - rbBExtents.y, 0.0f));
        this.configureBody(BodyPartType.LEFT_UPPER_LEG, colliders.get(BodyPartType.LEFT_UPPER_LEG), BodyPartType.CHEST,
                (rbAExtents, rbBExtents, outPivot) -> outPivot.set(rbAExtents.x, -rbAExtents.y - rbBExtents.y, 0.0f));
        this.configureBody(BodyPartType.RIGHT_UPPER_LEG, colliders.get(BodyPartType.RIGHT_UPPER_LEG), BodyPartType.CHEST,
                (rbAExtents, rbBExtents, outPivot) -> outPivot.set(-rbAExtents.x, -rbAExtents.y - rbBExtents.y, 0.0f));

        this.mesh.getSkeleton().getBones().forEach(bone -> this.boneMatrices.put(bone.getIndex(),
                bone.getOffsetMatrix()));

//        for (final RagdollLink ragdollLink : this.ragdollStructure.getBodyLinks()) {
//            this.createConstraintForLink(ragdollLink);
//        }

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

    Map<Integer, RagdollBody> getPartRigidBodies() {
        return partRigidBodies;
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
        if (this.enabled) {
            this.alignRagdollToModel();
        }
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

    private class RagdollBody {
        private final RigidBody rigidBody;
        private final Quaternionf initialRotation;
        private final Map<Integer, TransformComponents> initialBoneTransforms;
        private final List<Bone> assignedBones;

        RagdollBody(RigidBody rigidBody, List<Bone> bones, Quaternionf initialRotation) {
            this.rigidBody = rigidBody;
            this.initialRotation = new Quaternionf().set(initialRotation);

            this.initialBoneTransforms = new TreeMap<>();
            this.assignedBones = bones;
        }

        private void initializeBone(final Bone bone, final Matrix4f invRigidBodyTransform) {
            final TempVars vars = TempVars.get();
            final TransformComponents transformComponents = new TransformComponents();

            //  Get the bone's world bind matrix.
            vars.tempMat4x41.set(bone.getWorldBindMatrix());

            //  Set the unnormalized rotation taken from the world bind matrix as the initial rotation of the bone.
            vars.tempMat4x41.getUnnormalizedRotation(transformComponents.getRotation());

            //  Get the scale from the world bind matrix as well.
            vars.tempMat4x41.getScale(transformComponents.getScale());

            //  In order to get the translation of the bone, we need to convert the world bind matrix into the
            //  rigid body's sppace and get the translation then.
            invRigidBodyTransform.mul(vars.tempMat4x41, vars.tempMat4x41);
            vars.tempMat4x41.getTranslation(transformComponents.getTranslation());

            this.initialBoneTransforms.put(bone.getIndex(), transformComponents);

            vars.release();
        }

        void initializeBody() {
            final TempVars vars = TempVars.get();

            //  Get the inverse transformation matrix of the rigid body.
            Utils.transformToMatrix(vars.tempMat4x42, this.rigidBody.getCenterOfMassTransform(vars.vecmathTransform));
            vars.tempMat4x42.invert();

            //  Initialize each assigned bone.
            this.assignedBones.forEach(bone -> this.initializeBone(bone, vars.tempMat4x42));

            vars.release();
        }

        void getTransformedBone(final Bone bone, final Vector3f outPos, final Quaternionf outRot,
                                final Vector3f outScale) {
            final TempVars vars = TempVars.get();
            final TransformComponents initialTrans = this.initialBoneTransforms.get(bone.getIndex());

            //  Start by getting the current rigid body's transformation matrix.
            Utils.transformToMatrix(vars.tempMat4x41, this.rigidBody.getCenterOfMassTransform(vars.vecmathTransform));

            //  Transform the initial bone's position by the current rigid body's transformation matrix.
            vars.tempMat4x41.transformPosition(initialTrans.getTranslation(), outPos);

            //  In order to get the transformed rotation, we need to calculate the difference between the
            //  ** rigid body's ** initial rotation and its current rotation, and then multiply it by the ** bone's **
            //  initial rotation.
            vars.tempMat4x41.getUnnormalizedRotation(vars.quat1);
            final Quaternionf diff = this.initialRotation.invert(vars.quat2).mul(vars.quat1, vars.quat3);
            diff.mul(initialTrans.getRotation(), outRot);

            //  Scale doesn't change.
            outScale.set(initialTrans.getScale());

            vars.release();
        }

        Quaternionf getInitialBoneRotation(final Bone bone, final Quaternionf outQuat) {
            final TransformComponents transformComponents = this.initialBoneTransforms.get(bone.getIndex());
            outQuat.set(transformComponents.getRotation());
            return outQuat;
        }

        List<Bone> getAssignedBones() {
            return assignedBones;
        }

        RigidBody getRigidBody() {
            return rigidBody;
        }

        private class TransformComponents {
            private final Vector3f translation;
            private final Quaternionf rotation;
            private final Vector3f scale;

            TransformComponents() {
                this.translation = new Vector3f();
                this.rotation = new Quaternionf();
                this.scale = new Vector3f();
            }

            Vector3f getTranslation() {
                return translation;
            }

            Quaternionf getRotation() {
                return rotation;
            }

            Vector3f getScale() {
                return scale;
            }
        }
    }

    @FunctionalInterface
    private interface PivotCallback {
        void setPivot(final Vector3f rbAExtents, final Vector3f rbBExtents, final Vector3f outPivot);
    }
}
