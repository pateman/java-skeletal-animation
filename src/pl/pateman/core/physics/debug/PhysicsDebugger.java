package pl.pateman.core.physics.debug;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL20;
import pl.pateman.core.Clearable;
import pl.pateman.core.TempVars;
import pl.pateman.core.Utils;
import pl.pateman.core.entity.AbstractEntity;
import pl.pateman.core.entity.CameraEntity;
import pl.pateman.core.entity.MeshEntity;
import pl.pateman.core.entity.mesh.MeshRenderer;
import pl.pateman.core.mesh.Mesh;
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
        this.applyToCollisionObjects((entity, debugMesh, colObj) -> {
            final TempVars tempVars = TempVars.get();
            final Transform transform = tempVars.vecmathTransform;

            entity.getRigidBody().getMotionState().getWorldTransform(transform);

            //  Convert between different math libraries.
            transform.getRotation(tempVars.vecmathQuat);
            tempVars.quat1.set(tempVars.vecmathQuat.x, tempVars.vecmathQuat.y, tempVars.vecmathQuat.z,
                    tempVars.vecmathQuat.w);
            Utils.convert(tempVars.vect3d1, transform.origin);
            Utils.convert(tempVars.vect3d2, colObj.getCollisionShape().getLocalScaling(tempVars.vecmathVect3d1));

            //  Assign transformation computed by jBullet to the entity.
            debugMesh.setTransformation(tempVars.quat1, tempVars.vect3d1, tempVars.vect3d2);

            tempVars.release();
        }, true);
    }

    public void debugDrawWorld(final CameraEntity camera) {
        if (camera == null) {
            throw new IllegalArgumentException("A valid camera needs to be provided");
        }

        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        this.applyToCollisionObjects((entity, debugMesh, colObj) -> {
            final TempVars tempVars = TempVars.get();
            final Vector4f color = tempVars.vect4d1.set(1.0f, 0.0f, 0.0f, 1.0f);

            //  Prepare the model-view matrix.
            final Matrix4f modelViewMatrix = camera.getViewMatrix().mul(debugMesh.getTransformation(),
                    tempVars.tempMat4x41);

            //  Depending on the activation state, use the correct color.
            switch (colObj.getActivationState()) {
                case CollisionObject.ACTIVE_TAG:
                    color.set(1.0f, 1.0f, 1.0f, 1.0f);
                    break;
                case CollisionObject.ISLAND_SLEEPING:
                    color.set(0.0f, 1.0f, 0.0f, 1.0f);
                    break;
                case CollisionObject.WANTS_DEACTIVATION:
                    color.set(0.0f, 1.0f, 1.0f, 1.0f);
                    break;
                case CollisionObject.DISABLE_DEACTIVATION:
                    color.set(1.0f, 0.0f, 0.0f, 1.0f);
                    break;
                case CollisionObject.DISABLE_SIMULATION:
                    color.set(1.0f, 1.0f, 0.0f, 1.0f);
                    break;
            }

            //  Draw the mesh.
            this.drawMesh(modelViewMatrix, camera.getProjectionMatrix(), debugMesh, color);

            tempVars.release();
        }, false);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    }

    private void applyToCollisionObjects(final CollisionObjectsConsumer function, final boolean cleanup) {
        final List<CollisionObject> objectList = this.dynamicsWorld.getCollisionObjectArray().
                stream().
                filter(RigidBody.class::isInstance).
                filter(collisionObject -> collisionObject.getUserPointer() instanceof MeshEntity).
                map(CollisionObject.class::cast).
                collect(Collectors.toList());

        for (int idx = 0; idx < objectList.size(); idx++) {
            final CollisionObject collisionObject = objectList.get(idx);

            final MeshEntity entity = (MeshEntity) collisionObject.getUserPointer();
            final RigidBody rigidBody = (RigidBody) collisionObject;

            MeshEntity debugMesh = this.debugEntities.get(entity.getEntityId());
            if (debugMesh == null) {
                debugMesh = this.createDebugMeshFromRigidBody(entity, rigidBody, entity.getScale());
                this.debugEntities.put(entity.getEntityId(), debugMesh);
            }

            function.accept(entity, debugMesh, collisionObject);
        }

        if (cleanup && objectList.size() != this.debugEntities.size()) {
            final Set<Long> debugEntities = this.debugEntities.keySet();
            final Set<Long> entityIds = objectList.
                    stream().
                    map(collisionObject -> ((AbstractEntity) collisionObject.getUserPointer()).getEntityId()).
                    collect(Collectors.toSet());
            if (debugEntities.removeAll(entityIds)) {
                debugEntities.forEach(this.debugEntities::remove);
            }
        }
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

    private MeshEntity createDebugMeshFromRigidBody(final AbstractEntity entity, final RigidBody rigidBody,
                                                    final Vector3f originalScale) {
        int vertexCount = 0, indexCount = 0;
        if (entity instanceof MeshEntity && ((MeshEntity) entity).getMesh() != null) {
            final Mesh mesh = ((MeshEntity) entity).getMesh();

            vertexCount = mesh.getVertices().size();
            indexCount = mesh.getTriangles().size();
        }

        final List<org.joml.Vector3f> vertices = new ArrayList<>(vertexCount);
        final List<Integer> indices = new ArrayList<>(indexCount);
        PhysicsDebugMeshFactory.getMeshVertices(rigidBody.getCollisionShape(), vertices, indices);

        vertices.forEach(v -> v.mul(originalScale));

        final Mesh mesh = new Mesh();
        mesh.getVertices().addAll(vertices);
        mesh.getTriangles().addAll(indices);

        final MeshEntity meshEntity = new MeshEntity("DebugMesh-" + entity.getName() + "-" + entity.getEntityId());
        meshEntity.setShaderProgram(this.debugShaderProgram);
        meshEntity.setMesh(mesh);
        meshEntity.buildMesh();

        return meshEntity;
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

    @FunctionalInterface
    private interface CollisionObjectsConsumer {
        void accept(final MeshEntity entity, final MeshEntity debugMesh, final CollisionObject collisionObject);
    }
}
