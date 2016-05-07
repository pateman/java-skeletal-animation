package pl.pateman.core.physics.debug;

import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.collision.shapes.CompoundShapeChild;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.linearmath.DebugDrawModes;
import com.bulletphysics.linearmath.Transform;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import pl.pateman.core.Clearable;
import pl.pateman.core.TempVars;
import pl.pateman.core.Utils;
import pl.pateman.core.entity.CameraEntity;
import pl.pateman.core.entity.line.LineRenderer;

import javax.vecmath.Vector3f;
import java.util.List;

/**
 * Created by pateman.
 */
public class PhysicsDebugger extends IDebugDrawEx implements Clearable {
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
        final Vector4f lineColor = tempVars.vect4d.set(color.x / 255.0f, color.y / 255.0f, color.z / 255.0f, 1.0f);

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

    @Override
    public void debugDrawObject(Transform worldTransform, CollisionShape shape, Vector3f color) {
        if (shape instanceof CompoundShape) {
            final CompoundShape compoundShape = (CompoundShape) shape;
            for (final CompoundShapeChild shapeChild : compoundShape.getChildList()) {
                this.debugDrawObject(shapeChild.transform, shapeChild.childShape, color);
            }
        } else {
            this.drawObject(worldTransform, shape, color);
        }
    }

    private void drawObject(Transform worldTransform, CollisionShape shape, Vector3f color) {
        final List<org.joml.Vector3f> meshVertices = PhysicsDebugMeshFactory.getMeshVertices(shape);
        final TempVars vars = TempVars.get();

        //  Get the transformation matrix and transform the vertices.
        final Matrix4f transformMatrix = vars.tempMat4x41;
        Utils.transformToMatrix(transformMatrix, worldTransform);

        org.joml.Vector3f a, b;
        for (int i = 0; i < meshVertices.size(); i += 2) {
            a = meshVertices.get(i);
            b = meshVertices.get(i + 1);

            final org.joml.Vector3f transformedVertexA = transformMatrix.transformPosition(a, vars.vect3d1);
            final org.joml.Vector3f transformedVertexB = transformMatrix.transformPosition(b, vars.vect3d2);

            //  Convert between libraries.
            vars.vecmathVect3d1.set(transformedVertexA.x, transformedVertexA.y, transformedVertexA.z);
            vars.vecmathVect3d2.set(transformedVertexB.x, transformedVertexB.y, transformedVertexB.z);

            //  Draw the line.
            this.drawLine(vars.vecmathVect3d1, vars.vecmathVect3d2, color);
        }

        vars.release();
    }
}
