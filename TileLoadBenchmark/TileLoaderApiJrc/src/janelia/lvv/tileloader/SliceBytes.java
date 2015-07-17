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
 * Slice image as block of bytes, possibly suitable for loading to GPU.
 * Create for use in tile loading benchmarks.
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

    public SliceBytes(RenderedImage renderedImage, int sliceIndex) 
    {
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
    }
}
