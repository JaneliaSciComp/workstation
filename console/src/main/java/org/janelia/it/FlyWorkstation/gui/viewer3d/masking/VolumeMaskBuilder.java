package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.MaskTextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteOrder;
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

    private List<TextureDataI> maskingDataBeans = new ArrayList<TextureDataI>();
    private ByteOrder consensusByteOrder;
    private int consensusByteCount;
    private int consensusChannelCount;

    private String firstFileName = null;

    private Logger logger = LoggerFactory.getLogger( VolumeDataAcceptor.class );

    public VolumeMaskBuilder() {
    }

    public byte[] getVolumeMask() {
        if ( 0 == 0 )
            return ((MaskTextureDataBean)maskingDataBeans.get(0)).getTextureBytes();

        // This constructs a "color rolodex".
        Integer[][] colors = {
                { 255, 255, 0 },
                { 255, 128, 128 },
                { 0, 255, 255 },
                { 255, 255, 0 },
                { 0, 255, 0 },
                { 0, 0, 255 },
                { 255, 0, 0 },
        };
        //Color[] colors = { Color.yellow, Color.pink, Color.cyan, Color.orange, Color.green, Color.blue, Color.red };

        Integer[] volumeMaskVoxels = getVolumeMaskVoxels();

        // Build a volume big enough to hold them all.  The volume mask voxels array tells
        // the maximum number of voxels from any direction from all input masks.

        // This terribly naive first cut will make a huge box big enough to hold any direction
        // of any input.  Then the individual masks will be thrown into slots with an assumption
        // (even though wrong) that their voxels are the same size as all other voxels of
        // any other mask.
        int bufferSizeBytes = volumeMaskVoxels[0] * volumeMaskVoxels[1] * volumeMaskVoxels[2] * consensusByteCount;
        byte[] rtnValue = new byte[ bufferSizeBytes ];
        int dimMaskX = volumeMaskVoxels[ X_INX ];
        int dimMaskY = volumeMaskVoxels[ Y_INX ];
        //int dimMaskZ = volumeMaskVoxels[ Z_INX ];

        int colorOffset = 0;
        for ( TextureDataI bean: maskingDataBeans ) {
            int dimBeanX = bean.getSx();
            int dimBeanY = bean.getSy();
            int dimBeanZ = bean.getSz();

            byte[] maskData = ((MaskTextureDataBean)bean).getTextureBytes();

            for ( int z = 0; z < dimBeanZ; z++ ) {
                for ( int y = 0; y < dimBeanY; y++ ) {
                    for ( int x = 0; x < dimBeanX; x++ ) {
                        int outputOffset = ( z * dimMaskY * dimMaskX ) + ( y * dimMaskX ) + x;
                        int inputOffset = ( z * dimBeanX * dimBeanY ) + ( y * dimBeanX ) + x;

                        // Get the RGB breakdown of the input.

                        // This set-only technique will merely _set_ the value to the latest loaded mask's
                        // value at this location.  There is no overlap taken into account here.
                        // LAST PRECEDENT STRATEGY
                        byte voxelVal = maskData[ inputOffset ];
//                        int red   = (voxelVal & 0x00ff0000) >>> 16;
//                        int green = (voxelVal & 0x0000ff00) >>> 8;
//                        int blue  = (voxelVal & 0x000000ff);
//
//                        if (! (red == blue  &&  blue == green ) ) {
////                        if (! (red == 0  &&  blue == 0  &&  green == 0 ) ) {
//                            int colorInx = colorOffset % colors.length;
//                            red = colors[ colorInx ][0];
//                            green = colors[ colorInx ][1];
//                            blue = colors[ colorInx ][2];
//
//                            rtnValue[ outputOffset ] =
//                                    blue +
//                                    (green << 8) +
//                                    (red << 16)
//                            ;
                        rtnValue[ outputOffset ] = voxelVal;

                            //System.out.println( "Input = " + voxelVal + " output =" + rtnValue[ outputOffset ]);
//                        }

                    }
                }
            }

            colorOffset ++;
        }

        return rtnValue;
    }

    /** Size of volume mask.  Numbers of voxels in all three directions. */
    public Integer[] getVolumeMaskVoxels() {
        Integer[] voxels = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE };

        for ( TextureDataI bean: maskingDataBeans ) {
            adjustMaxValues( voxels, new Integer[] { bean.getSx(), bean.getSy(), bean.getSz() } );
        }

        return voxels;
    }

    public ByteOrder getPixelByteOrder() {
        return consensusByteOrder;
    }

    public int getPixelByteCount() {
        return consensusByteCount;
    }

    @Override
    public void setTextureData(TextureDataI textureData) {
        if ( consensusByteCount == 0 ) {
            consensusByteCount = textureData.getPixelByteCount();
        }
        else if ( consensusByteCount != textureData.getPixelByteCount() ) {
            //todo watch this output; consider how serious this mismatch becomes, and how frequent.
            logger.warn(
                    "Mismatch in pixel byte count.  Previously saw {}, now seeing {}.  Sticking with former value.",
                    consensusByteCount, textureData.getPixelByteCount()
            );
        }

        if ( firstFileName == null ) {
            firstFileName = textureData.getFilename();
        }

        if ( consensusByteOrder == null ) {
            consensusByteOrder = textureData.getByteOrder();
        }
        else
        if (! textureData.getByteOrder().equals( consensusByteOrder ) ) {
            //todo watch this output; consider how serious this mismatch becomes, and how frequent.
            logger.warn(
                    "Mismatch in byte order.  Previously saw {}, now seeing {}.  Sticking with former value.",
                    consensusByteOrder, textureData.getByteOrder()
            );
        }

        if ( consensusChannelCount == 0 ) {
            consensusChannelCount = textureData.getChannelCount();
            if ( consensusChannelCount != 1 ) {
                logger.warn(
                        "Mask files expect one-channel data."
                );
            }
        }
        else if ( consensusChannelCount != textureData.getChannelCount() ) {
            logger.warn(
                    "Mismatch in channel count.  Expecting value of {}.  Instead seeing {}.  Using former value.",
                    consensusChannelCount, textureData.getChannelCount()
            );
        }
        maskingDataBeans.add( textureData );
    }

    /**
     * This builds up a finalized texture data object for external use, combined from all texture data
     * objects set on this builder.
     *
     * @return "consensus" texture data object.
     */
    public TextureDataI getCombinedTextureData() {
        TextureDataI rtnVal = new MaskTextureDataBean( getVolumeMask(), getVolumeMaskVoxels() );
        rtnVal.setByteOrder( getPixelByteOrder() );
        rtnVal.setPixelByteCount(getPixelByteCount());
        rtnVal.setHeader("Accumulated");
        rtnVal.setColorSpace(getTextureColorSpace());
        rtnVal.setVoxelMicrometers(new Double[]{1.0, 1.0, 1.0});
        rtnVal.setFilename(firstFileName);
        rtnVal.setChannelCount( consensusChannelCount );
        rtnVal.setLoaded(false);

        return rtnVal;
    }

    public TextureColorSpace getTextureColorSpace() {
        TextureColorSpace space = null;
        for ( TextureDataI bean: maskingDataBeans ) {
            if ( space == null ) {
                space = bean.getColorSpace();
            }
            else {
                if (! space.equals( bean.getColorSpace() ) ) {
                    logger.warn( "Masking volume color space " + bean.getColorSpace() +
                            " differs from established space of " + space + ".  Sticking with latter value.",
                            bean.getColorSpace(), space );
                }
            }
        }
        return space;
    }

    private<T extends Comparable> void adjustMaxValues(T[] maxValues, T[] values) {
        for ( int dim = 0; dim < 3; dim ++ ) {
            if ( values[ dim ].compareTo( maxValues[ dim ] ) > 0 ) {
                maxValues[ dim ] = values[ dim ];
            }
        }
    }

}
