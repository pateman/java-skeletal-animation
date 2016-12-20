package pl.pateman.core.physics.ragdoll;

import com.bulletphysics.dynamics.RigidBody;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.pateman.core.TempVars;
import pl.pateman.core.Utils;
import pl.pateman.core.mesh.Bone;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Defines a single ragdoll body.
 *
 * Created by pateman.
 */
class RagdollBody {
    private final RigidBody rigidBody;
    private final Quaternionf initialRotation;
    private final Quaternionf inverseInitialRotation;
    private final Vector3f initialTranslation;
    private final Map<Integer, TransformComponents> initialBoneTransforms;
    private final List<Bone> assignedBones;

    RagdollBody(RigidBody rigidBody, List<Bone> bones, Quaternionf initialRotation, Vector3f initialTranslation) {
        this.rigidBody = rigidBody;
        this.initialRotation = new Quaternionf().set(initialRotation);
        this.inverseInitialRotation = initialRotation.invert(new Quaternionf());
        this.initialTranslation = new Vector3f().set(initialTranslation);

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
        final Quaternionf diff = this.inverseInitialRotation.mul(vars.quat1, vars.quat3);
        diff.mul(initialTrans.getRotation(), outRot);

        //  Scale doesn't change.
        outScale.set(initialTrans.getScale());

        vars.release();
    }

    Quaternionf getInitialRotation() {
        return initialRotation;
    }

    Vector3f getInitialTranslation() {
        return initialTranslation;
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
