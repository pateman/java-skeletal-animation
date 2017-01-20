package pl.pateman.core.text;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import pl.pateman.core.Clearable;
import pl.pateman.core.TempVars;
import pl.pateman.core.Utils;
import pl.pateman.core.entity.mesh.VertexBufferObject;
import pl.pateman.core.shader.Program;
import pl.pateman.core.shader.Shader;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL30.*;

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
    private boolean dirty;
    private final Matrix4f projMatrix;

    public Text2DRenderer(final TextFont textFont) {
        if (textFont == null) {
            throw new IllegalArgumentException("A valid font needs to be provided");
        }
        this.textFont = textFont;
        this.textLines = new ArrayList<>();
        this.dirty = true;

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

        this.projMatrix = new Matrix4f();
    }

    private void createCharacterQuad(float posX, float posY, final TextGlyph textGlyph, final List<Vector3f> vertices,
                                     final List<Vector2f> texCoords) {
        //  Calculate vertex positions.
        final float x2 = posX + textGlyph.getW();
        final float y2 = posY + textGlyph.getH();

        //  Calculate texture coordinates.
        final float s1 = (float) textGlyph.getX() / this.textFont.getFontTexture().getTextureInformation().getWidth();
        final float t1 = (float) textGlyph.getY() / this.textFont.getFontTexture().getTextureInformation().getHeight();
        final float s2 = (float) (textGlyph.getX() + textGlyph.getW()) / this.textFont.getFontTexture().getTextureInformation().getWidth();
        final float t2 = (float) (textGlyph.getY() + textGlyph.getH()) / this.textFont.getFontTexture().getTextureInformation().getHeight();

        vertices.add(new Vector3f(posX, posY, 0.0f)); texCoords.add(new Vector2f(s1, t1));
        vertices.add(new Vector3f(posX, y2, 0.0f)); texCoords.add(new Vector2f(s1, t2));
        vertices.add(new Vector3f(x2, y2, 0.0f)); texCoords.add(new Vector2f(s2, t2));

        vertices.add(new Vector3f(posX, posY, 0.0f)); texCoords.add(new Vector2f(s1, t1));
        vertices.add(new Vector3f(x2, y2, 0.0f)); texCoords.add(new Vector2f(s2, t2));
        vertices.add(new Vector3f(x2, posY, 0.0f)); texCoords.add(new Vector2f(s2, t1));
    }

    void bind() {
        glBindVertexArray(this.vao);
    }

    void unbind() {
        glBindVertexArray(0);
    }

    public void renderText() {
        //  Check if VBOs are created first.
        if (this.positionVBO == null) {
            this.positionVBO = new VertexBufferObject(this.shaderProgram.getAttributeLocation(Utils.POSITION_ATTRIBUTE),
                    VertexBufferObject.DEFAULT_COMPONENT_SIZE);
        }
        if (this.texcoordVBO == null) {
            this.texcoordVBO = new VertexBufferObject(this.shaderProgram.getAttributeLocation(Utils.TEXCOORD_ATTRIBUTE),
                    2);
        }

        final int numLines = this.textLines.size();

        this.bind();
        this.shaderProgram.bind();
        this.textFont.getFontTexture().bind();

        //  Check if the renderer is dirty. If so, we need to re-compute the VBOs.
        if (this.dirty) {
            final List<Vector3f> vector3fList = new ArrayList<>();
            final List<Vector2f> vector2fList = new ArrayList<>();

            for (int i = 0; i < numLines; i++) {
                final TextLine textLine = this.textLines.get(i);
                textLine.setVertexCount(0);

                float drawX = textLine.getPosition().x;
                float drawY = textLine.getPosition().y;
                if (textLine.getHeight() > this.textFont.getFontHeight()) {
                    drawY += textLine.getHeight() - this.textFont.getFontHeight();
                }

                for (int j = 0; j < textLine.getText().length(); j++) {
                    final char character = textLine.getText().charAt(j);
                    final TextGlyph glyph = this.textFont.getGlyphs().get(character);

                    //  If we have a glyph for this character, prepare a quad for it.
                    if (glyph != null) {
                        this.createCharacterQuad(drawX, drawY, glyph, vector3fList, vector2fList);
                        textLine.setVertexCount(textLine.getVertexCount() + 6);
                        drawX += glyph.getW();
                    }
                }
            }

            //  Update VBOs.
            this.positionVBO.update(Utils.vertices3fToBuffer(vector3fList));
            this.texcoordVBO.update(Utils.vertices2fToBuffer(vector2fList));

            this.dirty = false;
        }

        //  Start rendering.
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        this.shaderProgram.setUniformMatrix4(Utils.PROJECTION_UNIFORM, this.projMatrix);
        this.shaderProgram.setUniform1(Utils.TEXTURE_UNIFORM, this.textFont.getFontTexture().getUnit());
        final TempVars tempVars = TempVars.get();
        int totalVertexCount = 0;
        for (int i = 0; i < numLines; i++) {
            final TextLine textLine = this.textLines.get(i);

            //  Calculate the model-view matrix.
            tempVars.vect3d1.set(textLine.getPosition(), 0.0f);
            tempVars.tempMat4x41.identity().translate(tempVars.vect3d1);

            this.shaderProgram.setUniformMatrix4(Utils.MODELVIEW_UNIFORM, tempVars.tempMat4x41);
            this.shaderProgram.setUniform4(Utils.DIFFUSECOLOR_UNIFORM, textLine.getColor().x, textLine.getColor().y,
                    textLine.getColor().z, textLine.getColor().w);

            //  Render the text.
            glDrawArrays(GL_TRIANGLES, totalVertexCount, textLine.getVertexCount());
            totalVertexCount += textLine.getVertexCount();
        }
        tempVars.release();

        //  Turn OpenGL's state machine back to what we've had before.
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);

        this.textFont.getFontTexture().unbind();
        this.shaderProgram.unbind();
        this.unbind();
    }

    public int addLine(final String text, float x, float y, final Vector4f color) {
        if (text == null || text.isEmpty() || color == null) {
            throw new IllegalArgumentException("Valid text and color need to be provided");
        }

        //  Sanitize the text.
        final String textToAdd = text.replaceAll("\n", "").replaceAll("\r", "");
        if (textToAdd.isEmpty()) {
            throw new IllegalStateException("The given text is empty after sanitizing");
        }

        final TextLine textLine = new TextLine(textToAdd);
        textLine.getPosition().set(x, y);
        textLine.getColor().set(color);

        this.textLines.add(textLine);
        this.dirty = true;
        return this.textLines.indexOf(textLine);
    }

    public void removeLine(final int index) {
        this.textLines.remove(index);
        this.dirty = true;
    }

    public void clearLines() {
        this.textLines.clear();
        this.dirty = true;
    }

    public void setWindowDimensions(final int w, final int h) {
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Valid window dimensions are required");
        }

        this.projMatrix.ortho2D(0.0f, w, h, 0.0f);
        this.dirty = true;
    }

    @Override
    public void clear() {
        this.bind();

        if (this.positionVBO != null) {
            this.positionVBO.clearAndDestroy();
        }
        if (this.texcoordVBO != null) {
            this.texcoordVBO.clearAndDestroy();
        }

        this.unbind();
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
        private final int height;
        private int vertexCount;

        public TextLine(final String text) {
            this.text = text;
            this.position = new Vector2f();
            this.color = new Vector4f().set(1.0f);
            this.height = this.calculateHeight();
        }

        private int calculateHeight() {
            int height = 0;

            for (int i = 0; i < this.text.length(); i++) {
                final TextGlyph glyph = Text2DRenderer.this.textFont.getGlyphs().get(this.text.charAt(i));

                if (glyph != null) {
                    height = Math.max(height, glyph.getH());
                }
            }

            return height;
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

        public int getHeight() {
            return height;
        }

        public int getVertexCount() {
            return vertexCount;
        }

        public void setVertexCount(int vertexCount) {
            this.vertexCount = vertexCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TextLine textLine = (TextLine) o;

            if (!text.equals(textLine.text)) return false;
            if (!position.equals(textLine.position)) return false;
            return color.equals(textLine.color);

        }

        @Override
        public int hashCode() {
            return text.hashCode();
        }
    }

    private static final String TEXT_2D_VERTEX_SHADER = "#version 150 \n" +
            "precision highp float; \n" +
            "uniform mat4 projection; \n" +
            "uniform mat4 modelView; \n" +
            "in vec3 Position; \n" +
            "in vec2 TexCoord; \n" +
            "out vec2 textureCoord;\n" +
            " \n" +
            "void main(void) { \n" +
            "\ttextureCoord = TexCoord; \n" +
            "\tgl_Position = projection * modelView * vec4(Position, 1.0); \n" +
            "};";

    private static final String TEXT_2D_FRAGMENT_SHADER = "#version 150 \n" +
            "\n" +
            "precision highp float; \n" +
            "uniform vec4 diffuseColor; \n" +
            "uniform sampler2D texture; \n" +
            "in vec2 textureCoord; \n" +
            "out vec4 fragColor; \n" +
            "\n" +
            "void main(void) { \n" +
            "\tfragColor = diffuseColor * texture2D(texture, textureCoord); \n" +
            "}";
}
