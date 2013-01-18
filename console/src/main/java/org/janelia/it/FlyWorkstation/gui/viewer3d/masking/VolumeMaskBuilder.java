package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/10/13
 * Time: 10:39 AM
 *
 * Builds up the volume mask "voxels", where the final product can be treated as a texture for
 * uploading to opengl.
 */
public class VolumeMaskBuilder implements VolumeDataAcceptor {

    private static final int X_INX = 0;
    private static final int Y_INX = 1;
    private static final int Z_INX = 2;

    private boolean debug = true;

    private List<MaskingDataBean> maskingDataBeans = new ArrayList<MaskingDataBean>();
    private MaskingDataBean currentBean;

    private Logger logger = LoggerFactory.getLogger( VolumeDataAcceptor.class );

    public VolumeMaskBuilder() {
    }

    public int[] getVolumeMask() {

        // *** TEMP ***  Bypasses the combining of all these things.
        if (debug) {
            if ( maskingDataBeans.size() > 0 ) {
                return maskingDataBeans.get( 0 ).getMaskData().array();
            }
        }

        Integer[] volumeMaskVoxels = getVolumeMaskVoxels();

        // Build a volume big enough to hold them all.  The volume mask voxels array tells
        // the maximum number of voxels from any direction from all input masks.

        // This terribly naive first cut will make a huge box big enough to hold any direction
        // of any input.  Then the individual masks will be thrown into slots with an assumption
        // (even though wrong) that their voxels are the same size as all other voxels of
        // any other mask.
        int[] rtnValue = new int[ volumeMaskVoxels[0] * volumeMaskVoxels[1] * volumeMaskVoxels[2] ];
        int dimMaskX = volumeMaskVoxels[ X_INX ];
        int dimMaskY = volumeMaskVoxels[ Y_INX ];
        //int dimMaskZ = volumeMaskVoxels[ Z_INX ];

        for ( MaskingDataBean bean: maskingDataBeans ) {
            int dimBeanX = bean.getSx();
            int dimBeanY = bean.getSy();
            int dimBeanZ = bean.getSz();

            IntBuffer maskData = bean.getMaskData();

            for ( int z = 0; z < dimBeanZ; z++ ) {
                for ( int y = 0; y < dimBeanY; y++ ) {
                    for ( int x = 0; x < dimBeanX; x++ ) {
                        int outputOffset = ( z * dimMaskY * dimMaskX ) + ( y * dimMaskX ) + x;
                        int inputOffset = ( z * dimBeanX * dimBeanY ) + ( y * dimBeanX ) + x;

                        // This set-only technique will merely _set_ the value to the latest
                        // loaded mask.  There is no overlap taken into account here.
                        // LAST PRECEDENT STRATEGY
                        rtnValue[ outputOffset ] = maskData.get( inputOffset );
                        //rtnValue[ outputOffset ] = 255;  // TEMP assumption: allow all underlying data.
                    }
                }
            }
        }

        return rtnValue;
    }

    public IntBuffer getVolumeMaskBuffer() {
        if ( maskingDataBeans.size() == 0 )
            return null;
        return IntBuffer.wrap( getVolumeMask() );
    }

    /** Size of volume mask.  Numbers of voxels in all three directions. */
    public Integer[] getVolumeMaskVoxels() {
        // *** TEMP ***  Bypasses the combining of all these things.
        if (debug) {
            if ( maskingDataBeans.size() > 0 ) {
                return new Integer[] { maskingDataBeans.get( 0 ).getSx(),
                                       maskingDataBeans.get( 0 ).getSy(),
                                       maskingDataBeans.get( 0 ).getSz() };
            }
        }
        Integer[] voxels = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE };

        for ( MaskingDataBean bean: maskingDataBeans ) {
            adjustMaxValues( voxels, new Integer[] { bean.getSx(), bean.getSy(), bean.getSz() } );
        }

        return voxels;
    }

    public void beginVolume() {
        currentBean = new MaskingDataBean();
    }

    public void endVolume() {
        maskingDataBeans.add( currentBean );
        currentBean = null;
    }

    /**
     * Adds another volume of data to the collection being accumulated here.
     */
    @Override
    public void setVolumeData( int sx, int sy, int sz, int[] rgbaValues ) {
        if ( rgbaValues != null ) {
            IntBuffer maskData = IntBuffer.wrap( rgbaValues );
            currentBean.setMaskData(maskData, sx, sy, sz);
        }
        else {
            logger.warn( "Null values provided to set volume data." );
        }
    }

    /**
     * Adds another color space value to those collected here.
     */
    @Override
    public void setTextureColorSpace(TextureColorSpace colorSpace) {
        currentBean.setColorSpace( colorSpace );
    }

    public TextureColorSpace getTextureColorSpace() {
        // *** TEMP ***  Bypasses the combining of all these things.
        if (debug) {
            if ( maskingDataBeans.size() > 0 ) {
                return maskingDataBeans.get(0).getColorSpace();
            }
        }

        TextureColorSpace space = null;
        for ( MaskingDataBean bean: maskingDataBeans ) {
            if ( space == null ) {
                space = bean.getColorSpace();
            }
            else {
                if (! space.equals( bean.getColorSpace() ) ) {
                    throw new IllegalArgumentException(
                            "Masking volume color space " + bean.getColorSpace() +
                            " differs from established space of " + space
                    );
                }
            }
        }
        return space;
    }

    /**
     * Adds another volume micrometers (whole volume) 3-D dimension.
     */
    @Override
    public void setVolumeMicrometers( double sx, double sy, double sz ) {
        currentBean.setVolumeMicrometers( new Double[] {sx, sy, sz});
    }

    public Double[] getVolumeMicrometers() {
        Double[] maxValues = { Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE };
        for ( MaskingDataBean bean: maskingDataBeans ) {
            adjustMaxValues(maxValues, bean.getVolumeMicrometers());
        }
        return maxValues;
    }

    /**
     * Adds another voxel (size of one element) 3-D dimension.
     */
    @Override
    public void setVoxelMicrometers( double sx, double sy, double sz ) {
        currentBean.setVoxelMicrometers( new Double[] {sx, sy, sz} );
    }

    public Double[] getVoxelMicrometers() {
        Double[] maxValues = { Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE };
        for ( MaskingDataBean bean: maskingDataBeans ) {
            adjustMaxValues(maxValues, bean.getVoxelMicrometers());
        }
        return maxValues;
    }

    private<T extends Comparable> void adjustMaxValues(T[] maxValues, T[] values) {
        for ( int dim = 0; dim < 3; dim ++ ) {
            if ( values[ dim ].compareTo( maxValues[ dim ] ) > 0 ) {
                maxValues[ dim ] = values[ dim ];
            }
        }
    }

}
