package pl.pateman.core.physics.ragdoll;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.BoxShape;
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

import static pl.pateman.core.physics.ragdoll.RagdollUtils.getMatrixForBone;

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

    private void createColliderForBodyPart(final BodyPart bodyPart) {
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

        //  Create the rigid body.
        final TempVars vars = TempVars.get();

        //  Get the first and the last bone of the part.
        final Bone firstBone = bodyPart.getFirstBone();
        final Bone lastBone = bodyPart.getLastBone();

        //  Check what kind of a collider we're dealing with.
        if (bodyPart.getColliderType().equals(BodyPartCollider.BOX)) {
            //  Using the AABB that we've just computed, create the part's rigid body.
            final Vector3f halfExtents = result.getExtents(vars.vect3d1).mul(0.5f);
            final BoxShape boxShape = new BoxShape(Utils.convert(vars.vecmathVect3d1, halfExtents));

            //  Prepare bone matrices for calculating the rigid body's transformation. At the end of the operations:
            //  a)  vars.vect3d1 - will hold the rigid body's translation
            //  b)  vars.quat1 - will hold the rigid body's rotation.
            getMatrixForBone(vars.tempMat4x41, firstBone);
            getMatrixForBone(vars.tempMat4x42, lastBone);
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

            //  Create entity data as well. We don't really need a valid identifier, so a random value will do just
            //  fine.
            final EntityData entityData = new EntityData(System.currentTimeMillis() + this.random.nextInt() +
                    firstBone.getIndex() + lastBone.getIndex(), "RagdollBoxCollider-" + firstBone.getName() + "-" +
                    lastBone.getName(), null);
            final RigidBody rigidBody = RagdollUtils.createRigidBody(1.0f, boxShape, vars.tempMat4x41);
            rigidBody.setUserPointer(entityData);
            rigidBody.setDamping(0.05f, 0.85f);
            rigidBody.setDeactivationTime(0.8f);
            rigidBody.setSleepingThresholds(1.6f, 2.5f);
            this.dynamicsWorld.addRigidBody(rigidBody);

            final List<Bone> allBones = new ArrayList<>(bodyPart.getBones());
            allBones.addAll(bodyPart.getPivotBones());

            this.partRigidBodies.put(firstBone.getIndex(), new RagdollBody(rigidBody, allBones, vars.quat1, vars.vect3d1));
        } else {
            throw new UnsupportedOperationException("Unsupported collider type");
        }

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

    public void buildRagdoll() {
        if (this.dynamicsWorld == null) {
            throw new IllegalStateException("A valid physics world needs to be assigned");
        }
        if (this.ragdollStructure == null) {
            throw new IllegalStateException("A valid ragdoll structure needs to be assigned");
        }

        for (Map.Entry<BodyPartType, BodyPart> partEntry : this.ragdollStructure.getBodyParts().entrySet()) {
            final BodyPart bodyPart = partEntry.getValue();
            if (bodyPart.isConfigured()) {
                this.createColliderForBodyPart(bodyPart);
            } else {
                System.out.println("Ragdoll part '" + partEntry.getKey() + "' is not configured");
            }
        }

        this.mesh.getSkeleton().getBones().forEach(bone -> this.boneMatrices.put(bone.getIndex(),
                new Matrix4f().set(bone.getWorldBindMatrix())));

        for (final RagdollLink ragdollLink : this.ragdollStructure.getBodyLinks()) {
            this.createConstraintForLink(ragdollLink);
        }
        this.setEnabled(false);
    }

    public void updateRagdoll() {
        final TempVars vars = TempVars.get();

        for (final RagdollBody ragdollBody : this.partRigidBodies.values()) {
            for (Bone bone : ragdollBody.getAssignedBones()) {
                ragdollBody.getTransformedBone(bone, vars.vect3d1, vars.quat1);

                final Matrix4f boneMatrix = this.boneMatrices.get(bone.getIndex());
                Utils.fromRotationTranslationScale(boneMatrix, vars.quat1, vars.vect3d1, bone.getBindScale());
            }
        }

        vars.release();
    }

    Map<Integer, RagdollBody> getPartRigidBodies() {
        return partRigidBodies;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        final int activationState = this.enabled ? CollisionObject.ACTIVE_TAG : CollisionObject.DISABLE_SIMULATION;
        this.partRigidBodies.values().forEach(rb -> rb.getRigidBody().forceActivationState(activationState));
    }

    public void setDynamicsWorld(DiscreteDynamicsWorld dynamicsWorld) {
        this.dynamicsWorld = dynamicsWorld;
    }

    public RagdollStructure getRagdollStructure() {
        return ragdollStructure;
    }

    public void setRagdollStructure(RagdollStructure ragdollStructure) {
        this.ragdollStructure = ragdollStructure;
    }

    public Map<Integer, Matrix4f> getBoneMatrices() {
        return this.boneMatrices;
    }

    private class RagdollBody {
        private final RigidBody rigidBody;
        private final Quaternionf initialRotation;
        private final Vector3f initialTranslation;
        private final Map<Integer, TransformComponents> initialBoneTransforms;
        private final List<Bone> assignedBones;

        RagdollBody(RigidBody rigidBody, List<Bone> bones, Quaternionf initialRotation, Vector3f initialTranslation) {
            this.rigidBody = rigidBody;
            this.initialRotation = new Quaternionf().set(initialRotation);
            this.initialTranslation = new Vector3f().set(initialTranslation);

            this.initialBoneTransforms = new TreeMap<>();
            this.assignedBones = bones;
            this.initializeBody(bones);
        }

        private void initializeBone(final Bone bone, final Matrix4f invRigidBodyTransform) {
            final TempVars vars = TempVars.get();
            final TransformComponents transformComponents = new TransformComponents();

            RagdollUtils.getMatrixForBone(vars.tempMat4x41, bone);

            final Vector3f boneTranslation = vars.tempMat4x41.getTranslation(vars.vect3d1);
            invRigidBodyTransform.transformPosition(boneTranslation, transformComponents.getTranslation());
            vars.tempMat4x41.getNormalizedRotation(transformComponents.getRotation());

            this.initialBoneTransforms.put(bone.getIndex(), transformComponents);

            vars.release();
        }

        private void initializeBody(final List<Bone> boneList) {
            final TempVars vars = TempVars.get();

            Utils.transformToMatrix(vars.tempMat4x42, this.rigidBody.getCenterOfMassTransform(vars.vecmathTransform));
            vars.tempMat4x42.invert();

            boneList.forEach(bone -> this.initializeBone(bone, vars.tempMat4x42));

            vars.release();
        }

        void getTransformedBone(final Bone bone, final Vector3f outVec, final Quaternionf outQuat) {
            final TempVars vars = TempVars.get();
            final TransformComponents initialTrans = this.initialBoneTransforms.get(bone.getIndex());

            Utils.transformToMatrix(vars.tempMat4x41, this.rigidBody.getCenterOfMassTransform(vars.vecmathTransform));
            vars.tempMat4x41.transformPosition(initialTrans.getTranslation(), outVec);

            final Quaternionf rot = this.initialRotation.invert(vars.quat2).mul(vars.quat1, vars.quat1);
            initialTrans.getRotation().mul(rot, outQuat);

            vars.release();
        }

        List<Bone> getAssignedBones() {
            return assignedBones;
        }

        RigidBody getRigidBody() {
            return rigidBody;
        }

        Quaternionf getInitialRotation() {
            return initialRotation;
        }

        Vector3f getInitialTranslation() {
            return initialTranslation;
        }

        private class TransformComponents {
            private final Vector3f translation;
            private final Quaternionf rotation;

            TransformComponents() {
                this.translation = new Vector3f();
                this.rotation = new Quaternionf();
            }

            Vector3f getTranslation() {
                return translation;
            }

            Quaternionf getRotation() {
                return rotation;
            }
        }
    }
}
