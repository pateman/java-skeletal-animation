package pl.pateman.skeletal.entity;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.pateman.skeletal.Clearable;
import pl.pateman.skeletal.Utils;

/**
 * Created by pateman.
 */
public class AbstractEntity implements Clearable {
    private final Vector3f translation;
    private final Quaternionf rotation;
    private final Vector3f scale;

    private final Matrix4f transformation;

    public AbstractEntity() {
        this.translation = new Vector3f();
        this.rotation = new Quaternionf();
        this.scale = new Vector3f().set(1.0f, 1.0f, 1.0f);

        this.transformation = new Matrix4f();
    }

    protected void updateTransformationMatrix() {
        Utils.fromRotationTranslationScale(this.transformation, this.rotation, this.translation, this.scale);
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
    }

    public final void forceTransformationUpdate() {
        this.updateTransformationMatrix();
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
        Matrix4f rotationPart = new Matrix4f(this.transformation).setTranslation(0.0f, 0.0f, 0.0f);
        rotationPart.m33 = 1.0f;
        final Vector3f direction = new Vector3f(0.0f, 0.0f, -1.0f).mul(rotationPart.get3x3(new Matrix3f()));
        direction.normalize();

        return direction;
    }

    @Override
    public void clear() {

    }

    @Override
    public void clearAndDestroy() {

    }
}
