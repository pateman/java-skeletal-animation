package pl.pateman.skeletal.entity.mesh;

import org.lwjgl.opengl.GL11;
import pl.pateman.skeletal.Clearable;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

/**
 * Created by pateman
 */
public final class VertexBufferObject implements Clearable {
    public static final int DEFAULT_COMPONENT_SIZE = 3;

    private final int shaderAttributeLocation;
    private final int componentSize;
    private final int handle;

    private VertexBufferObject() {
        this(-1, 0);
    }

    VertexBufferObject(int attribLocation, int componentSize) {
        this.shaderAttributeLocation = attribLocation;
        this.componentSize = componentSize;

        this.handle = glGenBuffers();
    }

    public void update(final FloatBuffer fb) {
        glBindBuffer(GL_ARRAY_BUFFER, this.handle);
        glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
        glEnableVertexAttribArray(this.shaderAttributeLocation);
        glVertexAttribPointer(this.shaderAttributeLocation, this.componentSize, GL11.GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public void update(final IntBuffer ib) {
        glBindBuffer(GL_ARRAY_BUFFER, this.handle);
        glBufferData(GL_ARRAY_BUFFER, ib, GL_STATIC_DRAW);
        glEnableVertexAttribArray(this.shaderAttributeLocation);
        glVertexAttribPointer(this.shaderAttributeLocation, this.componentSize, GL11.GL_INT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void clear() {
        glBindBuffer(GL_ARRAY_BUFFER, this.handle);
        glBufferData(GL_ARRAY_BUFFER, 0, null, GL_STATIC_DRAW);
        glDisableVertexAttribArray(this.shaderAttributeLocation);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void clearAndDestroy() {
        this.clear();
        glDeleteBuffers(this.handle);
    }
}
