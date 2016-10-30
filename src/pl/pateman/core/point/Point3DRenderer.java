package pl.pateman.core.point;

import org.joml.Vector3f;
import org.joml.Vector4f;
import pl.pateman.core.Clearable;
import pl.pateman.core.Utils;
import pl.pateman.core.entity.CameraEntity;
import pl.pateman.core.entity.mesh.VertexBufferObject;
import pl.pateman.core.shader.Program;
import pl.pateman.core.shader.Shader;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_POINTS;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_PROGRAM_POINT_SIZE;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL30.*;

/**
 * Created by pateman.
 */
public final class Point3DRenderer implements Clearable {
    private static final String THICKNESS_ATTRIBUTE = "Thickness";

    private final List<Point3D> point3DList;
    private final int vao;
    private final Program shaderProgram;

    private VertexBufferObject positionVBO;
    private VertexBufferObject colorVBO;
    private VertexBufferObject thicknessVBO;
    private boolean dirty;

    public Point3DRenderer() {
        this.point3DList = new ArrayList<>();

        this.vao = glGenVertexArrays();

        //  Create the shader program.
        final Shader vertexShader = new Shader(GL_VERTEX_SHADER);
        final Shader fragmentShader = new Shader(GL_FRAGMENT_SHADER);
        vertexShader.setSource(POINT_VERTEX_SHADER);
        fragmentShader.setSource(POINT_FRAGMENT_SHADER);

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

        this.dirty = true;
    }

    private void rebuildPointsVAO() {
        //  Check if VBOs are created first.
        if (this.positionVBO == null) {
            this.positionVBO = new VertexBufferObject(this.shaderProgram.getAttributeLocation(Utils.POSITION_ATTRIBUTE),
                    VertexBufferObject.DEFAULT_COMPONENT_SIZE);
        }
        if (this.colorVBO == null) {
            this.colorVBO = new VertexBufferObject(this.shaderProgram.getAttributeLocation(Utils.COLOR_ATTRIBUTE),
                    4);
        }
        if (this.thicknessVBO == null) {
            this.thicknessVBO = new VertexBufferObject(this.shaderProgram.getAttributeLocation(THICKNESS_ATTRIBUTE), 1);
        }

        //  Iterate over the list of points and prepare VBOs.
        final List<Vector3f> positions = new ArrayList<>(this.point3DList.size());
        final List<Vector4f> colors = new ArrayList<>(this.point3DList.size());
        final List<Float> thicknesses = new ArrayList<>(this.point3DList.size());

        this.point3DList.forEach(point3D -> {
            positions.add(point3D.getPosition());
            colors.add(point3D.getColor());
            thicknesses.add(point3D.getThickness());
        });

        this.positionVBO.update(Utils.vertices3fToBuffer(positions));
        this.colorVBO.update(Utils.vertices4fToBuffer(colors));
        this.thicknessVBO.update(Utils.floatsToBuffer(thicknesses));
    }

    public int addPoint(final Vector3f position, final Vector4f color) {
        return this.addPoint(position, color, 1.0f);
    }

    public int addPoint(final Vector3f position, final Vector4f color, float thickness) {
        if (position == null || color == null) {
            throw new IllegalArgumentException("Valid position and color need to be provided");
        }

        final Point3D point3D = new Point3D();
        point3D.getPosition().set(position);
        point3D.getColor().set(color);
        point3D.setThickness(thickness);
        this.point3DList.add(point3D);

        this.dirty = true;
        return this.point3DList.size() - 1;
    }

    public Point3D getPoint(final int index) {
        return this.point3DList.get(index);
    }

    public Point3D removePoint(final int index) {
        final Point3D removedPoint = this.point3DList.remove(index);
        if (removedPoint != null) {
            this.dirty = true;
        }
        return removedPoint;
    }

    public void clearPoints() {
        this.point3DList.clear();
        this.dirty = true;
    }

    public void forceDirty() {
        this.dirty = true;
    }

    public void drawPoints(final CameraEntity cameraEntity) {
        if (cameraEntity == null) {
            throw new IllegalArgumentException("A valid camera needs to be provided");
        }

        this.shaderProgram.bind();
        this.bind();

        glEnable(GL_VERTEX_PROGRAM_POINT_SIZE);

        //  Rebuild if needed.
        if (this.dirty) {
            this.rebuildPointsVAO();
            this.dirty = false;
        }

        //  Pass the necessary shader uniforms.
        this.shaderProgram.setUniformMatrix4(Utils.MODELVIEW_UNIFORM, Utils.matrix4fToBuffer(cameraEntity.
                getViewMatrix()));
        this.shaderProgram.setUniformMatrix4(Utils.PROJECTION_UNIFORM, Utils.matrix4fToBuffer(cameraEntity.
                getProjectionMatrix()));

        //  Draw the points.
        glDrawArrays(GL_POINTS, 0, this.point3DList.size());

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

    private static final String POINT_VERTEX_SHADER = "#version 330\n" +
            "\n" +
            "in vec3 Position;\n" +
            "in vec4 Color;\n" +
            "in float Thickness;\n" +
            "\n" +
            "out vec4 pointColor;\n" +
            "\n" +
            "uniform mat4 projection;\n" +
            "uniform mat4 modelView;\n" +
            "\n" +
            "void main() {\n" +
            "\tgl_Position = projection * modelView * vec4(Position, 1.0);\n" +
            "\tgl_PointSize = Thickness;\n" +
            "\t\n" +
            "\tpointColor = Color;\n" +
            "}";

    private static final String POINT_FRAGMENT_SHADER = "#version 330\n" +
            "\n" +
            "in vec4 pointColor;\n" +
            "out vec4 FragColor;\n" +
            "\n" +
            "void main() {\n" +
            "\tFragColor = pointColor;\n" +
            "}";
}
