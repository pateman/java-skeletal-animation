package pl.pateman.core;

import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
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
    public static final String USELIGHTING_UNIFORM = "useLighting";
    public static final String CAMERADIRECTION_UNIFORM = "cameraDirection";
    public static final String DIFFUSECOLOR_UNIFORM = "diffuseColor";

    public static final float HALF_PI = 0.5f * (float) Math.PI;
    public static final float TWO_PI = 2.0f * (float) Math.PI;
    public static final Vector3f AXIS_Y = new Vector3f(0.0f, 1.0f, 0.0f);
    public static final Vector3f NEG_AXIS_Z = new Vector3f(0.0f, 0.0f, -1.0f);

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
        tempVars.vecmathMat4x4.set(tempVars.matrix4x4AsArray);
        out.set(tempVars.vecmathMat4x4);

        //  The original set(Matrix4f mat) method is broken, so we need to set the translation separately.
        out.origin.set(tempVars.matrix4x4AsArray[12], tempVars.matrix4x4AsArray[13], tempVars.matrix4x4AsArray[14]);

        tempVars.release();
    }

    public static void setRigidBodyMass(final RigidBody out, float mass) {
        if (out.getCollisionShape() == null) {
            throw new IllegalArgumentException("Rigid body needs a collider");
        }

        final TempVars vars = TempVars.get();
        out.getCollisionShape().calculateLocalInertia(mass, vars.vecmathVect3d);
        out.setMassProps(mass, vars.vecmathVect3d);
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
}
