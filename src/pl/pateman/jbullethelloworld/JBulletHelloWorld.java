package pl.pateman.jbullethelloworld;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;
import pl.pateman.skeletal.TempVars;
import pl.pateman.skeletal.Utils;
import pl.pateman.skeletal.entity.CameraEntity;
import pl.pateman.skeletal.entity.CubeMeshEntity;
import pl.pateman.skeletal.entity.MeshEntity;
import pl.pateman.skeletal.entity.SphereMeshEntity;
import pl.pateman.skeletal.entity.mesh.MeshRenderer;
import pl.pateman.skeletal.shader.Program;
import pl.pateman.skeletal.shader.Shader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Created by pateman.
 */
public class JBulletHelloWorld {
    public static final int WINDOW_WIDTH = 1024;
    public static final int WINDOW_HEIGHT = 768;

    private GLFWErrorCallback errorCallback;
    private GLFWKeyCallback keyCallback;
    private long window;

    private CameraEntity camera;
    private Program program;
    private double lastTime;
    private float deltaTime;

    private Scene scene;

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
        for (final MeshEntity meshEntity : this.scene) {
            //  Prepare the model-view matrix.
            final Matrix4f modelViewMatrix = this.camera.getViewMatrix().mul(meshEntity.getTransformation(),
                    tempVars.tempMat4x41);

            //  Start rendering.
            final MeshRenderer renderer = meshEntity.getMeshRenderer();
            renderer.initializeRendering();

            //  Pass matrices to the shader.
            this.program.setUniformMatrix4(Utils.MODELVIEW_UNIFORM, Utils.matrix4fToBuffer(modelViewMatrix));
            this.program.setUniformMatrix4(Utils.PROJECTION_UNIFORM, Utils.matrix4fToBuffer(this.camera.
                    getProjectionMatrix()));
            this.program.setUniform1(Utils.TEXTURE_UNIFORM, 0);
            this.program.setUniform1(Utils.USETEXTURING_UNIFORM, 0);
            this.program.setUniform1(Utils.USESKINNING_UNIFORM, 0);
            this.program.setUniform1(Utils.USELIGHTING_UNIFORM, (int) this.scene.getEntityParameter(
                    this.scene.getNameForEntity(meshEntity), "useLighting"));

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
            this.scene = new Scene();

            final MeshEntity sphereMeshEntity = this.scene.addEntity("sphere", new SphereMeshEntity(2.0f, 32, 32));
            sphereMeshEntity.setShaderProgram(this.program);
            sphereMeshEntity.buildMesh();
            this.scene.setEntityParameter("sphere", "useLighting", 1);

            final CubeMeshEntity ground = this.scene.addEntity("ground", new CubeMeshEntity(1.0f));
            ground.setTranslation(new Vector3f(0.0f, -5.0f, 0.0f));
            ground.setScale(new Vector3f(10.0f, 0.5f, 10.0f));
            ground.setShaderProgram(this.program);
            ground.buildMesh();
            this.scene.setEntityParameter("ground", "useLighting", 0);

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

    public static void main(String[] args) {
        new JBulletHelloWorld().run();
    }

    class Scene implements Iterable<MeshEntity> {
        private class Entity {
            private final MeshEntity entityInstance;
            private final Map<String, Object> properties;

            Entity(MeshEntity entityInstance) {
                this.entityInstance = entityInstance;
                this.properties = new HashMap<>();
            }

            public MeshEntity getEntityInstance() {
                return entityInstance;
            }

            public <T> T getProperty(final String name) {
                return (T) this.properties.get(name);
            }

            public void setProperty(final String name, Object value) {
                this.properties.put(name, value);
            }
        }

        private final Map<String, Entity> entities;
        private final Map<MeshEntity, String> entityNames;

        public Scene() {
            this.entities = new HashMap<>();
            this.entityNames = new HashMap<>();
        }

        public <T extends MeshEntity> T addEntity(final String name, final T entityInstance) {
            this.entities.put(name, new Entity(entityInstance));
            this.entityNames.put(entityInstance, name);

            return entityInstance;
        }

        public <T extends MeshEntity> T getEntity(final String name) {
            return (T) this.entities.get(name).entityInstance;
        }

        public String getNameForEntity(final MeshEntity meshEntity) {
            return this.entityNames.get(meshEntity);
        }

        public <T> T getEntityParameter(final String entity, final String parameter) {
            return (T) this.entities.get(entity).properties.get(parameter);
        }

        public void setEntityParameter(final String entity, final String parameterName, final Object value) {
            this.entities.get(entity).properties.put(parameterName, value);
        }

        @Override
        public Iterator<MeshEntity> iterator() {
            return this.entityNames.keySet().iterator();
        }
    }
}
