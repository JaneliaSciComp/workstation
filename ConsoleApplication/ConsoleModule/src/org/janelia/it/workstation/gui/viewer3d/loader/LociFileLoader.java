package org.janelia.it.workstation.gui.viewer3d.loader;

import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.gui.BufferedImageReader;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/6/13
 * Time: 3:33 PM
 *
 * This may be extended for any data builder that needs a loci read method.
 */
public abstract class LociFileLoader extends AbstractVolumeFileLoader {

    /** A facility for loci reader users. */
    protected void loadLociReader(IFormatReader reader) throws FormatException, IOException {
        BufferedImageReader in = new BufferedImageReader(reader);
        in.setId(getUnCachedFileName());
        loadLociReader(in);
    }

    protected boolean loadLociReader(BufferedImageReader in)
            throws IOException, FormatException
    {
        setSx(in.getSizeX());
        setSy(in.getSizeY());
        setSz(in.getSizeZ());
        setArgbTextureIntArray(new int[in.getSizeX()*in.getSizeY()*in.getSizeZ()]);
        int scanLineStride = in.getSizeX();
        for (int z = 0; z < in.getSizeZ(); z++) {
            BufferedImage zSlice = in.openImage(z);
            int zOffset = z * in.getSizeX() * in.getSizeY();
            // int[] pixels = ((DataBufferInt)zSlice.getData().getDataBuffer()).getData();
            zSlice.getRGB(0, 0,
                    in.getSizeX(), in.getSizeY(),
                    getArgbTextureIntArray(),
                    zOffset,
                    scanLineStride);
        }
        in.close();
        // This is believed to be called sufficiently from the Volume Loader
        //setAlphaToSaturateColors(colorSpace);
        return true;
    }

}
