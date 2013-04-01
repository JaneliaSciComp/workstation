package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.ChannelMetaData;
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
 * along with their applicable chunks of data, to produce its texture data volume, in memory. If multiple
 * renderables have parts of their volumes overlapping, the last one added takes precence over any previously-
 * added renderable.
 */
public class RenderablesMaskBuilder extends RenderablesVolumeBuilder implements MaskBuilderI {

    private static final int UNIVERSAL_MASK_BYTE_COUNT = 2;
    private static final int UNIVERSAL_MASK_CHANNEL_COUNT = 1;
    private Logger logger = LoggerFactory.getLogger( RenderablesMaskBuilder.class );
    private Collection<RenderableBean> renderableBeans;
    private byte[] volumeData;
    private int byteCount = UNIVERSAL_MASK_BYTE_COUNT;

    private boolean isInitialized = false;

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
    public synchronized int addMaskData(Integer maskNumber, long position) throws Exception {
        init();

        // Assumed little-endian.
        for ( int j = 0; j < getPixelByteCount(); j++ ) {
            int volumeLoc = j + ((int) position * getPixelByteCount());
            // Here enforced: last-added value at any given position takes precedence over any previously-added value.
            volumeData[ volumeLoc ] = (byte)( ( maskNumber >>> (8*j) ) & 0x00ff );
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

    public void setChannelMetaData( ChannelMetaData metaData ) {}
    //-------------------------END:-----------IMPLEMENT MaskChanDataAcceptorI

    //----------------------------------------IMPLEMENT MaskBuilderI

    /** Size of volume mask.  Numbers of voxels in all three directions. */
    @Override
    public Integer[] getVolumeMaskVoxels() {
        return new Integer[] { (int)sx, (int)sy, (int)sz };
    }

    @Override
    public ByteOrder getPixelByteOrder() {
        return ByteOrder.BIG_ENDIAN;
    }

    /** Single channel data here. */
    @Override
    public int getChannelCount() {
        return UNIVERSAL_MASK_CHANNEL_COUNT;
    }

    @Override
    public int getPixelByteCount() {
        return byteCount;
    }

    @Override
    public TextureDataI getCombinedTextureData() {
        logger.info( "Retrieving combined texture data." );
        // TODO: same decisioning as the RenderablesChannelsBuilder re how much to downsample.
        DownSampler downSampler = new DownSampler( sx, sy, sz );
        DownSampler.DownsampledTextureData downSampling = downSampler.getDownSampledVolume(
                volumeData, byteCount, 2.0, 2.0, 2.0
        );
        TextureDataI textureData = new TextureDataBean(
                downSampling.getVolume(), downSampling.getSx(), downSampling.getSy(), downSampling.getSz()
        );
        textureData.setVolumeMicrometers(
                new Double[] {
                        (double)downSampling.getSx(), (double)downSampling.getSy(), (double)downSampling.getSz()
                }
        );
        textureData.setInverted( false );
        textureData.setChannelCount( getChannelCount() );
        // See also VolumeLoader.resolveColorSpace()
        textureData.setColorSpace(this.getTextureColorSpace());
        textureData.setByteOrder(this.getPixelByteOrder());
        textureData.setFilename("Mask of All Renderables");
        textureData.setInterpolationMethod(GL2.GL_NEAREST);
        textureData.setRenderables(renderableBeans);
        textureData.setCoordCoverage(coordCoverage);
        textureData.setPixelByteCount(byteCount);
        textureData.setExplicitVoxelComponentType( GL2.GL_UNSIGNED_SHORT );
        textureData.setExplicitInternalFormat( GL2.GL_LUMINANCE16 );

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
        return volumeData;
    }

    //-------------END------------------------IMPLEMENT MaskBuilderI

    //----------------------------------------HELPER METHODS
    /**
     * ORDER DEPENDENCY: call this only after the super space-set, and byte count set have been called.
     */
    public void init() {
        if ( ! isInitialized ) {
            logger.info( "Initializing" );
            volumeData = new byte[ (int)(sx * sy * sz) * byteCount ];
            isInitialized = true;
        }
    }

}
