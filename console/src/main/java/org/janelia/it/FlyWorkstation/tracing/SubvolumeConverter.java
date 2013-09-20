package org.janelia.it.FlyWorkstation.tracing;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import org.janelia.it.FlyWorkstation.gui.slice_viewer.Subvolume;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

public class SubvolumeConverter {
    /**
     * Convert a Subvolume to an ImageJ ImagePlus hyperstack
     * @param subvolume
     * @return
     */
    public static ImagePlus toImagePlus(Subvolume subvolume) {
        int sx = subvolume.getExtent().getX();
        int sy = subvolume.getExtent().getY();
        int sz = subvolume.getExtent().getZ();
        int bitDepth = subvolume.getBytesPerIntensity() * 8;
        ImagePlus result = IJ.createHyperStack("subvolume",
                sx, // width
                sy, // height
                subvolume.getChannelCount(), // channels
                sz, // slices
                1, // frames
                bitDepth); // bit-depth
        result.setC(1);
        result.setZ(1);
        // Populate one slice at a time
        ImageProcessor ip;
        int slicePixelCount = sx*sy;
        ByteBuffer bb = subvolume.getByteBuffer();
        ShortBuffer sb = bb.asShortBuffer();
        bb.rewind();
        sb.rewind();
        // TODO - this might only work for grayscale volumes
        for (int z = 0; z < sz; ++z) {
            result.setZ(z+1);
            switch(bitDepth) {
            case 8:
                ip = new ByteProcessor(sx, sy);
                byte[] ar8 = new byte[slicePixelCount];
                bb.get(ar8, 0, slicePixelCount);
                ip.setPixels(ar8);
                break;
            case 16:
                ip = new ShortProcessor(sx, sy);
                short[] ar16 = new short[slicePixelCount];
                sb.get(ar16, 0, slicePixelCount);
                ip.setPixels(ar16);
                break;
            default:
                throw new RuntimeException("Unsupported bit depth");
            }
            result.setProcessor(ip);
        }
        result.setZ(1);
        result.setC(1);
        return result;
    }
}
