package pl.pateman.core.physics.ragdoll;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import pl.pateman.core.Clearable;
import pl.pateman.core.TempVars;
import pl.pateman.core.entity.CameraEntity;
import pl.pateman.core.point.Point3D;
import pl.pateman.core.point.Point3DRenderer;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;

/**
 * Created by pateman.
 */
public final class RagdollDebugger implements Clearable {
    private final Point3DRenderer point3DRenderer;
    private final Ragdoll ragdoll;
    private final Vector4f ragdollDebugBoneColor;
    private final Map<Integer, Integer> boneToPointMap;

    public RagdollDebugger(Ragdoll ragdoll) {
        if (ragdoll == null) {
            throw new IllegalArgumentException("A valid ragdoll needs to be provided");
        }

        this.ragdoll = ragdoll;
        this.point3DRenderer = new Point3DRenderer();
        this.ragdollDebugBoneColor = new Vector4f(0.0f, 1.0f, 0.0f, 1.0f);
        this.boneToPointMap = new HashMap<>();
    }

    public void buildDebugInfo() {
        final TempVars vars = TempVars.get();

        for (Map.Entry<Integer, Matrix4f> entry : this.ragdoll.getBoneMatrices().entrySet()) {
            final int pointIdx = this.point3DRenderer.addPoint(entry.getValue().getTranslation(vars.vect3d1),
                    this.ragdollDebugBoneColor, 5.0f);

            //  Bone index -> point index.
            this.boneToPointMap.put(entry.getKey(), pointIdx);
        }

        vars.release();
    }

    public void updateDebug() {
        final TempVars vars = TempVars.get();

        final Matrix4f invEntityTM = this.ragdoll.getEntity().getTransformation().invert(vars.tempMat4x41);
        for (Map.Entry<Integer, Matrix4f> entry : this.ragdoll.getBoneMatrices().entrySet()) {
            final Point3D point = this.point3DRenderer.getPoint(this.boneToPointMap.get(entry.getKey()));
            invEntityTM.mul(entry.getValue(), vars.tempMat4x42).getTranslation(point.getPosition());
        }

        vars.release();
        this.point3DRenderer.forceDirty();
    }

    public void drawDebug(final CameraEntity cameraEntity) {
        if (cameraEntity == null) {
            throw new IllegalArgumentException("A valid camera needs to be provided");
        }

        glClear(GL_DEPTH_BUFFER_BIT);
        this.point3DRenderer.drawPoints(cameraEntity);
    }

    public Vector4f getRagdollDebugBoneColor() {
        return ragdollDebugBoneColor;
    }

    @Override
    public void clear() {
        this.point3DRenderer.clearAndDestroy();
    }

    @Override
    public void clearAndDestroy() {
        this.clear();
    }
}
