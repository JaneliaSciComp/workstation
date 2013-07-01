package org.janelia.it.FlyWorkstation.gui.viewer3d.volume_builder;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardSettings;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.ChannelMetaData;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.MaskBuilderI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.TextureBuilderI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;

import javax.media.opengl.GL2;
import java.nio.ByteOrder;
import java.util.Collection;

import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
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
public class RenderablesMaskBuilder extends RenderablesVolumeBuilder implements MaskBuilderI, TextureBuilderI {

    private static final int UNIVERSAL_MASK_BYTE_COUNT = 2;
    private static final int UNIVERSAL_MASK_CHANNEL_COUNT = 1;
    private Logger logger = LoggerFactory.getLogger( RenderablesMaskBuilder.class );
    private Collection<RenderableBean> renderableBeans;
    private byte[] volumeData;
    private int byteCount = UNIVERSAL_MASK_BYTE_COUNT;
    private AlignmentBoardSettings settings;
    private boolean binary;   // Only two possible values for any given voxel.  1-byte per voxel.

    private boolean isInitialized = false;
    public RenderablesMaskBuilder( AlignmentBoardSettings settings, Collection<RenderableBean> renderableBeans ) {
        this.settings = settings;
        this.renderableBeans = renderableBeans;
    }

    public RenderablesMaskBuilder( AlignmentBoardSettings settings, Collection<RenderableBean> renderableBeans, boolean binary ) {
        this( settings, renderableBeans );
        this.binary = binary;
    }

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
    public synchronized int addMaskData(Integer maskNumber, long position, long x, long y, long z ) throws Exception {
        init();

        // Assumed little-endian.
        for ( int j = 0; j < getPixelByteCount(); j++ ) {
            int volumeLoc = j + ((int) position * getPixelByteCount());
            if ( binary ) {
                volumeData[ volumeLoc ] = (byte)255;
            }
            else {
                // Here enforced: last-added value at any given position takes precedence over any previously-added value.
                volumeData[ volumeLoc ] = (byte)( ( maskNumber >>> (8*j) ) & 0x00ff );
            }
        }

        return 1;
    }

    @Override
    public int addChannelData(byte[] channelData, long position, long x, long y, long z, ChannelMetaData channelMetaData) throws Exception {
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

    //----------------------------------------IMPLEMENT TextureBuilderI
    /** Alternate-use method. */
    public TextureDataI buildTextureData() {
        return this.getCombinedTextureData();
    }

    //----------------------------------------IMPLEMENT MaskBuilderI

    /** Size of volume mask.  Numbers of voxels in all three directions. */
    @Override
    public Integer[] getVolumeMaskVoxels() {
        return new Integer[] { (int)paddedSx, (int)paddedSy, (int)paddedSz };
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
        logger.debug( "Retrieving combined texture data." );
        TextureDataI textureData;
        double downSampleRate = settings.getAcceptedDownsampleRate();
        if ( downSampleRate != 1.0 ) {
            DownSampler downSampler = new DownSampler( paddedSx, paddedSy, paddedSz );
            DownSampler.DownsampledTextureData downSampling = downSampler.getDownSampledVolume(
                    volumeData, byteCount, downSampleRate, downSampleRate, downSampleRate
            );
            textureData = new TextureDataBean(
                    downSampling.getVolume(), downSampling.getSx(), downSampling.getSy(), downSampling.getSz()
            );
            textureData.setVolumeMicrometers(
                    new Double[]{(double) downSampling.getSx(), (double)downSampling.getSy(), (double)downSampling.getSz() }
            );
        }
        else {
            textureData = new TextureDataBean(
                    volumeData, (int)paddedSx, (int)paddedSy, (int)paddedSz
            );
            textureData.setVolumeMicrometers( new Double[] { (double)paddedSx, (double)paddedSy, (double)paddedSz } );
        }

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
            logger.debug( "Initializing" );
            volumeData = new byte[ (int)(paddedSx * paddedSy * paddedSz) * byteCount ];
            isInitialized = true;
        }
    }

}
