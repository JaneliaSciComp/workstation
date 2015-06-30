/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
