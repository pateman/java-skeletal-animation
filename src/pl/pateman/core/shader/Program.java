package pl.pateman.core.shader;

import org.lwjgl.system.MemoryUtil;
import pl.pateman.core.Clearable;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;

/**
 * Created by Patryk.
 */
public class Program implements Clearable {
    private final Set<Shader> shaders;
    private final int handle;
    private boolean linked = false;
    private boolean inUse = false;
    private final Map<String, Integer> uniformLocations = new HashMap<>();
    private final Map<String, Integer> attributeLocations = new HashMap<>();

    public Program() {
        this.shaders = new HashSet<>();
        this.handle = glCreateProgram();
    }

    public Shader attachShader(final Shader shader) {
        if (this.shaders.add(shader)) {
            glAttachShader(this.handle, shader.getHandle());

            return shader;
        }
        return null;
    }

    public Shader detachShader(final Shader shader) {
        if (this.shaders.remove(shader)) {
            glDetachShader(this.handle, shader.getHandle());

            return shader;
        }
        return null;
    }

    public boolean link(boolean compileShaders) {
        if (this.linked) {
            return false;
        }

        if (this.shaders.isEmpty()) {
            throw new IllegalStateException("You must attach at least one shader");
        }

        if (compileShaders) {
            for (Shader shader : this.shaders) {
                shader.compile();
                if (!shader.isCompiled()) {
                    throw new IllegalStateException(String.format("One of the shaders cannot be compiled! Source: " +
                            "\n======\n%s\n======\nInfo log:\n======\n%s\n======", shader.getSource(),
                            shader.getInfoLog()));
                }
            }
        }

        glValidateProgram(this.handle);
        glLinkProgram(this.handle);
        this.linked = glGetProgrami(this.handle, GL_LINK_STATUS) != GL_FALSE;
        return this.linked;
    }

    public void unlink() {
        this.linked = false;
        if (this.inUse) {
            this.unbind();
        }

        Shader[] shaders = this.shaders.toArray(new Shader[this.shaders.size()]);
        for (int i = this.shaders.size() -1; i >= 0; i--) {
            this.detachShader(shaders[i]);
        }
    }

    public void bind() {
        if (!this.inUse && this.linked) {
            glUseProgram(this.handle);
            this.inUse = true;
        }
    }

    public void unbind() {
        this.inUse = false;
        glUseProgram(0);
    }

    public void bindAttributeLocation(final String attributeName, int attributeLocation) {
        if (!this.linked) {
            throw new IllegalStateException("Program must be linked first");
        }

        glBindAttribLocation(this.handle, attributeLocation, attributeName);
        this.attributeLocations.put(attributeName, attributeLocation);
    }

    public int getUniformLocation(final String uniformName) {
        if (!this.linked) {
            throw new IllegalStateException("Program must be linked first");
        }

        Integer location = this.uniformLocations.get(uniformName);
        if (location == null) {
            location = glGetUniformLocation(this.handle, uniformName);
            this.uniformLocations.put(uniformName, location);
        }
        return location;
    }

    public int getAttributeLocation(final String attributeName) {
        if (!this.linked) {
            throw new IllegalStateException("Program must be linked first");
        }

        Integer location = this.attributeLocations.get(attributeName);
        if (location == null) {
            location = glGetAttribLocation(this.handle, attributeName);
            this.attributeLocations.put(attributeName, location);
        }
        return location;
    }

    @Override
    public void clear() {
        this.unbind();
        this.unlink();

        for (Shader shader : this.shaders) {
            shader.clearAndDestroy();
        }
    }

    @Override
    public void clearAndDestroy() {
        this.clear();
        glDeleteProgram(this.handle);
    }

    public int getHandle() {
        return handle;
    }

    public boolean isLinked() {
        return linked;
    }

    public boolean isInUse() {
        return inUse;
    }

    public void setUniform1(final String uniformName, float arg1) {
        glUniform1f(this.getUniformLocation(uniformName), arg1);
    }

    public void setUniform1(final String uniformName, int arg1) {
        glUniform1i(this.getUniformLocation(uniformName), arg1);
    }

    public void setUniform2(final String uniformName, float arg1, float arg2) {
        glUniform2f(this.getUniformLocation(uniformName), arg1, arg2);
    }

    public void setUniform2(final String uniformName, int arg1, int arg2) {
        glUniform2i(this.getUniformLocation(uniformName), arg1, arg2);
    }

    public void setUniform3(final String uniformName, float arg1, float arg2, float arg3) {
        glUniform3f(this.getUniformLocation(uniformName), arg1, arg2, arg3);
    }

    public void setUniform3(final String uniformName, int arg1, int arg2, int arg3) {
        glUniform3i(this.getUniformLocation(uniformName), arg1, arg2, arg3);
    }

    public void setUniform4(final String uniformName, float arg1, float arg2, float arg3, float arg4) {
        glUniform4f(this.getUniformLocation(uniformName), arg1, arg2, arg3, arg4);
    }

    public void setUniform4(final String uniformName, int arg1, int arg2, int arg3, int arg4) {
        glUniform4i(this.getUniformLocation(uniformName), arg1, arg2, arg3, arg4);
    }

    public void setUniformMatrix4(final String uniformName, final FloatBuffer matrix) {
        glUniformMatrix4fv(this.getUniformLocation(uniformName), false, matrix);
    }

    public void setUniformMatrix4Transposed(final String uniformName, final FloatBuffer matrix) {
        glUniformMatrix4fv(this.getUniformLocation(uniformName), true, matrix);
    }

    public void setUniformMatrix3(final String uniformName, final FloatBuffer matrix) {
        glUniformMatrix3fv(this.getUniformLocation(uniformName), false, matrix);
    }

    public void setUniformMatrix3Transposed(final String uniformName, final FloatBuffer matrix) {
        glUniformMatrix3fv(this.getUniformLocation(uniformName), true, matrix);
    }

    public void setUniformMatrix4Array(final String uniformName, int matricesCount, final FloatBuffer matrices) {
        //  LWJGL 3 is missing one overload...
        nglUniformMatrix4fv(this.getUniformLocation(uniformName), matricesCount, false, MemoryUtil.
                memAddress(matrices));
    }

    public void setUniformMatrix4ArrayTransposed(final String uniformName, int matricesCount,
                                                 final FloatBuffer matrices) {
        //  LWJGL 3 is missing one overload...
        nglUniformMatrix4fv(this.getUniformLocation(uniformName), matricesCount, true, MemoryUtil.
                memAddress(matrices));
    }

    public String getInfoLog() {
        return glGetProgramInfoLog(this.handle);
    }
}
