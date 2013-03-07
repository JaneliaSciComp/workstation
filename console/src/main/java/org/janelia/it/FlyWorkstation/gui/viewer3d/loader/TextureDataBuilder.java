package org.janelia.it.FlyWorkstation.gui.viewer3d.loader;

import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeBrick;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;
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

    protected int[] argbTextureIntArray;
    protected byte[] textureByteArray;
    protected int sx, sy, sz;
    protected int channelCount = 1; // Default for non-data-bearing file formats.
    protected VolumeDataAcceptor.TextureColorSpace colorSpace =
            VolumeBrick.TextureColorSpace.COLOR_SPACE_LINEAR;
    protected String header = null;
    protected int pixelBytes = 1;
    protected ByteOrder pixelByteOrder = ByteOrder.LITTLE_ENDIAN;
    protected String unCachedFileName;

    public void setColorSpace( VolumeDataAcceptor.TextureColorSpace colorSpace ) {
        this.colorSpace = colorSpace;
    }

    public TextureDataI buildTextureData( boolean isLuminance ) {
        TextureDataI textureData = createTextureDataBean();

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

        if (! isLuminance  &&  (pixelBytes == 4)  &&  argbTextureIntArray != null ) {
            setAlphaToSaturateColors( colorSpace );
        }
        else {
            if ( unCachedFileName.contains( V3dMaskFileLoader.COMPARTMENT_MASK_INDEX ) ) {
                textureData.setInverted( false );  // Do not invert the compartment mask.
            }
        }

        return textureData;
    }

    /**
     * Set alpha component of each voxel assuming that R,G,B
     * values represent a saturated color with premultiplied alpha.
     * Similar to Vaa3D.  In other words, alpha = max(R,G,B)
     */
    private void setAlphaToSaturateColors(VolumeDataAcceptor.TextureColorSpace space) {
        if ( space == null )
            return;

        // Use modified alpha value for sRGB textures
        int[] alphaMap = new int[256];
        double exponent = 1.0;
        if (space == VolumeDataAcceptor.TextureColorSpace.COLOR_SPACE_SRGB)
            exponent  = 2.2;
        for (int i = 0; i < 256; ++i) {
            double i0 = i / 255.0;
            double i1 = Math.pow(i0, exponent);
            alphaMap[i] = (int)(i1 * 255.0 + 0.5);
        }
        int numVoxels = argbTextureIntArray.length;
        for (int v = 0; v < numVoxels; ++v) {
            int argb = argbTextureIntArray[v];
            int red   = (argb & 0x00ff0000) >>> 16;
            int green = (argb & 0x0000ff00) >>> 8;
            int blue  = (argb & 0x000000ff);
            int alpha = Math.max(red, Math.max(green, blue));
            alpha = alphaMap[alpha];
            argb = (argb & 0x00ffffff) | (alpha << 24);
            argbTextureIntArray[v] = argb;
        }
    }

    abstract protected TextureDataI createTextureDataBean();

}
