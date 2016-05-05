package pl.pateman.core.shader;

import pl.pateman.core.Clearable;
import pl.pateman.core.Utils;

import java.io.IOException;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;

/**
 * Created by Patryk on 2015-07-28.
 */
public class Shader implements Clearable {
    private final int type;
    private final int handle;
    private boolean compiled = false;
    private String source;

    public Shader(int shaderType) {
        this.type = shaderType;
        this.handle = glCreateShader(this.type);
    }

    public boolean compile() {
        if (this.compiled) {
            return false;
        }

        if (this.source == null || this.source.trim().isEmpty()) {
            throw new IllegalStateException("You must provide a valid shader source");
        }

        if (this.handle == 0) {
            throw new IllegalStateException("Invalid shader handle");
        }

        glShaderSource(this.handle, this.source);
        glCompileShader(this.handle);

        this.compiled = glGetShaderi(this.handle, GL_COMPILE_STATUS) != GL_FALSE;
        return this.compiled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Shader shader = (Shader) o;

        if (getType() != shader.getType()) return false;
        return getHandle() == shader.getHandle();

    }

    @Override
    public int hashCode() {
        int result = getType();
        result = 31 * result + getHandle();
        return result;
    }

    public void load(final String shaderResource) throws IOException {
        this.source = Utils.readResource(shaderResource);
    }

    @Override
    public void clear() {
        glDeleteShader(this.handle);
    }

    @Override
    public void clearAndDestroy() {
        this.clear();
    }

    public int getType() {
        return type;
    }

    public int getHandle() {
        return handle;
    }

    public boolean isCompiled() {
        return compiled;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getInfoLog() {
        return glGetShaderInfoLog(this.handle);
    }
}
