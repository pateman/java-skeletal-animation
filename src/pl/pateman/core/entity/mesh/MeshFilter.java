package pl.pateman.core.entity.mesh;

import org.lwjgl.BufferUtils;
import pl.pateman.core.Clearable;
import pl.pateman.core.Utils;
import pl.pateman.core.mesh.Mesh;
import pl.pateman.core.mesh.MeshSkinningInfo;
import pl.pateman.core.shader.Program;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.lwjgl.opengl.GL30.*;

/**
 * Created by pateman.
 */
public final class MeshFilter implements Clearable {
    private final int vao;
    private final Map<String, VertexBufferObject> vbos;

    private final ElementBufferObject ebo;
    private Mesh meshData;
    private Program shaderProgram;

    public MeshFilter() {
        this.vbos = new HashMap<>();
        this.vao = glGenVertexArrays();
        this.ebo = new ElementBufferObject();
    }

    void bind() {
        glBindVertexArray(this.vao);
    }

    void unbind() {
        glBindVertexArray(0);
    }

    void addBuffer(final String attributeName, int attributeLocation) {
        this.addBuffer(attributeName, attributeLocation, VertexBufferObject.DEFAULT_COMPONENT_SIZE);
    }

    void addBuffer(final String attributeName, int attributeLocation, int componentSize) {
        if (attributeName == null) {
            throw new IllegalArgumentException();
        }

        if (this.vbos.containsKey(attributeName)) {
            throw new IllegalStateException("Buffer already defined for this attribute");
        }

        final VertexBufferObject newVBO = new VertexBufferObject(attributeLocation, componentSize);
        this.vbos.put(attributeName, newVBO);
    }

    void removeBuffer(final String attributeName) {
        this.vbos.remove(attributeName).clearAndDestroy();
    }

    void removeBuffers() {
        Iterator<Map.Entry<String, VertexBufferObject>> it = this.vbos.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, VertexBufferObject> buffer = it.next();
            buffer.getValue().clearAndDestroy();
            it.remove();
        }
    }

    void updateBufferData(final String attributeName, final FloatBuffer data) {
        if (attributeName == null) {
            throw new IllegalArgumentException();
        }

        VertexBufferObject vbo = this.vbos.get(attributeName);
        if (vbo == null) {
            throw new IllegalStateException("No buffer defined for this attribute");
        }
        vbo.update(data);
    }

    void updateBufferData(final String attributeName, final IntBuffer data) {
        if (attributeName == null) {
            throw new IllegalArgumentException();
        }

        VertexBufferObject vbo = this.vbos.get(attributeName);
        if (vbo == null) {
            throw new IllegalStateException("No buffer defined for this attribute");
        }
        vbo.update(data);
    }

    public Mesh getMeshData() {
        return meshData;
    }

    public void setMeshData(Mesh meshData) {
        this.meshData = meshData;
    }

    public Program getShaderProgram() {
        return shaderProgram;
    }

    public void setShaderProgram(Program shaderProgram) {
        this.shaderProgram = shaderProgram;
    }

    public void buildMeshFilter() {
        if (this.meshData == null || this.shaderProgram == null) {
            return;
        }

        //  Remove existing data.
        this.removeBuffers();
        this.clear();

        this.bind();

        //  Create the EBO and pass data to it.
        final IntBuffer eboBuffer = BufferUtils.createIntBuffer(this.meshData.getTriangles().size());
        for (int i = 0; i < this.meshData.getTriangles().size(); i++) {
            eboBuffer.put(this.meshData.getTriangles().get(i));
        }
        eboBuffer.flip();
        this.ebo.update(eboBuffer);

        //  Create VBOs.
        this.addBuffer(Utils.POSITION_ATTRIBUTE, this.shaderProgram.getAttributeLocation(Utils.POSITION_ATTRIBUTE));
        this.addBuffer(Utils.NORMAL_ATTRIBUTE, this.shaderProgram.getAttributeLocation(Utils.NORMAL_ATTRIBUTE));
        this.addBuffer(Utils.TEXCOORD_ATTRIBUTE, this.shaderProgram.getAttributeLocation(Utils.TEXCOORD_ATTRIBUTE), 2);

        //  Pass mesh data to VBOs.
        if (!this.meshData.getVertices().isEmpty()) {
            this.updateBufferData(Utils.POSITION_ATTRIBUTE, Utils.vertices3fToBuffer(this.meshData.getVertices()));
        }
        if (!this.meshData.getNormals().isEmpty()) {
            this.updateBufferData(Utils.NORMAL_ATTRIBUTE, Utils.vertices3fToBuffer(this.meshData.getNormals()));
        }
        if (!this.meshData.getTexcoords().isEmpty()) {
            this.updateBufferData(Utils.TEXCOORD_ATTRIBUTE, Utils.vertices2fToBuffer(this.meshData.getTexcoords()));
        }

        //  Check if skinning info is available.
        final MeshSkinningInfo skinningInfo = this.meshData.getSkinningInfo();
        if (skinningInfo.hasSkinningInfo()) {
            this.addBuffer(Utils.INDICES_ATTRIBUTE, this.shaderProgram.getAttributeLocation(Utils.INDICES_ATTRIBUTE));
            this.addBuffer(Utils.WEIGHTS_ATTRIBUTE, this.shaderProgram.getAttributeLocation(Utils.WEIGHTS_ATTRIBUTE));

            this.updateBufferData(Utils.INDICES_ATTRIBUTE, Utils.vertices3fToBuffer(skinningInfo.getBoneIndices()));
            this.updateBufferData(Utils.WEIGHTS_ATTRIBUTE, Utils.vertices3fToBuffer(skinningInfo.getBoneWeights()));
        }

        this.unbind();
    }

    public ElementBufferObject getEbo() {
        return this.ebo;
    }

    @Override
    public void clear() {
        this.bind();

        for (VertexBufferObject vbo : this.vbos.values()) {
            vbo.clearAndDestroy();
        }

        this.ebo.clear();

        this.unbind();
    }

    @Override
    public void clearAndDestroy() {
        this.clear();

        glDeleteVertexArrays(this.vao);
    }
}
