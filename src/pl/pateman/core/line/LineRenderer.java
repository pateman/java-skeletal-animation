package pl.pateman.core.line;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL30;
import pl.pateman.core.Clearable;
import pl.pateman.core.Utils;
import pl.pateman.core.entity.CameraEntity;
import pl.pateman.core.entity.mesh.VertexBufferObject;
import pl.pateman.core.shader.Program;
import pl.pateman.core.shader.Shader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;

/**
 * Created by pateman.
 */
public class LineRenderer implements Iterable<Line>, Clearable {
    private final int vao;
    private final List<Line> lineList;
    private final List<Vector3f> positions;
    private final List<Vector4f> colors;
    private final Program shaderProgram;

    private VertexBufferObject positionVBO;
    private VertexBufferObject colorVBO;
    private boolean isDirty;
    private LineRenderingMode lineRenderingMode;

    public LineRenderer() throws IllegalStateException {
        this.lineList = new ArrayList<>();
        this.positions = new ArrayList<>();
        this.colors = new ArrayList<>();
        this.isDirty = true;
        this.lineRenderingMode = LineRenderingMode.DEFAULT;

        this.vao = GL30.glGenVertexArrays();

        //  Create the shader program.
        final Shader vertexShader = new Shader(GL_VERTEX_SHADER);
        final Shader fragmentShader = new Shader(GL_FRAGMENT_SHADER);
        vertexShader.setSource(LINE_VERTEX_SHADER);
        fragmentShader.setSource(LINE_FRAGMENT_SHADER);

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

    private void rebuildLinesVBO() {
        //  Check if VBOs are created first.
        if (this.positionVBO == null) {
            this.positionVBO = new VertexBufferObject(this.shaderProgram.getAttributeLocation(Utils.POSITION_ATTRIBUTE),
                    VertexBufferObject.DEFAULT_COMPONENT_SIZE);
        }
        if (this.colorVBO == null) {
            this.colorVBO = new VertexBufferObject(this.shaderProgram.getAttributeLocation(Utils.COLOR_ATTRIBUTE),
                    4);
        }

        //  There are no lines to process.
        if (this.lineList.isEmpty()) {
            return;
        }

        //  Flatten the positions and colors.
        for (int i = 0; i < this.lineList.size(); i++) {
            final Line line = this.lineList.get(i);

            this.positions.add(line.getFrom());
            this.positions.add(line.getTo());

            this.colors.add(line.getColor());
            this.colors.add(line.getColor());
        }

        //  Pass the flattened lists to VBOs.
        this.positionVBO.update(Utils.vertices3fToBuffer(this.positions));
        this.colorVBO.update(Utils.vertices4fToBuffer(this.colors));

        //  After updating VBOs, clear the lists.
        this.positions.clear();
        this.colors.clear();
    }

    public int addLine(final Line line) {
        this.lineList.add(line);

        this.isDirty = true;
        return this.lineList.size() -1;
    }

    public int addLine(final Vector3f from, final Vector3f to, final Vector4f color) {
        return this.addLine(new Line(from, to, color));
    }

    public void addWireBox(final Vector3f extents, final Vector4f color) {
        //  Create the list od vertices.
        final List<Vector3f> vertices = new ArrayList<>(8);
        vertices.add(new Vector3f(-extents.x, -extents.y, extents.z));
        vertices.add(new Vector3f(extents.x, -extents.y, extents.z));
        vertices.add(new Vector3f(extents.x, extents.y, extents.z));
        vertices.add(new Vector3f(-extents.x, extents.y, extents.z));
        vertices.add(new Vector3f(-extents.x, -extents.y, -extents.z));
        vertices.add(new Vector3f(extents.x, -extents.y, -extents.z));
        vertices.add(new Vector3f(extents.x, extents.y, -extents.z));
        vertices.add(new Vector3f(-extents.x, extents.y, -extents.z));

        //  Add lines now.
        this.addLine(vertices.get(0), vertices.get(1), color);
        this.addLine(vertices.get(1), vertices.get(2), color);
        this.addLine(vertices.get(2), vertices.get(3), color);
        this.addLine(vertices.get(3), vertices.get(0), color);

        this.addLine(vertices.get(4), vertices.get(5), color);
        this.addLine(vertices.get(5), vertices.get(6), color);
        this.addLine(vertices.get(6), vertices.get(7), color);
        this.addLine(vertices.get(7), vertices.get(4), color);

        this.addLine(vertices.get(0), vertices.get(4), color);
        this.addLine(vertices.get(1), vertices.get(5), color);
        this.addLine(vertices.get(2), vertices.get(6), color);
        this.addLine(vertices.get(3), vertices.get(7), color);
    }

    public Line removeLine(int index) {
        final Line removedLine = this.lineList.remove(index);
        if (removedLine != null) {
            this.isDirty = true;
        }
        return removedLine;
    }

    public Line getLine(int index) {
        return this.lineList.get(index);
    }

    public int getLineCount() {
        return this.lineList.size();
    }

    public void clearLines() {
        this.lineList.clear();
        this.isDirty = true;
    }

    public final void forceDirty() {
        this.isDirty = true;
    }

    public void drawLines(final CameraEntity cameraEntity) {
        if (this.shaderProgram == null) {
            throw new IllegalStateException("A valid shader program needs to be assigned");
        }

        if (cameraEntity == null) {
            throw new IllegalStateException("A valid camera is required");
        }

        this.shaderProgram.bind();
        this.bind();

        //  Check if we need to rebuild VBOs.
        if (this.isDirty) {
            this.rebuildLinesVBO();
            this.isDirty = false;
        }

        //  Pass the necessary shader uniforms.
        this.shaderProgram.setUniformMatrix4(Utils.MODELVIEW_UNIFORM, cameraEntity.getViewMatrix());
        this.shaderProgram.setUniformMatrix4(Utils.PROJECTION_UNIFORM, cameraEntity.getProjectionMatrix());

        //  Draw the lines. We multiply the number of lines by two to get the number of vertices.
        glDrawArrays(this.lineRenderingMode.getOglConstant(), 0, this.lineList.size() * 2);

        this.shaderProgram.unbind();
        this.unbind();
    }

    void bind() {
        glBindVertexArray(this.vao);
    }

    void unbind() {
        glBindVertexArray(0);
    }

    @Override
    public void clear() {
        this.bind();

        if (this.positionVBO != null) {
            this.positionVBO.clearAndDestroy();
        }
        if (this.colorVBO != null) {
            this.colorVBO.clearAndDestroy();
        }

        this.unbind();
    }

    @Override
    public void clearAndDestroy() {
        this.clear();

        this.shaderProgram.clearAndDestroy();
        glDeleteVertexArrays(this.vao);
    }

    public LineRenderingMode getLineRenderingMode() {
        return lineRenderingMode;
    }

    public void setLineRenderingMode(LineRenderingMode lineRenderingMode) {
        this.lineRenderingMode = lineRenderingMode;
    }

    @Override
    public Iterator<Line> iterator() {
        return this.lineList.iterator();
    }

    private static final String LINE_VERTEX_SHADER = "#version 330\n" +
            "\n" +
            "in vec3 Position;\n" +
            "in vec4 Color;\n" +
            "\n" +
            "out vec4 lineColor;\n" +
            "\n" +
            "uniform mat4 projection;\n" +
            "uniform mat4 modelView;\n" +
            "\n" +
            "void main() {\n" +
            "    lineColor = Color;\n" +
            "    gl_Position = projection * modelView * vec4(Position, 1.0);\n" +
            "}";

    private static final String LINE_FRAGMENT_SHADER = "#version 330\n" +
            "\n" +
            "in vec4 lineColor;\n" +
            "out vec4 FragColor;\n" +
            "\n" +
            "void main() {\n" +
            "    FragColor = lineColor;\n" +
            "}\n";
}
