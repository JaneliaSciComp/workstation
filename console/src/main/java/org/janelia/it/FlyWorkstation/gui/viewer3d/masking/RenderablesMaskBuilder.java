package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;

import javax.media.opengl.GL2;
import java.nio.ByteOrder;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/13/13
 * Time: 4:21 PM
 *
 * This implementation of a mask builder takes renderables as its driving data.  It will accept the renderables,
 * along with their applicable chunks of data, to produce its texture data volume, in memory.
 */
public class RenderablesMaskBuilder extends RenderablesVolumeBuilder implements MaskBuilderI {

    private float[] coordCoverage;
    private Logger logger = LoggerFactory.getLogger( RenderablesMaskBuilder.class );
    private Collection<RenderableBean> renderableBeans;

    //----------------------------------------IMPLEMENT MaskChanDataAcceptorI
    /**
     * This is called with data to be loaded.
     *
     * @param maskNumber describes all points belonging to all pairs.
     * @param position where in the linear volume coords does this go?
     * @return total bytes read during this pairs-run.
     * @throws Exception thrown by called methods or if bad inputs are received.
     */
    @Override
    public int addMaskData(Integer maskNumber, long position) throws Exception {
        // Assumed little-endian and two bytes.
        byte[] maskData = super.getVolumeData();
        for ( int j = 0; j < getPixelByteCount(); j++ ) {
            maskData[ j + ((int)position * getPixelByteCount()) ] = (byte)( ( maskNumber >>> (8*j) ) & 0x00ff );
        }

        return 1;
    }

    @Override
    public int addChannelData(byte[] channelData, long position) throws Exception {
        throw new IllegalArgumentException( "Not implemented" );
    }

    /**
     * This tells the caller: only call me with mask data.  This is a mask builder.
     * @return mask
     */
    @Override
    public Acceptable getAcceptableInputs() {
        return Acceptable.mask;
    }

    //-------------------------END:-----------IMPLEMENT MaskChanDataAcceptorI

    //----------------------------------------IMPLEMENT MaskBuilderI

    /** Size of volume mask.  Numbers of voxels in all three directions. */
    @Override
    public Integer[] getVolumeMaskVoxels() {
        Integer[] voxels = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE };

        // May need to add more bytes to ensure that the coords are each multiples of 8 bytes.
        // If we do, we must take that into account for applying texture coordinates.
        coordCoverage = new float[] { 1.0f, 1.0f, 1.0f };
        for ( int i = 0; i < voxels.length; i++ ) {
            int leftover = voxels[i] % GPU_MULTIBYTE_DIVISIBILITY_VALUE;
            if ( leftover > 0 ) {
                int voxelModCount = GPU_MULTIBYTE_DIVISIBILITY_VALUE - leftover;
                int newVoxelCount = voxels[ i ] + voxelModCount;
                coordCoverage[ i ] = ((float)voxels[ i ]) / ((float)newVoxelCount);
                voxels[ i ] = newVoxelCount;
                logger.info("Expanding edge by " + voxelModCount);
            }
        }

        return voxels;
    }

    @Override
    public ByteOrder getPixelByteOrder() {
        return ByteOrder.nativeOrder();
    }

    @Override
    public int getPixelByteCount() {
        return super.getByteCount();
    }

    @Override
    public TextureDataI getCombinedTextureData() {
        byte[] maskData = super.getVolumeData();
        Integer[] volumeVoxels = this.getVolumeMaskVoxels();
        TextureDataI textureData = new TextureDataBean( maskData, volumeVoxels[0], volumeVoxels[1], volumeVoxels[2] );
        textureData.setInverted( false );
        textureData.setChannelCount( getChannelByteCount() );
        // See also VolumeLoader.resolveColorSpace()
        textureData.setColorSpace( this.getTextureColorSpace() );
        textureData.setByteOrder( this.getPixelByteOrder() );
        textureData.setFilename( "Mask of All Renderables" );
        textureData.setInterpolationMethod( GL2.GL_NEAREST );
        textureData.setRenderables( renderableBeans );
        textureData.setCoordCoverage( coordCoverage );

        return textureData;
    }

    @Override
    public VolumeDataAcceptor.TextureColorSpace getTextureColorSpace() {
        // See also VolumeLoader.resolveColorSpace()
        return VolumeDataAcceptor.TextureColorSpace.COLOR_SPACE_LINEAR;
    }

    @Override
    public void setRenderables(Collection<RenderableBean> renderables) {
        renderableBeans = renderables;
    }

    @Override
    public byte[] getVolumeData() {
        return super.getVolumeData();
    }

    //-------------END------------------------IMPLEMENT MaskBuilderI

    //----------------------------------------HELPER METHODS
}
