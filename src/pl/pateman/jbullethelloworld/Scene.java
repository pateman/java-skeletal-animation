package pl.pateman.jbullethelloworld;

import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.Transform;
import pl.pateman.core.TempVars;
import pl.pateman.core.entity.AbstractEntity;

import javax.vecmath.Vector3f;
import java.util.*;

/**
 * Created by pateman.
 */
public final class Scene implements Iterable<AbstractEntity> {
    public static final Vector3f DEFAULT_GRAVITY = new Vector3f(0.0f, -9.81f, 0.0f);
    private final Map<String, AbstractEntity> entities;
    private final Map<String, Map<String, Object>> parameters;
    private final List<String> physicsBodies;

    private final DynamicsWorld dynamicsWorld;

    public Scene() {
        this.entities = new HashMap<>();
        this.parameters = new HashMap<>();
        this.physicsBodies = new ArrayList<>();

        final DbvtBroadphase broadphase = new DbvtBroadphase();
        final DefaultCollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();
        final CollisionDispatcher collisionDispatcher = new CollisionDispatcher(collisionConfiguration);
        final SequentialImpulseConstraintSolver constraintSolver = new SequentialImpulseConstraintSolver();

        this.dynamicsWorld = new DiscreteDynamicsWorld(collisionDispatcher, broadphase, constraintSolver,
                collisionConfiguration);
        this.dynamicsWorld.setGravity(DEFAULT_GRAVITY);
    }

    public <T extends AbstractEntity> T addEntity(final T entityInstance) {
        final String name = entityInstance.getName();

        this.entities.put(name, entityInstance);
        this.parameters.put(name, new HashMap<String, Object>());

        return entityInstance;
    }

    public void addEntityToPhysicsWorld(final String name) {
        final AbstractEntity entity = this.entities.get(name);
        this.physicsBodies.add(name);

        entity.forceTransformationUpdate(true);
        this.dynamicsWorld.addRigidBody(entity.getRigidBody());
    }

    public <T extends AbstractEntity> T getEntity(final String name) {
        return (T) this.entities.get(name);
    }

    public <T> T getEntityParameter(final String entity, final String parameter) {
        return (T) this.parameters.get(entity).get(parameter);
    }

    public void updateScene(float deltaTime) {
        this.dynamicsWorld.stepSimulation(deltaTime);

        final TempVars tempVars = TempVars.get();
        final Transform transform = tempVars.vecmathTransform;
        for (final String physicsBody : this.physicsBodies) {
            final AbstractEntity abstractEntity = this.entities.get(physicsBody);
            abstractEntity.getRigidBody().getWorldTransform(transform);

            //  Convert between different math libraries.
            transform.getRotation(tempVars.vecmathQuat);
            tempVars.quat1.set(tempVars.vecmathQuat.x, tempVars.vecmathQuat.y, tempVars.vecmathQuat.z,
                    tempVars.vecmathQuat.w);
            tempVars.vect3d1.set(transform.origin.x, transform.origin.y, transform.origin.z);

            //  Assign transformation computed by jBullet to the entity.
            abstractEntity.setTransformation(tempVars.quat1, tempVars.vect3d1, abstractEntity.getScale());
            abstractEntity.forceTransformationUpdate(false);
        }
        tempVars.release();
    }

    public org.joml.Vector3f getGravity() {
        final Vector3f gravity = this.dynamicsWorld.getGravity(new Vector3f());
        return new org.joml.Vector3f(gravity.x, gravity.y, gravity.z);
    }

    public void setGravity(org.joml.Vector3f gravity) {
        this.dynamicsWorld.setGravity(new Vector3f(gravity.x, gravity.y, gravity.z));
    }

    public void setEntityParameter(final String entity, final String parameterName, final Object value) {
        this.parameters.get(entity).put(parameterName, value);
    }

    @Override
    public Iterator<AbstractEntity> iterator() {
        return this.entities.values().iterator();
    }
}
