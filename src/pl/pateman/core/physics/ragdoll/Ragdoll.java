package pl.pateman.core.physics.ragdoll;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.constraintsolver.Generic6DofConstraint;
import org.joml.Vector3f;
import org.joml.Vector4f;
import pl.pateman.core.TempVars;
import pl.pateman.core.Utils;
import pl.pateman.core.entity.CameraEntity;
import pl.pateman.core.entity.EntityData;
import pl.pateman.core.entity.line.Line;
import pl.pateman.core.entity.line.LineRenderer;
import pl.pateman.core.mesh.Bone;
import pl.pateman.core.mesh.Mesh;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by pateman.
 */
public final class Ragdoll {
    public static final Vector4f RAGDOLL_DEBUG_BONE_COLOR = new Vector4f(0.0f, 1.0f, 0.0f, 1.0f);

    private final Mesh mesh;
    private final Random random;
    private DiscreteDynamicsWorld dynamicsWorld;
    private boolean enabled;
    private final LineRenderer lineRenderer;
    private RagdollStructure ragdollStructure;
    private final Map<BodyPartType, RigidBody> partRigidBodies;

    public Ragdoll(Mesh mesh) {
        if (mesh == null) {
            throw new IllegalArgumentException();
        }
        this.mesh = mesh;
        this.enabled = false;
        this.lineRenderer = new LineRenderer();
        this.random = new Random();
        this.partRigidBodies = new HashMap<>();
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
            RagdollUtils.getMatrixForBone(vars.tempMat4x41, firstBone);
            RagdollUtils.getMatrixForBone(vars.tempMat4x42, lastBone);
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

            this.partRigidBodies.put(bodyPart.getPartType(), rigidBody);
        }

        //  Draw debug lines between the first and the last bone.
        firstBone.getWorldBindMatrix().getTranslation(vars.vect3d1);
        lastBone.getWorldBindMatrix().getTranslation(vars.vect3d2);
        this.lineRenderer.addLine(vars.vect3d1, vars.vect3d2, RAGDOLL_DEBUG_BONE_COLOR);

        vars.release();
    }

    private void createConstraintForLink(RagdollLink ragdollLink) {
        final RigidBody rigidBodyA = this.partRigidBodies.get(ragdollLink.getPartA().getPartType());
        final RigidBody rigidBodyB = this.partRigidBodies.get(ragdollLink.getPartB().getPartType());

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
            if (partEntry.getValue().isConfigured()) {
                this.createColliderForBodyPart(partEntry.getValue());
            } else {
                System.out.println("Ragdoll part '" + partEntry.getKey() + "' is not configured");
            }
        }

        for (final RagdollLink ragdollLink : this.ragdollStructure.getBodyLinks()) {
            this.createConstraintForLink(ragdollLink);
        }
        this.setEnabled(false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        final int activationState = this.enabled ? CollisionObject.ACTIVE_TAG : CollisionObject.DISABLE_SIMULATION;
        this.partRigidBodies.values().forEach(rb -> rb.forceActivationState(activationState));
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

    public void drawRagdollLines(final CameraEntity cameraEntity, final Vector3f translation) {
        for (final Line line : this.lineRenderer) {
            line.getTo().add(translation);
            line.getFrom().add(translation);
        }
        this.lineRenderer.drawLines(cameraEntity);
    }
}
