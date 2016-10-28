package pl.pateman.core.text;

import org.joml.Vector2f;
import org.joml.Vector4f;
import pl.pateman.core.Clearable;
import pl.pateman.core.entity.mesh.VertexBufferObject;
import pl.pateman.core.shader.Program;
import pl.pateman.core.shader.Shader;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

/**
 * Renders a 2D text on the screen.
 *
 * Created by pateman.
 */
public class Text2DRenderer implements Clearable {
    private final TextFont textFont;
    private final int vao;
    private final Program shaderProgram;
    private VertexBufferObject positionVBO;
    private VertexBufferObject texcoordVBO;
    private final List<TextLine> textLines;

    public Text2DRenderer(final TextFont textFont) {
        if (textFont == null) {
            throw new IllegalArgumentException("A valid font needs to be provided");
        }
        this.textFont = textFont;
        this.textLines = new ArrayList<>();

        this.vao = glGenVertexArrays();

        //  Create the shader program.
        final Shader vertexShader = new Shader(GL_VERTEX_SHADER);
        final Shader fragmentShader = new Shader(GL_FRAGMENT_SHADER);
        vertexShader.setSource(TEXT_2D_VERTEX_SHADER);
        fragmentShader.setSource(TEXT_2D_FRAGMENT_SHADER);

        if (!vertexShader.compile()) {
            throw new IllegalStateException(vertexShader.getInfoLog());
        }
        if (!fragmentShader.compile()) {
            throw new IllegalStateException(fragmentShader.getInfoLog());
        }

        this.shaderProgram = new Program();
        this.shaderProgram.attachShader(vertexShader);
        this.shaderProgram.attachShader(fragmentShader);
        if (!this.shaderProgram.link(false)) {
            throw new IllegalStateException(this.shaderProgram.getInfoLog());
        }
    }

    @Override
    public void clear() {
        if (this.positionVBO != null) {
            this.positionVBO.clearAndDestroy();
        }
        if (this.texcoordVBO != null) {
            this.texcoordVBO.clearAndDestroy();
        }
    }

    @Override
    public void clearAndDestroy() {
        this.clear();

        this.shaderProgram.clearAndDestroy();
        glDeleteVertexArrays(this.vao);
    }

    private class TextLine {
        private final String text;
        private final Vector2f position;
        private final Vector4f color;

        public TextLine(final String text) {
            this.text = text;
            this.position = new Vector2f();
            this.color = new Vector4f().set(1.0f);
        }

        public String getText() {
            return text;
        }

        public Vector2f getPosition() {
            return position;
        }

        public Vector4f getColor() {
            return color;
        }
    }

    private static final String TEXT_2D_VERTEX_SHADER = "#version 150 \n" +
            "precision highp float; \n" +
            "uniform mat4 proj; \n" +
            "uniform mat4 modelview; \n" +
            "in vec4 vertex; \n" +
            "in vec2 texCoord; \n" +
            "out vec2 textureCoord;\n" +
            " \n" +
            "void main(void) { \n" +
            "\ttextureCoord = texCoord; \n" +
            "\tgl_Position = proj * modelview * vertex; \n" +
            "};";

    private static final String TEXT_2D_FRAGMENT_SHADER = "#version 150 \n" +
            "\n" +
            "precision highp float; \n" +
            "uniform vec4 fontColor; \n" +
            "uniform sampler2D texture; \n" +
            "in vec2 textureCoord; \n" +
            "out vec4 fragColor; \n" +
            "\n" +
            "void main(void) { \n" +
            "\tfragColor = fontColor * texture2D(texture, textureCoord); \n" +
            "}";
}
