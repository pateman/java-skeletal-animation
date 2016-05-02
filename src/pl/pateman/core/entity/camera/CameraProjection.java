package pl.pateman.core.entity.camera;

import pl.pateman.core.entity.CameraEntity;

/**
 * Created by pateman on 2015-08-01.
 */
public final class CameraProjection {
    private final CameraEntity owner;
    private int viewportWidth;
    private int viewportHeight;
    private float fieldOfView;
    private float farPlane;
    private float nearPlane;
    private float aspectRatio;

    private void notifyOwner() {
        if (this.owner != null) {
            this.owner.updateProjectionMatrix();
        }
    }

    private void calculateAspectRatio() {
        this.aspectRatio = ((float) this.viewportWidth) / ((float) this.viewportHeight);
    }

    public CameraProjection(final CameraEntity camera, int width, int height, float fov, float near, float far) {
        this.owner = camera;

        this.viewportWidth = width;
        this.viewportHeight = (height == 0) ? 1 : height;
        this.calculateAspectRatio();
        this.fieldOfView = fov;
        this.farPlane = far;
        this.nearPlane = near;
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public void setViewportWidth(int viewportWidth) {
        this.viewportWidth = viewportWidth;
        this.calculateAspectRatio();
        this.notifyOwner();
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    public void setViewportHeight(int viewportHeight) {
        this.viewportHeight = viewportHeight;
        if (this.viewportHeight == 0) {
            this.viewportHeight = 1;
        }
        this.calculateAspectRatio();
        this.notifyOwner();
    }

    public void setViewport(int w, int h) {
        this.viewportWidth = w;
        this.viewportHeight = h;
        if (this.viewportHeight == 0) {
            this.viewportHeight = 1;
        }
        this.calculateAspectRatio();
        this.notifyOwner();
    }

    public float getFieldOfView() {
        return fieldOfView;
    }

    public void setFieldOfView(float fieldOfView) {
        this.fieldOfView = fieldOfView;
        this.notifyOwner();
    }

    public float getFarPlane() {
        return farPlane;
    }

    public void setFarPlane(float farPlane) {
        this.farPlane = farPlane;
        this.notifyOwner();
    }

    public float getNearPlane() {
        return nearPlane;
    }

    public void setNearPlane(float nearPlane) {
        this.nearPlane = nearPlane;
        this.notifyOwner();
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    CameraEntity getOwner() {
        return owner;
    }
}
