package pl.pateman.core.physics.raycast;

import pl.pateman.core.entity.AbstractEntity;

import javax.vecmath.Vector3f;

/**
 * Created by pateman.
 */
public final class PhysicsRaycastResult {
    private final org.joml.Vector3f normal;
    private final org.joml.Vector3f hit;
    private final AbstractEntity entity;
    private final int shapePart;
    private final int triangleIndex;

    PhysicsRaycastResult(Vector3f normal, Vector3f hit, AbstractEntity entity, int shapePart, int triangleIndex) {
        this.normal = new org.joml.Vector3f(normal.x, normal.y, normal.z);
        this.hit = new org.joml.Vector3f(hit.x, hit.y, hit.z);
        this.entity = entity;
        this.shapePart = shapePart;
        this.triangleIndex = triangleIndex;
    }

    public org.joml.Vector3f getNormal() {
        return normal;
    }

    public org.joml.Vector3f getHit() {
        return hit;
    }

    public AbstractEntity getEntity() {
        return entity;
    }

    public int getShapePart() {
        return shapePart;
    }

    public int getTriangleIndex() {
        return triangleIndex;
    }
}
