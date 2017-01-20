package pl.pateman.core.entity.mesh;

import pl.pateman.core.Clearable;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;

/**
 * Created by pateman.
 */
public final class ElementBufferObject implements Clearable {
    private final int handle;

    public ElementBufferObject() {
        this.handle = glGenBuffers();
    }

    public void bind() {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.handle);
    }

    public void unbind() {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void update(final IntBuffer intBuffer) {
        this.bind();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, intBuffer, GL_STATIC_DRAW);
        this.unbind();
    }

    @Override
    public void clear() {
        this.bind();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, 0, null, GL_STATIC_DRAW);
        this.unbind();
    }

    @Override
    public void clearAndDestroy() {
        this.clear();
        glDeleteBuffers(this.handle);
    }
}
