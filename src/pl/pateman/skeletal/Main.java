package pl.pateman.skeletal;

import org.joml.Matrix4f;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;
import pl.pateman.skeletal.entity.CameraEntity;
import pl.pateman.skeletal.entity.MeshEntity;
import pl.pateman.skeletal.entity.SkeletonMeshEntity;
import pl.pateman.skeletal.entity.mesh.MeshRenderer;
import pl.pateman.skeletal.mesh.Bone;
import pl.pateman.skeletal.mesh.Mesh;
import pl.pateman.skeletal.ogrexml.OgreXMLImporter;
import pl.pateman.skeletal.shader.Program;
import pl.pateman.skeletal.shader.Shader;
import pl.pateman.skeletal.texture.Texture;
import pl.pateman.skeletal.texture.TextureLoader;

import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Created by pateman on 2016-03-17.
 */
public class Main {
    public static final int WINDOW_WIDTH = 1024;
    public static final int WINDOW_HEIGHT = 768;

    private GLFWErrorCallback errorCallback;
    private GLFWKeyCallback keyCallback;
    private long window;
    private double lastTime;
    private float deltaTime;

    private CameraEntity camera;
    private Program meshProgram;
    private MeshEntity meshEntity;
    private SkeletonMeshEntity skeletonMeshEntity;
    private Texture meshTexture;

    public void run() {
        System.out.println("LWJGL " + Version.getVersion() + "!");

        try {
            this.initWindow();
            this.loop();

            glfwDestroyWindow(window);
            this.keyCallback.release();
        } finally {
            if (this.meshEntity != null) {
                this.meshEntity.clearAndDestroy();
            }
            if (this.meshTexture != null) {
                this.meshTexture.clearAndDestroy();
            }
            if (this.meshProgram != null) {
                this.meshProgram.clearAndDestroy();
            }

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

        this.window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "Skeletal Animation", NULL, NULL);

        if (this.window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        glfwSetKeyCallback(this.window, this.keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                    glfwSetWindowShouldClose(window, GLFW_TRUE);
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

    private void initScene() {
        final Shader vertexShader = new Shader(GL20.GL_VERTEX_SHADER);
        final Shader fragmentShader = new Shader(GL20.GL_FRAGMENT_SHADER);

        try {
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
            this.meshProgram = new Program();
            this.meshProgram.attachShader(vertexShader);
            this.meshProgram.attachShader(fragmentShader);
            if (!this.meshProgram.link(false)) {
                throw new IllegalStateException(this.meshProgram.getInfoLog());
            }

            //  Load the texture.
            final TextureLoader textureLoader = new TextureLoader();
            this.meshTexture = textureLoader.load("test_m.jpg");

            //  Load the mesh.
            final OgreXMLImporter importer = new OgreXMLImporter();
            final Mesh mesh = importer.load("test.mesh.xml");
            this.meshEntity = new MeshEntity();
            this.meshEntity.setMesh(mesh);
            this.meshEntity.setShaderProgram(this.meshProgram);
            this.meshEntity.buildMesh();
            this.meshEntity.translate(0.25f, 0.0f, 0.0f);
            this.meshEntity.rotate(0.0f, (float) Math.toRadians(180.0f), 0.0f);

            //  Create the skeleton mesh.
            this.skeletonMeshEntity = new SkeletonMeshEntity(mesh.getSkeleton());
            this.skeletonMeshEntity.translate(-0.25f, 0.0f, 0.0f);
            this.skeletonMeshEntity.rotate(0.0f, (float) Math.toRadians(180.0f), 0.0f);

            //  Setup the camera.
            this.camera = new CameraEntity();
            this.camera.translate(0.0f, 0.2f, -0.55f);
            this.camera.getCameraProjection().setViewport(WINDOW_WIDTH, WINDOW_HEIGHT);
            this.camera.updateProjectionMatrix();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void updateScene() {
        final double currentTime = glfwGetTime();

        this.deltaTime = (float) (currentTime - this.lastTime);
        this.lastTime = currentTime;

        this.meshEntity.getAnimationController().stepAnimation(this.deltaTime);
        this.skeletonMeshEntity.applyAnimation(this.meshEntity.getMeshRenderer().getBoneMatrices());
    }

    private void drawScene() {
        final TempVars tempVars = TempVars.get();

        //  Draw the skeleton mesh first.
        this.skeletonMeshEntity.drawSkeletonMesh(this.camera.getViewMatrix(), this.camera.getProjectionMatrix());

        //  Prepare the model-view matrix.
        final Matrix4f modelViewMatrix = this.camera.getViewMatrix().mul(this.meshEntity.getTransformation(),
                tempVars.tempMat4x41);

        //  Start rendering.
        final MeshRenderer renderer = this.meshEntity.getMeshRenderer();
        renderer.initializeRendering();
        this.meshTexture.bind();

        //  Pass matrices to the shader.
        this.meshProgram.setUniformMatrix4(Utils.MODELVIEW_UNIFORM, Utils.matrix4fToBuffer(modelViewMatrix));
        this.meshProgram.setUniformMatrix4(Utils.PROJECTION_UNIFORM, Utils.matrix4fToBuffer(this.camera.
                getProjectionMatrix()));
        this.meshProgram.setUniform1(Utils.TEXTURE_UNIFORM, 0);
        this.meshProgram.setUniform1(Utils.USESKINNING_UNIFORM, 1);
        this.meshProgram.setUniform1(Utils.USETEXTURING_UNIFORM, 1);

        //  Apply the inverse bind transform to bone matrices.
        final List<Matrix4f> boneMatrices = renderer.getBoneMatrices();
        for (int i = 0; i < this.meshEntity.getMesh().getSkeleton().getBones().size(); i++) {
            final Bone bone = this.meshEntity.getMesh().getSkeleton().getBoneByIndex(i);
            final Matrix4f boneMatrix = tempVars.boneMatricesList.get(i).set(boneMatrices.get(i));

            boneMatrix.mul(bone.getInverseBindMatrix(), boneMatrix);
        }
        this.meshProgram.setUniformMatrix4Array(Utils.BONES_UNIFORM, tempVars.boneMatricesList.size(),
                Utils.matrices4fToBuffer(tempVars.boneMatricesList));

        //  Draw the entity.
        renderer.renderMesh();

        //  Finalize rendering.
        renderer.finalizeRendering();
        this.meshTexture.unbind();

        tempVars.release();
    }

    private void loop() {
        GL.createCapabilities();
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);

        this.initScene();
        this.meshEntity.getAnimationController().switchToAnimation("run");
        this.lastTime = glfwGetTime();

        while (glfwWindowShouldClose(this.window) == GLFW_FALSE) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            //  Update the scene.
            this.updateScene();

            //  Draw the scene.
            this.drawScene();

            glfwSwapBuffers(this.window);
            glfwPollEvents();
        }
    }

    public static void main(String[] args) {
        new Main().run();
    }
}
