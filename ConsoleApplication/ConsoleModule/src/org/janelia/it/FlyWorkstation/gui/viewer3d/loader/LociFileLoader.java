package org.janelia.it.FlyWorkstation.gui.viewer3d.loader;

import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.gui.BufferedImageReader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;

import java.awt.image.BufferedImage;
import java.io.IOException;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.TextureDataBuilder;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.VolumeFileLoaderI;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/6/13
 * Time: 3:33 PM
 *
 * This may be extended for any data builder that needs a loci read method.
 */
public abstract class LociFileLoader extends TextureDataBuilder implements VolumeFileLoaderI {

    @Override
    public TextureDataI createTextureDataBean() {
        return new TextureDataBean(argbTextureIntArray, sx, sy, sz );
    }

    /** A facility for loci reader users. */
    protected void loadLociReader(IFormatReader reader) throws FormatException, IOException {
        BufferedImageReader in = new BufferedImageReader(reader);
        in.setId(unCachedFileName);
        loadLociReader(in);
    }

    protected boolean loadLociReader(BufferedImageReader in)
            throws IOException, FormatException
    {
        sx = in.getSizeX();
        sy = in.getSizeY();
        sz = in.getSizeZ();
        argbTextureIntArray = new int[sx*sy*sz];
        int scanLineStride = sx;
        for (int z = 0; z < sz; z++) {
            BufferedImage zSlice = in.openImage(z);
            int zOffset = z * sx * sy;
            // int[] pixels = ((DataBufferInt)zSlice.getData().getDataBuffer()).getData();
            zSlice.getRGB(0, 0,
                    sx, sy,
                    argbTextureIntArray,
                    zOffset,
                    scanLineStride);
        }
        in.close();
        // This is believed to be called sufficiently from the Volume Loader
        //setAlphaToSaturateColors(colorSpace);
        return true;
    }

}
