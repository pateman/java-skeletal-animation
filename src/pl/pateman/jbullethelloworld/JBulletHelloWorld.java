package pl.pateman.jbullethelloworld;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.dynamics.RigidBody;
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
import pl.pateman.core.physics.raycast.PhysicsRaycastResult;
import pl.pateman.core.shader.Program;
import pl.pateman.core.shader.Shader;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

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
    private static final float SPHERE_RADIUS = 2.0f;
    private static final String GROUND_ENTITY_NAME = "ground";

    private GLFWErrorCallback errorCallback;
    private GLFWKeyCallback keyCallback;
    private long window;

    private CameraEntity camera;
    private Program program;
    private double lastTime;
    private float deltaTime;

    private JBulletHelloWorldScene scene;

    private void run() {
        try {
            this.initWindow();
            this.loop();

            glfwDestroyWindow(this.window);
            this.keyCallback.release();
        } finally {
            glfwTerminate();
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
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (action == GLFW_RELEASE) {
                    switch (key) {
                        //  Escape key.
                        case GLFW_KEY_ESCAPE:
                            glfwSetWindowShouldClose(window, GLFW_TRUE);
                            break;
                        //  'G' key:
                        case GLFW_KEY_G:
                            final Vector3f sphereTranslation = JBulletHelloWorld.this.scene.
                                    getEntity(SPHERE_ENTITY_NAME).getTranslation();
                            final PhysicsRaycastResult raycastResult = JBulletHelloWorld.this.scene.
                                    raycast(sphereTranslation, Utils.NEG_AXIS_Y, SPHERE_RADIUS);

                            System.out.printf("Ball is%son the ground\n", raycastResult == null ? " NOT " : " ");
                            break;
                        //  Space bar.
                        case GLFW_KEY_SPACE:
                            final RigidBody sphereBody = JBulletHelloWorld.this.scene.getEntity(SPHERE_ENTITY_NAME).
                                    getRigidBody();
                            sphereBody.applyImpulse(new javax.vecmath.Vector3f(0.0f, SPHERE_RADIUS * 3.0f, 0.0f),
                                    new javax.vecmath.Vector3f(0.0f, -SPHERE_RADIUS, 0.0f));
                            break;
                    }
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
            this.program.setUniform1(Utils.USELIGHTING_UNIFORM, (int) this.scene.getEntityParameter(
                    meshEntity.getName(), USE_LIGHTING_PARAM));

            final Vector4f diffuseColor = this.scene.getEntityParameter(meshEntity.getName(), DIFFUSE_COLOR_PARAM);
            this.program.setUniform4(Utils.DIFFUSECOLOR_UNIFORM, diffuseColor.x, diffuseColor.y, diffuseColor.z,
                    diffuseColor.w);

            //  Draw the entity.
            renderer.renderMesh();

            //  Finalize rendering.
            renderer.finalizeRendering();
        }
        tempVars.release();
    }

    private void updateScene() {
        final double currentTime = glfwGetTime();

        this.deltaTime = (float) (currentTime - this.lastTime);
        this.lastTime = currentTime;

        this.scene.updateScene(this.deltaTime);
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

            final MeshEntity sphere = this.scene.addEntity(new SphereMeshEntity(SPHERE_ENTITY_NAME, SPHERE_RADIUS, 32,
                    32));
            sphere.setTranslation(new Vector3f(0.0f, 3.0f, 0.0f));
            sphere.setShaderProgram(this.program);
            sphere.buildMesh();
            sphere.getRigidBody().setCollisionShape(new SphereShape(SPHERE_RADIUS));
            sphere.getRigidBody().setRestitution(1f);
            Utils.setRigidBodyMass(sphere.getRigidBody(), 1.0f);
            this.setEntityLightingParams(sphere, new Vector4f(0.8f, 0.0f, 0.0f, 1.0f));

            final CubeMeshEntity ground = this.scene.addEntity(new CubeMeshEntity(GROUND_ENTITY_NAME, 1.0f));
            ground.setTranslation(new Vector3f(0.0f, -5.0f, 0.0f));
            ground.setScale(new Vector3f(10.0f, 0.5f, 10.0f));
            ground.setShaderProgram(this.program);
            ground.buildMesh();
            ground.getRigidBody().setCollisionShape(new BoxShape(new javax.vecmath.Vector3f(5.0f, 0.75f, 5.0f)));
            ground.getRigidBody().setRestitution(0.25f);
            Utils.setRigidBodyMass(ground.getRigidBody(), 0.0f);
            this.setEntityLightingParams(ground, new Vector4f(0.8f, 0.8f, 0.8f, 1.0f));

            //  Add the objects to the physics world.
            this.scene.addEntityToPhysicsWorld(GROUND_ENTITY_NAME);
            this.scene.addEntityToPhysicsWorld(SPHERE_ENTITY_NAME);

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

    private void setEntityLightingParams(final AbstractEntity entity, final Vector4f diffuseColor) {
        final String entityName = entity.getName();

        this.scene.setEntityParameter(entityName, USE_LIGHTING_PARAM, 1);
        this.scene.setEntityParameter(entityName, DIFFUSE_COLOR_PARAM, diffuseColor);
    }

    public static void main(String[] args) {
        new JBulletHelloWorld().run();
    }

}
