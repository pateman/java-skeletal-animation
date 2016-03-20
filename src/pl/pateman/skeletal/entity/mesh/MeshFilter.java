package pl.pateman.skeletal.entity.mesh;

import org.lwjgl.BufferUtils;
import pl.pateman.skeletal.Clearable;
import pl.pateman.skeletal.Utils;
import pl.pateman.skeletal.mesh.Mesh;
import pl.pateman.skeletal.shader.Program;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Created by pateman.
 */
public final class MeshFilter implements Clearable {
    private final int vao;
    private final Map<String, VertexBufferObject> vbos;

    private final List<MeshFace> faces;
    private Mesh meshData;
    private Program shaderProgram;

    public MeshFilter() {
        this.faces = new ArrayList<>();
        this.vbos = new HashMap<>();
        this.vao = glGenVertexArrays();
    }

    public void bind() {
        glBindVertexArray(this.vao);
    }

    public void unbind() {
        glBindVertexArray(0);
    }

    public void addBuffer(final String attributeName, int attributeLocation) {
        this.addBuffer(attributeName, attributeLocation, VertexBufferObject.DEFAULT_COMPONENT_SIZE);
    }

    public void addBuffer(final String attributeName, int attributeLocation, int componentSize) {
        if (attributeName == null) {
            throw new IllegalArgumentException();
        }

        if (this.vbos.containsKey(attributeName)) {
            throw new IllegalStateException("Buffer already defined for this attribute");
        }

        final VertexBufferObject newVBO = new VertexBufferObject(attributeLocation, componentSize);
        this.vbos.put(attributeName, newVBO);
    }

    public void removeBuffer(final String attributeName) {
        this.vbos.remove(attributeName).clearAndDestroy();
    }

    public void removeBuffers() {
        Iterator<Map.Entry<String, VertexBufferObject>> it = this.vbos.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, VertexBufferObject> buffer = it.next();
            buffer.getValue().clearAndDestroy();
            it.remove();
        }
    }

    public void updateBufferData(final String attributeName, final FloatBuffer data) {
        if (attributeName == null) {
            throw new IllegalArgumentException();
        }

        VertexBufferObject vbo = this.vbos.get(attributeName);
        if (vbo == null) {
            throw new IllegalStateException("No buffer defined for this attribute");
        }
        vbo.update(data);
    }

    public void updateBufferData(final String attributeName, final IntBuffer data) {
        if (attributeName == null) {
            throw new IllegalArgumentException();
        }

        VertexBufferObject vbo = this.vbos.get(attributeName);
        if (vbo == null) {
            throw new IllegalStateException("No buffer defined for this attribute");
        }
        vbo.update(data);
    }

    public void updateFacesData() {
        for (MeshFace face : this.faces) {
            face.rebuild();
        }
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
        this.faces.clear();

        this.bind();

        //  Create mesh faces.
        for (int i = 0; i < this.meshData.getTriangles().size(); i+=3) {
            final MeshFace meshFace = new MeshFace(this.meshData.getTriangles().get(i),
                    this.meshData.getTriangles().get(i + 1), this.meshData.getTriangles().get(i + 2));
            this.faces.add(meshFace);
        }

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

        //  Update mesh faces.
        this.updateFacesData();

        this.unbind();
    }

    List<MeshFace> getFaces() {
        return faces;
    }

    @Override
    public void clear() {
        this.bind();

        for (VertexBufferObject vbo : this.vbos.values()) {
            vbo.clearAndDestroy();
        }

        for (MeshFace face : this.faces) {
            face.clearAndDestroy();
        }

        this.unbind();
    }

    @Override
    public void clearAndDestroy() {
        this.clear();

        glDeleteVertexArrays(this.vao);
    }

    class MeshFace implements Clearable {
        private final int handle;
        private final List<Integer> indices;
        private IntBuffer faceBuffer;

        public MeshFace() {
            this.indices = new ArrayList<>();

            //  Generate the handle.
            this.handle = glGenBuffers();
        }

        public MeshFace(int... indicesList) {
            this();

            for (int index : indicesList) {
                this.indices.add(index);
            }
        }

        public void rebuild() {
            if (!this.indices.isEmpty()) {
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.handle);

                glBufferData(GL_ELEMENT_ARRAY_BUFFER, this.getFaceBuffer(), GL_STATIC_DRAW);
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
            }
        }

        @Override
        public void clear() {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.handle);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, 0, null, GL_STATIC_DRAW);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        }

        @Override
        public void clearAndDestroy() {
            this.clear();
            glDeleteBuffers(this.handle);
        }

        public List<Integer> getIndices() {
            return indices;
        }

        public IntBuffer getFaceBuffer() {
            //  Rebuild the buffer.
            if (this.faceBuffer != null) {
                this.faceBuffer.clear();
            }
            this.faceBuffer = BufferUtils.createIntBuffer(this.indices.size());
            for (int index : this.indices) {
                this.faceBuffer.put(index);
            }
            this.faceBuffer.flip();

            return this.faceBuffer;
        }

        public int getHandle() {
            return handle;
        }
    }
}
