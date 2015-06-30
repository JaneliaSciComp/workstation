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

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.SeekableStream;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;

/**
 * ClackTiffSliceLoader is based on BlockTiffOctreeLoadAdapter, but with hopefully
 * fewer responsibilities.
 * For loading slices from Mouse Light tiff images created 2013-2015 at Janelia.
 * 
 * @author Christopher Bruns
 */
public class ClackTiffSliceLoader implements BrickSliceLoader
{
    final static String tiffBaseName = "default";

    @Override
    public SliceBytes loadSlice(URL brickSource, int sliceNumber) throws IOException
    {
        // 1 - load each channel individually, since that's how Nathan arranged the files
        File folder = fileFromUrl(brickSource);
        if (! folder.exists()) {
            throw new IOException("No such folder: " + folder);
        }
        int channelCount = countChannels(folder);
        ImageDecoder[] decoders = channelDecodersFromFolder(folder, channelCount);
        
        // 2 - interleave channels
        RenderedImage image = renderedImageFromChannelDecoders(decoders, sliceNumber);
        SliceBytes sliceBytes = new SliceBytes(image, sliceNumber);
        return sliceBytes;
    }
    
    @Override
    public SliceBytes[] loadSliceRange(URL brickSource, List<Integer> sliceIndices) throws IOException
    {
        File folder = fileFromUrl(brickSource);
        int channelCount = countChannels(folder);
        ImageDecoder[] decoders = channelDecodersFromFolder(folder, channelCount);
        int sliceCount = sliceIndices.size();
        SliceBytes[] result = new SliceBytes[sliceCount];
        int ix = 0;
        for (int s : sliceIndices) {
            RenderedImage image = renderedImageFromChannelDecoders(decoders, s);
            result[ix] = new SliceBytes(image, s);
            ix += 1;
        }
        return result;
    }
    
    // Each color channel is found in a separate monochromatic tiff file
    public int countChannels(File folder) {
        int c = 0;
        File tiff = new File(folder, tiffBaseName+"."+c+".tif");
        while (tiff.exists()) {
            c += 1;
            tiff = new File(folder, tiffBaseName+"."+c+".tif");
        }
        return c;
    }
    
    // https://weblogs.java.net/blog/kohsuke/archive/2007/04/how_to_convert.html
    public File fileFromUrl(URL url) {
        File f;
        try {
          f = new File(url.toURI());
        } catch(URISyntaxException e) {
          f = new File(url.getPath());
        }
        return f;
    }
    
    public ImageDecoder[] channelDecodersFromFolder(File folder, int channelCount) throws IOException
	{
		ImageDecoder decoders[] = new ImageDecoder[channelCount];
		for (int c = 0; c < channelCount; ++c) {
			File tiff = new File(folder, tiffBaseName+"."+c+".tif");
            SeekableStream s = new FileSeekableStream(tiff);
            decoders[c] = ImageCodec.createImageDecoder("tiff", s, null);
		}
		return decoders;
	}

    RenderedImage renderedImageFromChannelDecoders(ImageDecoder[] decoders, int sliceNumber) throws IOException
    {
        int sc = decoders.length;
        if (sc < 1) return null;
        RenderedImage channels[] = new RenderedImage[sc];
        for (int c = 0; c < sc; ++c) {
            ImageDecoder decoder = decoders[c];
            channels[c] = decoder.decodeAsRenderedImage(sliceNumber);
        } 
        RenderedImage composite = channels[0];
        if (sc > 1) {
            ParameterBlockJAI pb = new ParameterBlockJAI("bandmerge");
            for (int c = 0; c < sc; ++c) {
                pb.addSource(channels[c]);
            }
            composite = JAI.create("bandmerge", pb);
        }
        return composite;
    }

}
