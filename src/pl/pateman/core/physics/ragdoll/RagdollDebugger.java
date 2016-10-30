package pl.pateman.core.physics.ragdoll;

import org.joml.Vector4f;
import pl.pateman.core.Clearable;
import pl.pateman.core.TempVars;
import pl.pateman.core.entity.CameraEntity;
import pl.pateman.core.line.LineRenderer;
import pl.pateman.core.point.Point3DRenderer;

import java.util.Map;

/**
 * Created by pateman.
 */
public final class RagdollDebugger implements Clearable {
    private final LineRenderer lineRenderer;
    private final Point3DRenderer point3DRenderer;
    private final Ragdoll ragdoll;
    private final Vector4f ragdollDebugBoneColor;

    public RagdollDebugger(Ragdoll ragdoll) {
        if (ragdoll == null) {
            throw new IllegalArgumentException("A valid ragdoll needs to be provided");
        }

        this.ragdoll = ragdoll;
        this.point3DRenderer = new Point3DRenderer();
        this.lineRenderer = new LineRenderer();
        this.ragdollDebugBoneColor = new Vector4f(0.0f, 1.0f, 0.0f, 1.0f);
    }

    public void buildDebugInfo() {
        final TempVars vars = TempVars.get();

        for (Map.Entry<BodyPartType, BodyPart> entry : this.ragdoll.getRagdollStructure().getBodyParts().entrySet()) {
            final BodyPart bodyPart = entry.getValue();
            if (bodyPart.isConfigured()) {
                //  Draw debug lines between the first and the last bone.
                bodyPart.getFirstBone().getWorldBindMatrix().getTranslation(vars.vect3d1);
                bodyPart.getLastBone().getWorldBindMatrix().getTranslation(vars.vect3d2);

                this.lineRenderer.addLine(vars.vect3d1, vars.vect3d2, this.ragdollDebugBoneColor);
            }
        }

        vars.release();
    }

    public void drawDebug(final CameraEntity cameraEntity) {
        if (cameraEntity == null) {
            throw new IllegalArgumentException("A valid camera needs to be provided");
        }

        this.lineRenderer.drawLines(cameraEntity);
    }

    public Vector4f getRagdollDebugBoneColor() {
        return ragdollDebugBoneColor;
    }

    @Override
    public void clear() {
        this.lineRenderer.clearAndDestroy();
        this.point3DRenderer.clearAndDestroy();
    }

    @Override
    public void clearAndDestroy() {
        this.clear();
    }
}
