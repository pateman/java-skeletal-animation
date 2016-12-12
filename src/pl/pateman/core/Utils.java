package pl.pateman.core;

import com.bulletphysics.linearmath.Transform;
import org.joml.*;
import org.lwjgl.BufferUtils;

import javax.vecmath.Quat4f;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pateman.
 */
public final class Utils {
    public static final String POSITION_ATTRIBUTE = "Position";
    public static final String NORMAL_ATTRIBUTE = "Normal";
    public static final String TEXCOORD_ATTRIBUTE = "TexCoord";
    public static final String INDICES_ATTRIBUTE = "BoneIndices";
    public static final String WEIGHTS_ATTRIBUTE = "BoneWeights";
    public static final String COLOR_ATTRIBUTE = "Color";
    public static final String MODELVIEW_UNIFORM = "modelView";
    public static final String PROJECTION_UNIFORM = "projection";
    public static final String TEXTURE_UNIFORM = "texture";
    public static final String BONES_UNIFORM = "bones";
    public static final String USESKINNING_UNIFORM = "useSkinning";
    public static final String USETEXTURING_UNIFORM = "useTexturing";
    public static final String USELIGHTING_UNIFORM = "useLighting";
    public static final String CAMERADIRECTION_UNIFORM = "cameraDirection";
    public static final String DIFFUSECOLOR_UNIFORM = "diffuseColor";

    public static final float QUARTER_PI = 0.25f * (float) Math.PI;
    public static final float HALF_PI = 0.5f * (float) Math.PI;
    public static final float TWO_PI = 2.0f * (float) Math.PI;
    public static final float EPSILON = 1.19209290e-07f;
    public static final float PI = (float) Math.PI;
    public static final Vector3f AXIS_X = new Vector3f(1.0f, 0.0f, 0.0f);
    public static final Vector3f AXIS_Y = new Vector3f(0.0f, 1.0f, 0.0f);
    public static final Vector3f AXIS_Z = new Vector3f(0.0f, 0.0f, 1.0f);
    public static final Vector3f NEG_AXIS_X = new Vector3f(-1.0f, 0.0f, 0.0f);
    public static final Vector3f NEG_AXIS_Y = new Vector3f(0.0f, -1.0f, 0.0f);
    public static final Vector3f NEG_AXIS_Z = new Vector3f(0.0f, 0.0f, -1.0f);
    public static final Quaternionf IDENTITY_QUATERNION = new Quaternionf();
    public static final Vector3f IDENTITY_VECTOR = new Vector3f(1.0f, 1.0f, 1.0f);
    public static final Vector3f ZERO_VECTOR = new Vector3f(0.0f, 0.0f, 0.0f);

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

    public static FloatBuffer vertices4fToBuffer(final List<Vector4f> vertices) {
        if (vertices == null || vertices.isEmpty()) {
            throw new IllegalArgumentException();
        }

        final FloatBuffer fb = BufferUtils.createFloatBuffer(vertices.size() * 4);
        for (Vector4f vector : vertices) {
            vector.get(fb);
            fb.position(fb.position() + 4);
        }
        fb.flip();

        return fb;
    }

    public static FloatBuffer matrix4fToBuffer(final Matrix4f matrix) {
        if (matrix == null) {
            throw new IllegalArgumentException();
        }
        final TempVars tempVars = TempVars.get();

        final FloatBuffer buffer = matrix.get(tempVars.floatBuffer16);
        buffer.rewind();

        tempVars.release();
        return buffer;
    }

    public static FloatBuffer matrices4fToBuffer(final List<Matrix4f> matrices) {
        if (matrices == null) {
            throw new IllegalArgumentException();
        }

        final TempVars tempVars = TempVars.get();

        for (Matrix4f matrix4f : matrices) {
            matrix4f.get(tempVars.paletteSkinningBuffer);
            tempVars.paletteSkinningBuffer.position(tempVars.paletteSkinningBuffer.position() + 16);
        }
        tempVars.paletteSkinningBuffer.rewind();

        tempVars.release();
        return tempVars.paletteSkinningBuffer;
    }

    public static FloatBuffer floatsToBuffer(final List<Float> floats) {
        if (floats == null) {
            throw new IllegalArgumentException();
        }

        final FloatBuffer fb = BufferUtils.createFloatBuffer(floats.size());
        for (final Float aFloat : floats) {
            fb.put(aFloat);
        }
        fb.flip();

        return fb;
    }

    public static void fromRotationTranslationScale(final Matrix4f out, final Quaternionf rotation,
                                                    final Vector3f translation, final Vector3f scale) {
        final TempVars tempVars = TempVars.get();

        final Matrix4f S = tempVars.tempMat4x41.scaling(scale);
        final Matrix4f R = tempVars.tempMat4x42.rotation(rotation);
        final Matrix4f T = tempVars.tempMat4x43.translation(translation);

        out.set(T.mul(R).mul(S));
        tempVars.release();
    }

    public static void matrixToTransform(final Transform out, final Matrix4f transformMatrix) {
        final TempVars tempVars = TempVars.get();

        transformMatrix.get(tempVars.matrix4x4AsArray);
        out.setFromOpenGLMatrix(tempVars.matrix4x4AsArray);

        tempVars.release();
    }

    public static void transformToMatrix(final Matrix4f out, final Transform transform) {
        final TempVars vars = TempVars.get();

        transform.getOpenGLMatrix(vars.matrix4x4AsArray);
        out.set(vars.matrix4x4AsArray);

        vars.release();
    }

    public static List<Vector3f> arrayToVector3fList(float... components) {
        if (components == null || components.length % 3 != 0) {
            throw new IllegalArgumentException("Array of components must be divisible by 3");
        }

        int numOfVectors = components.length / 3;
        final List<Vector3f> vertexList = new ArrayList<>(numOfVectors);
        for (int i = 0; i < numOfVectors; i++) {
            Vector3f v = new Vector3f(components[i * 3], components[(i * 3) + 1], components[(i * 3) + 2]);
            vertexList.add(v);
        }

        return vertexList;
    }

    public static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    public static javax.vecmath.Vector3f convert(final javax.vecmath.Vector3f out, final Vector3f vector3f) {
        out.set(vector3f.x, vector3f.y, vector3f.z);
        return out;
    }

    public static Vector3f convert(final Vector3f out, final javax.vecmath.Vector3f vector3f) {
        out.set(vector3f.x, vector3f.y, vector3f.z);
        return out;
    }

    public static Quaternionf convert(final Quaternionf out, final Quat4f quat4f) {
        out.set(quat4f.x, quat4f.y, quat4f.z, quat4f.w);
        return out;
    }

    public static void rotationBetweenVectors(final Quaternionf out, final Vector3f a, final Vector3f b) {
        final TempVars vars = TempVars.get();

        //  Calculate the axis-angle between two vectors.
        final Vector3f v = a.cross(b, vars.vect3d1).normalize();
        final float angle = -a.angle(b);
        vars.axisAngle4f1.set(angle, v);

        //  Convert the axis-angle to quaternion.
        out.set(vars.axisAngle4f1);

        vars.release();
    }
}
