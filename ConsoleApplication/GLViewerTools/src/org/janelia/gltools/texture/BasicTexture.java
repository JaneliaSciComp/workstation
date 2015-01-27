/* 
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used. to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.gltools.texture;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;
import org.apache.commons.lang3.ArrayUtils;
import org.janelia.gltools.GL3Resource;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public abstract class BasicTexture implements GL3Resource 
{
    protected int handle;
    
    protected final int border = 0;
    
    protected int mipMapLevel = 0;
    protected int numberOfComponents = 3;
    protected int internalFormat = GL.GL_RGB8; // "internal format"
    protected int width = 0;
    protected int format = GL.GL_RGB;
    protected int type = GL.GL_UNSIGNED_BYTE;
    protected int bytesPerIntensity = 1;
    
    protected ByteBuffer pixels;
    protected ShortBuffer shortPixels; // used only in case of 16-bit data
    protected IntBuffer intPixels; // used only in the case of 32-bit int data
    
    protected int textureTarget = GL.GL_TEXTURE_2D;
    protected boolean doSwapBytes = false;
    protected int magFilter = GL3.GL_LINEAR;
    protected int minFilter = GL3.GL_LINEAR;
    protected int textureWrap = GL3.GL_CLAMP_TO_EDGE;
    protected boolean generateMipmaps = false;
    protected List<BasicTexture> mipmaps = new ArrayList<BasicTexture>();
    protected int unpackAlignment = 1;
    protected boolean useImmutableTexture = false;
    
    protected boolean needsUpload = false;
    
    protected void copyParameters(BasicTexture rhs) {
        mipMapLevel = rhs.mipMapLevel;
        numberOfComponents = rhs.numberOfComponents;
        internalFormat = rhs.internalFormat;
        width = rhs.width;
        format = rhs.format;
        type = rhs.type;
        bytesPerIntensity = rhs.bytesPerIntensity;
        textureTarget = rhs.textureTarget;
        doSwapBytes = rhs.doSwapBytes;
        magFilter = rhs.magFilter;
        minFilter = rhs.minFilter;
        textureWrap = rhs.textureWrap;
        generateMipmaps = rhs.generateMipmaps;
    }

    protected void updateParameters(int internalFormat) {
        this.internalFormat = internalFormat;
        
        // TODO move this logic to something static
        Set<Integer> sixteenBits = new HashSet<Integer>(
                Arrays.asList(ArrayUtils.toObject(new int[] 
        {
            GL3.GL_R16,
            GL3.GL_R16UI,
            GL3.GL_RG16,
            GL3.GL_RG16UI,
            GL3.GL_RGB16,
            GL3.GL_RGB16UI,
            GL3.GL_RGBA16,
            GL3.GL_RGBA16UI,
        })));
        Set<Integer> thirtyTwoBits = new HashSet<Integer>(
                Arrays.asList(ArrayUtils.toObject(new int[]
        {
            GL3.GL_R32I,
            GL3.GL_R32UI,
            GL3.GL_RG32I,
            GL3.GL_RG32UI,
            GL3.GL_RGB32I,
            GL3.GL_RGB32UI,
            GL3.GL_RGBA32I,
            GL3.GL_RGBA32UI,
        })));
        
        if (thirtyTwoBits.contains(internalFormat)) {
            bytesPerIntensity = 4;
            type = GL3.GL_UNSIGNED_INT;
        }
        else if (sixteenBits.contains(internalFormat)) {
            bytesPerIntensity = 2;
            type = GL3.GL_UNSIGNED_SHORT;
        }
        else {
            bytesPerIntensity = 1; // other sizes not supported at the moment...
            type = GL3.GL_UNSIGNED_BYTE;
        }

        Set<Integer> oneComponent = new HashSet<Integer>(
                Arrays.asList(ArrayUtils.toObject(new int[] 
        {
            GL3.GL_R8,
            GL3.GL_R8UI,
            GL3.GL_R16,
            GL3.GL_R16I,
            GL3.GL_R16UI,
            GL3.GL_R32I,
            GL3.GL_R32UI,
            GL3.GL_RED
        })));
        Set<Integer> twoComponents = new HashSet<Integer>(
                Arrays.asList(ArrayUtils.toObject(new int[] 
        {
            GL3.GL_RG8,
            GL3.GL_RG8UI,
            GL3.GL_RG16,
            GL3.GL_RG16UI,
            GL3.GL_RG32UI,
            GL3.GL_RG32I,
            GL3.GL_RG,
        })));
        Set<Integer> threeComponents = new HashSet<Integer>(
                Arrays.asList(ArrayUtils.toObject(new int[] 
        {
            GL3.GL_RGB8,
            GL3.GL_RGB8UI,
            GL3.GL_RGB16,
            GL3.GL_RGB16UI,
            GL3.GL_RGB32UI,
            GL3.GL_RGB32I,
            GL3.GL_RGB,
        })));
        Set<Integer> integerFormat = new HashSet<Integer>(
                Arrays.asList(ArrayUtils.toObject(new int[] 
        {
            GL3.GL_R8UI,
            GL3.GL_R8I,
            GL3.GL_R16UI,
            GL3.GL_R16I,
            GL3.GL_R32UI,
            GL3.GL_R32I,
            GL3.GL_RG8UI,
            GL3.GL_RG8I,
            GL3.GL_RG16UI,
            GL3.GL_RG16I,
            GL3.GL_RG32UI,
            GL3.GL_RG32I,
            GL3.GL_RGB8UI,
            GL3.GL_RGB8I,
            GL3.GL_RGB16UI,
            GL3.GL_RGB16I,
            GL3.GL_RGB32UI,
            GL3.GL_RGB32I,
        })));        
        
        
        if (oneComponent.contains(internalFormat)) {
            numberOfComponents = 1;
            if (integerFormat.contains(internalFormat))
                format = GL3.GL_RED_INTEGER;
            else
                format = GL3.GL_RED;
        }
        else if (twoComponents.contains(internalFormat)) {
            numberOfComponents = 2;
            if (integerFormat.contains(internalFormat))
                format = GL3.GL_RG_INTEGER;
            else
                format = GL3.GL_RG;
        }
        else if (threeComponents.contains(internalFormat)) {
            numberOfComponents = 3;
            if (integerFormat.contains(internalFormat))
                format = GL3.GL_RGB_INTEGER;
            else
                format = GL3.GL_RGB;
        }
        else {
            numberOfComponents = 4;
            if (integerFormat.contains(internalFormat))
                format = GL3.GL_RGBA_INTEGER;
            else
                format = GL3.GL_RGBA;
        }
    }
    
    public void bind(GL3 gl) {
        bind(gl, 0);
    }
    
    public void bind(GL3 gl, int textureUnit) {
        if (needsUpload)
            initAndLeaveBound(gl);
        gl.glActiveTexture(GL.GL_TEXTURE0 + textureUnit);
        gl.glBindTexture(textureTarget, handle);
        // Texture filtering might vary dynamically, so set it every time...
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MAG_FILTER, magFilter);
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MIN_FILTER, minFilter);
    }
    
    @Override
    public void dispose(GL3 gl) {
        if (handle != 0) {
            int[] h = {handle};
            gl.glDeleteTextures(1, h, 0);
            handle = 0;
        }
    }

    public int getHandle() {
        return handle;
    }

    public int getNumberOfComponents() {
        return numberOfComponents;
    }

    public int getInternalFormat() {
        return internalFormat;
    }

    public int getWidth() {
        return width;
    }

    public int getFormat() {
        return format;
    }

    public int getType() {
        return type;
    }

    public int getBytesPerIntensity() {
        return bytesPerIntensity;
    }

    public int getTextureTarget() {
        return textureTarget;
    }

    public int getMagFilter() {
        return magFilter;
    }

    public int getMinFilter() {
        return minFilter;
    }

    public int getTextureWrap() {
        return textureWrap;
    }

    @Override
    public void init(GL3 gl) {
        initAndLeaveBound(gl);
        unbind(gl);
    }
    
    protected void initAndLeaveBound(GL3 gl) {
        if (width == 0)
            return;

        // Compute texture storage requirements
        int numDimensions = 1;
        if (textureTarget == GL3.GL_TEXTURE_2D) numDimensions = 2;
        if (textureTarget == GL3.GL_TEXTURE_3D) numDimensions = 3;
        int mipmapCount = 1;
        if (generateMipmaps) {
            // Compute number of mipmaps needed
            mipmapCount = (int)Math.floor(
                    Math.log(maxDimension())/Math.log(2.0))
                    + 1;
            // Unless someone has already precomputed the mipmaps
            if (mipmaps.size() > 0)
                mipmapCount = mipmaps.size() + 1;
        }
        
        // allocate texture handle
        boolean isFreshTexture = false;
        if (handle == 0) {
            int[] h = {0};
            gl.glGenTextures(1, h, 0);
            handle = h[0];
            isFreshTexture = true;
        }
        gl.glBindTexture(textureTarget, handle);
        
        gl.glTexParameteri(textureTarget, GL3.GL_TEXTURE_WRAP_S, textureWrap);
        if (numDimensions > 1)
            gl.glTexParameteri(textureTarget, GL3.GL_TEXTURE_WRAP_T, textureWrap);
        if (numDimensions > 2)
            gl.glTexParameteri(textureTarget, GL3.GL_TEXTURE_WRAP_R, textureWrap);
        
        gl.glPixelStorei(GL3.GL_UNPACK_ALIGNMENT, unpackAlignment);
        
        // PPM is different endian than OpenGL texture
        if (doSwapBytes)
            gl.glPixelStorei(GL2GL3.GL_UNPACK_SWAP_BYTES, GL.GL_TRUE);
        else
            gl.glPixelStorei(GL2GL3.GL_UNPACK_SWAP_BYTES, GL.GL_FALSE);

        // only allocate storage when creating a fresh texture
        if (isFreshTexture)
            allocateTextureStorage(gl, mipmapCount);
        uploadTexture(gl);
        
        if (generateMipmaps) {
            if (mipmaps.size() > 0) { // if have precomputed mipmaps
                for (BasicTexture mipmap : mipmaps) {
                    mipmap.uploadTexture(gl);
                }
            }
            else { // autogenerate mipmaps
                gl.glGenerateMipmap(textureTarget);
            }
        }
        
        else {
            
        }
        
    }

    protected void uploadTexture(GL3 gl) 
    {
        gl.glTexSubImage1D(
                textureTarget,
                mipMapLevel,
                0, // offsets
                width,
                format,
                type,
                pixels);
        needsUpload = false;
    }
    
    protected void allocateTextureStorage(GL3 gl, int mipmapCount) {
        gl.glTexStorage1D(textureTarget, 
                mipmapCount, 
                internalFormat, 
                width);        
    }
    
    // Maximum of all texture dimensions
    protected int maxDimension() {
        return width;
    }
    
    public void setGenerateMipmaps(boolean generateMipmaps) {
        this.generateMipmaps = generateMipmaps;
    }

    public void setMagFilter(int magFilter) {
        this.magFilter = magFilter;
    }

    public void setMinFilter(int minFilter) {
        this.minFilter = minFilter;
    }

    public void unbind(GL3 gl) {
        gl.glBindTexture(textureTarget, 0);
        gl.glActiveTexture(GL.GL_TEXTURE0);
    }

}
