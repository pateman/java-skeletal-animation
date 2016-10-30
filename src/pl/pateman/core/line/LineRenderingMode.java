package pl.pateman.core.line;

import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_LINE_LOOP;
import static org.lwjgl.opengl.GL11.GL_LINE_STRIP;

/**
 * Created by pateman.
 */
public enum LineRenderingMode {
    DEFAULT(GL_LINES),
    LOOP(GL_LINE_LOOP),
    STRIP(GL_LINE_STRIP);

    private int oglConstant;
    LineRenderingMode(int oglConstant) {
        this.oglConstant = oglConstant;
    }

    int getOglConstant() {
        return oglConstant;
    }
}
