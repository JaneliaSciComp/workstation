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
 *        of its contributors may be used to endorse or promote products derived 
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

import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.FileCacheSeekableStream;
import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.MemoryCacheSeekableStream;
import com.sun.media.jai.codec.SeekableStream;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import javax.media.opengl.GL3;
import org.apache.commons.io.IOUtils;
import org.janelia.geometry.util.PerformanceTimer;
import org.janelia.gltools.GL3Resource;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class Texture3d extends BasicTexture implements GL3Resource 
{
    protected int height = 0;
    protected int depth = 0;
    protected int pixelBufferObject = 0;
    
    public Texture3d() {
        textureTarget = GL3.GL_TEXTURE_3D;
        magFilter = GL3.GL_LINEAR;
        minFilter = GL3.GL_LINEAR;
        useImmutableTexture = true;
    }

    @Override
    public void dispose(GL3 gl) {
        super.dispose(gl);
        int[] pbos = {pixelBufferObject};
        gl.glDeleteBuffers(1, pbos, 0);
        pixelBufferObject = 0;
    }

    @Override
    protected void uploadTexture(GL3 gl) 
    {
        gl.glTexSubImage3D(
                textureTarget,
                mipMapLevel,
                0, 0, 0,// offsets
                width, height, depth,
                format,
                type,
                pixels);
        needsUpload = false;
    }
    
    @Override
    public void init(GL3 gl) {
        if (width*height*depth == 0)
            return;        
        PerformanceTimer timer = new PerformanceTimer();
        super.init(gl);
        System.out.println("Uploading texture and mipmaps took "+timer.reportMsAndRestart()+" ms");
        unbind(gl);
    }
    
    @Override
    protected void allocateTextureStorage(GL3 gl, int mipmapCount) {
        gl.glTexStorage3D(textureTarget, 
                mipmapCount, 
                internalFormat, 
                width, height, depth);        
    }    
    
    @Override
    protected int maxDimension() {
        return Math.max(width, Math.max(height, depth));
    }
    
    // Simple 3D texture for testing
    public Texture3d loadTestCheckerPattern() {
        width = 4;
        height = 4;
        depth = 4;
        bytesPerIntensity = 1;
        type = GL3.GL_UNSIGNED_BYTE;
        numberOfComponents = 1;
        int byteCount = numberOfComponents * bytesPerIntensity * width * height * depth;
        byte[] pixelBytes = new byte[byteCount];
        pixels = ByteBuffer.wrap(pixelBytes);
        pixels.rewind();
        format = internalFormat = GL3.GL_RED;
        for (int p = 0; p < width*height*depth; ++p) {
            int z = p / (width*height);
            int y = (p - z*width*height) / width;
            int x = p - z*width*height - y*width;
            if ((x+y+z)%2 == 0) pixels.put((byte)0);
            else pixels.put((byte)(255 & 0xFF));
        }
        pixels.flip();
        return this;
    }
    
    public Texture3d loadTiffStack(File tiffFile) throws IOException {
        PerformanceTimer timer = new PerformanceTimer();
        RenderedImage[] slices = renderedImagesFromTiffStack(tiffFile);
        // System.out.println("Tiff load to RenderedImages took "+timer.reportMsAndRestart()+" ms");
        Texture3d result = loadStack(slices);
        // System.out.println("Tiff RenderedImages to Buffer took "+timer.reportMsAndRestart()+" ms");
        return result;
    }
    
    protected void allocatePixels() {
        int byteCount = numberOfComponents * bytesPerIntensity * width * height * depth;
        byte[] pixelBytes = new byte[byteCount];
        pixels = ByteBuffer.wrap(pixelBytes);
        pixels.order(ByteOrder.nativeOrder());
        pixels.rewind();
    }
    
    /**
     * 
     * @param x
     * @param y
     * @param z
     * @param channel
     * @return null if raster data are absent
     */
    public Integer getIntensity(int x, int y, int z, int channel) {
        if (pixels == null) return null;
        int offset = channel 
                + numberOfComponents*x 
                + numberOfComponents*width*y 
                + numberOfComponents*width*height*z;
        if (bytesPerIntensity == 1)
            return new Integer(pixels.get(offset));
        else // if (bytesPerIntensity == 2)
            return new Integer(shortPixels.get(offset));
        // TODO - 32 bit integers...
    }
    
    public Texture3d loadStack(RenderedImage[] stack) {
        PerformanceTimer timer = new PerformanceTimer();
        if (stack.length < 1)
            return this;
        depth = stack.length;
        RenderedImage slice = stack[0];
        width = slice.getWidth();
        height = slice.getHeight();
        ColorModel colorModel = slice.getColorModel();
        bytesPerIntensity = colorModel.getComponentSize(0)/8;
        bytesPerIntensity = Math.max(1, bytesPerIntensity);
        // NOTE - we might want to support more data types than byte and short eventually.
        if (bytesPerIntensity < 2) type = GL3.GL_UNSIGNED_BYTE;
        else type = GL3.GL_UNSIGNED_SHORT;
        numberOfComponents = colorModel.getNumComponents();
        switch (numberOfComponents) {
            case 1:
                format = internalFormat = GL3.GL_RED;
                if (bytesPerIntensity > 1)  internalFormat = GL3.GL_R16;
                break;
            case 2:
                format = internalFormat = GL3.GL_RG;
                if (bytesPerIntensity > 1)  internalFormat = GL3.GL_RG16;
                break;
            case 3:
                format = internalFormat = GL3.GL_RGB;
                if (bytesPerIntensity > 1)  internalFormat = GL3.GL_RGB16;
                break;
            case 4:
                format = internalFormat = GL3.GL_RGBA;
                if (bytesPerIntensity > 1)  internalFormat = GL3.GL_RGBA16;
                break;
        }

        allocatePixels();
        
        System.out.println("Initializing texture buffer took "+timer.reportMsAndRestart()+" ms");
        
        int[] pxl = new int[numberOfComponents];
        if (bytesPerIntensity < 2) {
            pixels.rewind();
            for (int z = 0; z < depth; ++z) {
                Raster raster = stack[z].getData();
                for (int y = 0; y < height; ++y) {
                    for (int x = 0; x < width; ++x) {
                        raster.getPixel(x, y, pxl);
                        for (int c = 0; c < numberOfComponents; ++c) {
                            pixels.put((byte)(pxl[c] & 0xFF));
                        }
                    }
                }
            }
            pixels.flip();
        }
        else { // 16 bit
            shortPixels = pixels.asShortBuffer();
            shortPixels.rewind();
            // System.out.println("Casting short buffer took "+timer.reportMsAndRestart()+" ms");
            Raster[] sliceRasters = new Raster[depth];
            
            // TODO Run this in parallel
            for (int z = 0; z < depth; ++z) {
                sliceRasters[z] = stack[z].getData(); // slow 35-40 ms per slice                
            }
            System.out.println("Getting Raster data took "+timer.reportMsAndRestart()+" ms");
            
            for (int z = 0; z < depth; ++z) {
                final boolean useRawBytes = true;
                if (useRawBytes) { // not faster! 12 seconds
                    DataBufferUShort dbu = (DataBufferUShort)sliceRasters[z].getDataBuffer();
                    // System.out.println("Slice "+z+" getDataBuffer() took "+timer.reportMsAndRestart()+" ms");
                    short[] sliceData = dbu.getData();
                    // System.out.println("Slice "+z+" getData() [2] took "+timer.reportMsAndRestart()+" ms");
                    shortPixels.put(sliceData);
                    /// System.out.println("Slice "+z+" put() took "+timer.reportMsAndRestart()+" ms");
                }
                else { // 12 seconds
                    for (int y = 0; y < height; ++y) {
                        for (int x = 0; x < width; ++x) {
                            sliceRasters[z].getPixel(x, y, pxl);
                            for (int c = 0; c < numberOfComponents; ++c) {
                                shortPixels.put((short)(pxl[c] & 0xFFFF));
                            }
                        }
                    }
                }
            }
            shortPixels.flip();
        }
        pixels.rewind();   

        System.out.println("Populating texture buffer took "+timer.reportMsAndRestart()+" ms");

        computeMipmaps();

        System.out.println("Computing mipmaps took "+timer.reportMsAndRestart()+" ms");
        
        return this;
    }

    protected void computeMipmaps() {
        mipmaps.clear();
        PerformanceTimer timer = new PerformanceTimer();
        Texture3d mipmap = createMipmapUsingMaxFilter();
        while (mipmap != null) {
            // System.out.println("Creating mipmap took "+timer.reportMsAndRestart()+" ms");
            mipmaps.add(mipmap);
            mipmap = mipmap.createMipmapUsingMaxFilter();
        }        
    }
    
    protected void copyParameters(Texture3d rhs) {
        super.copyParameters(rhs);
        height = rhs.height;
        depth = rhs.depth;
    }
    
    private int largestIntensity(int[] samples, int sampleCount) {
        int result = samples[0];
        for (int i = 1; i < sampleCount; ++i)
            result = Math.max(result, samples[i]);
        return result;
    }
    
    
    private int secondLargestIntensity(int[] samples, int sampleCount) {
        if (sampleCount == 1)
            return samples[0];
        
        int best, second;
        if (samples[0] > samples[1]) {
            best = samples[0];
            second = samples[1];
        }
        else {
            best = samples[1];
            second = samples[0];
        }

        for (int i = 2; i < sampleCount; ++i) {
            if (samples[i] <= second) continue;
            if (samples[i] > best) {
                second = best;
                best = samples[i];
            }
            else {
                second = samples[i];
            }
        }
        
        return second;
    }
    
    public Texture3d createMipmapUsingMaxFilter() {
        // Check whether smaller mipmap is possible
        if ( (width <= 1) && (height <= 1) && (depth <= 1) )
            return null; // already smallest possible texture

        // Create a new texture at half the original size
        Texture3d result = new Texture3d();
        result.copyParameters(this);
        result.width = Math.max(width/2, 1);
        result.height = Math.max(height/2, 1);
        result.depth = Math.max(depth/2, 1);
        result.mipMapLevel = mipMapLevel + 1;
        result.allocatePixels();
        
        ByteBuffer bytesIn = pixels;
        ByteBuffer bytesOut = result.pixels;
        ShortBuffer shortsIn = pixels.asShortBuffer();
        ShortBuffer shortsOut = result.pixels.asShortBuffer();
        
        // New way - TODO - output oriented, with kernel
        float [] halfInputDeltaUvw = new float[] { // normalized inter-pixel distance of input texture
            0.5f/width, 0.5f/height, 0.5f/depth // Not useful for s
        };
        shortsOut.rewind();
        bytesOut.rewind();
        int [] zeroOnly = new int [] {0};
        int [] samples = new int [8];
        // Outer loops over output texture voxels
        for (int z = 0; z < result.depth; ++z) {
            float fractionalZOut = (z + 0.5f) / result.depth;
            int [] zIn = new int [] {
                (int)( (fractionalZOut - halfInputDeltaUvw[2]) * depth),
                (int)( (fractionalZOut + halfInputDeltaUvw[2]) * depth),
            };
            if (depth == 1) zIn = zeroOnly;
            else if (zIn[0] == zIn[1]) zIn = new int[] {zIn[0]};
            for (int y = 0; y < result.height; ++y) {
                float fractionalYOut = (y + 0.5f) / result.height;
                int [] yIn = new int [] {
                    (int)( (fractionalYOut - halfInputDeltaUvw[1]) * height),
                    (int)( (fractionalYOut + halfInputDeltaUvw[1]) * height),
                };
                if (height == 1) yIn = zeroOnly;
                else if (yIn[0] == yIn[1]) yIn = new int[] {yIn[0]};
                for (int x = 0; x < result.width; ++x) {
                    float fractionalXOut = (x + 0.5f) / result.width;
                    int [] xIn = new int [] {
                        (int)( (fractionalXOut - halfInputDeltaUvw[0]) * width),
                        (int)( (fractionalXOut + halfInputDeltaUvw[0]) * width),
                    };
                    if (width == 1) 
                        xIn = zeroOnly;
                    else if (xIn[0] == xIn[1]) 
                        xIn = new int[] {xIn[0]};
                    int sampleCount = 0;
                    for (int c = 0; c < numberOfComponents; ++c) {
                        // Inner loops over input texture voxels
                        for (int iz : zIn) {
                            for (int iy : yIn)
                                for (int ix : xIn) {
                                    int offset = iz * height * width * numberOfComponents
                                            + iy * width * numberOfComponents
                                            + ix * numberOfComponents
                                            + c;
                                    if (bytesPerIntensity > 1)
                                        samples[sampleCount] = shortsIn.get(offset) & 0xffff;
                                    else
                                        samples[sampleCount] = bytesIn.get(offset) & 0xff;
                                    sampleCount += 1;
                                }                    
                        }
                        int maxIntensity = secondLargestIntensity(samples, sampleCount);
                        if (bytesPerIntensity > 1)
                            shortsOut.put((short)(maxIntensity & 0xffff));
                        else
                            bytesOut.put((byte)(maxIntensity & 0xff));
                    }
                }
            }
        }

        /*
        // Discard the final value of odd sized dimensions, to avoid index overflow
        int sx = Math.max(1, width - width%2);
        int sy = Math.max(1, height - height%2);
        int sz = Math.max(1, depth - depth%2);
        // Populate mipmap using MAX filter
        shortsOut.rewind();
        bytesOut.rewind();
        for (int z = 0; z < sz; ++z) {
            int zOff0 = z * height * width * numberOfComponents;
            int zOff1 = (z/2) * result.height * result.width * numberOfComponents;
            for (int y = 0; y < sy; ++y) {
                int yOff0 = zOff0 + y * width * numberOfComponents;
                int yOff1 = zOff1 + (y/2) * result.width * numberOfComponents;
                for (int x = 0; x < sx; ++x) {
                    int xOff0 = yOff0 + x * numberOfComponents;
                    int xOff1 = yOff1 + (x/2) * numberOfComponents;
                    for (int c = 0; c < numberOfComponents; ++c) {
                        int cOff0 = xOff0 + c;
                        int cOff1 = xOff1 + c;
                        if (bytesPerIntensity > 1) {
                            int i0 = shortsIn.get(cOff0) & 0xffff;
                            int i1 = shortsOut.get(cOff1) & 0xffff;
                            int intensity = Math.max(i0, i1);
                            shortsOut.put(cOff1, (short)(intensity & 0xffff)); 
                        } else {
                            byte intensity = (byte) Math.max(
                                    bytesIn.get(cOff0) & 0xff,
                                    bytesOut.get(cOff1) & 0xff);
                            shortsOut.put(cOff1, intensity);
                        }
                    }
                }
            }
        }
         */
        return result;
    }
    
    private RenderedImage[] renderedImagesFromTiffStack(File tiffFile) throws IOException {
        PerformanceTimer timer = new PerformanceTimer();
        // FileSeekableStream is the fastest load method I tested, by far
        
        ImageDecoder decoder;
        
        // Performance results for various load strategies below:
        final boolean useMemoryCache = false;
        final boolean useFileCache = false;
        final boolean useFileSS = true; // FASTEST
        final boolean useFileStream = false;
        if (useMemoryCache) { // [20, 18] seconds buffered;[30,22,41] seconds unbuffered
            InputStream tiffStream = new BufferedInputStream( new FileInputStream(tiffFile) );
            SeekableStream s = new MemoryCacheSeekableStream(tiffStream);
            decoder = ImageCodec.createImageDecoder("tiff", s, null);
        }
        else if (useFileCache) { // [59] seconds ;[100] seconds unbuffered
            InputStream tiffStream = new BufferedInputStream( new FileInputStream(tiffFile) );
            SeekableStream s = new FileCacheSeekableStream(tiffStream); 
            decoder = ImageCodec.createImageDecoder("tiff", s, null);
        }
        else if (useFileSS) { // [4,0.6,3.2,1.2] seconds BEST
            SeekableStream s = new FileSeekableStream(tiffFile);
            decoder = ImageCodec.createImageDecoder("tiff", s, null);
        }
        else if (useFileStream) { // [55] seconds
            InputStream tiffStream = new BufferedInputStream( new FileInputStream(tiffFile) );
            decoder = ImageCodec.createImageDecoder("tiff", tiffStream, null); // 55 seconds
        }
        else { // [33] seconds
            InputStream tiffStream = new BufferedInputStream( new FileInputStream(tiffFile) );
            byte[] bytes = IOUtils.toByteArray(tiffStream);
            SeekableStream s = new ByteArraySeekableStream(bytes);
            decoder = ImageCodec.createImageDecoder("tiff", s, null);
        }
        
        System.out.println("Creating image decoder from tiff file took "+timer.reportMsAndRestart()+" ms");

        int sz = decoder.getNumPages();
        RenderedImage slices[] = new RenderedImage[sz];
        for (int z = 0; z < sz; ++z)
            slices[z] = decoder.decodeAsRenderedImage(z);
        System.out.println("Creating RenderedImages for all slices took "+timer.reportMsAndRestart()+" ms");
        return slices;
    }
        

}
