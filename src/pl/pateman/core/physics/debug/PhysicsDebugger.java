package pl.pateman.core.physics.debug;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL20;
import pl.pateman.core.Clearable;
import pl.pateman.core.TempVars;
import pl.pateman.core.Utils;
import pl.pateman.core.entity.CameraEntity;
import pl.pateman.core.entity.MeshEntity;
import pl.pateman.core.entity.mesh.MeshRenderer;
import pl.pateman.core.mesh.Mesh;
import pl.pateman.core.entity.EntityData;
import pl.pateman.core.shader.Program;
import pl.pateman.core.shader.Shader;

import java.util.*;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11.*;

/**
 * Created by pateman.
 */
public final class PhysicsDebugger implements Clearable {
    private static final String DEBUG_COLOR_UNIFORM = "color";
    private final DynamicsWorld dynamicsWorld;
    private final Map<Long, MeshEntity> debugEntities;
    private Program debugShaderProgram;

    private final Vector4f activeColor = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private final Vector4f sleepingColor = new Vector4f(0.0f, 1.0f, 0.0f, 1.0f);
    private final Vector4f wantsDeactivationColor = new Vector4f(0.0f, 1.0f, 1.0f, 1.0f);
    private final Vector4f disableDeactivationColor = new Vector4f(1.0f, 0.0f, 0.0f, 1.0f);
    private final Vector4f disableSimulationColor = new Vector4f(1.0f, 1.0f, 0.0f, 1.0f);

    public PhysicsDebugger(final DynamicsWorld dynamicsWorld) {
        this.dynamicsWorld = dynamicsWorld;
        if (this.dynamicsWorld == null) {
            throw new IllegalArgumentException();
        }
        this.debugEntities = new HashMap<>();
        this.createDebugProgram();
    }

    @Override
    public void clear() {
        for (final Map.Entry<Long, MeshEntity> debugEntityEntry : this.debugEntities.entrySet()) {
            debugEntityEntry.getValue().clearAndDestroy();
        }
        this.debugEntities.clear();
    }

    @Override
    public void clearAndDestroy() {
        this.clear();
    }

    public void updateDebugEntities() {
        //  Filter the available collision objects.
        final List<CollisionObject> rigidBodies = this.getCollisionObjects();

        final Set<Long> remainingDebugEntityIDs = new HashSet<>(this.debugEntities.keySet());
        final TempVars tempVars = TempVars.get();
        for (int bodyIdx = 0; bodyIdx < rigidBodies.size(); bodyIdx++) {
            final RigidBody rigidBody = (RigidBody) rigidBodies.get(bodyIdx);
            final EntityData entityData = (EntityData) rigidBody.getUserPointer();
            final Long entityId = entityData.getEntityID();

            final MeshEntity debugMeshFromRigidBody;

            //  If the set of remaining debug entities does not contain the current rigid body, create a new debug
            //  entity for it. Otherwise, get the entity from the cache and remove its ID from the set of debug
            //  entities.
            if (!remainingDebugEntityIDs.contains(entityId)) {
                debugMeshFromRigidBody = this.createDebugMeshFromEntityData(entityData, rigidBody);
                this.debugEntities.put(entityId, debugMeshFromRigidBody);
            } else {
                debugMeshFromRigidBody = this.debugEntities.get(entityId);
                remainingDebugEntityIDs.remove(entityId);
            }

            //  Update the debug mesh's transformation.
            final Transform centerOfMassTransform = rigidBody.getCenterOfMassTransform(tempVars.vecmathTransform);
            centerOfMassTransform.getRotation(tempVars.vecmathQuat);
            rigidBody.getCollisionShape().getLocalScaling(tempVars.vecmathVect3d1);

            final Vector3f position = Utils.convert(tempVars.vect3d1, centerOfMassTransform.origin);
            final Quaternionf rotation = Utils.convert(tempVars.quat1, tempVars.vecmathQuat);
            final Vector3f scale = Utils.convert(tempVars.vect3d2, tempVars.vecmathVect3d1);

            debugMeshFromRigidBody.setTransformation(rotation, position, scale);
        }
        tempVars.release();

        //  If the set is not empty, remove the obsolete debug entities.
        if (!remainingDebugEntityIDs.isEmpty()) {
            remainingDebugEntityIDs.forEach(this.debugEntities::remove);
        }
    }

    public void debugDrawWorld(final CameraEntity camera) {
        if (camera == null) {
            throw new IllegalArgumentException("A valid camera needs to be provided");
        }

        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

        final TempVars tempVars = TempVars.get();
        final List<CollisionObject> collisionObjects = this.getCollisionObjects();
        for (int i = 0; i < collisionObjects.size(); i++) {
            final CollisionObject colObj = collisionObjects.get(i);
            final MeshEntity debugMesh = this.debugEntities.get(((EntityData) colObj.getUserPointer()).getEntityID());

            //  We're unable to get the debug mesh from the cache. Skip it.
            if (debugMesh == null) {
                continue;
            }

            final Vector4f color = tempVars.vect4d1.set(1.0f, 0.0f, 0.0f, 1.0f);

            //  Prepare the model-view matrix.
            final Matrix4f modelViewMatrix = camera.getViewMatrix().mul(debugMesh.getTransformation(),
                    tempVars.tempMat4x41);

            //  Depending on the activation state, use the correct color.
            switch (colObj.getActivationState()) {
                case CollisionObject.ACTIVE_TAG:
                    color.set(this.activeColor);
                    break;
                case CollisionObject.ISLAND_SLEEPING:
                    color.set(this.sleepingColor);
                    break;
                case CollisionObject.WANTS_DEACTIVATION:
                    color.set(this.wantsDeactivationColor);
                    break;
                case CollisionObject.DISABLE_DEACTIVATION:
                    color.set(this.disableDeactivationColor);
                    break;
                case CollisionObject.DISABLE_SIMULATION:
                    color.set(this.disableSimulationColor);
                    break;
            }

            //  Draw the mesh.
            this.drawMesh(modelViewMatrix, camera.getProjectionMatrix(), debugMesh, color);
        }
        tempVars.release();

        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    }

    public Vector4f getActiveColor() {
        return activeColor;
    }

    public Vector4f getSleepingColor() {
        return sleepingColor;
    }

    public Vector4f getWantsDeactivationColor() {
        return wantsDeactivationColor;
    }

    public Vector4f getDisableDeactivationColor() {
        return disableDeactivationColor;
    }

    public Vector4f getDisableSimulationColor() {
        return disableSimulationColor;
    }

    private List<CollisionObject> getCollisionObjects() {
        return this.dynamicsWorld.getCollisionObjectArray().
                stream().
                filter(RigidBody.class::isInstance).
                filter(x -> x.getUserPointer() instanceof EntityData).
                collect(Collectors.toList());
    }

    //  TODO Refactor the whole rendering thing, because the code is duplicated (or even worse).
    private void drawMesh(final Matrix4f modelViewMatrix, final Matrix4f projectionMatrix, final MeshEntity meshEntity,
                          final Vector4f meshColor) {
        //  Start rendering.
        final MeshRenderer renderer = meshEntity.getMeshRenderer();
        renderer.initializeRendering();

        //  Pass uniforms to the shader.
        this.debugShaderProgram.setUniformMatrix4(Utils.MODELVIEW_UNIFORM, Utils.matrix4fToBuffer(modelViewMatrix));
        this.debugShaderProgram.setUniformMatrix4(Utils.PROJECTION_UNIFORM, Utils.matrix4fToBuffer(projectionMatrix));
        this.debugShaderProgram.setUniform4(DEBUG_COLOR_UNIFORM, meshColor.x, meshColor.y, meshColor.z, meshColor.w);

        //  Draw the entity.
        renderer.renderMesh();

        //  Finalize rendering.
        renderer.finalizeRendering();
    }

    private MeshEntity createDebugMeshFromEntityData(final EntityData entityData, final RigidBody rigidBody) {
        final List<org.joml.Vector3f> vertices = new ArrayList<>();
        final List<Integer> indices = new ArrayList<>();
        PhysicsDebugMeshFactory.getMeshVertices(rigidBody.getCollisionShape(), vertices, indices);

        final Mesh mesh = new Mesh();
        mesh.getVertices().addAll(vertices);
        mesh.getTriangles().addAll(indices);

        final MeshEntity debugEntity = new MeshEntity("DebugMesh-" + entityData.getEntityName() + "-" +
                entityData.getEntityID());
        debugEntity.setShaderProgram(this.debugShaderProgram);
        debugEntity.setMesh(mesh);
        debugEntity.buildMesh();

        return debugEntity;
    }

    private void createDebugProgram() {
        this.debugShaderProgram = new Program();

        final Shader vsShader = new Shader(GL20.GL_VERTEX_SHADER);
        final Shader fsShader = new Shader(GL20.GL_FRAGMENT_SHADER);
        vsShader.setSource(DEBUG_VERTEX_SHADER);
        fsShader.setSource(DEBUG_FRAGMENT_SHADER);

        if (!vsShader.compile()) {
            throw new IllegalStateException(vsShader.getInfoLog());
        }
        if (!fsShader.compile()) {
            throw new IllegalStateException(fsShader.getInfoLog());
        }

        this.debugShaderProgram.attachShader(vsShader);
        this.debugShaderProgram.attachShader(fsShader);
        if (!this.debugShaderProgram.link(false)) {
            throw new IllegalStateException(this.debugShaderProgram.getInfoLog());
        }
    }

    private static final String DEBUG_VERTEX_SHADER = "#version 330\n" +
            "\n" +
            "in vec3 Position;\n" +
            "\n" +
            "uniform mat4 projection;\n" +
            "uniform mat4 modelView;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_Position = projection * modelView * vec4(Position, 1.0);\n" +
            "}";

    private static final String DEBUG_FRAGMENT_SHADER = "#version 330\n" +
            "\n" +
            "uniform vec4 color;\n"+
            "out vec4 FragColor;\n" +
            "\n" +
            "void main() {\n" +
            "    FragColor = color;\n" +
            "}\n";
}
