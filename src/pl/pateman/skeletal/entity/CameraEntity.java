package pl.pateman.skeletal.entity;

import org.joml.Matrix4f;
import org.joml.Vector3f;
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

    public void updateViewMatrix() {
        final Vector3f cameraPosition = this.getTranslation();

        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);

        Vector3f target = new Vector3f();
        cameraPosition.add(this.getDirection().negate(), target);

        Vector3f z = new Vector3f();
        cameraPosition.sub(target, z);
        z.normalize();

        Vector3f x = new Vector3f();
        up.cross(z, x);
        x.normalize();

        Vector3f y = new Vector3f();
        z.cross(x, y);
        y.normalize();

        final Matrix4f tmp = new Matrix4f();
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
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f(viewMatrix);
    }

    public Matrix4f getProjectionMatrix() {
        return new Matrix4f(projectionMatrix);
    }

    public CameraProjection getCameraProjection() {
        return cameraProjection;
    }
}
