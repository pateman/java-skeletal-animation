package pl.pateman.skeletal;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

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
    public static final String MODELVIEW_UNIFORM = "modelView";
    public static final String PROJECTION_UNIFORM = "projection";
    public static final String TEXTURE_UNIFORM = "texture";
    public static final String BONES_UNIFORM = "bones";
    public static final String USESKINNING_UNIFORM = "useSkinning";
    public static final String USETEXTURING_UNIFORM = "useTexturing";

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

        final FloatBuffer buffer = matrix.get(BufferUtils.createFloatBuffer(16));
        buffer.rewind();
        return buffer;
    }

    public static FloatBuffer matrices4fToBuffer(final List<Matrix4f> matrices) {
        if (matrices == null) {
            throw new IllegalArgumentException();
        }

        final FloatBuffer buffer = BufferUtils.createFloatBuffer(16 * matrices.size());
        for (Matrix4f matrix4f : matrices) {
            matrix4f.get(buffer);
            buffer.position(buffer.position() + 16);
        }
        buffer.flip();

        return buffer;
    }

    public static Matrix4f fromRotationTranslation(final Quaternionf rotation, final Vector3f translation) {
        final Matrix4f R = new Matrix4f().rotation(rotation);
        final Matrix4f T = new Matrix4f().translation(translation);
        return T.mul(R);

//        float norm = rotation.lengthSquared();
//        // we explicitly test norm against one here, saving a division
//        // at the cost of a test and branch.  Is it worth it?
//        float s = (norm == 1f) ? 2f : (norm > 0f) ? 2f / norm : 0;
//
//        // compute xs/ys/zs first to save 6 multiplications, since xs/ys/zs
//        // will be used 2-4 times each.
//        float xs = rotation.x * s;
//        float ys = rotation.y * s;
//        float zs = rotation.z * s;
//        float xx = rotation.x * xs;
//        float xy = rotation.x * ys;
//        float xz = rotation.x * zs;
//        float xw = rotation.w * xs;
//        float yy = rotation.y * ys;
//        float yz = rotation.y * zs;
//        float yw = rotation.w * ys;
//        float zz = rotation.z * zs;
//        float zw = rotation.w * zs;
//
//        // using s=2/norm (instead of 1/norm) saves 9 multiplications by 2 here
//        result.m00 = 1 - (yy + zz);
//        result.m01 = (xy - zw);
//        result.m02 = (xz + yw);
//        result.m10 = (xy + zw);
//        result.m11 = 1 - (xx + zz);
//        result.m12 = (yz - xw);
//        result.m20 = (xz - yw);
//        result.m21 = (yz + xw);
//        result.m22 = 1 - (xx + yy);
//
//        //  Add translation.
//        result.setTranslation(translation);

    }
    
    public static Matrix4f fromRotationTranslationScale(final Quaternionf rotation, final Vector3f translation,
                                                        final Vector3f scale) {
//        final Matrix3f rotMat = rotation.get(new Matrix3f());
//        final Matrix4f result = new Matrix4f();
//
//        result.m00 = scale.x * rotMat.m00;
//        result.m01 = scale.y * rotMat.m01;
//        result.m02 = scale.z * rotMat.m02;
//        result.m03 = translation.x;
//        result.m10 = scale.x * rotMat.m10;
//        result.m11 = scale.y * rotMat.m11;
//        result.m12 = scale.z * rotMat.m12;
//        result.m13 = translation.y;
//        result.m20 = scale.x * rotMat.m20;
//        result.m21 = scale.y * rotMat.m21;
//        result.m22 = scale.z * rotMat.m22;
//        result.m23 = translation.z;
//        result.m30 = 0.0F;
//        result.m31 = 0.0F;
//        result.m32 = 0.0F;
//        result.m33 = 1.0F;
//
//        return result;

        final Matrix4f S = new Matrix4f().scaling(scale);
        final Matrix4f R = new Matrix4f().rotation(rotation);
        final Matrix4f T = new Matrix4f().translation(translation);

        return T.mul(R).mul(S);
    }

    public static List<Vector2f> arrayToVector2fList(float... components) {
        if (components == null || components.length % 2 != 0) {
            throw new IllegalArgumentException("Array of components must be divisible by 2");
        }

        int numOfVectors = components.length / 2;
        final List<Vector2f> vertexList = new ArrayList<>(numOfVectors);
        for (int i = 0; i < numOfVectors; i++) {
            Vector2f v = new Vector2f(components[i * 2], components[(i * 2) + 1]);
            vertexList.add(v);
        }

        return vertexList;
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
}
