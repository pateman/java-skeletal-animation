package pl.pateman.core.physics;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.DefaultMotionState;
import org.joml.*;
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
//    private final Map<String, RigidBody> parts;
    private final Map<RagdollBodyPart, Bone> ragdollBones;

    public Ragdoll(Mesh mesh) {
        if (mesh == null) {
            throw new IllegalArgumentException();
        }
        this.mesh = mesh;
        this.enabled = false;
        this.lineRenderer = new LineRenderer();
//        this.parts = new HashMap<>();

        //  Initialize the bones map.
        final RagdollBodyPart[] ragdollBodyParts = RagdollBodyPart.values();
        this.ragdollBones = new HashMap<>(ragdollBodyParts.length);
        for (RagdollBodyPart bodyPart : ragdollBodyParts) {
            this.ragdollBones.put(bodyPart, null);
        }
    }

    private RigidBody createRigidBody(float mass, final CollisionShape collisionShape, final Matrix4f initialTransform,
                                      final TempVars vars) {
        //  Calculate the local inertia and convert the initial transformation matrix to JBullet.
        collisionShape.calculateLocalInertia(mass, vars.vecmathVect3d1);
        Utils.matrixToTransform(vars.vecmathTransform, initialTransform);

        final RigidBody body = new RigidBody(mass, new DefaultMotionState(vars.vecmathTransform), collisionShape,
                vars.vecmathVect3d1);
        return body;
    }

    private List<Vector3f> getInfluencingVerticesForBone(final Bone bone) {
        final Set<Map.Entry<Integer, Float>> vertexWeights = bone.getVertexWeights().entrySet();
        final List<Vector3f> vertices = new ArrayList<>(vertexWeights.size());

        for (Map.Entry<Integer, Float> vertexWeight : vertexWeights) {
            if (vertexWeight.getValue() != 0.0f) {
                vertices.add(this.mesh.getVertices().get(vertexWeight.getKey()));
            }
        }

        return vertices;
    }

    private BoxShape createBoxShapeForBone(final Bone a) {
        final List<Vector3f> influencingVerticesForBone = this.getInfluencingVerticesForBone(a);
        final SimpleAABB aabb = new SimpleAABB();

        final TempVars tempVars = TempVars.get();

        for (Vector3f boneVertex : influencingVerticesForBone) {
            aabb.expand(a.getLocalBindMatrix().transformPosition(boneVertex, tempVars.vect3d1));
        }
        final Vector3f extents = aabb.getExtents(tempVars.vect3d1);
        final BoxShape boxShape = new BoxShape(Utils.convert(tempVars.vecmathVect3d1, extents));

        tempVars.release();
        return boxShape;
    }

    private Quaternionf rotationTo(Quaternionf out, Vector3f a, Vector3f b) {
        final float dot = a.dot(b);

        final TempVars tempVars = TempVars.get();
        final Vector3f xUnit = tempVars.vect3d1.set(1.0f, 0.0f, 0.0f);
        final Vector3f yUnit = tempVars.vect3d2.set(0.0f, 1.0f, 0.0f);
        final Vector3f tmp = tempVars.vect3d3;

        if (dot < -0.999999f) {
            a.cross(xUnit, tmp);
            if (tmp.length() < 0.000001f) {
                a.cross(yUnit, tmp);
            }
            tmp.normalize();
            out.setAngleAxis((float) Math.PI, tmp.x, tmp.y, tmp.z);
        } else if (dot > 0.999999f) {
            out.identity();
        } else {
            a.cross(b, tmp);
            out.set(tmp.x, tmp.y, tmp.z, 1.0f + dot).normalize();
        }

        tempVars.release();
        return out;
    }

    private void createBonesConnection(final Bone a, final Bone b, RagdollBodyPartCollider partCollider,
                                       final TempVars vars) {
        if (partCollider.equals(RagdollBodyPartCollider.BOX)) {
            final BoxShape boxShape = this.createBoxShapeForBone(a);
            boxShape.setLocalScaling(Utils.convert(vars.vecmathVect3d1, a.getBindScale()));

            final Vector3f aTrans = a.getWorldBindMatrix().getTranslation(vars.vect3d1);
            final Vector3f bTrans = b.getWorldBindMatrix().getTranslation(vars.vect3d2);

            final Quaternionf rotation = this.rotationTo(vars.quat1, bTrans, aTrans);
            final Vector3f position = bTrans.sub(aTrans, vars.vect3d3).mul(0.5f);
            a.getWorldBindMatrix().rotate(rotation, vars.tempMat4x41).translate(position);
//            final Quaternionf rotation = a.getWorldBindMatrix().getNormalizedRotation(vars.quat1);
//            Utils.fromRotationTranslationScale(vars.tempMat4x41, rotation, aTrans, a.getBindScale());

            final RigidBody rigidBody = this.createRigidBody(1.0f, boxShape, a.getWorldBindMatrix(), vars);
            this.dynamicsWorld.addRigidBody(rigidBody);
        }

        //  Add the connection to the debug drawer.
        a.getWorldBindMatrix().getTranslation(vars.vect3d1);
        b.getWorldBindMatrix().getTranslation(vars.vect3d2);
        this.lineRenderer.addLine(vars.vect3d1, vars.vect3d2, RAGDOLL_DEBUG_BONE_COLOR);
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

        final TempVars vars = TempVars.get();
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.HEAD),
                this.ragdollBones.get(RagdollBodyPart.CHEST), RagdollBodyPartCollider.BOX, vars);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.CHEST),
                this.ragdollBones.get(RagdollBodyPart.LEFT_UPPER_ARM), RagdollBodyPartCollider.BOX, vars);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.CHEST),
                this.ragdollBones.get(RagdollBodyPart.RIGHT_UPPER_ARM), RagdollBodyPartCollider.BOX, vars);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.LEFT_UPPER_ARM),
                this.ragdollBones.get(RagdollBodyPart.LEFT_ELBOW), RagdollBodyPartCollider.CAPSULE, vars);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.LEFT_ELBOW),
                this.ragdollBones.get(RagdollBodyPart.LEFT_LOWER_ARM), RagdollBodyPartCollider.CAPSULE, vars);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.RIGHT_UPPER_ARM),
                this.ragdollBones.get(RagdollBodyPart.RIGHT_ELBOW), RagdollBodyPartCollider.CAPSULE, vars);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.RIGHT_ELBOW),
                this.ragdollBones.get(RagdollBodyPart.RIGHT_LOWER_ARM), RagdollBodyPartCollider.CAPSULE, vars);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.CHEST),
                this.ragdollBones.get(RagdollBodyPart.HIPS), RagdollBodyPartCollider.CAPSULE, vars);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.HIPS),
                this.ragdollBones.get(RagdollBodyPart.LEFT_UPPER_LEG), RagdollBodyPartCollider.CAPSULE, vars);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.HIPS),
                this.ragdollBones.get(RagdollBodyPart.RIGHT_UPPER_LEG), RagdollBodyPartCollider.CAPSULE, vars);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.LEFT_UPPER_LEG),
                this.ragdollBones.get(RagdollBodyPart.LEFT_KNEE), RagdollBodyPartCollider.CAPSULE, vars);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.LEFT_KNEE),
                this.ragdollBones.get(RagdollBodyPart.LEFT_LOWER_LEG), RagdollBodyPartCollider.CAPSULE, vars);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.RIGHT_UPPER_LEG),
                this.ragdollBones.get(RagdollBodyPart.RIGHT_KNEE), RagdollBodyPartCollider.CAPSULE, vars);
        this.createBonesConnection(this.ragdollBones.get(RagdollBodyPart.RIGHT_KNEE),
                this.ragdollBones.get(RagdollBodyPart.RIGHT_LOWER_LEG), RagdollBodyPartCollider.CAPSULE, vars);

        vars.release();
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

//    public RigidBody addRagdollPart(final String name, final Matrix4f initialTransform,
//                                    final Vector3f offsetTranslation, final Quaternionf offsetRotation,
//                                    final Vector3f offsetScale, String... boneNames) {
//        //  Check prerequisites before proceeding.
//        if (this.dynamicsWorld == null) {
//            throw new IllegalStateException("A valid physics world needs to be assigned");
//        }
//
//        if (name == null || name.isEmpty()) {
//            throw new IllegalArgumentException("A valid part name needs to be specified");
//        }
//
//        if (initialTransform == null || offsetTranslation == null || offsetRotation == null || offsetScale == null) {
//            throw new IllegalArgumentException("Valid transformation for the ragdoll part needs to be specified");
//        }
//
//        if (boneNames.length == 0) {
//            throw new IllegalArgumentException("At least one bone must be specified");
//        }
//
//        //  Calculate the transformation matrix that will be used to transform vertices influenced by the provided
//        //  bones.
//        final TempVars vars = TempVars.get();
//
//        final Matrix4f vertexTransformMatrix = vars.tempMat4x41;
//        Utils.fromRotationTranslationScale(vertexTransformMatrix, offsetRotation, offsetTranslation, offsetScale);
//
//        final ObjectArrayList<javax.vecmath.Vector3f> vertices = new ObjectArrayList<>();
//        for (final String boneName : boneNames) {
//            final Bone bone = this.mesh.getSkeleton().getBoneByName(boneName);
//            if (bone == null) {
//                throw new IllegalStateException("Invalid bone name " + boneName);
//            }
//
//            //  Iterate over the vertices which are influenced by this bone and transform them using the matrix
//            //  that we have just computed.
//            final List<Integer> vertexIndices = new ArrayList<>(bone.getVertexWeights().keySet());
//            Collections.sort(vertexIndices);
//            for (final Integer vertexIndex : vertexIndices) {
//                final Vector3f vertex = vars.vect3d1.set(this.mesh.getVertices().get(vertexIndex));
//                vertexTransformMatrix.transformPosition(vertex);
//
//                //  Add the transformed vertex to the list of vertices.
//                vertices.add(new javax.vecmath.Vector3f(vertex.x, vertex.y, vertex.z));
//            }
//        }
//
//        if (vertices.isEmpty()) {
//            throw new IllegalStateException("The given bones do not influence any vertex");
//        }
//
//        //  Create the collider using the list of vertices that we have just prepared.
//        final ConvexHullShape hullShape = new ConvexHullShape(vertices);
//        hullShape.calculateLocalInertia(1.0f, vars.vecmathVect3d1);
//
//        //  Finally, create a rigidbody for this ragdoll part. Use the provided initial transform matrix, too.
//        Utils.matrixToTransform(vars.vecmathTransform, initialTransform);
//        final DefaultMotionState motionState = new DefaultMotionState(vars.vecmathTransform);
//        final RigidBodyConstructionInfo rigidBodyConstructionInfo = new RigidBodyConstructionInfo(1.0f, motionState, hullShape, vars.vecmathVect3d1);
//        final RigidBody rigidBody = new RigidBody(rigidBodyConstructionInfo);
//
//        this.dynamicsWorld.addRigidBody(rigidBody);
//        this.parts.put(name, rigidBody);
//
//        vars.release();
//        return rigidBody;
//    }
//
//    public RigidBody addRagdollPart(final String name, boolean useBox, final Vector3f dimensions,
//                                    final Vector3f offset, final Matrix4f initialTransform) {
//        final TempVars vars = TempVars.get();
//
//        final CollisionShape collisionShape = useBox ?
//                new BoxShape(new javax.vecmath.Vector3f(dimensions.x * 0.5f, dimensions.y * 0.5f, dimensions.z * 0.5f)) :
//                new CapsuleShape(dimensions.x * 0.5f, dimensions.y);
//
//        final Vector3f scale = initialTransform.getScale(vars.vect3d1);
//        collisionShape.setLocalScaling(new javax.vecmath.Vector3f(scale.x, scale.y, scale.z));
//
//        Utils.matrixToTransform(vars.vecmathTransform, initialTransform);
//        vars.vecmathTransform.origin.add(new javax.vecmath.Vector3f(offset.x, offset.y, offset.z));
//        final DefaultMotionState motionState = new DefaultMotionState(vars.vecmathTransform);
//
//        final RigidBody rigidBody = new RigidBody(1.0f, motionState, collisionShape);
//        rigidBody.setUserPointer(1);
//
//        this.dynamicsWorld.addRigidBody(rigidBody);
//        this.parts.put(name, rigidBody);
//
//        vars.release();
//        return rigidBody;
//    }

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

    private enum RagdollBodyPartCollider {
        BOX,
        CAPSULE,
        SPHERE
    }

    private class SimpleAABB {
        private final Vector3f min;
        private final Vector3f max;

        public SimpleAABB() {
            this.min = new Vector3f();
            this.max = new Vector3f();
        }

        public void expand(final Vector3f point) {
            min.x = Math.min(min.x, point.x);
            min.y = Math.min(min.y, point.y);
            min.z = Math.min(min.z, point.z);

            max.x = Math.max(max.x, point.x);
            max.y = Math.max(max.y, point.y);
            max.z = Math.max(max.z, point.z);
        }

        public Vector3f getExtents(final Vector3f out) {
            this.max.sub(this.min, out);
            return out;
        }
    }
}
