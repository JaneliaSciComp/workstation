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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import javax.media.opengl.GL3;
import org.apache.commons.io.IOUtils;
import org.janelia.geometry.util.PerformanceTimer;
import org.janelia.gltools.GL3Resource;
import org.janelia.it.jacs.shared.img_3d_loader.FileByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class Texture3d extends BasicTexture implements GL3Resource
{

    private static final Logger log = LoggerFactory.getLogger(Texture3d.class);

    private static ScheduledThreadPoolExecutor scheduledThreadPoolExecutor=new ScheduledThreadPoolExecutor(6);

    protected int height = 0;
    protected int depth = 0;
    protected int pixelBufferObject = 0;
    byte[] pixelBytes;
    short[] shortBytes;
    FileByteSource optionalFileByteSource;

    public Texture3d() {
        textureTarget = GL3.GL_TEXTURE_3D;
        magFilter = GL3.GL_LINEAR;
        minFilter = GL3.GL_LINEAR;
        useImmutableTexture = true;
    }

    @Override
    public void dispose(GL3 gl) {
        //log.info("dispose() begin");
        super.dispose(gl);
        int[] pbos = {pixelBufferObject};
        gl.glDeleteBuffers(1, pbos, 0);
        pixelBufferObject = 0;
        //log.info("dispose() end");
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
        if (reclaimRamAfterUpload) {
            deallocateRam();
        }
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
        float t1=timer.reportMsAndRestart();
        System.out.println("Tiff load to RenderedImages took "+t1+" ms");
        Texture3d result = loadStack(slices);
        float t2=timer.reportMsAndRestart();
        System.out.println("Tiff RenderedImages to Buffer took "+t2+" ms");
        System.out.println("loadTiffStack() total time="+(t1+t2)+" ms");
        return result;
    }

    protected void allocatePixels() {
        int byteCount = numberOfComponents * bytesPerIntensity * width * height * depth;
        pixelBytes = new byte[byteCount];
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

        // NOTE: empirically, this step cannot be done with multiple-threads. It is mysteriously not thread-safe.
        Raster[] raster=new Raster[stack.length];
        for (int i=0;i<stack.length;i++) {
            raster[i]=stack[i].getData();
        }

        System.out.println("Getting Rasters from RenderedImages took "+timer.reportMsAndRestart()+" ms");

        if (bytesPerIntensity<2) { // 8-bit
            pixels.rewind();
            if (depth<6) {
                LoadStackZSlice8bit loadStackZSlice8bit=new LoadStackZSlice8bit(0,depth,pixels,raster,depth,height,width,numberOfComponents);
                loadStackZSlice8bit.run();
            } else {
                List<Future> threadList=new ArrayList<>();
                for (int z=0;z<depth;) {
                    int remainingZ=depth-z;
                    int zCount=3;
                    if (remainingZ<zCount) {
                        zCount=remainingZ;
                    }
                    LoadStackZSlice8bit loadStackZSlice8bit=new LoadStackZSlice8bit(z,zCount,pixels,raster,depth,height,width,numberOfComponents);
                    threadList.add(scheduledThreadPoolExecutor.submit(loadStackZSlice8bit));
                    z+=zCount;
                }
                int doneCount = 0;
                long startTime = new Date().getTime();
                while (doneCount < threadList.size()) {
                    long currentTime = new Date().getTime();
                    if (currentTime - startTime > 30000) {
                        log.error("loadStack() exceeded max thread pool wait time");
                        break;
                    }
                    try {
                        Thread.sleep(10);
                    }
                    catch (Exception ex) {
                    }
                    doneCount = 0;
                    for (Future f : threadList) {
                        if (f.isDone()) doneCount++;
                    }
                }
            }
            pixels.flip();
        } else { // 16-bit
            shortPixels=pixels.asShortBuffer();
            shortPixels.rewind();
            if (depth<6) {
                LoadStackZSlice16bit loadStackZSlice16bit=new LoadStackZSlice16bit(0,depth,shortPixels,raster,depth,height,width,numberOfComponents);
                loadStackZSlice16bit.run();
            } else {
                List<Future> threadList=new ArrayList<>();
                for (int z=0;z<depth;) {
                    int remainingZ=depth-z;
                    int zCount=3;
                    if (remainingZ<zCount) {
                        zCount=remainingZ;
                    }
                    LoadStackZSlice16bit loadStackZSlice16bit=new LoadStackZSlice16bit(z,zCount,shortPixels,raster,depth,height,width,numberOfComponents);
                    threadList.add(scheduledThreadPoolExecutor.submit(loadStackZSlice16bit));
                    //loadStackZSlice16bit.run();
                    z+=zCount;
                }
                int doneCount = 0;
                long startTime = new Date().getTime();
                while (doneCount < threadList.size()) {
                    long currentTime = new Date().getTime();
                    if (currentTime - startTime > 30000) {
                        log.error("loadStack() exceeded max thread pool wait time");
                        break;
                    }
                    try {
                        Thread.sleep(10);
                    }
                    catch (Exception ex) {
                    }
                    doneCount = 0;
                    for (Future f : threadList) {
                        if (f.isDone()) doneCount++;
                    }
                }
            }
            shortPixels.flip();
        }
        pixels.rewind();

//        int[] pxl = new int[numberOfComponents];
//        if (bytesPerIntensity < 2) {
//            pixels.rewind();
//            for (int z = 0; z < depth; ++z) {
//                Raster raster = stack[z].getData();
//                for (int y = 0; y < height; ++y) {
//                    for (int x = 0; x < width; ++x) {
//                        raster.getPixel(x, y, pxl);
//                        for (int c = 0; c < numberOfComponents; ++c) {
//                            pixels.put((byte)(pxl[c] & 0xFF));
//                        }
//                    }
//                }
//            }
//            pixels.flip();
//        }
//        else { // 16 bit
//            shortPixels = pixels.asShortBuffer();
//            shortPixels.rewind();
//            // System.out.println("Casting short buffer took "+timer.reportMsAndRestart()+" ms");
//            Raster[] sliceRasters = new Raster[depth];
//
//            // TODO Run this in parallel
//            for (int z = 0; z < depth; ++z) {
//                sliceRasters[z] = stack[z].getData(); // slow 35-40 ms per slice
//            }
//            System.out.println("Getting Raster data took "+timer.reportMsAndRestart()+" ms");
//
//            for (int z = 0; z < depth; ++z) {
//                final boolean useRawBytes = true;
//                if (useRawBytes) { // not faster! 12 seconds
//                    DataBufferUShort dbu = (DataBufferUShort)sliceRasters[z].getDataBuffer();
//                    // System.out.println("Slice "+z+" getDataBuffer() took "+timer.reportMsAndRestart()+" ms");
//                    short[] sliceData = dbu.getData();
//                    // System.out.println("Slice "+z+" getData() [2] took "+timer.reportMsAndRestart()+" ms");
//                    shortPixels.put(sliceData);
//                    /// System.out.println("Slice "+z+" put() took "+timer.reportMsAndRestart()+" ms");
//                }
//                else { // 12 seconds
//                    for (int y = 0; y < height; ++y) {
//                        for (int x = 0; x < width; ++x) {
//                            sliceRasters[z].getPixel(x, y, pxl);
//                            for (int c = 0; c < numberOfComponents; ++c) {
//                                shortPixels.put((short)(pxl[c] & 0xFFFF));
//                            }
//                        }
//                    }
//                }
//            }
//            shortPixels.flip();
//        }
//        pixels.rewind();

        System.out.println("Getting Raster data and populating texture buffer took "+timer.reportMsAndRestart()+" ms");

        computeMipmaps();

        System.out.println("Computing mipmaps took "+timer.reportMsAndRestart()+" ms");

        needsUpload = true;

        return this;
    }

    private static class LoadStackZSlice8bit implements Runnable {
        int zStart, zCount, depth, height, width, numberOfComponents;
        ByteBuffer pixels;
        Raster[] raster;

        public LoadStackZSlice8bit(int zStart, int zCount, ByteBuffer pixels, Raster[] raster, int depth, int height, int width, int numberOfComponents) {
            this.zStart=zStart;
            this.zCount=zCount;
            this.pixels=pixels;
            this.raster=raster;
            this.depth=depth;
            this.height=height;
            this.width=width;
            this.numberOfComponents=numberOfComponents;
        }

        public void run() {
            int pixelOffset=zStart*height*width*numberOfComponents;
            int[] pxl = new int[numberOfComponents];
            int zMax=zStart+zCount;
            for (int z = zStart; z < zMax; ++z) {
                Raster r = raster[z];
                for (int y = 0; y < height; ++y) {
                    for (int x = 0; x < width; ++x) {
                        r.getPixel(x, y, pxl);
                        for (int c = 0; c < numberOfComponents; ++c) {
                            pixels.put(pixelOffset++, (byte)(pxl[c] & 0xFF));
                        }
                    }
                }
            }
        }

    }

    private static class LoadStackZSlice16bit implements Runnable {
        int zStart, zCount, depth, height, width, numberOfComponents;
        ShortBuffer shortPixels;
        Raster[] raster;

        public LoadStackZSlice16bit(int zStart, int zCount, ShortBuffer shortPixels, Raster[] raster, int depth, int height, int width, int numberOfComponents) {
            this.zStart=zStart;
            this.zCount=zCount;
            this.shortPixels=shortPixels;
            this.raster=raster;
            this.depth=depth;
            this.height=height;
            this.width=width;
            this.numberOfComponents=numberOfComponents;
        }

        public void run() {
            int shortOffset=zStart*height*width*numberOfComponents;
            //log.info("LoadStackZSlice16bit run() zStart="+zStart+" zCount="+zCount+" shortOffset="+shortOffset);
            int zMax=zStart+zCount;
            for (int z = zStart; z < zMax; ++z) {
                Raster sliceRaster=raster[z];
                DataBufferUShort dbu = (DataBufferUShort)sliceRaster.getDataBuffer();
                short[] sliceData = dbu.getData();
                addArrayToBuffer(shortPixels, sliceData, shortOffset);
                shortOffset+=sliceData.length;
            }
        }

        private void addArrayToBuffer(ShortBuffer shortBuffer, short[] arr, int bufferOffset) {
            for (int i=0;i<arr.length;i++) {
                shortBuffer.put(bufferOffset+i, arr[i]);
            }
        }

    }

    protected void computeMipmaps() {
        //log.info("computeMipmaps() numberOfComponents="+numberOfComponents);
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


    private static int secondLargestIntensity(int[] samples, int sampleCount) {
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
        //log.info("createMipmapUsingMaxFilter() numberOfComponents="+numberOfComponents);
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

        //log.info("Creating mipmap width="+result.width+" height="+result.height+" depth="+result.depth+" level="+result.mipMapLevel);

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

        int HWN = height * width * numberOfComponents;
        int WN = width * numberOfComponents;

        short[] shortArr=null;
        byte[] byteArr=null;

        if (bytesPerIntensity>1) {
            if (shortBytes==null) {
                int length = pixelBytes.length / 2;
                shortBytes = new short[length];
                for (int i=0;i<length;i++) {
                    int o=i*2;
                    shortBytes[i]=(short)(((pixelBytes[o] & 0xff) | (pixelBytes[o+1] & 0xff) << 8) & 0xffff);
                }
            }
            shortArr=shortBytes;
        } else {
            byteArr=pixelBytes;
        }

        // Outer loops over output texture voxels
        int zh1=(int)(halfInputDeltaUvw[2]*depth);
        int yh1=(int)(halfInputDeltaUvw[1]*height);
        int xh1=(int)(halfInputDeltaUvw[0]*width);

        if (result.depth<8) {
            MipMapMaxFilterZSlice zRunnable = new MipMapMaxFilterZSlice(0, result.depth, zh1, yh1, xh1, HWN, WN, result, depth,
                    width, height, numberOfComponents, shortArr, byteArr, bytesPerIntensity, shortsOut, bytesOut);
            zRunnable.run();
            return result;
        } else {
            List<Future> threadList = new ArrayList<>();
            for (int z = 0; z < result.depth; ) {
                int zCount = 4;
                int zRemaining = result.depth - z;
                if (zRemaining < zCount) {
                    zCount = zRemaining;
                }
                MipMapMaxFilterZSlice zRunnable = new MipMapMaxFilterZSlice(z, zCount, zh1, yh1, xh1, HWN, WN, result, depth,
                        width, height, numberOfComponents, shortArr, byteArr, bytesPerIntensity, shortsOut, bytesOut);
                threadList.add(scheduledThreadPoolExecutor.submit(zRunnable));
                z += zCount;
            }
            int doneCount = 0;
            long startTime = new Date().getTime();
            while (doneCount < threadList.size()) {
                long currentTime = new Date().getTime();
                if (currentTime - startTime > 30000) {
                    log.error("createMipmapUsingMaxFilter() exceeded max thread pool wait time");
                    break;
                }
                try {
                    Thread.sleep(10);
                }
                catch (Exception ex) {
                }
                doneCount = 0;
                for (Future f : threadList) {
                    if (f.isDone()) doneCount++;
                }
            }
            return result;
        }
    }

    private static class MipMapMaxFilterZSlice implements Runnable {

        final int IGNORE_VALUE=Integer.MIN_VALUE;

        int z, zCount, zh1, yh1, xh1, HWN, WN, depth, height, width, numberOfComponents, bytesPerIntensity;
        Texture3d result;
        short[] shortArr;
        byte[] byteArr;
        ShortBuffer shortsOut;
        ByteBuffer bytesOut;

        int [] zIn = new int [2];
        int [] yIn = new int [2];
        int [] xIn = new int [2];

        int [] samples = new int [8];

        public MipMapMaxFilterZSlice(int z, int zCount, int zh1, int yh1, int xh1, int HWN, int WN, Texture3d result, int depth,
                                     int width, int height, int numberOfComponents, short[] shortArr, byte[] byteArr,
                                     int bytesPerIntensity, ShortBuffer shortsOut, ByteBuffer bytesOut) {
            this.z=z;
            this.zCount=zCount;
            this.zh1=zh1;
            this.yh1=yh1;
            this.xh1=xh1;
            this.HWN=HWN;
            this.WN=WN;
            this.result=result;
            this.depth=depth;
            this.width=width;
            this.height=height;
            this.numberOfComponents=numberOfComponents;
            this.shortArr=shortArr;
            this.byteArr=byteArr;
            this.bytesPerIntensity=bytesPerIntensity;
            this.shortsOut=shortsOut;
            this.bytesOut=bytesOut;
        }

        public void run() {

            int zMax = z + zCount;

            int RHWC=result.height*result.width*numberOfComponents;

            while (z < zMax) {

                int outputIndex = z * RHWC;

                if (depth == 1) {
                    zIn[0] = 0;
                    xIn[1] = IGNORE_VALUE;
                } else {
                    float fractionalZOut = (z + 0.5f) / result.depth;
                    int zf1 = (int) (fractionalZOut * depth);
                    zIn[0] = (zf1 - zh1) * HWN;
                    zIn[1] = (zf1 + zh1) * HWN;
                    if (zIn[0] == zIn[1]) zIn[1] = IGNORE_VALUE;
                }
                for (int y = 0; y < result.height; ++y) {
                    if (height == 1) {
                        yIn[0] = 0;
                        yIn[1] = IGNORE_VALUE;
                    } else {
                        float fractionalYOut = (y + 0.5f) / result.height;
                        int yf1 = (int) (fractionalYOut * height);
                        yIn[0] = (yf1 - yh1) * WN;
                        yIn[1] = (yf1 + yh1) * WN;
                        if (yIn[0] == yIn[1]) yIn[1] = IGNORE_VALUE;
                    }
                    for (int x = 0; x < result.width; ++x) {
                        if (width == 1) {
                            xIn[0] = 0;
                            xIn[1] = IGNORE_VALUE;
                        } else {
                            float fractionalXOut = (x + 0.5f) / result.width;
                            int xf1 = (int) (fractionalXOut * width);
                            xIn[0] = (xf1 - xh1);
                            xIn[1] = (xf1 + xh1);
                            if (xIn[0] == xIn[1]) xIn[1] = IGNORE_VALUE;
                        }
                        int sampleCount = 0;

                        if (numberOfComponents == 1) {

                            if (shortArr != null) {

                                // Inner loops over input texture voxels
                                for (int iz : zIn) {
                                    if (iz != IGNORE_VALUE) {
                                        for (int iy : yIn) {
                                            if (iy != IGNORE_VALUE) {
                                                int ZYWN = iy + iz;
                                                for (int ix : xIn) {
                                                    if (ix != IGNORE_VALUE) {
                                                        int offset = ZYWN + ix;
                                                        samples[sampleCount++] = shortArr[offset];
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                            } else {

                                // Inner loops over input texture voxels
                                for (int iz : zIn) {
                                    if (iz != IGNORE_VALUE) {
                                        for (int iy : yIn) {
                                            if (iy != IGNORE_VALUE) {
                                                int ZYWN = iy + iz;
                                                for (int ix : xIn) {
                                                    if (ix != IGNORE_VALUE) {
                                                        int offset = ZYWN + ix;
                                                        samples[sampleCount++] = byteArr[offset] & 0xff;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            int maxIntensity = secondLargestIntensity(samples, sampleCount);

                            if (bytesPerIntensity > 1) {
                                shortsOut.put(outputIndex, (short) (maxIntensity & 0xffff));
                            } else {
                                bytesOut.put(outputIndex, (byte) (maxIntensity & 0xff));
                            }

                            outputIndex++;

                        } else {

                            for (int c = 0; c < numberOfComponents; ++c) {

                                if (shortArr != null) {

                                    // Inner loops over input texture voxels
                                    for (int iz : zIn) {
                                        if (iz != IGNORE_VALUE) {
                                            for (int iy : yIn) {
                                                if (iy != IGNORE_VALUE) {
                                                    int ZYWN = iy + iz;
                                                    for (int ix : xIn) {
                                                        if (ix != IGNORE_VALUE) {
                                                            int offset = ZYWN
                                                                    + ix * numberOfComponents
                                                                    + c;
                                                            samples[sampleCount++] = shortArr[offset];
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                } else {

                                    // Inner loops over input texture voxels
                                    for (int iz : zIn) {
                                        if (iz != IGNORE_VALUE) {
                                            for (int iy : yIn) {
                                                if (iy != IGNORE_VALUE) {
                                                    int ZYWN = iy + iz;
                                                    for (int ix : xIn) {
                                                        if (ix != IGNORE_VALUE) {
                                                            int offset = ZYWN
                                                                    + ix * numberOfComponents
                                                                    + c;
                                                            samples[sampleCount++] = byteArr[offset] & 0xff;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                }

                                int maxIntensity = secondLargestIntensity(samples, sampleCount);

                                if (bytesPerIntensity > 1) {
                                    shortsOut.put(outputIndex, (short) (maxIntensity & 0xffff));
                                } else {
                                    bytesOut.put(outputIndex, (byte) (maxIntensity & 0xff));
                                }

                                outputIndex++;

                            }

                        }
                    }
                }
                z++;
            }
        }
    }

    public void setOptionalFileByteSource(FileByteSource fileByteSource) {
        this.optionalFileByteSource=fileByteSource;
    }

    private String getHttpPathFromFilePath(String filePath) {
        int startPosition=0;
        int colonPosition=filePath.indexOf(":");
        if (colonPosition>-1) {
            startPosition=colonPosition+1;
        }
        String result=filePath.replaceAll("\\\\","/");
        return result.substring(startPosition);
    }

    private RenderedImage[] renderedImagesFromTiffStack(File tiffFile) throws IOException {
        PerformanceTimer timer = new PerformanceTimer();
        // FileSeekableStream is the fastest load method I tested, by far

        ImageDecoder decoder;

        // Performance results for various load strategies below. NOTE: ALL STEPS INCLUDING:
        //   1) Decoder creation (here)
        //   2) RenderedImage generation (next step)
        //   3) Raster creation from RenderedImage (2nd next step)
        //
        // MUST be considered wrt performance implication of the choice of decoder configuration.
        // The total timings of these 3 steps is mentioned below (in seconds)

        final boolean useMemoryCache = false;
        final boolean useFileCache = false;
        final boolean useFileSS = false;
        final boolean useFileStream = false;

        final boolean useFilesReadAllBytesAndByteArraySeekableStream = true; // BY FAR THE FASTEST

        if (optionalFileByteSource!=null) {
            byte[] bytes=null;
            try {
                String httpPathFromFilePath=getHttpPathFromFilePath(tiffFile.getAbsolutePath());
                log.info("renderedImagesFromTiffStack - using httpPathFromFilePath="+httpPathFromFilePath);
                bytes = optionalFileByteSource.loadBytesForFile(httpPathFromFilePath);
                System.out.println("Texture3D HTTP byte load time ms="+timer.reportMsAndRestart());
            } catch (Exception ex) { ex.printStackTrace(); }
            SeekableStream s = new ByteArraySeekableStream(bytes);
            decoder = ImageCodec.createImageDecoder("tiff", s, null);
        }
        else if (useFilesReadAllBytesAndByteArraySeekableStream) { // 6.2, 7.0, 7.9, 6.5 seconds - PLEASE USE THIS ONE
            byte[] bytes= Files.readAllBytes(tiffFile.toPath());
            System.out.println("Texture3D FILE byte load time ms="+timer.reportMsAndRestart());
            SeekableStream s = new ByteArraySeekableStream(bytes);
            decoder = ImageCodec.createImageDecoder("tiff", s, null);
        }
        else if (useMemoryCache) { // 18.0, 15.5, 13.0, 13.2 seconds
            InputStream tiffStream = new BufferedInputStream( new FileInputStream(tiffFile) );
            SeekableStream s = new MemoryCacheSeekableStream(tiffStream);
            decoder = ImageCodec.createImageDecoder("tiff", s, null);
        }
        else if (useFileCache) { // >65.0 seconds
            InputStream tiffStream = new BufferedInputStream( new FileInputStream(tiffFile) );
            SeekableStream s = new FileCacheSeekableStream(tiffStream);
            decoder = ImageCodec.createImageDecoder("tiff", s, null);
        }
        else if (useFileSS) { // 15.3, 16.3, 13.3, 14.6 seconds
            SeekableStream s = new FileSeekableStream(tiffFile);
            decoder = ImageCodec.createImageDecoder("tiff", s, null);
        }
        else if (useFileStream) { // >68.0 seconds
            InputStream tiffStream = new BufferedInputStream( new FileInputStream(tiffFile) );
            decoder = ImageCodec.createImageDecoder("tiff", tiffStream, null); // 55 seconds
        }
        else { // 68.0, 37.8 seconds
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
