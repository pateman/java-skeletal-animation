package pl.pateman.skeletal;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.List;

/**
 * Created by pateman.
 */
public final class Utils {
    public static final String POSITION_ATTRIBUTE = "Position";
    public static final String NORMAL_ATTRIBUTE = "Normal";
    public static final String TEXCOORD_ATTRIBUTE = "TexCoord";
    public static final String MODELVIEW_UNIFORM = "modelView";
    public static final String PROJECTION_UNIFORM = "projection";
    public static final String TEXTURE_UNIFORM = "texture";

    private Utils() {
    }

    public static String readResource(String resourcePath) throws IOException {
        try (InputStream is = Utils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

    public static InputStream getResourceStream(String resourceName) throws IOException {
        InputStream is = Utils.class.getClassLoader().getResourceAsStream(resourceName);
        if (is == null) {
            throw new IOException("Unable to locate resource");
        }
        return is;
    }

    public static FloatBuffer vertices2fToBuffer(final List<Vector2f> vertices) {
        if (vertices == null || vertices.isEmpty()) {
            throw new IllegalArgumentException();
        }

        final FloatBuffer fb = BufferUtils.createFloatBuffer(vertices.size() * 2);
        for (Vector2f vector : vertices) {
            vector.get(fb);
            fb.position(fb.position() + 2);
        }
        fb.flip();

        return fb;
    }

    public static FloatBuffer vertices3fToBuffer(final List<Vector3f> vertices) {
        if (vertices == null || vertices.isEmpty()) {
            throw new IllegalArgumentException();
        }

        final FloatBuffer fb = BufferUtils.createFloatBuffer(vertices.size() * 3);
        for (Vector3f vector : vertices) {
            vector.get(fb);
            fb.position(fb.position() + 3);
        }
        fb.flip();

        return fb;
    }

    public static FloatBuffer matrix4fToBuffer(final Matrix4f matrix) {
        if (matrix == null) {
            throw new IllegalArgumentException();
        }

        return matrix.get(BufferUtils.createFloatBuffer(16));
    }
}
