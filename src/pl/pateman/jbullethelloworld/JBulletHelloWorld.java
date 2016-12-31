package pl.pateman.jbullethelloworld;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.*;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.constraintsolver.Generic6DofConstraint;
import com.bulletphysics.linearmath.MatrixUtil;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.util.ObjectArrayList;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;
import pl.pateman.core.TempVars;
import pl.pateman.core.Utils;
import pl.pateman.core.entity.*;
import pl.pateman.core.entity.mesh.MeshRenderer;
import pl.pateman.core.mesh.Mesh;
import pl.pateman.core.physics.raycast.PhysicsRaycastResult;
import pl.pateman.core.shader.Program;
import pl.pateman.core.shader.Shader;
import pl.pateman.importer.json.JSONImporter;

import java.util.concurrent.ThreadLocalRandom;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static pl.pateman.core.entity.AbstractEntity.COLLISION_GROUP_02;

/**
 * Created by pateman.
 */
public class JBulletHelloWorld {
    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 768;
    private static final Vector3f LIGHT_DIR = new Vector3f(-0.1f, 0.75f, 0.05f).normalize();
    private static final String USE_LIGHTING_PARAM = "useLighting";
    private static final String DIFFUSE_COLOR_PARAM = "diffuseColor";
    private static final String SPHERE_ENTITY_NAME = "sphere";
    private static final float SPHERE_RADIUS = 1.0f;
    private static final String GROUND_ENTITY_NAME = "ground";
    private static final String BANANA_ENTITY_NAME = "banana";
    private static final String CUBE_1_ENTITY_NAME = "Cube1";

    private GLFWErrorCallback errorCallback;
    private GLFWKeyCallback keyCallback;
    private long window;

    private CameraEntity camera;
    private Program program;
    private double lastTime;
    private float deltaTime;
    private boolean drawDebug;

    private JBulletHelloWorldScene scene;

    private void run() {
        try {
            this.initWindow();
            this.loop();

            glfwDestroyWindow(this.window);
            this.keyCallback.release();
        } finally {
            glfwTerminate();

            if (this.scene != null) {
                this.scene.clearAndDestroy();
            }
            if (this.program != null) {
                this.program.clearAndDestroy();
            }

            this.errorCallback.release();
        }
    }

    private void initWindow() {
        glfwSetErrorCallback(this.errorCallback = GLFWErrorCallback.createPrint(System.err));
        if (glfwInit() != GLFW_TRUE) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        this.window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "JBullet Hello World!", NULL, NULL);

        if (this.window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        glfwSetKeyCallback(this.window, this.keyCallback = new GLFWKeyCallback() {
            private void getMovementVector(final Vector3f direction, final AbstractEntity entity,
                                           final javax.vecmath.Vector3f out) {
                final TempVars vars = TempVars.get();

                direction.mul(entity.getTransformation().get3x3(vars.tempMat3x3), vars.vect3d1);
                vars.vect3d1.normalize().mul(1.5f);
                Utils.convert(out, vars.vect3d1);

                vars.release();
            }

            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (action == GLFW_RELEASE) {
                    final TempVars vars = TempVars.get();
                    AbstractEntity entity;
                    switch (key) {
                        //  Escape key.
                        case GLFW_KEY_ESCAPE:
                            glfwSetWindowShouldClose(window, GLFW_TRUE);
                            break;
                        //  'D' key.
                        case GLFW_KEY_D:
                            JBulletHelloWorld.this.drawDebug = !JBulletHelloWorld.this.drawDebug;
                            break;
                        //  'G' key:
                        case GLFW_KEY_G:
                            final Vector3f sphereTranslation = JBulletHelloWorld.this.scene.
                                    getEntity(SPHERE_ENTITY_NAME).getTranslation();
                            final PhysicsRaycastResult raycastResult = JBulletHelloWorld.this.scene.
                                    raycast(sphereTranslation, Utils.NEG_AXIS_Y, SPHERE_RADIUS + 0.1f, COLLISION_GROUP_02);

                            System.out.printf("Ball is%son the ground\n", raycastResult == null ? " NOT " : " ");
                            break;
                        //  Space bar.
                        case GLFW_KEY_SPACE:
                            final RigidBody sphereBody = JBulletHelloWorld.this.scene.getEntity(SPHERE_ENTITY_NAME).
                                    getRigidBody();
                            sphereBody.applyImpulse(new javax.vecmath.Vector3f(0.0f, SPHERE_RADIUS * 3.0f, 0.0f),
                                    new javax.vecmath.Vector3f(0.0f, -SPHERE_RADIUS, 0.0f));
                            break;
                        //  Up arrow.
                        case GLFW_KEY_UP:
                            entity = JBulletHelloWorld.this.scene.getEntity(CUBE_1_ENTITY_NAME);
                            this.getMovementVector(Utils.AXIS_Z, entity, vars.vecmathVect3d1);
                            entity.getRigidBody().applyCentralImpulse(vars.vecmathVect3d1);

                            break;
                        //  Down arrow.
                        case GLFW_KEY_DOWN:
                            entity = JBulletHelloWorld.this.scene.getEntity(CUBE_1_ENTITY_NAME);
                            this.getMovementVector(Utils.NEG_AXIS_Z, entity, vars.vecmathVect3d1);
                            entity.getRigidBody().applyCentralImpulse(vars.vecmathVect3d1);

                            break;
                        //  Left arrow.
                        case GLFW_KEY_LEFT:
                            entity = JBulletHelloWorld.this.scene.getEntity(CUBE_1_ENTITY_NAME);
                            this.getMovementVector(Utils.AXIS_X, entity, vars.vecmathVect3d1);
                            entity.getRigidBody().applyCentralImpulse(vars.vecmathVect3d1);

                            break;
                        //  Down arrow.
                        case GLFW_KEY_RIGHT:
                            entity = JBulletHelloWorld.this.scene.getEntity(CUBE_1_ENTITY_NAME);
                            this.getMovementVector(Utils.NEG_AXIS_X, entity, vars.vecmathVect3d1);
                            entity.getRigidBody().applyCentralImpulse(vars.vecmathVect3d1);

                            break;
                        case GLFW_KEY_Q:
                            entity = JBulletHelloWorld.this.scene.getEntity(CUBE_1_ENTITY_NAME);
                            vars.vecmathVect3d1.set(0.0f, 10.0f, 0.0f);
                            vars.vecmathVect3d2.set(0.0f, -1.0f, 0.0f);
                            entity.getRigidBody().applyImpulse(vars.vecmathVect3d1, vars.vecmathVect3d2);
                            break;
                    }
                    vars.release();
                }
            }
        });
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

        glfwSetWindowPos(
                this.window,
                (vidmode.width() - WINDOW_WIDTH) / 2,
                (vidmode.height() - WINDOW_HEIGHT) / 2
        );
        glfwMakeContextCurrent(this.window);

        glfwSwapInterval(1);
        glfwShowWindow(this.window);
    }

    private void loop() {
        GL.createCapabilities();
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glEnable(GL_DEPTH_TEST);

        this.initScene();
        this.lastTime = glfwGetTime();

        while (glfwWindowShouldClose(this.window) == GLFW_FALSE) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            //  Update the scene.
            this.updateScene();

            //  Draw the scene.
            this.renderScene();

            glfwSwapBuffers(this.window);
            glfwPollEvents();
        }
    }

    private void renderScene() {
        final TempVars tempVars = TempVars.get();
        for (final AbstractEntity meshEntity : this.scene) {
            //  Prepare the model-view matrix.
            final Matrix4f modelViewMatrix = this.camera.getViewMatrix().mul(meshEntity.getTransformation(),
                    tempVars.tempMat4x41);

            //  Start rendering.
            final MeshRenderer renderer = ((MeshEntity) meshEntity).getMeshRenderer();
            renderer.initializeRendering();

            //  Pass uniforms to the shader.
            this.program.setUniformMatrix4(Utils.MODELVIEW_UNIFORM, Utils.matrix4fToBuffer(modelViewMatrix));
            this.program.setUniformMatrix4(Utils.PROJECTION_UNIFORM, Utils.matrix4fToBuffer(this.camera.
                    getProjectionMatrix()));
            this.program.setUniform1(Utils.TEXTURE_UNIFORM, 0);
            this.program.setUniform1(Utils.USETEXTURING_UNIFORM, 0);
            this.program.setUniform1(Utils.USESKINNING_UNIFORM, 0);
            this.program.setUniform3(Utils.CAMERADIRECTION_UNIFORM, LIGHT_DIR.x, LIGHT_DIR.y, LIGHT_DIR.z);
            this.program.setUniform1(Utils.USELIGHTING_UNIFORM, this.scene.getEntityParameter(meshEntity.getName(),
                    USE_LIGHTING_PARAM));

            final Vector4f diffuseColor = this.scene.getEntityParameter(meshEntity.getName(), DIFFUSE_COLOR_PARAM);
            this.program.setUniform4(Utils.DIFFUSECOLOR_UNIFORM, diffuseColor.x, diffuseColor.y, diffuseColor.z,
                    diffuseColor.w);

            //  Draw the entity.
            renderer.renderMesh();

            //  Finalize rendering.
            renderer.finalizeRendering();
        }
        tempVars.release();

        //  If debug information is enabled, draw it.
        if (this.drawDebug) {
            this.scene.debugDrawWorld(this.camera);
        }
    }

    private void updateScene() {
        final double currentTime = glfwGetTime();

        this.deltaTime = (float) (currentTime - this.lastTime);
        this.lastTime = currentTime;

        this.scene.updateScene(this.deltaTime);
    }

    private MeshEntity createCube(final String cubeName, final float cubeSize, final Vector3f translation,
                                  final Vector4f color) {
        final CubeMeshEntity cube1 = this.scene.addEntity(new CubeMeshEntity(cubeName, cubeSize));
        cube1.setShaderProgram(this.program);
        cube1.buildMesh();

        final BoxShape boxShape = new BoxShape(new javax.vecmath.Vector3f(cubeSize, cubeSize, cubeSize));
        cube1.createRigidBody(boxShape, 1.0f);
        cube1.getRigidBody().setActivationState(CollisionObject.DISABLE_DEACTIVATION);
        this.setEntityLightingParams(cube1, color);
        cube1.setTranslation(translation);

        return cube1;
    }

    private void initScene() {
        try {
            final Shader vertexShader = new Shader(GL20.GL_VERTEX_SHADER);
            final Shader fragmentShader = new Shader(GL20.GL_FRAGMENT_SHADER);

            //  Load shaders.
            vertexShader.load("helloworld.vert");
            fragmentShader.load("helloworld.frag");

            if (!vertexShader.compile()) {
                throw new IllegalStateException(vertexShader.getInfoLog());
            }
            if (!fragmentShader.compile()) {
                throw new IllegalStateException(vertexShader.getInfoLog());
            }

            //  Create the program.
            this.program = new Program();
            this.program.attachShader(vertexShader);
            this.program.attachShader(fragmentShader);
            if (!this.program.link(false)) {
                throw new IllegalStateException(this.program.getInfoLog());
            }

            //  Create the scene.
            this.scene = new JBulletHelloWorldScene();

            //  A red sphere.
            final MeshEntity sphere = this.scene.addEntity(new SphereMeshEntity(SPHERE_ENTITY_NAME, SPHERE_RADIUS, 32,
                    32));
            sphere.setTranslation(new Vector3f(0.0f, 3.0f, 0.0f));
            sphere.setShaderProgram(this.program);
            sphere.buildMesh();
            final SphereShape sphereShape = new SphereShape(SPHERE_RADIUS);
            sphere.createRigidBody(sphereShape, 1.0f);
            sphere.getRigidBody().setRestitution(0.5f);
            sphere.getRigidBody().setActivationState(CollisionObject.DISABLE_DEACTIVATION);
            this.setEntityLightingParams(sphere, new Vector4f(0.8f, 0.0f, 0.0f, 1.0f));

            //  Ground.
            final CubeMeshEntity ground = this.scene.addEntity(new CubeMeshEntity(GROUND_ENTITY_NAME, 1.0f));
            ground.setTranslation(new Vector3f(0.0f, -5.0f, 0.0f));
            ground.setScale(new Vector3f(10.0f, 0.5f, 10.0f));
            ground.setShaderProgram(this.program);
            ground.buildMesh();
            //  The ball can sometimes fall through the ground and this link describes the problem:
            //  https://wiki.jmonkeyengine.org/doku.php/jme3:advanced:bullet_pitfalls
            //  I'm too lazy to fix it right now :)
            final BoxShape boxShape = new BoxShape(new javax.vecmath.Vector3f(10.0f, 1f, 10.0f));
            ground.createRigidBody(boxShape, 0.0f);
            ground.getRigidBody().setRestitution(1f);
            this.setEntityLightingParams(ground, new Vector4f(0.8f, 0.8f, 0.8f, 1.0f));

            //  A banana mesh :)
            final MeshEntity bananaMesh = new JSONImporter().load("banana.json");
            bananaMesh.setName(BANANA_ENTITY_NAME);
            bananaMesh.setScale(new Vector3f(15.0f, 15.0f, 15.0f));
            bananaMesh.setTranslation(new Vector3f(0.0f, 8.0f, 0.0f));
            bananaMesh.setShaderProgram(this.program);
            bananaMesh.buildMesh();
            bananaMesh.createRigidBody(this.createConvexHullShape(bananaMesh.getMesh(), new Vector3f(2.65f, 2.65f, 2.65f)), 1.0f);
            bananaMesh.getRigidBody().setRestitution(0.1f);

            this.scene.addEntity(bananaMesh);
            this.setEntityLightingParams(bananaMesh, new Vector4f(1.0f, 1.0f, 0.2f, 1.0f));

//            final MeshEntity cube1 = this.createCube(CUBE_1_ENTITY_NAME, 1.0f,
//                    new Vector3f(-5.0f, -1.0f, -3.0f), new Vector4f(1.0f, 0.0f, 0.0f, 1.0f));
//            final MeshEntity cube2 = this.createCube("Cube2", 0.5f,
//                    new Vector3f(-5.0f, 0.5f, -3.0f), new Vector4f(1.0f, 0.0f, 1.0f, 1.0f));
//
//            this.createConstraintBetweenEntities(cube1, cube2);

            final TempVars vars = TempVars.get();
            final Vector4f redColor = new Vector4f(1.0f, 0.0f, 0.0f, 1.0f);
            for (int i = 0; i < 10; i++) {
                final String entityName = "Cube" + (i + 1);
                final MeshEntity cube = this.createCube(entityName, 0.5f,
                        new Vector3f(-5.0f, i + 0.5f, -3.0f),
                        i == 0 ? redColor : this.randColor());
                this.scene.addEntityToPhysicsWorld(entityName);
                cube.setCollisionGroup(COLLISION_GROUP_02);

                if (i == 0) {
                    continue;
                }

                final RigidBody rbA = cube.getRigidBody();
                final RigidBody rbB = this.scene.getEntity("Cube" + i).getRigidBody();

                vars.vecmathTransform.setIdentity();
                MatrixUtil.setEulerZYX(vars.vecmathTransform.basis, 0.0f, 0.0f, 0.0f);
                vars.vecmathTransform.origin.set(0.0f, -0.5f, 0.0f);
                vars.vecmathTransform2.setIdentity();
                MatrixUtil.setEulerZYX(vars.vecmathTransform2.basis, 0.0f, 0.0f, 0.0f);
                vars.vecmathTransform2.origin.set(0.0f, 0.5f, 0.0f);

                final Generic6DofConstraint constraint = new Generic6DofConstraint(rbA, rbB, vars.vecmathTransform,
                        vars.vecmathTransform2, true);
                constraint.setLimit(0, 0.0f, 0.0f);
                constraint.setLimit(1, 0.0f, 0.0f);
                constraint.setLimit(2, 0.0f, 0.0f);
                constraint.setLimit(3, -30.0f * Utils.DEG2RAD, 30.0f * Utils.DEG2RAD);
                constraint.setLimit(4, 0.0f, 0.0f);
                constraint.setLimit(5, 0.0f, 0.0f);
                this.scene.addConstraint(constraint, true);
            }
            vars.release();

            //  Add the objects to the physics world.
            this.scene.addEntityToPhysicsWorld(GROUND_ENTITY_NAME);
            this.scene.addEntityToPhysicsWorld(SPHERE_ENTITY_NAME);
            this.scene.addEntityToPhysicsWorld(BANANA_ENTITY_NAME);
//            this.scene.addEntityToPhysicsWorld(CUBE_1_ENTITY_NAME);
//            this.scene.addEntityToPhysicsWorld("Cube2");

            //  Set collision groups and mask. Note that setting groups/masks is possible AFTER an entity has been
            //  added to the physics world.
            sphere.setCollisionGroup(COLLISION_GROUP_02);
            bananaMesh.setCollisionGroup(COLLISION_GROUP_02);
//            cube1.setCollisionGroup(COLLISION_GROUP_02);
//            cube2.setCollisionGroup(COLLISION_GROUP_02);
            ground.setCollisionMask(COLLISION_GROUP_02);

            //  Setup the camera.
            this.camera = new CameraEntity();
            this.camera.translate(-13.0f, 0.0f, -20f);
            this.camera.rotate(0.0f, (float) Math.toRadians(35.0f), 0.0f);
            this.camera.getCameraProjection().setViewport(WINDOW_WIDTH, WINDOW_HEIGHT);
            this.camera.updateProjectionMatrix();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void getExtents(final CollisionShape collisionShape, final Transform bodyTransform,
                            final Vector3f outExtents) {
        final TempVars vars = TempVars.get();

        collisionShape.getAabb(bodyTransform, vars.vecmathVect3d1, vars.vecmathVect3d2);
        Utils.convert(vars.vect3d1, vars.vecmathVect3d2).
                add(vars.vecmathVect3d1.x, vars.vecmathVect3d1.y, vars.vecmathVect3d1.z, vars.vect3d2).
                mul(0.5f);
        vars.vect3d1.sub(vars.vect3d2, outExtents);

        vars.release();
    }

    private Vector4f randColor() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();

        return new Vector4f(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1.0f);
    }

    private void setEntityLightingParams(final AbstractEntity entity, final Vector4f diffuseColor) {
        final String entityName = entity.getName();

        this.scene.setEntityParameter(entityName, USE_LIGHTING_PARAM, 1);
        this.scene.setEntityParameter(entityName, DIFFUSE_COLOR_PARAM, diffuseColor);
    }

    private ConvexHullShape createConvexHullShape(final Mesh mesh, final Vector3f scale) {
        //  Convert the list of mesh vertices.
        final ObjectArrayList<javax.vecmath.Vector3f> vertices = new ObjectArrayList<>(mesh.getTriangles().size() * 3);
        for (Integer meshTriangle : mesh.getTriangles()) {
            final Vector3f vertex = mesh.getVertices().get(meshTriangle);
            vertices.add(Utils.convert(new javax.vecmath.Vector3f(), vertex));
        }
        final javax.vecmath.Vector3f localScaling = Utils.convert(new javax.vecmath.Vector3f(), scale);

        //  Create the first convex hull shape.
        final ConvexHullShape firstShape = new ConvexHullShape(vertices);
        firstShape.setMargin(0.0f);
        firstShape.setLocalScaling(localScaling);

        //  Create a shape hull from the convex shape.
        final ShapeHull shapeHull = new ShapeHull(firstShape);
        shapeHull.buildHull(firstShape.getMargin());

        //  Collect the calculated shape's vertices and create another convex hull shape from them.
        final ObjectArrayList<javax.vecmath.Vector3f> shapeHullVertices = new ObjectArrayList<>(shapeHull.numVertices());
        int triangleIndex = 0;
        for (int i = 0; i < shapeHull.numTriangles(); i++) {
            shapeHullVertices.add(shapeHull.getVertexPointer().get(shapeHull.getIndexPointer().get(triangleIndex++)));
            shapeHullVertices.add(shapeHull.getVertexPointer().get(shapeHull.getIndexPointer().get(triangleIndex++)));
            shapeHullVertices.add(shapeHull.getVertexPointer().get(shapeHull.getIndexPointer().get(triangleIndex++)));
        }

        final ConvexHullShape finalShape = new ConvexHullShape(shapeHullVertices);
        finalShape.setLocalScaling(localScaling);
        finalShape.setMargin(0.0f);
        return finalShape;
    }

    public static void main(String[] args) {
        new JBulletHelloWorld().run();
    }

}
