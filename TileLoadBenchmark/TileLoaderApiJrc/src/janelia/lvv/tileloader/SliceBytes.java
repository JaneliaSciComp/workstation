/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package janelia.lvv.tileloader;

import java.awt.Dimension;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Slice image as block of bytes, possibly suitable for loading to GPU
 * @author Christopher Bruns
 */
public class SliceBytes
{
    private final int channelCount;
    private final Dimension sizePixels;
    private final int bitDepth;
    private final boolean isSRgb;
    private final ByteBuffer pixels;
    private final int sliceIndex;
    // Timing performance
    private final long preBinarizedNanoTime;
    private final long finalLoadedNanoTime;
    
    // TODO - align to multiple of 8?
    public SliceBytes(RenderedImage renderedImage, int sliceIndex) 
    {
        this.preBinarizedNanoTime = System.nanoTime();
        this.sliceIndex = sliceIndex;
        RenderedImage image = renderedImage;
		ColorModel colorModel = image.getColorModel();
		// If input image uses indexed color table, convert to RGB first.
		if (colorModel instanceof IndexColorModel) {
			IndexColorModel indexColorModel = (IndexColorModel) colorModel;
			image = indexColorModel.convertToIntDiscrete(image.getData(), false);
			colorModel = image.getColorModel();
		}
        int width = image.getWidth();
        int height = image.getHeight();
        sizePixels = new Dimension(width, height);
		isSRgb = colorModel.getColorSpace().isCS_sRGB();
		channelCount = colorModel.getNumComponents();
		int bd = colorModel.getPixelSize() / this.channelCount;
		// treat indexed image as rgb
		if (bd < 8)
			bd = 8;
        this.bitDepth = bd;
		assert((this.bitDepth == 8) || (this.bitDepth == 16));
		int pixelByteCount = this.channelCount * this.bitDepth/8;
		int rowByteCount = pixelByteCount * this.sizePixels.width;
		int imageByteCount = this.sizePixels.height * rowByteCount;
		// Allocate image store buffer, exactly as it will be passed to openGL
		// TODO - Consider sharing a buffer among multiple textures to save allocation time.
		// (there would need to be a separate one for each thread)
		byte byteArray[] = new byte[imageByteCount];
		ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
		byteBuffer.order(ByteOrder.nativeOrder());
		ShortBuffer shortBuffer = byteBuffer.asShortBuffer(); // for 16-bit case
		//
		Raster raster = image.getData();
		int pixelData[] = new int[this.channelCount];
		final boolean is16Bit = (this.bitDepth == 16);
		if (is16Bit) {
			for (int y = 0; y < this.sizePixels.height; ++y) {
				// Choose ragged right edge pad color from right
				// edge of used portion of scan line.
				for (int x = 0; x < this.sizePixels.width; ++x) {
                    raster.getPixel(x, y, pixelData);
                    for (int i : pixelData) {
                        shortBuffer.put((short)i);
                    }
				}
			}
		} else { // 8-bit
			for (int y = 0; y < this.sizePixels.height; ++y) {
				for (int x = 0; x < this.sizePixels.width; ++x) {
                    raster.getPixel(x, y, pixelData);
                    for (int i : pixelData) {
                        byteBuffer.put((byte)i);
                    }
				}
			}			
		}
		pixels = byteBuffer;
        finalLoadedNanoTime = System.nanoTime();
    }

    public long getPreBinarizedNanoTime()
    {
        return preBinarizedNanoTime;
    }

    public long getFinalLoadedNanoTime()
    {
        return finalLoadedNanoTime;
    }
    
}
