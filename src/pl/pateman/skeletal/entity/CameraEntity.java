package pl.pateman.skeletal.entity;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import pl.pateman.skeletal.TempVars;
import pl.pateman.skeletal.Utils;
import pl.pateman.skeletal.entity.camera.CameraProjection;

/**
 * Created by pateman.
 */
public class CameraEntity extends AbstractEntity {
    private final CameraProjection cameraProjection;
    private final Matrix4f viewMatrix;
    private final Matrix4f projectionMatrix;

    public CameraEntity() {
        this.cameraProjection = new CameraProjection(this, 0, 0, 60.0f, 0.01f, 1000.0f);

        this.viewMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();

        this.updateViewMatrix();
        this.updateTransformationMatrix();
    }

    @Override
    protected void updateTransformationMatrix() {
        super.updateTransformationMatrix();
        this.updateViewMatrix();
    }

    public void updateProjectionMatrix() {
        if (this.cameraProjection.getViewportWidth() <= 0 || this.cameraProjection.getViewportHeight() <= 0) {
            this.projectionMatrix.zero();
            return;
        }

        this.projectionMatrix.setPerspective((float) Math.toRadians(this.cameraProjection.getFieldOfView()),
                this.cameraProjection.getAspectRatio(), this.cameraProjection.getNearPlane(),
                this.cameraProjection.getFarPlane());
    }

    private void updateViewMatrix() {
        final TempVars tempVars = TempVars.get();

        final Vector3f cameraPosition = this.getTranslation();
        final Vector3f up = tempVars.vect3d1.set(Utils.AXIS_Y);
        final Vector3f target = tempVars.vect3d2;
        final Vector3f z = tempVars.vect3d3;
        final Vector3f x = tempVars.vect3d4;
        final Vector3f y =  tempVars.vect3d5;
        final Matrix4f tmp = tempVars.tempMat4x41;

        cameraPosition.add(this.getDirection().negate(), target);
        cameraPosition.sub(target, z);
        z.normalize();

        up.cross(z, x);
        x.normalize();

        z.cross(x, y);
        y.normalize();

        tmp.m00 = x.x;
        tmp.m10 = x.y;
        tmp.m20 = x.z;
        tmp.m30 = -cameraPosition.dot(x);
        tmp.m01 = y.x;
        tmp.m11 = y.y;
        tmp.m21 = y.z;
        tmp.m31 = -cameraPosition.dot(y);
        tmp.m02 = z.x;
        tmp.m12 = z.y;
        tmp.m22 = z.z;
        tmp.m32 = -cameraPosition.dot(z);
        tmp.m03 = 0.0f;
        tmp.m13 = 0.0f;
        tmp.m23 = 0.0f;
        tmp.m33 = 1.0f;

        this.viewMatrix.set(tmp);
        tempVars.release();
    }

    public Matrix4f getViewMatrix() {
        return this.viewMatrix;
    }

    public Matrix4f getProjectionMatrix() {
        return this.projectionMatrix;
    }

    public CameraProjection getCameraProjection() {
        return cameraProjection;
    }
}
