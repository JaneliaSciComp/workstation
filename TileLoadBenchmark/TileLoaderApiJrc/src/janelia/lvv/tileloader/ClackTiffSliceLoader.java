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
import java.nio.ByteBuffer;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;

/**
 *
 * @author Christopher Bruns
 */
public class ClackTiffSliceLoader implements BrickSliceLoader
{
    final static String tiffBaseName = "default";

    @Override
    public ByteBuffer loadSlice(URL brickSource, int sliceNumber) throws IOException
    {
        // TODO - internal timing
        
        // 1 - load each channel individually, since that's how Nathan arranged the files
        File folder = fileFromUrl(brickSource);
        int channelCount = countChannels(folder);
        ImageDecoder[] decoders = createImageDecoders(folder, channelCount);
        RenderedImage image = renderedImageFromChannelDecoders(decoders, sliceNumber);
        
        // TODO : 2 - interleave channels
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
    
    RenderedImage renderedImageFromChannelDecoders(ImageDecoder[] decoders, int sliceNumber) throws IOException
    {
        int sc = decoders.length;
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
    
    public ImageDecoder[] createImageDecoders(File folder, int channelCount) throws IOException
	{
		ImageDecoder decoders[] = new ImageDecoder[channelCount];
		for (int c = 0; c < channelCount; ++c) {
			File tiff = new File(folder, tiffBaseName+"."+c+".tif");
            SeekableStream s = new FileSeekableStream(tiff);
            decoders[c] = ImageCodec.createImageDecoder("tiff", s, null);
		}
		return decoders;
	}

}
