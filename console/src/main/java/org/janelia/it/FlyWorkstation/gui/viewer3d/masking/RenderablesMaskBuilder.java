package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL2;
import java.nio.ByteOrder;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/13/13
 * Time: 4:21 PM
 *
 * This implementation of a mask builder takes renderables as its driving data.  It will accept the renderables,
 * along with their applicable chunks of data, to produce its texture data volume, in memory.
 */
public class RenderablesMaskBuilder implements MaskBuilderI {

    private byte[] maskData;
    private long sx;
    private long sy;
    private long sz;

    private long fastestSrcVaryingMax;
    private long fastestSrcVaryingCoord;
    private long secondFastestSrcVaryingMax;
    private long secondFastestSrcVaryingCoord;
    private long slowestSrcVaryingMax;
    private long slowestSrcVaryingCoord;

    private Collection<RenderableBean> renderableBeans;
    private int byteCount = 0;
    private float[] coordCoverage;
    private int channelCount = 1;
    private int dimensionOrder = -1;

    // Tells how many rays have been passed over up to the current add call.
    private long latestRayNumber = 0;   // This is evolving state: it depends on previously-updated data.

    private Logger logger = LoggerFactory.getLogger( RenderablesMaskBuilder.class );

    public RenderablesMaskBuilder( long x, long y, long z ) {
        init();
        sx = x;
        sy = y;
        sz = z;
    }

    public void setByteCount( int byteCount ) {
        this.byteCount = byteCount;
    }

    public void setChannelCount( int channelCount ) {
        this.channelCount = channelCount;
    }

    public void setDimensionOrder( int dimensionOrder ) {
        this.dimensionOrder = dimensionOrder;

        // Expected orderings are:  0=yz(x), 1=xz(y), 2=xy(z)
        if ( dimensionOrder == 0 ) {
            fastestSrcVaryingMax = sx;
            secondFastestSrcVaryingMax = sy;
            slowestSrcVaryingMax = sz;

            fastestSrcVaryingCoord = 0;
            secondFastestSrcVaryingCoord = 1;
            slowestSrcVaryingCoord = 2;

        }
        else if ( dimensionOrder == 1 ) {
            fastestSrcVaryingMax = sy;
            secondFastestSrcVaryingMax = sx;
            slowestSrcVaryingMax = sz;

            fastestSrcVaryingCoord = 1;
            secondFastestSrcVaryingCoord = 0;
            slowestSrcVaryingCoord = 2;
        }
        else if ( dimensionOrder == 2 ) {
            fastestSrcVaryingMax = sz;
            secondFastestSrcVaryingMax = sx;
            slowestSrcVaryingMax = sy;

            fastestSrcVaryingCoord = 2;
            secondFastestSrcVaryingCoord = 0;
            slowestSrcVaryingCoord = 1;
        }
        else {
            throw new IllegalArgumentException( "Dimension order of " + dimensionOrder + " unexpected." );
        }

    }

    /**
     * This is called with relative ray "coords".  Here, a ray is a multiple of the length along the fastest-varying
     * axis.  All dimensions of a rectangular solid are made up of as rays whose logical end points precede
     * the logical start points of the ones which follow, but stacked into sheets which are in turn stacked
     * into the rect-solid.  Expected orderings are:  0=yz(x), 1=xz(y), 2=xy(z).
     *
     * @param renderable describes all points belonging to all pairs.
     * @param skippedRayCount tells how many of these rays to bypass before interpreting first pair.
     * @param pairsAlongRay all these pairs define interval parts of the current ray.
     * @return total bytes read during this pairs-run.
     * @throws Exception thrown by caller or if bad inputs are received.
     */
    public int addData( RenderableBean renderable, long skippedRayCount, long[][] pairsAlongRay ) throws Exception {
        latestRayNumber += skippedRayCount;
        long nextRayOffset = latestRayNumber * fastestSrcVaryingMax;

        long[] srcRayStartCoords = convertTo3D( nextRayOffset, fastestSrcVaryingMax, secondFastestSrcVaryingMax );
        long[] standardRayStartCoords = convertToStandard3D( srcRayStartCoords );

        int totalBytesRead = 0;

        // Now, given we have dimension orderings, can leave two out of three coords in stasis, while only
        // the fastest-varying one changes.
        for ( long[] pairAlongRay: pairsAlongRay ) {
            for ( long rayOffset = pairAlongRay[ 0 ]; rayOffset < pairAlongRay[ 1 ]; rayOffset++ ) {
                standardRayStartCoords[ (int)fastestSrcVaryingCoord ] = rayOffset;

                long zOffset = standardRayStartCoords[ 2 ] * sz * sy;
                long yOffset = standardRayStartCoords[ 1 ] * sx + zOffset;
                long final1DCoord = yOffset + standardRayStartCoords[ 0 ];

                // Assumed little-endian and two bytes.
                short label = (short)renderable.getTranslatedNum();
                for ( int j = 0; j < byteCount; j++ ) {
                    maskData[ (int)final1DCoord * byteCount + j ] = (byte)( ( label >>> (8*j) ) & 0x00ff );
                }

                totalBytesRead += pairAlongRay[ 1 ] - pairAlongRay[ 0 ];
            }


        }

        // Necessary to bump latest-ray, in order to move on to the "expected next" value.
        //   Here, it is assumed that if the next "addData" is called and the ray _after_
        //   this one contains non-zero voxels, a skipped ray count of 0 will be passed.
        latestRayNumber ++;

        return totalBytesRead;
    }

    @Override
    public byte[] getVolumeMask() {
        return maskData;
    }

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
        return byteCount;
    }

    @Override
    public TextureDataI getCombinedTextureData() {
        TextureDataI textureData = new TextureDataBean( maskData, (int)sx, (int)sy, (int)sz );
        textureData.setInverted( false );
        textureData.setChannelCount( this.channelCount );
        // See also VolumeLoader.resolveColorSpace()
        textureData.setColorSpace( this.getTextureColorSpace() );
        textureData.setByteOrder( this.getPixelByteOrder() );
        textureData.setFilename( "Combined Mask and Channel" );
        textureData.setInterpolationMethod(GL2.GL_NEAREST );
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

    /** Call this prior to any update-data operations. */
    private void init() {
        long arrayLength = sx * sy * sz * byteCount;
        if ( arrayLength > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException(
                    "Total length of input: " + arrayLength  +
                    " exceeds maximum array size capacity.  If this is truly required, code redesign will be necessary."
            );
        }

        if ( sx > Integer.MAX_VALUE || sy > Integer.MAX_VALUE || sz > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException(
                    "One or more of the axial lengths (" + sx + "," + sy + "," + sz +
                    ") exceeds max value for an integer.  If this is truly required, code redesign will be necessary."
            );
        }

        maskData = new byte[ (int) arrayLength ];
    }

    private long[] convertTo3D( long coord1DSource, long fastestSrcVaryingMax, long nextFastestSrcVaryingMax ) {
        long sizeOfSlice = fastestSrcVaryingMax * nextFastestSrcVaryingMax;
        long sliceNumber = coord1DSource % sizeOfSlice;
        long sizeOfLine = nextFastestSrcVaryingMax;
        long sliceRemainder = coord1DSource - (sizeOfSlice * sliceNumber);
        long lineNumber = sliceRemainder % sizeOfLine;
        long pointNumber = sliceRemainder - (lineNumber * sizeOfLine);

        // After these calculations, the three-D coord of the original point in its coord system is:
        //  pointNumber, lineNumber, sliceNumber
        return new long[] {
                pointNumber, lineNumber, sliceNumber
        };
    }

    /**
     * The coordinates must be moved around to represent the final axis order.
     * 0=yz(x), 1=xz(y), 2=xy(z)
     *
     * @param srcCoords ray-finding axis order from input data.
     * @return cannonical axis-ordered coordinate triple for output.
     */
    private long[] convertToStandard3D( long[] srcCoords ) {
        long[] returnVal = null;
        if ( dimensionOrder == 0 ) {
            returnVal = srcCoords;
        }
        else if ( dimensionOrder == 1 ) {
            returnVal = new long[ 3 ];
            returnVal[ 0 ] = srcCoords[ 1 ];
            returnVal[ 1 ] = srcCoords[ 2 ];
            returnVal[ 2 ] = srcCoords[ 0 ];
        }
        else if ( dimensionOrder == 2 ) {
            returnVal = new long[ 3 ];
            returnVal[ 0 ] = srcCoords[ 2 ];
            returnVal[ 1 ] = srcCoords[ 0 ];
            returnVal[ 2 ] = srcCoords[ 1 ];
        }
        return returnVal;
    }
}
