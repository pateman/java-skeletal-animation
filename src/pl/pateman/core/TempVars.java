/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package pl.pateman.core;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Temporary variables assigned to each thread. Engine classes may access
 * these temp variables with TempVars.get(), all retrieved TempVars
 * instances must be returned via TempVars.release().
 * This returns an available instance of the TempVar class ensuring this
 * particular instance is never used elsewhere in the mean time.
 *
 * Adapted to the project's needs.
 *
 * @author jMonkeyEngine
 * @author pateman
 */
public final class TempVars {

    /**
     * Allow X instances of TempVars in a single thread.
     */
    private static final int STACK_SIZE = 5;

    /**
     * <code>TempVarsStack</code> contains a stack of TempVars.
     * Every time TempVars.get() is called, a new entry is added to the stack,
     * and the index incremented.
     * When TempVars.release() is called, the entry is checked against
     * the current instance and  then the index is decremented.
     */
    private static class TempVarsStack {

        int index = 0;
        TempVars[] tempVars = new TempVars[STACK_SIZE];
    }

    /**
     * ThreadLocal to store a TempVarsStack for each thread.
     * This ensures each thread has a single TempVarsStack that is
     * used only in method calls in that thread.
     */
    private static final ThreadLocal<TempVarsStack> varsLocal = new ThreadLocal<TempVarsStack>() {

        @Override
        public TempVarsStack initialValue() {
            return new TempVarsStack();
        }
    };
    /**
     * This instance of TempVars has been retrieved but not released yet.
     */
    private boolean isUsed = false;

    private TempVars() {
    }

    /**
     * Acquire an instance of the TempVar class.
     * You have to release the instance after use by calling the
     * release() method.
     * If more than STACK_SIZE (currently 5) instances are requested
     * in a single thread then an ArrayIndexOutOfBoundsException will be thrown.
     *
     * @return A TempVar instance
     */
    public static TempVars get() {
        TempVarsStack stack = varsLocal.get();

        TempVars instance = stack.tempVars[stack.index];

        if (instance == null) {
            // Create new
            instance = new TempVars();

            // Put it in there
            stack.tempVars[stack.index] = instance;
        }

        stack.index++;

        instance.isUsed = true;

        return instance;
    }

    /**
     * Releases this instance of TempVars.
     * Once released, the contents of the TempVars are undefined.
     * The TempVars must be released in the opposite order that they are retrieved,
     * e.g. Acquiring vars1, then acquiring vars2, vars2 MUST be released
     * first otherwise an exception will be thrown.
     */
    public void release() {
        if (!isUsed) {
            throw new IllegalStateException("This instance of TempVars was already released!");
        }

        isUsed = false;

        TempVarsStack stack = varsLocal.get();

        // Return it to the stack
        stack.index--;

        // Check if it is actually there
        if (stack.tempVars[stack.index] != this) {
            throw new IllegalStateException("An instance of TempVars has not been released in a called method!");
        }
    }

    /**
     * Initializes storage for skinning. Note that this method must be called by each thread separately if it's planned
     * to use the skinning storage, otherwise {@code paletteSkinningBuffer} and {@code boneMatricesList} will be
     * {@code NULL}.
     *
     * @param numberOfBones Number of bones.
     */
    public static void initializeStorageForSkinning(int numberOfBones) {
        final List<TempVars> tempVarsList = new ArrayList<>(STACK_SIZE);
        for (int i = 0; i < STACK_SIZE; i++) {
            final TempVars tempVars = TempVars.get();
            tempVarsList.add(tempVars);

            tempVars.paletteSkinningBuffer = BufferUtils.createFloatBuffer(16 * numberOfBones);

            tempVars.boneMatricesList = new ArrayList<>(numberOfBones);
            for (int j = 0; j < numberOfBones; j++) {
                tempVars.boneMatricesList.add(new Matrix4f());
            }
        }

        for (int i = tempVarsList.size() -1; i >= 0; i--) {
            tempVarsList.get(i).release();
        }
    }

    /**
     * Buffers.
     */
    public final FloatBuffer floatBuffer16 = BufferUtils.createFloatBuffer(16);
    public FloatBuffer paletteSkinningBuffer;

    /**
     * Lists.
     */
    public List<Matrix4f> boneMatricesList;

    /**
     * General vectors.
     */
    public final Vector3f vect3d1 = new Vector3f();
    public final Vector3f vect3d2 = new Vector3f();
    public final Vector3f vect3d3 = new Vector3f();
    public final Vector3f vect3d4 = new Vector3f();
    public final Vector3f vect3d5 = new Vector3f();

    /**
     * General matrices.
     */
    public final Matrix4f tempMat4x41 = new Matrix4f();
    public final Matrix4f tempMat4x42 = new Matrix4f();
    public final Matrix4f tempMat4x43 = new Matrix4f();
    public final Matrix3f tempMat3x3 = new Matrix3f();

    /**
     * General quaternions.
     */
    public final Quaternionf quat1 = new Quaternionf();
    public final Quaternionf quat2 = new Quaternionf();
    public final Quaternionf quat3 = new Quaternionf();
}
