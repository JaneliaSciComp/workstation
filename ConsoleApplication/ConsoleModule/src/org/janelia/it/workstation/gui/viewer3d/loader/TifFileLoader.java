package org.janelia.it.workstation.gui.viewer3d.loader;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javax.media.jai.NullOpImage;
import javax.media.jai.OpImage;
import javax.media.jai.RenderedImageAdapter;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/6/13
 * Time: 3:33 PM
 *
 * Handles TIFF via Loci reading capability.
 */
public class TifFileLoader extends TextureDataBuilder implements VolumeFileLoaderI {

    @Override
    public TextureDataI createTextureDataBean() {
        return new TextureDataBean(argbTextureIntArray, sx, sy, sz );
    }

    @Override
    public void loadVolumeFile( String fileName ) throws Exception {
        this.unCachedFileName = fileName;
        sx = -1;

        Collection<BufferedImage> allImages = loadTIFF( new File(fileName) );        
        int zOffset = 0;
        for ( BufferedImage zSlice: allImages ) {            
            if ( sx == -1 ) {
                sx = zSlice.getWidth();
                sy = zSlice.getHeight();
                sz = allImages.size();
                argbTextureIntArray = new int[sx*sy*sz];
            }
            else {
                if ( sx != zSlice.getWidth()  ||  sy != zSlice.getHeight() ) {
                    throw new IllegalStateException( "Image number " + zOffset +
                            " with HEIGHT=" + zSlice.getHeight() + " and WIDTH=" + 
                            zSlice.getWidth() + " has dimensions which do not match previous width * height of " + sx + " * " + sy );
                }
            }
            
            zSlice.getRGB(0, 0,
                    sx, sy,
                    argbTextureIntArray,
                    zOffset * sx * sy,
                    sx);

            zOffset ++;
        }
    }

    /**
     * Load specified tiff page and return as buffered zSlice.
     * From: http://opencapture.googlecode.com/svn/0.0.2/OpenCapture/src/net/filterlogic/util/imaging/ToTIFF.java
     * 
     * @param file
     * @param imageToLoad Page to load
     * @return BufferedImage
     */
    private static Collection<BufferedImage> loadTIFF(File file) {
        Collection<BufferedImage> imageCollection = new ArrayList<>();
        try {
            BufferedImage wholeImage = null;

            SeekableStream s = new FileSeekableStream(file);
            TIFFDecodeParam param = null;
            ImageDecoder dec = ImageCodec.createImageDecoder("tiff", s, param);
            final int numPages = dec.getNumPages();
            
            for (int imageToLoad = 0; imageToLoad < numPages; imageToLoad++) {
                RenderedImage op = dec.decodeAsRenderedImage();

                        /*
                        = new NullOpImage(dec.decodeAsRenderedImage(imageToLoad),
                                null,
                                OpImage.OP_IO_BOUND,
                                null);
                        */

                wholeImage = renderedToBuffered(op);
                imageCollection.add(wholeImage);
            }
            
            return imageCollection;


        } catch (IOException e) {
            System.out.println(e.toString());

            return null;
        }

    }
    
    /**
     * Convert RenderedImage to BufferedImage
     * @param img
     * @return BufferedImage
     */
    private static BufferedImage renderedToBuffered(RenderedImage img) {
        if (img instanceof BufferedImage) 
        {
            return (BufferedImage) img;
        }

        RenderedImageAdapter imageAdapter = new RenderedImageAdapter(img);
        BufferedImage bufImage = imageAdapter.getAsBufferedImage();
        return bufImage;
    }
}
