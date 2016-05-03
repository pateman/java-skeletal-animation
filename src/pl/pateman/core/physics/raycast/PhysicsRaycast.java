package pl.pateman.core.physics.raycast;

import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.dynamics.DynamicsWorld;
import pl.pateman.core.TempVars;
import pl.pateman.core.entity.AbstractEntity;

import javax.vecmath.Vector3f;

/**
 * Created by pateman.
 */
public final class PhysicsRaycast {
    public static final float DEFAULT_RAY_LENGTH = 1000.0f;
    private final DynamicsWorld dynamicsWorld;

    public PhysicsRaycast(final DynamicsWorld dynamicsWorld) {
        this.dynamicsWorld = dynamicsWorld;
    }

    public PhysicsRaycastResult raycast(final org.joml.Vector3f rayOrigin, final org.joml.Vector3f rayDirection,
                                        float rayLength) {
        final TempVars tempVars = TempVars.get();

        //  Calculate the end part of the ray.
        final org.joml.Vector3f directionScaled = rayDirection.mul(rayLength, tempVars.vect3d1);
        final org.joml.Vector3f rayTo = rayOrigin.add(directionScaled, tempVars.vect3d2);

        tempVars.vecmathVect3d1.set(rayOrigin.x, rayOrigin.y, rayOrigin.z);
        tempVars.vecmathVect3d2.set(rayTo.x, rayTo.y, rayTo.z);

        //  Perform the actual raycasting.
        final ClosestRayResultCallbackEx resultCallbackEx = new ClosestRayResultCallbackEx(tempVars.vecmathVect3d1,
                tempVars.vecmathVect3d2);
        this.dynamicsWorld.rayTest(tempVars.vecmathVect3d1, tempVars.vecmathVect3d2, resultCallbackEx);
        tempVars.release();

        return resultCallbackEx.getResult();
    }

    private class ClosestRayResultCallbackEx extends CollisionWorld.ClosestRayResultCallback {
        private CollisionWorld.LocalRayResult rayResult;

        ClosestRayResultCallbackEx(Vector3f rayFromWorld, Vector3f rayToWorld) {
            super(rayFromWorld, rayToWorld);
        }

        @Override
        public float addSingleResult(CollisionWorld.LocalRayResult rayResult, boolean normalInWorldSpace) {
            final float result = super.addSingleResult(rayResult, normalInWorldSpace);

            this.rayResult = rayResult;
            return result;
        }

        PhysicsRaycastResult getResult() {
            if (!this.hasHit() || this.rayResult == null) {
                return null;
            }

            int shapePart = -1, triangleIndex = -1;
            if (this.rayResult.localShapeInfo != null) {
                shapePart = this.rayResult.localShapeInfo.shapePart;
                triangleIndex = this.rayResult.localShapeInfo.triangleIndex;
            }

            final PhysicsRaycastResult raycastResult = new PhysicsRaycastResult(this.hitNormalWorld, this.hitPointWorld,
                    (AbstractEntity) this.rayResult.collisionObject.getUserPointer(), shapePart, triangleIndex);
            return raycastResult;
        }
    }

}
