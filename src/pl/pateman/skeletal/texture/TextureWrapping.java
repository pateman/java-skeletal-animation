package pl.pateman.skeletal.texture;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

/**
 * Created by pateman.
 */
public enum TextureWrapping {
    REPEAT(GL11.GL_REPEAT),
    CLAMP_TO_EDGE(GL12.GL_CLAMP_TO_EDGE),
    CLAMP_TO_BORDER(GL13.GL_CLAMP_TO_BORDER),
    MIRRORED_REPEAT(GL14.GL_MIRRORED_REPEAT);

    private int glValue;

    TextureWrapping(int val) {
        this.glValue = val;
    }

    public int getGlValue() {
        return glValue;
    }
}
