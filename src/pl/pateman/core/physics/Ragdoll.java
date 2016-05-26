package pl.pateman.core.physics;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.DefaultMotionState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import pl.pateman.core.TempVars;
import pl.pateman.core.Utils;
import pl.pateman.core.entity.CameraEntity;
import pl.pateman.core.entity.line.Line;
import pl.pateman.core.entity.line.LineRenderer;
import pl.pateman.core.mesh.Bone;
import pl.pateman.core.mesh.Mesh;

import java.util.*;

/**
 * Created by pateman.
 */
public final class Ragdoll {
    public static final Vector4f RAGDOLL_DEBUG_BONE_COLOR = new Vector4f(0.0f, 1.0f, 0.0f, 1.0f); 

    private final Mesh mesh;
    private DiscreteDynamicsWorld dynamicsWorld;
    private boolean enabled;
    private final LineRenderer lineRenderer;
    private final Map<RagdollBodyPart, Bone> ragdollBones;

    public Ragdoll(Mesh mesh) {
        if (mesh == null) {
            throw new IllegalArgumentException();
        }
        this.mesh = mesh;
        this.enabled = false;
        this.lineRenderer = new LineRenderer();

        //  Initialize the bones map.
        final RagdollBodyPart[] ragdollBodyParts = RagdollBodyPart.values();
        this.ragdollBones = new HashMap<>(ragdollBodyParts.length);
        for (RagdollBodyPart bodyPart : ragdollBodyParts) {
            this.ragdollBones.put(bodyPart, null);
        }
    }

    private RigidBody createRigidBody(float mass, final CollisionShape collisionShape, final Matrix4f initialTransform) {
        final TempVars vars = TempVars.get();

        //  Calculate the local inertia and convert the initial transformation matrix to JBullet.
        collisionShape.calculateLocalInertia(mass, vars.vecmathVect3d1);
        Utils.matrixToTransform(vars.vecmathTransform, initialTransform);

        final RigidBody body = new RigidBody(mass, new DefaultMotionState(vars.vecmathTransform), collisionShape,
                vars.vecmathVect3d1);
        vars.release();
        return body;
    }

    private BoxShape createBoxShapeForBone(final Bone a) {
        //  Get all vertices that are influenced by the given bone.
        final Collection<Vector3f> influencingVerticesForBone = RagdollUtils.getInfluencingVerticesForBone(a, this.mesh);
        final RagdollUtils.SimpleAABB aabb = new RagdollUtils.SimpleAABB();
        final TempVars tempVars = TempVars.get();

        //  Calculate an AABB of the influencing vertices.
        for (Vector3f boneVertex : influencingVerticesForBone) {
            aabb.expand(boneVertex);
        }

        //  Create the shape.
        final Vector3f extents = aabb.getExtents(tempVars.vect3d1).mul(0.5f);
        final BoxShape boxShape = new BoxShape(Utils.convert(tempVars.vecmathVect3d1, extents));

        tempVars.release();
        return boxShape;
    }

    private void createBonesConnection(final Bone a, final Bone b, RagdollUtils.RagdollBodyPartCollider partCollider) {
        final TempVars vars = TempVars.get();        
        if (partCollider.equals(RagdollUtils.RagdollBodyPartCollider.BOX)) {
            //  First bone.
            BoxShape boxShape = this.createBoxShapeForBone(a);
            RagdollUtils.getMatrixForBone(vars.tempMat4x41, a);
            final RigidBody rigidBodyA = this.createRigidBody(1.0f, boxShape, vars.tempMat4x41);

            this.dynamicsWorld.addRigidBody(rigidBodyA);
        }

        //  Add the connection to the debug drawer.
        a.getWorldBindMatrix().getTranslation(vars.vect3d1);
        b.getWorldBindMatrix().getTranslation(vars.vect3d2);
        this.lineRenderer.addLine(vars.vect3d1, vars.vect3d2, RAGDOLL_DEBUG_BONE_COLOR);
        
        vars.release();
    }

    public void setRagdollBone(final RagdollBodyPart bodyPart, final Bone bone) {
        this.ragdollBones.put(bodyPart, bone);
    }

    public void setRagdollBone(final RagdollBodyPart bodyPart, final String boneName) {
        final Bone bone = this.mesh.getSkeleton().getBoneByName(boneName);
        if (bone == null) {
            throw new IllegalArgumentException("Invalid bone name '" + boneName + "'");
        }
        this.setRagdollBone(bodyPart, bone);
    }

    public void buildRagdoll() {
        if (this.dynamicsWorld == null) {
            throw new IllegalStateException("A valid physics world needs to be assigned");
        }
        for (Map.Entry<RagdollBodyPart, Bone> entry : this.ragdollBones.entrySet()) {
            if (entry.getValue() == null) {
                throw new IllegalStateException("Missing bone for ragdoll part '" + entry.getKey() + "'");
            }
        }

        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.HEAD),
                this.ragdollBones.get(RagdollBodyPart.CHEST), RagdollUtils.RagdollBodyPartCollider.BOX);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.CHEST),
                this.ragdollBones.get(RagdollBodyPart.LEFT_UPPER_ARM), RagdollUtils.RagdollBodyPartCollider.CAPSULE);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.CHEST),
                this.ragdollBones.get(RagdollBodyPart.RIGHT_UPPER_ARM), RagdollUtils.RagdollBodyPartCollider.CAPSULE);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.LEFT_UPPER_ARM),
                this.ragdollBones.get(RagdollBodyPart.LEFT_ELBOW), RagdollUtils.RagdollBodyPartCollider.CAPSULE);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.LEFT_ELBOW),
                this.ragdollBones.get(RagdollBodyPart.LEFT_LOWER_ARM), RagdollUtils.RagdollBodyPartCollider.CAPSULE);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.RIGHT_UPPER_ARM),
                this.ragdollBones.get(RagdollBodyPart.RIGHT_ELBOW), RagdollUtils.RagdollBodyPartCollider.CAPSULE);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.RIGHT_ELBOW),
                this.ragdollBones.get(RagdollBodyPart.RIGHT_LOWER_ARM), RagdollUtils.RagdollBodyPartCollider.CAPSULE);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.CHEST),
                this.ragdollBones.get(RagdollBodyPart.HIPS), RagdollUtils.RagdollBodyPartCollider.CAPSULE);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.HIPS),
                this.ragdollBones.get(RagdollBodyPart.LEFT_UPPER_LEG), RagdollUtils.RagdollBodyPartCollider.CAPSULE);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.HIPS),
                this.ragdollBones.get(RagdollBodyPart.RIGHT_UPPER_LEG), RagdollUtils.RagdollBodyPartCollider.CAPSULE);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.LEFT_UPPER_LEG),
                this.ragdollBones.get(RagdollBodyPart.LEFT_KNEE), RagdollUtils.RagdollBodyPartCollider.CAPSULE);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.LEFT_KNEE),
                this.ragdollBones.get(RagdollBodyPart.LEFT_LOWER_LEG), RagdollUtils.RagdollBodyPartCollider.CAPSULE);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.RIGHT_UPPER_LEG),
                this.ragdollBones.get(RagdollBodyPart.RIGHT_KNEE), RagdollUtils.RagdollBodyPartCollider.CAPSULE);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.RIGHT_KNEE),
                this.ragdollBones.get(RagdollBodyPart.RIGHT_LOWER_LEG), RagdollUtils.RagdollBodyPartCollider.CAPSULE);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setDynamicsWorld(DiscreteDynamicsWorld dynamicsWorld) {
        this.dynamicsWorld = dynamicsWorld;
    }

    public void drawRagdollLines(final CameraEntity cameraEntity, final Vector3f translation) {
        for (final Line line : this.lineRenderer) {
            line.getTo().add(translation);
            line.getFrom().add(translation);
        }
        this.lineRenderer.drawLines(cameraEntity);
    }

    public enum RagdollBodyPart {
        HEAD,
        LEFT_UPPER_ARM,
        LEFT_ELBOW,
        RIGHT_UPPER_ARM,
        RIGHT_ELBOW,
        CHEST,
        LEFT_LOWER_ARM,
        RIGHT_LOWER_ARM,
        HIPS,
        LEFT_UPPER_LEG,
        LEFT_KNEE,
        RIGHT_UPPER_LEG,
        RIGHT_KNEE,
        LEFT_LOWER_LEG,
        RIGHT_LOWER_LEG
    }
}
