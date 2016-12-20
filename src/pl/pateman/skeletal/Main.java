package pl.pateman.skeletal;

import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.shapes.StaticPlaneShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
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
import pl.pateman.core.physics.debug.PhysicsDebugger;
import pl.pateman.core.physics.ragdoll.*;
import pl.pateman.core.shader.Program;
import pl.pateman.core.shader.Shader;
import pl.pateman.core.text.Text2DRenderer;
import pl.pateman.core.text.impl.TrueTypeTextFont;
import pl.pateman.core.texture.Texture;
import pl.pateman.core.texture.TextureLoader;
import pl.pateman.importer.json.JSONImporter;

import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static pl.pateman.core.physics.ragdoll.BodyPartType.*;

/**
 * Created by pateman on 2016-03-17.
 */
public class Main {
    public static final int WINDOW_WIDTH = 1024;
    public static final int WINDOW_HEIGHT = 768;
    public static final Vector4f DIFFUSE_COLOR = new Vector4f(0.8f, 0.8f, 0.8f, 1.0f);
    public static final float CAMERA_SPEED = 5.0f;
    public static final float CAMERA_ROTATION_SPEED = 50.0f;
    public static final String HELP_TEXT = "Skeletal animation demo by Patryk Nusbaum\n" +
            " \n" +
            "Esc - close the window\n" +
            "0 - switch anim from 'idle' to 'run'\n" +
            "1 - switch upper body anim to 'alert', lower body to 'run'\n" +
            "P - toggle physics simulation\n" +
            "R - toggle ragdoll\n" +
            "M - toggle manual bone control mode (off, blend_with_anim, full)\n" +
            "Left arrow - rotate the controlled bone left\n" +
            "Right arrow - rotate the controlled bone right\n" +
            "W,S,A,D - camera movement\n" +
            "G,H - camera move up/down\n" +
            "Q,E - camera rotate\n" +
            "F12 - toggle physics debug\n" +
            "F1 - toggle help display";

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

    private DiscreteDynamicsWorld dynamicsWorld;
    private PhysicsDebugger physicsDebugger;
    private RagdollDebugger ragdollDebugger;
    private boolean physicsDebug;
    private boolean physicsSimulation;

    private boolean displayHelp;
    private Text2DRenderer text2DRenderer;

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
            if (this.text2DRenderer != null) {
                this.text2DRenderer.clearAndDestroy();
            }
            if (this.physicsDebugger != null) {
                this.physicsDebugger.clearAndDestroy();
            }
            if (this.ragdollDebugger != null) {
                this.ragdollDebugger.clearAndDestroy();
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
                        //  F12 key.
                        case GLFW_KEY_F12:
                            Main.this.physicsDebug = !Main.this.physicsDebug;
                            break;
                        //  F1 key.
                        case GLFW_KEY_F1:
                            Main.this.displayHelp = !Main.this.displayHelp;
                            break;
                        //  'P' key
                        case GLFW_KEY_P:
                            Main.this.physicsSimulation = !Main.this.physicsSimulation;
                            break;
                        //  'R' key
                        case GLFW_KEY_R:
                            final Ragdoll ragdoll = Main.this.meshEntity.getAnimationController().getRagdoll();
                            ragdoll.setEnabled(!ragdoll.isEnabled());
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
                } else if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                    final TempVars vars = TempVars.get();
                    final Vector3f movement = vars.vect3d1.set(Utils.ZERO_VECTOR);
                    final Vector3f rotation = vars.vect3d2.set(Utils.ZERO_VECTOR);

                    switch (key) {
                        case GLFW_KEY_W:
                            movement.set(0.0f, 0.0f, CAMERA_SPEED * Main.this.deltaTime);
                            break;
                        case GLFW_KEY_S:
                            movement.set(0.0f, 0.0f, -CAMERA_SPEED * Main.this.deltaTime);
                            break;
                        case GLFW_KEY_A:
                            movement.set(CAMERA_SPEED * Main.this.deltaTime, 0.0f, 0.0f);
                            break;
                        case GLFW_KEY_D:
                            movement.set(-CAMERA_SPEED * Main.this.deltaTime, 0.0f, 0.0f);
                            break;
                        case GLFW_KEY_G:
                            movement.set(0.0f, CAMERA_SPEED * Main.this.deltaTime, 0.0f);
                            break;
                        case GLFW_KEY_H:
                            movement.set(0.0f, -CAMERA_SPEED * Main.this.deltaTime, 0.0f);
                            break;
                        case GLFW_KEY_Q:
                            rotation.set(0.0f, (float) Math.toRadians(CAMERA_ROTATION_SPEED * Main.this.deltaTime), 0.0f);
                            break;
                        case GLFW_KEY_E:
                            rotation.set(0.0f, (float) Math.toRadians(-CAMERA_ROTATION_SPEED * Main.this.deltaTime), 0.0f);
                            break;
                    }

                    if (movement.lengthSquared() != 0.0f) {
                        Main.this.camera.getTransformation().get3x3(vars.tempMat3x3).transform(movement);
                        Main.this.camera.translate(movement);
                    }
                    if (rotation.lengthSquared() != 0.0f) {
                        Main.this.camera.rotate(rotation.x, rotation.y, rotation.z);
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
            this.meshEntity.setScale(new Vector3f(1f, 1f, 1f));
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
            this.skeletonMeshEntity.setJointColor("Bip01 L Thigh", new Vector4f(0.78f, 0.98f, 1.0f, 1.0f));
            this.skeletonMeshEntity.setJointColor("Bip01 L Calf", new Vector4f(0.74f, 0.53f, 0.37f, 1.0f));
            this.skeletonMeshEntity.setJointColor("Bip01 R Thigh", new Vector4f(0.58f, 0.27f, 0.64f, 1.0f));
            this.skeletonMeshEntity.setJointColor("Bip01 R Calf", new Vector4f(0.53f, 0.18f, 0.36f, 1.0f));
            this.skeletonMeshEntity.setJointColor("Bip01 Neck", new Vector4f(0.88f, 0.78f, 0.88f, 1.0f));
            this.skeletonMeshEntity.setJointColor("Bip01 HeadNub", new Vector4f(0.47f, 0.83f, 0.22f, 1.0f));
            this.skeletonMeshEntity.setJointColor("Bip01 L Clavicle", new Vector4f(0.93f, 0.15f, 0.95f, 1.0f));
            this.skeletonMeshEntity.setJointColor("Bip01 L Forearm", new Vector4f(0.95f, 0.03f, 0.63f, 1.0f));
            this.skeletonMeshEntity.setJointColor("Bip01 R Clavicle", new Vector4f(0.59f, 0.68f, 0.56f, 1.0f));
            this.skeletonMeshEntity.setJointColor("Bip01 R Forearm", new Vector4f(0.85f, 0.69f, 0.19f, 1.0f));

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

            this.dynamicsWorld = new DiscreteDynamicsWorld(collisionDispatcher, broadphase, constraintSolver,
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
            final RagdollStructure ragdollStructure = new RagdollStructureBuilder(this.meshEntity.getMesh())
                    .startPart(HEAD)
                        .addBones("Bip01 HeadNub", "Bip01 Head", "Bip01 Neck").endPart()
                    .startPart(CHEST)
                        .addBones("Bip01 Spine", "Bip01 Spine1", "Bip01 Pelvis").endPart()
                    .startPart(LEFT_UPPER_ARM)
                        .addBones("Bip01 L UpperArm").endPart()
                    .startPart(LEFT_LOWER_ARM)
                        .addBones("Bip01 L Forearm", "Bip01 L Hand", "Bip01 L Finger3").endPart()
                    .startPart(RIGHT_UPPER_ARM)
                        .addBones("Bip01 R UpperArm").endPart()
                    .startPart(RIGHT_LOWER_ARM)
                        .addBones("Bip01 R Forearm", "Bip01 R Hand", "Bip01 R Finger3").endPart()
                    .startPart(LEFT_UPPER_LEG)
                        .addBones("Bip01 L Thigh").endPart()
                    .startPart(LEFT_LOWER_LEG)
                        .addBones("Bip01 L Calf", "Bip01 L Foot").endPart()
                    .startPart(RIGHT_UPPER_LEG)
                        .addBones("Bip01 R Thigh").endPart()
                    .startPart(RIGHT_LOWER_LEG)
                        .addBones("Bip01 R Calf", "Bip01 R Foot").endPart()
                    .pivotBonesTo(LEFT_LOWER_LEG, "Bip01 L Toe0", "Bip01 L Toe0Nub")
                    .pivotBonesTo(RIGHT_LOWER_LEG, "Bip01 R Toe0", "Bip01 R Toe0Nub")
                    .pivotBonesTo(LEFT_LOWER_ARM, "Bip01 L Finger0", "Bip01 L Finger01", "Bip01 L Finger0Nub",
                            "Bip01 L Finger1", "Bip01 L Finger11", "Bip01 L Finger1Nub", "Bip01 L Finger2",
                            "Bip01 L Finger21", "Bip01 L Finger2Nub", "Bip01 L Finger31", "Bip01 L Finger3Nub",
                            "Bip01 L Finger4", "Bip01 L Finger41", "Bip01 L Finger4Nub")
                    .pivotBonesTo(RIGHT_LOWER_ARM, "Bip01 R Finger0", "Bip01 R Finger01", "Bip01 R Finger0Nub",
                            "Bip01 R Finger1", "Bip01 R Finger11", "Bip01 R Finger1Nub", "Bip01 R Finger2",
                            "Bip01 R Finger21", "Bip01 R Finger2Nub", "Bip01 R Finger31", "Bip01 R Finger3Nub",
                            "Bip01 R Finger4", "Bip01 R Finger41", "Bip01 R Finger4Nub")
                    .pivotBonesTo(CHEST, "Bip01", "Bip01 R Clavicle", "Bip01 L Clavicle")
                    .startLink(CHEST, HEAD)
                        .coneTwist(Utils.PI * 0.15f, Utils.PI * 0.15f, 0.05f).endLink()
                    .startLink(CHEST, LEFT_UPPER_ARM)
                        .coneTwist(Utils.PI * 0.6f, Utils.PI * 0.6f, 0.05f).endLink()
                    .startLink(LEFT_UPPER_ARM, LEFT_LOWER_ARM)
                        .hinge(0.0f, 2.0f, new Vector3f(0.0f, 0.0f, Utils.HALF_PI),
                                new Vector3f(0.0f, 0.0f, Utils.HALF_PI)).endLink()
                    .startLink(CHEST, RIGHT_UPPER_ARM)
                        .coneTwist(Utils.PI * 0.6f, Utils.PI * 0.6f, 0.05f).endLink()
                    .startLink(RIGHT_UPPER_ARM, RIGHT_LOWER_ARM)
                        .hinge(0.0f, 2.0f, new Vector3f(0.0f, 0.0f, -Utils.HALF_PI),
                                new Vector3f(0.0f, 0.0f, -Utils.HALF_PI)).endLink()
                    .startLink(CHEST, LEFT_UPPER_LEG)
                        .coneTwist(Utils.HALF_PI, Utils.HALF_PI, 0.05f).endLink()
                    .startLink(LEFT_UPPER_LEG, LEFT_LOWER_LEG)
                        .hinge(-2.0f, 0.0f, new Vector3f(0.0f, 0.0f, Utils.HALF_PI),
                                new Vector3f(0.0f, 0.0f, Utils.HALF_PI)).endLink()
                    .startLink(CHEST, RIGHT_UPPER_LEG)
                        .coneTwist(Utils.HALF_PI, Utils.HALF_PI, 0.05f).endLink()
                    .startLink(RIGHT_UPPER_LEG, RIGHT_LOWER_LEG)
                        .hinge(-2.0f, 0.0f, new Vector3f(0.0f, 0.0f, Utils.HALF_PI),
                            new Vector3f(0.0f, 0.0f, Utils.HALF_PI)).endLink()
                    .build();
            ragdoll.setRagdollStructure(ragdollStructure);
            ragdoll.buildRagdoll();

            //  Prepare debugging data.
            this.ragdollDebugger = new RagdollDebugger(ragdoll);
            this.ragdollDebugger.buildDebugInfo();
            this.physicsDebugger.updateDebugEntities();

            //  Create the text renderer and prepare the help text.
            final TrueTypeTextFont trueTypeTextFont = new TrueTypeTextFont("Arial", 12, "");
            this.text2DRenderer = new Text2DRenderer(trueTypeTextFont);
            this.text2DRenderer.setWindowDimensions(WINDOW_WIDTH, WINDOW_HEIGHT);
            final Vector4f fontColor = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);

            final String[] helpStrings = HELP_TEXT.split("\n");
            for (int i = 0; i < helpStrings.length; i++) {
                this.text2DRenderer.addLine(helpStrings[i], 10.0f, 10.0f + (10.0f * i), fontColor);
            }
            this.displayHelp = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void updateScene() {
        final double currentTime = glfwGetTime();

        this.deltaTime = (float) (currentTime - this.lastTime);
        this.lastTime = currentTime;

        if (this.physicsSimulation) {
            this.dynamicsWorld.stepSimulation(this.deltaTime, 10);

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

                    //  Convert between different math libraries.
                    tempVars.vecmathTransform.getRotation(tempVars.vecmathQuat);
                    Utils.convert(tempVars.quat1, tempVars.vecmathQuat);
                    Utils.convert(tempVars.vect3d1, tempVars.vecmathTransform.origin);

                    //  Assign transformation computed by jBullet to the entity.
                    final AbstractEntity abstractEntity = (AbstractEntity) collisionObject.getUserPointer();
                    abstractEntity.setTransformation(tempVars.quat1, tempVars.vect3d1, abstractEntity.getScale());
                }
            }
            tempVars.release();

            this.physicsDebugger.updateDebugEntities();
            this.ragdollDebugger.updateDebug();
        }
        this.meshEntity.getAnimationController().stepAnimation(this.deltaTime);
        this.skeletonMeshEntity.applyAnimation(this.meshEntity.getMeshRenderer().getBoneMatrices());
    }

    private void drawMeshEntity(final MeshEntity meshEntity, final Program program, final CameraEntity camera,
                                final Texture texture, final Vector4f diffuseColor) {
        final TempVars tempVars = TempVars.get();

        //  Prepare the model-view matrix.
        final Matrix4f modelViewMatrix = camera.getViewMatrix().mul(meshEntity.getTransformation(), tempVars.tempMat4x41);

        //  Start rendering.
        final MeshRenderer renderer = meshEntity.getMeshRenderer();
        renderer.initializeRendering();

        byte useTexturing = 0;
        if (texture != null) {
            texture.bind();
            useTexturing = 1;
        }

        //  Pass uniforms to the shader.
        program.setUniformMatrix4(Utils.MODELVIEW_UNIFORM, Utils.matrix4fToBuffer(modelViewMatrix));
        program.setUniformMatrix4(Utils.PROJECTION_UNIFORM, Utils.matrix4fToBuffer(camera.getProjectionMatrix()));
        program.setUniform1(Utils.TEXTURE_UNIFORM, 0);
        program.setUniform1(Utils.USETEXTURING_UNIFORM, useTexturing);
        program.setUniform1(Utils.USELIGHTING_UNIFORM, 0);
        program.setUniform4(Utils.DIFFUSECOLOR_UNIFORM, diffuseColor.x, diffuseColor.y, diffuseColor.z, diffuseColor.w);
        program.setUniform3(Utils.CAMERADIRECTION_UNIFORM, 0.0f, 0.0f, 1.0f);
        if (meshEntity.getMesh().hasSkeleton()) {
            program.setUniform1(Utils.USESKINNING_UNIFORM, 1);

            //  Apply the inverse bind transform to bone matrices.
            final List<Matrix4f> boneMatrices = renderer.getBoneMatrices();
            for (int i = 0; i < meshEntity.getMesh().getSkeleton().getBones().size(); i++) {
                final Bone bone = meshEntity.getMesh().getSkeleton().getBone(i);
                final Matrix4f boneMatrix = tempVars.boneMatricesList.get(i).set(boneMatrices.get(i));

                boneMatrix.mul(bone.getInverseBindMatrix(), boneMatrix);
            }

            program.setUniformMatrix4Array(Utils.BONES_UNIFORM, tempVars.boneMatricesList.size(),
                    Utils.matrices4fToBuffer(tempVars.boneMatricesList));
        } else {
            program.setUniform1(Utils.USESKINNING_UNIFORM, 0);
        }

        //  Draw the entity.
        renderer.renderMesh();

        //  Finalize rendering.
        renderer.finalizeRendering();
        if (texture != null) {
            texture.unbind();
        }

        tempVars.release();
    }

    private void drawScene() {
        //  Draw the skeleton mesh first.
        this.skeletonMeshEntity.drawSkeletonMesh(this.camera);

        //  Draw the skinned mesh.
        this.drawMeshEntity(this.meshEntity, this.meshProgram, this.camera, this.meshTexture, DIFFUSE_COLOR);

        //  Draw the physics debug.
        if (this.physicsDebug) {
            this.ragdollDebugger.drawDebug(this.camera);
            this.physicsDebugger.debugDrawWorld(this.camera);
        }

        if (this.displayHelp) {
            this.text2DRenderer.renderText();
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
