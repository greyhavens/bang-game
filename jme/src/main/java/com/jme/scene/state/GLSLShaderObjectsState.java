/*
 * Copyright (c) 2003-2006 jMonkeyEngine
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

package com.jme.scene.state;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;

import com.jme.math.Matrix3f;
import com.jme.math.Matrix4f;
import com.jme.util.ShaderAttribute;
import com.jme.util.ShaderUniform;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.OutputCapsule;
import com.jme.util.export.Savable;

/**
 * Implementation of the GL_ARB_shader_objects extension.
 *
 * @author Thomas Hourdel
 */
public abstract class GLSLShaderObjectsState extends RenderState {

    public HashMap<String, ShaderUniform> uniforms = new HashMap<String, ShaderUniform>();
    public HashMap<String, ShaderAttribute> attribs = new HashMap<String, ShaderAttribute>();

    /** Identifier for this program. * */
    protected int programID = -1;

    /**
     * <code>isSupported</code> determines if the ARB_shader_objects extension
     * is supported by current graphics configuration.
     *
     * @return if ARB shader objects are supported
     */
    public abstract boolean isSupported();

    /**
     * <code>relinkProgram</code> instructs openGL to relink the associated
     * program and sets the attributes.  This should be used after setting
     * ShaderAttributes.
     */
    public abstract void relinkProgram();

    /**
     * <code>getProgramID</code> returns the program id of this shader.
     * @return the id of the program.
     */
    public int getProgramID() {
        return programID;
    }

    /**
     * <code>setProgramID</code> sets the program id for this
     * shader.
     * @param programID the program id of this shader.
     */
    public void setProgramID(int programID) {
        this.programID = programID;
    }

    /**
     * Set an uniform value for this shader object.
     *
     * @param var
     *            uniform variable to change
     * @param value
     *            the new value
     */

    public void setUniform(String var, int value) {
        ShaderUniform object = getShaderUniform(var, ShaderUniform.SU_INT);
        object.vint = new int[1];
        object.vint[0] = value;
        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     *
     * @param var
     *            uniform variable to change
     * @param value
     *            the new value
     */

    public void setUniform(String var, float value) {
        ShaderUniform object = getShaderUniform(var, ShaderUniform.SU_FLOAT);
        object.vfloat = new float[1];
        object.vfloat[0] = value;
        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     *
     * @param var
     *            uniform variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     */

    public void setUniform(String var, int value1, int value2) {
        ShaderUniform object = getShaderUniform(var, ShaderUniform.SU_INT2);
        object.vint = new int[2];
        object.vint[0] = value1;
        object.vint[1] = value2;
        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     *
     * @param var
     *            uniform variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     */

    public void setUniform(String var, float value1, float value2) {
        ShaderUniform object = getShaderUniform(var, ShaderUniform.SU_FLOAT2);
        object.vfloat = new float[2];
        object.vfloat[0] = value1;
        object.vfloat[1] = value2;
        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     *
     * @param var
     *            uniform variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     * @param value3
     *            the new value
     */

    public void setUniform(String var, int value1, int value2, int value3) {
        ShaderUniform object = getShaderUniform(var, ShaderUniform.SU_INT3);
        object.vint = new int[3];
        object.vint[0] = value1;
        object.vint[1] = value2;
        object.vint[2] = value3;
        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     *
     * @param var
     *            uniform variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     * @param value3
     *            the new value
     */

    public void setUniform(String var, float value1, float value2, float value3) {
        ShaderUniform object = getShaderUniform(var, ShaderUniform.SU_FLOAT3);
        object.vfloat = new float[3];
        object.vfloat[0] = value1;
        object.vfloat[1] = value2;
        object.vfloat[2] = value3;
        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     *
     * @param var
     *            uniform variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     * @param value3
     *            the new value
     * @param value4
     *            the new value
     */

    public void setUniform(String var, int value1, int value2, int value3,
            int value4) {
        ShaderUniform object = getShaderUniform(var, ShaderUniform.SU_INT4);
        object.vint = new int[4];
        object.vint[0] = value1;
        object.vint[1] = value2;
        object.vint[2] = value3;
        object.vint[3] = value4;
        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     *
     * @param var
     *            uniform variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     * @param value3
     *            the new value
     * @param value4
     *            the new value
     */

    public void setUniform(String var, float value1, float value2,
            float value3, float value4) {
        ShaderUniform object = getShaderUniform(var, ShaderUniform.SU_FLOAT4);
        object.vfloat = new float[4];
        object.vfloat[0] = value1;
        object.vfloat[1] = value2;
        object.vfloat[2] = value3;
        object.vfloat[3] = value4;
        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     *
     * @param var
     *            uniform variable to change
     * @param value
     *            the new value (a float buffer of size 4)
     * @param transpose
     *            transpose the matrix ?
     */

    public void setUniform(String var, float value[], boolean transpose) {
        if (value.length != 4) return;

        ShaderUniform object = getShaderUniform(var, ShaderUniform.SU_MATRIX2);
        object.matrix2f = new float[4];
        object.matrix2f = value;
        object.transpose = transpose;
        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     *
     * @param var
     *            uniform variable to change
     * @param value
     *            the new value
     * @param transpose
     *            transpose the matrix ?
     */

    public void setUniform(String var, Matrix3f value, boolean transpose) {
        ShaderUniform object = getShaderUniform(var, ShaderUniform.SU_MATRIX3);
        object.matrix3f = value;
        object.transpose = transpose;
        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     *
     * @param var
     *            uniform variable to change
     * @param value
     *            the new value
     * @param transpose
     *            transpose the matrix ?
     */

    public void setUniform(String var, Matrix4f value, boolean transpose) {
        ShaderUniform object = getShaderUniform(var, ShaderUniform.SU_MATRIX4);
        object.matrix4f = value;
        object.transpose = transpose;
        setNeedsRefresh(true);
    }

    /**
     * <code>clearUniforms</code> clears all uniform values from this state.
     *
     */
    public void clearUniforms() {
        uniforms.clear();
    }

    /**
     * Set an attribute value for this shader object.
     *
     * @param var
     *            attribute variable to change
     * @param value
     *            the new value
     */

    public void setAttribute(String var, short value) {
        ShaderAttribute object = getShaderAttribute(var, ShaderAttribute.SU_SHORT);
        object.s1 = value;
        setNeedsRefresh(true);
    }

    /**
     * Set an attribute value for this shader object.
     *
     * @param var
     *            attribute variable to change
     * @param value
     *            the new value
     */

    public void setAttribute(String var, float value) {
        ShaderAttribute object = getShaderAttribute(var, ShaderAttribute.SU_FLOAT);
        object.f1 = value;
        setNeedsRefresh(true);
    }

    /**
     * Set an attribute value for this shader object.
     *
     * @param var
     *            attribute variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     */

    public void setAttribute(String var, short value1, short value2) {
        ShaderAttribute object = getShaderAttribute(var, ShaderAttribute.SU_SHORT2);
        object.s1 = value1;
        object.s2 = value2;
        setNeedsRefresh(true);
    }

    /**
     * Set an attribute value for this shader object.
     *
     * @param var
     *            attribute variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     */

    public void setAttribute(String var, float value1, float value2) {
        ShaderAttribute object = getShaderAttribute(var, ShaderAttribute.SU_FLOAT2);
        object.f1 = value1;
        object.f2 = value2;
        setNeedsRefresh(true);
    }

    /**
     * Set an attribute value for this shader object.
     *
     * @param var
     *            attribute variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     * @param value3
     *            the new value
     */

    public void setAttribute(String var, short value1, short value2, short value3) {
        ShaderAttribute object = getShaderAttribute(var, ShaderAttribute.SU_SHORT3);
        object.s1 = value1;
        object.s2 = value2;
        object.s3 = value3;
        setNeedsRefresh(true);
    }

    /**
     * Set an attribute value for this shader object.
     *
     * @param var
     *            attribute variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     * @param value3
     *            the new value
     */

    public void setAttribute(String var, float value1, float value2, float value3) {
        ShaderAttribute object = getShaderAttribute(var, ShaderAttribute.SU_FLOAT3);
        object.f1 = value1;
        object.f2 = value2;
        object.f3 = value3;
        setNeedsRefresh(true);
    }

    /**
     * Set an attribute value for this shader object.
     *
     * @param var
     *            attribute variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     * @param value3
     *            the new value
     * @param value4
     *            the new value
     */

    public void setAttribute(String var, short value1, short value2, short value3,
            short value4) {
        ShaderAttribute object = getShaderAttribute(var, ShaderAttribute.SU_SHORT4);
        object.s1 = value1;
        object.s2 = value2;
        object.s3 = value3;
        object.s4 = value4;
        setNeedsRefresh(true);
    }

    /**
     * Set an attribute value for this shader object.
     *
     * @param var
     *            attribute variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     * @param value3
     *            the new value
     * @param value4
     *            the new value
     */

    public void setAttribute(String var, float value1, float value2,
            float value3, float value4) {
        ShaderAttribute object = getShaderAttribute(var, ShaderAttribute.SU_FLOAT4);
        object.f1 = value1;
        object.f2 = value2;
        object.f3 = value3;
        object.f4 = value4;
        setNeedsRefresh(true);
    }

    /**
     * Set an attribute value for this shader object.
     *
     * @param var
     *            attribute variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     * @param value3
     *            the new value
     * @param value4
     *            the new value
     */
    public void setAttribute(String var, byte value1, byte value2,
            byte value3, byte value4) {
        ShaderAttribute object = getShaderAttribute(var, ShaderAttribute.SU_NORMALIZED_UBYTE4);
        object.b1 = value1;
        object.b2 = value2;
        object.b3 = value3;
        object.b4 = value4;
        setNeedsRefresh(true);
    }

    /**
     * Set an attribute pointer value for this shader object.
     *
     * @param var
     *            attribute variable to change
     */
    public void setAttributePointer(String var, int size, boolean normalized,
            int stride, FloatBuffer data) {
        ShaderAttribute object = getShaderAttribute(var, ShaderAttribute.SU_POINTER_FLOAT);
        object.size = size;
        object.normalized = normalized;
        object.stride = stride;
        object.data = data;
        object.bufferType = ShaderAttribute.SB_FLOAT;
        setNeedsRefresh(true);
    }

    /**
     * Set an attribute pointer value for this shader object.
     *
     * @param var
     *            attribute variable to change
     */
    public void setAttributePointer(String var, int size, boolean normalized,
            boolean unsigned, int stride, ByteBuffer data) {
        ShaderAttribute object = getShaderAttribute(var, ShaderAttribute.SU_POINTER_BYTE);
        object.size = size;
        object.normalized = normalized;
        object.unsigned = unsigned;
        object.stride = stride;
        object.data = data;
        object.bufferType = ShaderAttribute.SB_BYTE;
        setNeedsRefresh(true);
    }

    /**
     * Set an attribute pointer value for this shader object.
     *
     * @param var
     *            attribute variable to change
     */
    public void setAttributePointer(String var, int size, boolean normalized,
            boolean unsigned, int stride, IntBuffer data) {
        ShaderAttribute object = getShaderAttribute(var, ShaderAttribute.SU_POINTER_INT);
        object.size = size;
        object.normalized = normalized;
        object.unsigned = unsigned;
        object.stride = stride;
        object.data = data;
        object.bufferType = ShaderAttribute.SB_INT;
        setNeedsRefresh(true);
    }

    /**
     * Set an attribute pointer value for this shader object.
     *
     * @param var
     *            attribute variable to change
     */
    public void setAttributePointer(String var, int size, boolean normalized,
            boolean unsigned, int stride, ShortBuffer data) {
        ShaderAttribute object = getShaderAttribute(var, ShaderAttribute.SU_POINTER_SHORT);
        object.size = size;
        object.normalized = normalized;
        object.unsigned = unsigned;
        object.stride = stride;
        object.data = data;
        object.bufferType = ShaderAttribute.SB_SHORT;
        setNeedsRefresh(true);
    }

    /**
     * <code>clearAttributes</code> clears all attribute values from this state.
     *
     */
    public void clearAttributes() {
        attribs.clear();
    }


    /**
     * @return RS_SHADER_OBJECTS
     * @see com.jme.scene.state.RenderState#getType()
     */
    @Override
	public int getType() {
        return RS_GLSL_SHADER_OBJECTS;
    }

    private ShaderUniform getShaderUniform(String name, int type) {
        ShaderUniform uniform = uniforms.get(name);
        if (uniform == null) {
            uniforms.put(name, uniform = new ShaderUniform(name, type));
        } else {
            uniform.type = type;
        }
        return uniform;
    }

    private ShaderAttribute getShaderAttribute(String name, int type) {
        ShaderAttribute attrib = attribs.get(name);
        if (attrib == null) {
            attribs.put(name, attrib = new ShaderAttribute(name, type));
        } else {
            attrib.type = type;
        }
        return attrib;
    }

    /**
     * <code>load</code> loads the shader object from the specified file. The
     * program must be in ASCII format. We delegate the loading to each
     * implementation because we do not know in what format the underlying API
     * wants the data.
     *
     * @param vert
     *            text file containing the vertex shader object
     * @param frag
     *            text file containing the fragment shader object
     */
    public abstract void load(URL vert, URL frag);

    public abstract void load(String vert, String frag);

    @Override
	public void write(JMEExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);
        Savable[] uarray = uniforms.values().toArray(new Savable[0]);
        capsule.write(uarray,"uniforms", new Savable[0]);
        Savable[] aarray = attribs.values().toArray(new Savable[0]);
        capsule.write(aarray,"attribs", new Savable[0]);
    }

	@Override
	public void read(JMEImporter e) throws IOException {
        super.read(e);
        InputCapsule capsule = e.getCapsule(this);

        uniforms.clear();
        Savable[] uarray = capsule.readSavableArray("uniforms", new Savable[0]);
        for (Savable savable : uarray) {
            ShaderUniform uniform = (ShaderUniform)savable;
            uniforms.put(uniform.name, uniform);
        }

        attribs.clear();
        Savable[] aarray = capsule.readSavableArray("attribs", new Savable[0]);
        for (Savable savable : aarray) {
            ShaderAttribute attrib = (ShaderAttribute)savable;
            attribs.put(attrib.name, attrib);
        }
    }

    @Override
	public Class<GLSLShaderObjectsState> getClassTag() {
        return GLSLShaderObjectsState.class;
    }
}
