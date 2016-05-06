package pl.pateman.core.physics;

import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.Dispatcher;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.linearmath.Transform;
import pl.pateman.core.physics.debug.IDebugDrawEx;

import javax.vecmath.Vector3f;

/**
 * Simple extension of the {@code DiscreteDynamicsWorld} class which allows for debugging the world.
 *
 * Created by Patryk on 06.05.2016.
 */
public class DiscreteDynamicsWorldEx extends DiscreteDynamicsWorld {
    public DiscreteDynamicsWorldEx(Dispatcher dispatcher, BroadphaseInterface pairCache,
                                   ConstraintSolver constraintSolver, CollisionConfiguration collisionConfiguration) {
        super(dispatcher, pairCache, constraintSolver, collisionConfiguration);
    }

    @Override
    public void debugDrawObject(Transform worldTransform, CollisionShape shape, Vector3f color) {
        if (this.getDebugDrawer() != null && this.getDebugDrawer() instanceof IDebugDrawEx) {
            ((IDebugDrawEx) this.getDebugDrawer()).debugDrawObject(worldTransform, shape, color);
        }
    }
}
