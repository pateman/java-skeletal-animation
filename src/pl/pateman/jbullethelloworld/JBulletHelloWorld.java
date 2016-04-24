package pl.pateman.jbullethelloworld;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
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

    void run() {
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

    void loop() {
        GL.createCapabilities();
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glEnable(GL_DEPTH_TEST);

        while (glfwWindowShouldClose(this.window) == GLFW_FALSE) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glfwSwapBuffers(this.window);
            glfwPollEvents();
        }
    }

    public static void main(String[] args) {
        new JBulletHelloWorld().run();
    }
}
