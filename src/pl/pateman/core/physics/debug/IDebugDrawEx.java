package pl.pateman.core.physics.debug;

import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.linearmath.IDebugDraw;
import com.bulletphysics.linearmath.Transform;

import javax.vecmath.Vector3f;

/**
 * Created by Patryk on 06.05.2016.
 */
public abstract class IDebugDrawEx extends IDebugDraw {
    public abstract void debugDrawObject(Transform worldTransform, CollisionShape shape, Vector3f color);

    @Override
    public void drawAabb(Vector3f from, Vector3f to, Vector3f color) {
        color.set(255.0f, 197.0f, 61.f);
        super.drawAabb(from, to, color);
    }
}
