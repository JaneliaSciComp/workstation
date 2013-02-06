package org.janelia.it.FlyWorkstation.gui.viewer3d.loader;

import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeBrick;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.MaskTextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;

import java.nio.ByteOrder;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/6/13
 * Time: 2:31 PM
 *
 * Extend this with something that can supply the data array, x, y, and z sizes.
 */
public abstract class TextureDataBuilder {

    protected int[] argbIntArray;
    protected byte[] maskByteArray;
    protected int sx, sy, sz;
    protected int channelCount = 1; // Default for non-data-bearing file formats.
    protected VolumeDataAcceptor.TextureColorSpace colorSpace =
            VolumeBrick.TextureColorSpace.COLOR_SPACE_LINEAR;
    protected String header = null;
    protected int pixelBytes = 1;
    protected ByteOrder pixelByteOrder = ByteOrder.LITTLE_ENDIAN;
    protected String unCachedFileName;

    public TextureDataI buildTextureData( boolean isMask ) {
        TextureDataI textureData = createTextureDataBean();

        /*
        if ( isMask ) {
            textureData = new MaskTextureDataBean( maskByteArray, sx, sy, sz );
        }
        else {
            textureData = new TextureDataBean( argbIntArray, sx, sy, sz );
        }
        */
        textureData.setSx(sx);
        textureData.setSy(sy);
        textureData.setSz(sz);

        textureData.setColorSpace(colorSpace);
        textureData.setVolumeMicrometers(new Double[]{(double) sx, (double) sy, (double) sz});
        textureData.setVoxelMicrometers(new Double[]{1.0, 1.0, 1.0});
        if ( header != null ) {
            textureData.setHeader(header);
        }
        textureData.setByteOrder(pixelByteOrder);
        textureData.setPixelByteCount(pixelBytes);
        textureData.setFilename( unCachedFileName );
        textureData.setChannelCount(channelCount);

        return textureData;
    }

    abstract protected TextureDataI createTextureDataBean();
}
