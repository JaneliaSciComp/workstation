package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;

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

    private List<MaskingDataBean> maskingDataBeans = new ArrayList<MaskingDataBean>();
    private MaskingDataBean currentBean;

    public VolumeMaskBuilder() {
    }

    public int[] getVolumeMask() {
        int[] rtnValue = null;

        for ( MaskingDataBean bean: maskingDataBeans ) {

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
        IntBuffer maskData = IntBuffer.wrap( rgbaValues );
        currentBean.setMaskData(maskData, sx, sy, sz);
    }

    /**
     * Adds another color space value to those collected here.
     */
    @Override
    public void setTextureColorSpace(TextureColorSpace colorSpace) {
        currentBean.setColorSpace( colorSpace );
    }

    public TextureColorSpace getTextureColorSpace() {
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
