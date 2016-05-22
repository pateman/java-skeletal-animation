package pl.pateman.skeletal;

import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.shapes.StaticPlaneShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.DebugDrawModes;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;
import pl.pateman.core.MeshImporter;
import pl.pateman.core.TempVars;
import pl.pateman.core.Utils;
import pl.pateman.core.entity.AbstractEntity;
import pl.pateman.core.entity.CameraEntity;
import pl.pateman.core.entity.MeshEntity;
import pl.pateman.core.entity.SkeletonMeshEntity;
import pl.pateman.core.entity.mesh.MeshRenderer;
import pl.pateman.core.entity.mesh.animation.AnimationPlaybackMode;
import pl.pateman.core.entity.mesh.animation.BoneAnimationChannel;
import pl.pateman.core.mesh.Animation;
import pl.pateman.core.mesh.Bone;
import pl.pateman.core.mesh.BoneManualControlType;
import pl.pateman.core.physics.DiscreteDynamicsWorldEx;
import pl.pateman.core.physics.Ragdoll;
import pl.pateman.core.physics.debug.PhysicsDebugger;
import pl.pateman.core.shader.Program;
import pl.pateman.core.shader.Shader;
import pl.pateman.core.texture.Texture;
import pl.pateman.core.texture.TextureLoader;
import pl.pateman.importer.json.JSONImporter;

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

    private BoneAnimationChannel upperBodyChannel;
    private BoneAnimationChannel lowerBodyChannel;
    private String wholeBodyCurrentAnimation;

    private int currentManualControlMode;
    private Bone manualBone;

    private DiscreteDynamicsWorldEx dynamicsWorld;
    private PhysicsDebugger physicsDebugger;
    private boolean physicsDebug;
    private boolean physicsSimulation;

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
            private boolean canRotateFurther(boolean toTheLeft) {
                final TempVars vars = TempVars.get();
                Main.this.manualBone.getManualControlRotation().getEulerAnglesXYZ(vars.vect3d1);
                vars.release();

                final float degrees = (float) Math.toDegrees(vars.vect3d1.x);
                return toTheLeft ? degrees >= -45.0f : degrees <= 45.0f;
            }

            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (action == GLFW_RELEASE) {
                    switch (key) {
                        //  Escape key.
                        case GLFW_KEY_ESCAPE:
                            glfwSetWindowShouldClose(window, GLFW_TRUE);
                            break;
                        //  '0' key.
                        case GLFW_KEY_0:
                            Main.this.upperBodyChannel.setPlaybackMode(AnimationPlaybackMode.LOOP);
                            Main.this.wholeBodyCurrentAnimation = Main.this.wholeBodyCurrentAnimation.equals("run") ? "Idle" :
                                    "run";

                            Main.this.meshEntity.getAnimationController().switchToAnimation(
                                    Main.this.wholeBodyCurrentAnimation);
                            break;
                        //  '1' key
                        case GLFW_KEY_1:
                            Main.this.upperBodyChannel.switchToAnimation("alert");
                            Main.this.upperBodyChannel.setPlaybackMode(AnimationPlaybackMode.ONCE);
                            Main.this.lowerBodyChannel.switchToAnimation("run");
                            break;
                        //  'D' key.
                        case GLFW_KEY_D:
                            Main.this.physicsDebug = !Main.this.physicsDebug;
                            break;
                        //  'P' key
                        case GLFW_KEY_P:
                            Main.this.physicsSimulation = !Main.this.physicsSimulation;
                            break;
                        //  'M' key.
                        case GLFW_KEY_M:
                            Main.this.currentManualControlMode = Utils.clamp(++Main.this.currentManualControlMode % 3, 0, 2);
                            Main.this.manualBone.setManualControlType(BoneManualControlType.
                                    values()[Main.this.currentManualControlMode]);
                            System.out.printf("Current mode: %s\n", Main.this.manualBone.getManualControlType());
                            break;
                        //  Left arrow key.
                        case GLFW_KEY_LEFT:
                            if (this.canRotateFurther(true)) {
                                Main.this.manualBone.getManualControlRotation().rotate(
                                    (float) Math.toRadians(-120.0f * Main.this.deltaTime), 0.0f, 0.0f);
                            }
                            break;
                        //  Right arrow key.
                        case GLFW_KEY_RIGHT:
                            if (this.canRotateFurther(false)) {
                                Main.this.manualBone.getManualControlRotation().rotate(
                                    (float) Math.toRadians(120.0f * Main.this.deltaTime), 0.0f, 0.0f);
                            }
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
            final MeshImporter importer = new JSONImporter();
            this.meshEntity = importer.load("test.json");
            this.meshEntity.setShaderProgram(this.meshProgram);
            this.meshEntity.buildMesh();
            this.meshEntity.translate(0.25f, 0.0f, 0.0f);
            this.meshEntity.rotate(0.0f, (float) Math.toRadians(180.0f), 0.0f);
            this.meshEntity.setScale(new Vector3f(0.95f, 0.95f, 0.95f));
            this.manualBone = this.meshEntity.getMesh().getSkeleton().getBoneByName("Bip01 Head");

            //  Print information about the mesh.
            System.out.println("*** ANIMATIONS ***");
            for (Animation animation : this.meshEntity.getMesh().getAnimations()) {
                System.out.printf("%s (%d frames, %.4f length)\n", animation.getName(), animation.getFrameCount(),
                        animation.getLength());
            }
            System.out.println("*** SKELETON ***");
            System.out.println(this.meshEntity.getMesh().getSkeleton());

            //  Create animation channels.
            this.upperBodyChannel = this.meshEntity.getAnimationController().addAnimationChannel("Upper Body");
            this.lowerBodyChannel = this.meshEntity.getAnimationController().addAnimationChannel("Lower Body");

            if (this.meshEntity.getMesh().hasSkeleton()) {
                this.lowerBodyChannel.addBone("Bip01");
//                this.lowerBodyChannel.addBone("Bip01 Footsteps");
                this.lowerBodyChannel.addBone("Bip01 Pelvis");
                this.lowerBodyChannel.addBone("Bip01 Spine");
                this.lowerBodyChannel.addBonesTree("Bip01 L Thigh");
                this.lowerBodyChannel.addBonesTree("Bip01 R Thigh");

                this.upperBodyChannel.addBonesTree("Bip01 Spine1");
            }

            //  Create the skeleton mesh.
            this.skeletonMeshEntity = new SkeletonMeshEntity(this.meshEntity.getMesh().getSkeleton());
            this.skeletonMeshEntity.translate(-0.25f, 0.0f, 0.0f);
            this.skeletonMeshEntity.rotate(0.0f, (float) Math.toRadians(180.0f), 0.0f);

            //  Setup the camera.
            this.camera = new CameraEntity();
            this.camera.translate(0.0f, 0.2f, -0.65f);
            this.camera.getCameraProjection().setViewport(WINDOW_WIDTH, WINDOW_HEIGHT);
            this.camera.updateProjectionMatrix();

            //  Create the physics world.
            final DbvtBroadphase broadphase = new DbvtBroadphase();
            final DefaultCollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();
            final CollisionDispatcher collisionDispatcher = new CollisionDispatcher(collisionConfiguration);
            final SequentialImpulseConstraintSolver constraintSolver = new SequentialImpulseConstraintSolver();

            this.dynamicsWorld = new DiscreteDynamicsWorldEx(collisionDispatcher, broadphase, constraintSolver,
                    collisionConfiguration);
            this.dynamicsWorld.setGravity(new javax.vecmath.Vector3f(0.0f, -9.81f, 0.0f));
            this.physicsDebugger = new PhysicsDebugger(this.dynamicsWorld);

            final StaticPlaneShape ground = new StaticPlaneShape(new javax.vecmath.Vector3f(0.0f, 1.0f, 0.0f), 0.0f);
            final Transform transform = new Transform();
            transform.setIdentity();
            transform.origin.set(0.0f, 0.0f, 0.0f);
            RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(0.0f, new DefaultMotionState(transform),
                    ground);
            final RigidBody rigidBody = new RigidBody(rbInfo);
            this.dynamicsWorld.addRigidBody(rigidBody);

            //  Build the ragdoll.
            final Ragdoll ragdoll = this.meshEntity.getAnimationController().getRagdoll();
            ragdoll.setDynamicsWorld(this.dynamicsWorld);
            ragdoll.setRagdollBone(Ragdoll.RagdollBodyPart.HEAD, "Bip01 Head");
            ragdoll.setRagdollBone(Ragdoll.RagdollBodyPart.LEFT_UPPER_ARM, "Bip01 L UpperArm");
            ragdoll.setRagdollBone(Ragdoll.RagdollBodyPart.LEFT_ELBOW, "Bip01 L Forearm");
            ragdoll.setRagdollBone(Ragdoll.RagdollBodyPart.RIGHT_UPPER_ARM, "Bip01 R UpperArm");
            ragdoll.setRagdollBone(Ragdoll.RagdollBodyPart.RIGHT_ELBOW, "Bip01 R Forearm");
            ragdoll.setRagdollBone(Ragdoll.RagdollBodyPart.CHEST, "Bip01 Neck");
            ragdoll.setRagdollBone(Ragdoll.RagdollBodyPart.LEFT_LOWER_ARM, "Bip01 L Hand");
            ragdoll.setRagdollBone(Ragdoll.RagdollBodyPart.RIGHT_LOWER_ARM, "Bip01 R Hand");
            ragdoll.setRagdollBone(Ragdoll.RagdollBodyPart.HIPS, "Bip01 Pelvis");
            ragdoll.setRagdollBone(Ragdoll.RagdollBodyPart.LEFT_UPPER_LEG, "Bip01 L Thigh");
            ragdoll.setRagdollBone(Ragdoll.RagdollBodyPart.LEFT_KNEE, "Bip01 L Calf");
            ragdoll.setRagdollBone(Ragdoll.RagdollBodyPart.RIGHT_UPPER_LEG, "Bip01 R Thigh");
            ragdoll.setRagdollBone(Ragdoll.RagdollBodyPart.RIGHT_KNEE, "Bip01 R Calf");
            ragdoll.setRagdollBone(Ragdoll.RagdollBodyPart.LEFT_LOWER_LEG, "Bip01 L Foot");
            ragdoll.setRagdollBone(Ragdoll.RagdollBodyPart.RIGHT_LOWER_LEG, "Bip01 R Foot");
            ragdoll.buildRagdoll();

//            ragdoll.addRagdollPart("Head", this.meshEntity.getTransformation(), new Vector3f(0.0f, 0.34f, 0.0f),
//                    Utils.IDENTITY_QUATERNION, new Vector3f(0.05f, 0.2f, 0.35f), "Bip01 Neck", "Bip01 Head");
//            ragdoll.addRagdollPart("Torso", this.meshEntity.getTransformation(), new Vector3f(0.0f, 0.13f, 0.0f),
//                    Utils.IDENTITY_QUATERNION, new Vector3f(0.15f, 0.54f, 0.5f), "Bip01 Spine", "Bip01 Spine1");
//            ragdoll.addRagdollPart("Left arm", this.meshEntity.getTransformation(), new Vector3f(0.0f, 0.15f, 0.0f),
//                    Utils.IDENTITY_QUATERNION, new Vector3f(0.5f, 0.5f, 0.5f), "Bip01 L Clavicle", "Bip01 L UpperArm");
//            ragdoll.addRagdollPart("Head", true, new Vector3f(0.05f, 0.05f, 0.05f), this.getBoneBindPosition("Bip01 Head"),
//                    this.meshEntity.getTransformation());
//            ragdoll.addRagdollPart("Torso", true, new Vector3f(0.08f, 0.16f, 0.05f),
//                    this.getBoneBindPosition("Bip01 Spine").add(0.0f, 0.025f, 0.0f),
//                    this.meshEntity.getTransformation());
////            ragdoll.addRagdollPart("Left Upperarm", false, new Vector3f(0.035f, 0.045f, 0.0f),
////                    this.getBoneBindPosition("Bip01 L UpperArm").add(0.0f, -0.04f, 0.0f),
////                    this.meshEntity.getTransformation());
////            ragdoll.addRagdollPart("Right Upperarm", false, new Vector3f(0.035f, 0.045f, 0.0f),
////                    this.getBoneBindPosition("Bip01 R UpperArm").add(0.0f, -0.04f, 0.0f),
////                    this.meshEntity.getTransformation());
////            ragdoll.addRagdollPart("Left Lowerarm", false, new Vector3f(0.035f, 0.085f, 0.0f),
////                    this.getBoneBindPosition("Bip01 L Forearm").add(0.0f, -0.06f, 0.0f),
////                    this.meshEntity.getTransformation());
////            ragdoll.addRagdollPart("Right Lowerarm", false, new Vector3f(0.035f, 0.085f, 0.0f),
////                    this.getBoneBindPosition("Bip01 R Forearm").add(0.0f, -0.06f, 0.0f),
////                    this.meshEntity.getTransformation());
//            ragdoll.addRagdollPart("Left Upperleg", true, new Vector3f(0.03f, 0.1f, 0.03f),
//                    this.getBoneBindPosition("Bip01 L Thigh").add(-0.01f, -0.07f, -0.02f),
//                    this.meshEntity.getTransformation());
//            ragdoll.addRagdollPart("Right Upperleg", true, new Vector3f(0.03f, 0.1f, 0.03f),
//                    this.getBoneBindPosition("Bip01 R Thigh").add(0f, -0.07f, -0.02f),
//                    this.meshEntity.getTransformation());
//            ragdoll.addRagdollPart("Left Lowerleg", true, new Vector3f(0.03f, 0.1f, 0.03f),
//                    this.getBoneBindPosition("Bip01 L Calf").add(-0.01f, -0.03f, -0.02f),
//                    this.meshEntity.getTransformation());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Vector3f getBoneBindPosition(final String boneName) {
        final Matrix4f worldBindMatrix = this.meshEntity.getMesh().getSkeleton().getBoneByName(boneName).getWorldBindMatrix();
        final Vector3f bindPosition = worldBindMatrix.getTranslation(new Vector3f());
        return bindPosition;
    }

    private void updateScene() {
        final double currentTime = glfwGetTime();

        this.deltaTime = (float) (currentTime - this.lastTime);
        this.lastTime = currentTime;

        if (this.physicsSimulation) {
            this.dynamicsWorld.stepSimulation(this.deltaTime);

            //  After stepping the physics simulation, update graphical representations of objects.
            final TempVars tempVars = TempVars.get();
            for (CollisionObject collisionObject : this.dynamicsWorld.getCollisionObjectArray()) {
                if (collisionObject.getUserPointer() == null) {
                    continue;
                }

                if (collisionObject.getUserPointer() instanceof AbstractEntity) {
                    //  Get the collision object's world transformation.
                    if (collisionObject instanceof RigidBody) {
                        ((RigidBody) collisionObject).getMotionState().getWorldTransform(tempVars.vecmathTransform);
                    } else {
                        collisionObject.getWorldTransform(tempVars.vecmathTransform);
                    }

                    //  Set the transformation of the entity attached to this collision object.
                    AbstractEntity entity = (AbstractEntity) collisionObject.getUserPointer();
                    Utils.transformToMatrix(tempVars.tempMat4x41, tempVars.vecmathTransform);
                    entity.getTransformation().set(tempVars.tempMat4x41);
                }
            }
            tempVars.release();
        }
        this.meshEntity.getAnimationController().stepAnimation(this.deltaTime);
        this.skeletonMeshEntity.applyAnimation(this.meshEntity.getMeshRenderer().getBoneMatrices());
    }

    private void drawScene() {
        final TempVars tempVars = TempVars.get();

        //  Draw the skeleton mesh first.
        this.skeletonMeshEntity.drawSkeletonMesh(this.camera);

        //  Prepare the model-view matrix.
        final Matrix4f modelViewMatrix = this.camera.getViewMatrix().mul(this.meshEntity.getTransformation(),
                tempVars.tempMat4x41);

        //  Start rendering.
        final MeshRenderer renderer = this.meshEntity.getMeshRenderer();
        renderer.initializeRendering();
        this.meshTexture.bind();

        //  Pass uniforms to the shader.
        this.meshProgram.setUniformMatrix4(Utils.MODELVIEW_UNIFORM, Utils.matrix4fToBuffer(modelViewMatrix));
        this.meshProgram.setUniformMatrix4(Utils.PROJECTION_UNIFORM, Utils.matrix4fToBuffer(this.camera.
                getProjectionMatrix()));
        this.meshProgram.setUniform1(Utils.TEXTURE_UNIFORM, 0);
        this.meshProgram.setUniform1(Utils.USETEXTURING_UNIFORM, 1);
        this.meshProgram.setUniform1(Utils.USELIGHTING_UNIFORM, 1);
        this.meshProgram.setUniform4(Utils.DIFFUSECOLOR_UNIFORM, 0.8f, 0.8f, 0.8f, 1.0f);
        this.meshProgram.setUniform3(Utils.CAMERADIRECTION_UNIFORM, this.camera.getDirection().x,
                this.camera.getDirection().y, this.camera.getDirection().z);
        if (this.meshEntity.getMesh().hasSkeleton()) {
            this.meshProgram.setUniform1(Utils.USESKINNING_UNIFORM, 1);

            //  Apply the inverse bind transform to bone matrices.
            final List<Matrix4f> boneMatrices = renderer.getBoneMatrices();
            for (int i = 0; i < this.meshEntity.getMesh().getSkeleton().getBones().size(); i++) {
                final Bone bone = this.meshEntity.getMesh().getSkeleton().getBone(i);
                final Matrix4f boneMatrix = tempVars.boneMatricesList.get(i).set(boneMatrices.get(i));

                boneMatrix.mul(bone.getInverseBindMatrix(), boneMatrix);
            }
            this.meshProgram.setUniformMatrix4Array(Utils.BONES_UNIFORM, tempVars.boneMatricesList.size(),
                    Utils.matrices4fToBuffer(tempVars.boneMatricesList));
        } else {
            this.meshProgram.setUniform1(Utils.USESKINNING_UNIFORM, 0);
        }

        //  Draw the entity.
        renderer.renderMesh();

        //  Finalize rendering.
        renderer.finalizeRendering();
        this.meshTexture.unbind();

        tempVars.release();

        //  Draw the physics debug.
        if (this.physicsDebug) {
            this.meshEntity.getAnimationController().getRagdoll().drawRagdollLines(this.camera, Utils.ZERO_VECTOR);
            this.physicsDebugger.setDebugMode(DebugDrawModes.DRAW_WIREFRAME);
            this.physicsDebugger.debugDrawWorld(this.camera);
        }
    }

    private void loop() {
        GL.createCapabilities();
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);

        this.initScene();

        this.wholeBodyCurrentAnimation = "run";
        this.meshEntity.getAnimationController().switchToAnimation(this.wholeBodyCurrentAnimation);
        if (this.meshEntity.getMesh().hasSkeleton()) {
            this.upperBodyChannel.setSpeed(1.5f);
            this.lowerBodyChannel.setSpeed(1.5f);
        }

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
