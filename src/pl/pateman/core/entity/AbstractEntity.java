package pl.pateman.core.entity;

import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.DefaultMotionState;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.pateman.core.Clearable;
import pl.pateman.core.TempVars;
import pl.pateman.core.Utils;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by pateman.
 */
public class AbstractEntity implements Clearable {
    private static final AtomicLong ID_GENERATOR = new AtomicLong(0L);

    public static final short COLLISION_GROUP_NONE = 0x00000000;
    public static final short COLLISION_GROUP_01 = 0x00000001;
    public static final short COLLISION_GROUP_02 = 0x00000002;
    public static final short COLLISION_GROUP_03 = 0x00000004;
    public static final short COLLISION_GROUP_04 = 0x00000008;
    public static final short COLLISION_GROUP_05 = 0x00000010;
    public static final short COLLISION_GROUP_06 = 0x00000020;
    public static final short COLLISION_GROUP_07 = 0x00000040;
    public static final short COLLISION_GROUP_08 = 0x00000080;
    public static final short COLLISION_GROUP_09 = 0x00000100;
    public static final short COLLISION_GROUP_10 = 0x00000200;
    public static final short COLLISION_GROUP_11 = 0x00000400;
    public static final short COLLISION_GROUP_12 = 0x00000800;
    public static final short COLLISION_GROUP_13 = 0x00001000;
    public static final short COLLISION_GROUP_14 = 0x00002000;
    public static final short COLLISION_GROUP_15 = 0x00004000;

    private final Long entityId = ID_GENERATOR.getAndIncrement();
    private String name;

    private final Vector3f translation;
    private final Quaternionf rotation;
    private final Vector3f scale;
    private final Vector3f direction;
    private final Matrix4f transformation;

    private final RigidBody rigidBody;

    public AbstractEntity() {
        this(null);
    }

    public AbstractEntity(final String entityName) {
        this.translation = new Vector3f();
        this.rotation = new Quaternionf();
        this.scale = new Vector3f().set(1.0f, 1.0f, 1.0f);
        this.transformation = new Matrix4f();

        this.name = entityName == null ? "Entity " + this.entityId : entityName;

        this.direction = new Vector3f();
        this.updateDirection();

        this.rigidBody = new RigidBody(1.0f, new DefaultMotionState(), null);
        this.rigidBody.setUserPointer(this);
    }

    protected void updateTransformationMatrix() {
        this.updateTransformationMatrix(true);
    }

    private void updateTransformationMatrix(boolean updateRigidBody) {
        Utils.fromRotationTranslationScale(this.transformation, this.rotation, this.translation, this.scale);
        this.updateDirection();

        //  Update the rigid body's transformation.
        if (updateRigidBody) {
            final TempVars vars = TempVars.get();
            Utils.matrixToTransform(vars.vecmathTransform, this.transformation);
            this.rigidBody.setWorldTransform(vars.vecmathTransform);
            vars.release();
        }
    }

    protected void updateDirection() {
        final TempVars vars = TempVars.get();

        final Matrix4f rotationPart = vars.tempMat4x41.set(this.transformation).setTranslation(0.0f, 0.0f, 0.0f);
        rotationPart.m33 = 1.0f;

        Utils.NEG_AXIS_Z.mul(rotationPart.get3x3(vars.tempMat3x3), this.direction);
        this.direction.normalize();

        vars.release();
    }

    public final void translate(final Vector3f offset) {
        this.translation.add(offset);
        this.updateTransformationMatrix();
    }

    public final void translate(float x, float y, float z) {
        this.translation.add(x, y, z);
        this.updateTransformationMatrix();
    }

    public final void rotate(final Quaternionf offset) {
        this.rotation.mul(offset);
        this.updateTransformationMatrix();
    }

    public final void rotate(float x, float y, float z) {
        this.rotation.rotate(x, y, z);
        this.updateTransformationMatrix();
    }

    public final void scale(final Vector3f offset) {
        this.scale.add(offset);
        this.updateTransformationMatrix();
    }

    public final void scale(float x, float y, float z) {
        this.scale.add(x, y, z);
        this.updateTransformationMatrix();
    }

    public final void transform(final Quaternionf rotation, final Vector3f translation, final Vector3f scale) {
        this.rotation.mul(rotation);
        this.translation.add(translation);
        this.scale(scale);
        this.updateTransformationMatrix();
    }

    public final void forceTransformationUpdate() {
        this.forceTransformationUpdate(true);
    }

    public final void forceTransformationUpdate(boolean updateRigidBody) {
        this.updateTransformationMatrix(updateRigidBody);
    }

    public Vector3f getTranslation() {
        return translation;
    }

    public void setTranslation(final Vector3f translation) {
        this.translation.set(translation);
        this.updateTransformationMatrix();
    }

    public Quaternionf getRotation() {
        return rotation;
    }

    public void setRotation(final Quaternionf rotation) {
        this.rotation.set(rotation);
        this.updateTransformationMatrix();
    }

    public Vector3f getScale() {
        return scale;
    }

    public void setScale(final Vector3f scale) {
        this.scale.set(scale);
        this.updateTransformationMatrix();
    }

    public Matrix4f getTransformation() {
        return transformation;
    }

    public void setTransformation(final Quaternionf rotation, final Vector3f translation, final Vector3f scale) {
        this.rotation.set(rotation);
        this.translation.set(translation);
        this.setScale(scale);
    }

    public Vector3f getDirection() {
        return this.direction;
    }

    public final Long getEntityId() {
        return entityId;
    }

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public RigidBody getRigidBody() {
        return rigidBody;
    }

    public int getCollisionGroup() {
        if (this.rigidBody.getBroadphaseHandle() == null) {
            return COLLISION_GROUP_NONE;
        }
        return this.rigidBody.getBroadphaseHandle().collisionFilterGroup;
    }

    public void setCollisionGroup(short collisionGroup) {
        if (this.rigidBody.getBroadphaseHandle() != null) {
            this.rigidBody.getBroadphaseHandle().collisionFilterGroup = collisionGroup;
        }
    }

    public int getCollisionMask() {
        if (this.rigidBody.getBroadphaseHandle() == null) {
            return COLLISION_GROUP_NONE;
        }
        return this.rigidBody.getBroadphaseHandle().collisionFilterMask;
    }

    public void setCollisionMask(short collisionMask) {
        if (this.rigidBody.getBroadphaseHandle() != null) {
            this.rigidBody.getBroadphaseHandle().collisionFilterMask = collisionMask;
        }
    }

    @Override
    public void clear() {

    }

    @Override
    public void clearAndDestroy() {

    }
}
