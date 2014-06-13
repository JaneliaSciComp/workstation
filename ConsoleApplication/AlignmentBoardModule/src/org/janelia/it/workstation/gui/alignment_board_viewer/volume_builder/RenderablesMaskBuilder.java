package org.janelia.it.workstation.gui.alignment_board_viewer.volume_builder;

import org.janelia.it.workstation.gui.alignment_board.loader.ChannelMetaData;
import org.janelia.it.workstation.gui.alignment_board.loader.MaskChanDataAcceptorI;
import org.janelia.it.workstation.gui.alignment_board_viewer.AlignmentBoardSettings;
import org.janelia.it.workstation.gui.alignment_board_viewer.masking.TextureBuilderI;
import org.janelia.it.workstation.gui.viewer3d.masking.VolumeDataI;
import org.janelia.it.workstation.gui.viewer3d.VolumeDataAcceptor;
import org.janelia.it.workstation.gui.viewer3d.masking.MaskBuilderI;
import org.janelia.it.workstation.gui.viewer3d.renderable.RenderableBean;

import javax.media.opengl.GL2;
import java.nio.ByteOrder;
import java.util.Collection;

import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.workstation.gui.viewer3d.volume_builder.VolumeDataChunk;
import org.janelia.it.workstation.shared.annotations.NotThreadSafe;
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

    public static final int UNIVERSAL_MASK_BYTE_COUNT = 2;
    private static final int UNIVERSAL_MASK_CHANNEL_COUNT = 1;
    private final Logger logger = LoggerFactory.getLogger( RenderablesMaskBuilder.class );
    private final Collection<RenderableBean> renderableBeans;
    private VolumeDataI volumeData;
    //private byte[] volumeData;
    private int maskByteCount = UNIVERSAL_MASK_BYTE_COUNT;
    private final AlignmentBoardSettings settings;
    private boolean binary;   // Only two possible values for any given voxel.  1-byte per voxel.

    private boolean isInitialized = false;
    public RenderablesMaskBuilder( AlignmentBoardSettings settings, Collection<RenderableBean> renderableBeans ) {
        this.settings = settings;
        this.renderableBeans = renderableBeans;
    }

    public RenderablesMaskBuilder( AlignmentBoardSettings settings, Collection<RenderableBean> renderableBeans, boolean binary ) {
        this( settings, renderableBeans );
        this.binary = binary;
        if ( binary ) {
            maskByteCount = 1;
        }
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
    @NotThreadSafe( why="writes direct to volume data. May be called with diff masks.  No synchronized." )
    @Override
    public int addMaskData(Integer maskNumber, long position, long x, long y, long z ) throws Exception {
        init();

        // Assumed little-endian.
        for ( int j = 0; j < maskByteCount; j++ ) {
            long volumeLoc = (long)j + (position * maskByteCount);
            if ( binary ) {
                volumeData.setValueAt(volumeLoc, (byte) 255);
            }
            else {
                // Here enforced: last-added value at any given position takes precedence over any previously-added value.
                volumeData.setValueAt(volumeLoc, (byte) ((maskNumber >>> (8 * j)) & 0x00ff));
            }
        }

        return 1;
    }

    @Override
    public int addChannelData(
            Integer orignalMaskNum,
            byte[] channelData,
            long position,
            long x, long y, long z,
            ChannelMetaData channelMetaData) throws Exception {
        throw new IllegalArgumentException( "Not implemented" );
    }

    /**
     * This tells the caller: only call me with mask data.  This is a mask builder.
     * @return mask
     */
    @Override
    public MaskChanDataAcceptorI.Acceptable getAcceptableInputs() {
        return MaskChanDataAcceptorI.Acceptable.mask;
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
        return maskByteCount;
    }

    @Override
    public TextureDataI getCombinedTextureData() {
        logger.debug( "Retrieving combined texture data." );
        TextureDataI textureData;
        double downSampleRate = settings.getAcceptedDownsampleRate();
        if ( downSampleRate != 1.0  &&  downSampleRate != 0.0 ) {
            DownSampler downSampler = new DownSampler( paddedSx, paddedSy, paddedSz );
            DownSampler.DownsampledTextureData downSampling = downSampler.getDownSampledVolume(
                    volumeData, maskByteCount, downSampleRate, downSampleRate, downSampleRate
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
        textureData.setPixelByteCount(maskByteCount);
        textureData.setExplicitVoxelComponentType( GL2.GL_UNSIGNED_SHORT );
        textureData.setExplicitInternalFormat( GL2.GL_LUMINANCE16 );

        return textureData;
    }

    @Override
    public VolumeDataAcceptor.TextureColorSpace getTextureColorSpace() {
        // See also VolumeLoader.resolveColorSpace()
        return VolumeDataAcceptor.TextureColorSpace.COLOR_SPACE_LINEAR;
    }

    //-------------END------------------------IMPLEMENT MaskBuilderI
    //----------------------------------------IMPLEMENT extended interface VolumeDataI
    @Override
    public boolean isVolumeAvailable() {
        return true;
    }

    @Override
    public VolumeDataChunk[] getVolumeChunks() {
        init();
        return volumeData.getVolumeChunks();
    }

    /**
     * Return the appropriate byte from the volume data.
     *
     * @param location which offset, in bytes.  This impl can only return an int of addressing.
     * @return a byte at the location given.
     */
    @Override
    public byte getValueAt(long location) {
        init();
        return volumeData.getValueAt(location);
    }

    @Override
    public void setValueAt(long location, byte value) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public long length() {
        init();
        return volumeData.length();
    }
    //-----------END--------------------------IMPLEMENT VolumeDataI

    //----------------------------------------HELPER METHODS
    /**
     * ORDER DEPENDENCY: call this only after the super space-set, and byte count set have been called.
     */
    protected void init() {
        if ( ! isInitialized ) {
            synchronized ( this ) {
                // Protective inner shield against initialization as the volume is being used.
                if ( isInitialized ) {
                    return;
                }
                // This check and exception enforces an order dependency.  Order dependencies are undesirable,
                // but since lazy-init of these padded sizes is needed, we throw this exception so that the
                // programmer knows to delay any method that calls init.
                if ( paddedSx == 0 || paddedSy == 0 || paddedSz == 0 ) {
                    throw new RuntimeException("Space size not yet initialized.  Cannot initialize volume.");
                }
                logger.debug( "Initializing" );
                // Old Way:
                // volumeData = new VolumeDataBean((int)( paddedSx * paddedSy * paddedSz) * maskByteCount, (int)paddedSx, (int)paddedSy, (int)paddedSz );
                volumeData = new VeryLargeVolumeData( (int)paddedSx, (int)paddedSy, (int)paddedSz, maskByteCount );
                isInitialized = true;
            }
        }
    }

}
