package pl.pateman.core.physics.debug;

import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.linearmath.DebugDrawModes;
import com.bulletphysics.linearmath.IDebugDraw;
import org.joml.Vector4f;
import pl.pateman.core.Clearable;
import pl.pateman.core.TempVars;
import pl.pateman.core.entity.CameraEntity;
import pl.pateman.core.entity.line.LineRenderer;

import javax.vecmath.Vector3f;

/**
 * Created by pateman.
 */
public class PhysicsDebugger extends IDebugDraw implements Clearable {
    private int debugMode;
    private final LineRenderer lineRenderer;
    private final DynamicsWorld dynamicsWorld;

    public PhysicsDebugger(final DynamicsWorld dynamicsWorld) {
        this.lineRenderer = new LineRenderer();
        this.debugMode = DebugDrawModes.DRAW_WIREFRAME | DebugDrawModes.DRAW_AABB;

        this.dynamicsWorld = dynamicsWorld;
        if (this.dynamicsWorld == null) {
            throw new IllegalArgumentException();
        }
        this.dynamicsWorld.setDebugDrawer(this);
    }

    @Override
    public void drawLine(Vector3f from, Vector3f to, Vector3f color) {
        final TempVars tempVars = TempVars.get();

        final org.joml.Vector3f lineFrom = tempVars.vect3d1.set(from.x, from.y, from.z);
        final org.joml.Vector3f lineTo = tempVars.vect3d2.set(to.x, to.y, to.z);
        final Vector4f lineColor = tempVars.vect4d.set(color.x, color.y, color.z, 1.0f);

        this.lineRenderer.addLine(lineFrom, lineTo, lineColor);

        tempVars.release();
    }

    @Override
    public void drawContactPoint(Vector3f PointOnB, Vector3f normalOnB, float distance, int lifeTime, Vector3f color) {

    }

    @Override
    public void reportErrorWarning(String warningString) {

    }

    @Override
    public void draw3dText(Vector3f location, String textString) {

    }

    @Override
    public final void setDebugMode(int debugMode) {
        this.debugMode = debugMode;
    }

    @Override
    public final int getDebugMode() {
        return this.debugMode;
    }

    public void debugDrawWorld(final CameraEntity cameraEntity) {
        this.dynamicsWorld.debugDrawWorld();
        this.lineRenderer.drawLines(cameraEntity);
        this.lineRenderer.clearLines();
    }

    @Override
    public void clear() {
        this.lineRenderer.clearAndDestroy();
    }

    @Override
    public void clearAndDestroy() {
        this.clear();
    }
}
