
package org.janelia.gltools.texture;

import java.awt.image.ColorModel;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.media.opengl.GL3;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.MemoryCacheSeekableStream;
import com.sun.media.jai.codec.SeekableStream;

import org.apache.commons.lang3.tuple.Pair;
import org.janelia.geometry.util.PerformanceTimer;
import org.janelia.gltools.GL3Resource;
import org.janelia.gltools.MJ2Parser;
import org.janelia.gltools.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.options.ApplicationOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class Texture3d extends BasicTexture implements GL3Resource {

    private static final Logger LOG = LoggerFactory.getLogger(Texture3d.class);

    private static final ActivityLogHelper activityLog = ActivityLogHelper.getInstance();
    
    private static final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor=new ScheduledThreadPoolExecutor(6);

    protected int height = 0;
    protected int depth = 0;
    private int pixelBufferObject = 0;
    private byte[] pixelBytes;
    private short[] shortBytes;

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
    protected void uploadTexture(GL3 gl) {
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
        LOG.debug("Uploading texture and mipmaps took {} ms", timer.reportMsAndRestart());
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

    /**
     * Loads the given tiff stack into memory and returns true. If the load is not completed for any reason,
     * this method returns false.
     * @param stackName
     * @param stackStream
     * @return
     * @throws IOException
     */
    public boolean loadTiffStack(String stackName, InputStream stackStream) throws IOException {
        PerformanceTimer timer = new PerformanceTimer();
        if (stackStream == null) {
            return false;
        }
        RenderedImage[] slices = renderedImagesFromTiffStack(stackStream);
        if (slices==null) return false;

        long logId = System.currentTimeMillis();

        float t1 = timer.reportMsAndRestart();
        LOG.debug("Tiff load to RenderedImages took {} ms", t1);

        if (slices.length > 0) {
            Pair<Raster[], ColorModel> slicePair = prepTiffStack(slices);

            float t2=timer.reportMsAndRestart();
            LOG.debug("Tiff RenderedImages to raster took {} ms", t2);
            activityLog.logBrickLoadToRendered(logId, stackName, ApplicationOptions.getInstance().isUseHTTPForTileAccess(), t1 + t2);

            loadStack(slicePair.getLeft(), slicePair.getRight());

            float t3 = timer.reportMsAndRestart();
            LOG.debug("Tiff load raster slices to texture buffer took {} ms", t3);
            LOG.info(">>> loadTiffStack() total time = {} ms", (t1 + t2 + t3));
            return true;
        } else {
            return false;
        }
    }

    public boolean loadMJ2Stack(String stackName, InputStream stackStream) throws IOException {
        if (stackStream == null) {
            return false;
        }
        try {
            PerformanceTimer timer = new PerformanceTimer();
            MJ2Parser parser = new MJ2Parser();
            Pair<Raster[], ColorModel> slicePair = parser.extractSlices(stackStream);
            Raster[] slices = slicePair.getLeft();
            if (slices==null) return false;

            if (slices.length > 0) {
                depth = slices.length;
                width = slices[0].getWidth();
                height = slices[0].getHeight();
                loadStack(slicePair.getLeft(), slicePair.getRight());

                float t1 = timer.reportMsAndRestart();
                LOG.info(">>> loadTiffStack() total time = {} ms", (t1));
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to parse", e);
        }
    }

    /**
     * Safe version of internal loadStack() for raster images that are already loaded/parsed elsewhere.
     * @param slices stack of Raster images
     * @param colorModel Raster image color model
     * @return true if stack is successfully loaded
     */
    public boolean loadRasterSlices(Raster[] slices, ColorModel colorModel) {
        try {
            PerformanceTimer timer = new PerformanceTimer();

            if (slices == null) {
                return false;
            }

            if (slices.length > 0) {
                depth = slices.length;
                width = slices[0].getWidth();
                height = slices[0].getHeight();

                loadStack(slices, colorModel);

                float t1 = timer.reportMsAndRestart();
                LOG.info(">>> loadRasterSlices() total time = {} ms", (t1));
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to parse", e);
        }
    }

    private void allocatePixels() {
        int byteCount = numberOfComponents * bytesPerIntensity * width * height * depth;
        pixelBytes = new byte[byteCount];
        pixels = ByteBuffer.wrap(pixelBytes);
        pixels.order(ByteOrder.nativeOrder());
        pixels.rewind();
    }

    private Pair<Raster[], ColorModel> prepTiffStack(RenderedImage[] stack) {
        PerformanceTimer timer = new PerformanceTimer();

        depth = stack.length;
        RenderedImage slice = stack[0];
        width = slice.getWidth();
        height = slice.getHeight();

        // NOTE: empirically, this step cannot be done with multiple-threads. It is mysteriously not thread-safe.
        Raster[] raster = new Raster[stack.length];
        for (int i = 0; i < stack.length; i++) {
            raster[i] = stack[i].getData();
        }

        LOG.debug("Getting Rasters from RenderedImages took {} ms", timer.reportMsAndRestart());
        return Pair.of(raster, slice.getColorModel());
    }

    private void loadStack(Raster[] raster, ColorModel colorModel) {
        PerformanceTimer timer = new PerformanceTimer();

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

        LOG.debug("Initializing texture buffer took {} ms", timer.reportMsAndRestart());

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
                        LOG.error("loadStack() exceeded max thread pool wait time");
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
                        LOG.error("loadStack() exceeded max thread pool wait time");
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

        LOG.debug("Getting Raster data and populating texture buffer took {} ms", timer.reportMsAndRestart());

        computeMipmaps();

        LOG.debug("Computing mipmaps took {} ms", timer.reportMsAndRestart());

        needsUpload = true;
    }

    private static class LoadStackZSlice8bit implements Runnable {
        int zStart, zCount, depth, height, width, numberOfComponents;
        ByteBuffer pixels;
        Raster[] raster;

        LoadStackZSlice8bit(int zStart, int zCount, ByteBuffer pixels, Raster[] raster, int depth, int height, int width, int numberOfComponents) {
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

        LoadStackZSlice16bit(int zStart, int zCount, ShortBuffer shortPixels, Raster[] raster, int depth, int height, int width, int numberOfComponents) {
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

    private void computeMipmaps() {
        mipmaps.clear();
        PerformanceTimer timer = new PerformanceTimer();
        Texture3d mipmap = createMipmapUsingMaxFilter();
        while (mipmap != null) {
            LOG.trace("Creating mipmap took {} ms", timer.reportMsAndRestart());
            mipmaps.add(mipmap);
            mipmap = mipmap.createMipmapUsingMaxFilter();
        }
    }

    private void copyParameters(Texture3d rhs) {
        super.copyParameters(rhs);
        height = rhs.height;
        depth = rhs.depth;
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

    private Texture3d createMipmapUsingMaxFilter() {
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
                    LOG.error("createMipmapUsingMaxFilter() exceeded max thread pool wait time");
                    break;
                }
                try {
                    Thread.sleep(10);
                } catch (Exception ex) {
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

        MipMapMaxFilterZSlice(int z, int zCount, int zh1, int yh1, int xh1, int HWN, int WN, Texture3d result, int depth,
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

    private RenderedImage[] renderedImagesFromTiffStack(InputStream stackStream) throws IOException {
        PerformanceTimer timer = new PerformanceTimer();

        SeekableStream tiffStream;
        if (stackStream instanceof SeekableStream) {
            tiffStream = (SeekableStream) stackStream;
        } else {
            tiffStream = new MemoryCacheSeekableStream(stackStream);
        }

        ImageDecoder decoder = ImageCodec.createImageDecoder("tiff", tiffStream, null);
        if (decoder != null) {
            LOG.debug("Creating image decoder from tiff file took {} ms", timer.reportMsAndRestart());
            int sz = decoder.getNumPages();
            RenderedImage slices[] = new RenderedImage[sz];
            for (int z = 0; z < sz; ++z) {
                slices[z] = decoder.decodeAsRenderedImage(z);
            }
            LOG.debug("Creating RenderedImages for all slices took {} ms", timer.reportMsAndRestart());
            return slices;
        } else {
            return null;
        }
    }


}
