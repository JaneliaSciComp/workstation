package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.FragmentBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.MaskTextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.*;

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

    private List<TextureDataI> maskingDataBeans = new ArrayList<TextureDataI>();
    private ByteOrder consensusByteOrder;
    private int consensusByteCount;
    private int consensusChannelCount;
    private List<FragmentBean> fragments;

    private String firstFileName = null;

    private Logger logger = LoggerFactory.getLogger( VolumeDataAcceptor.class );

    public VolumeMaskBuilder() {
    }

    public byte[] getVolumeMask() {
//        if ( 0 == 0 )
//            return ((MaskTextureDataBean)maskingDataBeans.get(0)).getTextureBytes();

        Map<String,Set<FragmentBean>> fileNameToFragment = new HashMap<String,Set<FragmentBean>>();
        for ( FragmentBean bean: fragments ) {
            Set<FragmentBean> beans = fileNameToFragment.get( bean.getLabelFile() );
            if ( beans == null ) {
                beans = new HashSet<FragmentBean>();
                fileNameToFragment.put( bean.getLabelFile(), beans );
            }
            beans.add(bean);
        }

        Integer[] volumeMaskVoxels = getVolumeMaskVoxels();

        // Build a volume big enough to hold them all.  The volume mask voxels array tells
        // the maximum number of voxels from any direction from all input masks.

        // This terribly naive first cut will make a huge box big enough to hold any direction
        // of any input.  Then the individual masks will be thrown into slots with an assumption
        // (even though wrong) that their voxels are the same size as all other voxels of
        // any other mask.
        int maskBytCt = consensusByteCount;
        int bufferSizeBytes = (volumeMaskVoxels[0] * maskBytCt) * volumeMaskVoxels[1] * volumeMaskVoxels[2];
        byte[] rtnValue = new byte[ bufferSizeBytes ];
        int dimMaskX = volumeMaskVoxels[ X_INX ];
        int dimMaskY = volumeMaskVoxels[ Y_INX ];

        // Shortcut bypass
        if ( fileNameToFragment.size() == 0  &&  maskingDataBeans.size() == 1 ) {
            rtnValue = ((MaskTextureDataBean)maskingDataBeans.get(0)).getTextureBytes();
        }
        else {
            for ( TextureDataI texBean: maskingDataBeans ) {
                Set<Integer> values = new TreeSet<Integer>();

                int dimBeanX = texBean.getSx();
                int dimBeanY = texBean.getSy();
                int dimBeanZ = texBean.getSz();

                Set<FragmentBean> fragmentBeans = fileNameToFragment.get( texBean.getFilename() );

                byte[] maskData = ((MaskTextureDataBean)texBean).getTextureBytes();

                for ( int z = 0; z < dimBeanZ; z++ ) {
                    int zOffsetOutput = z * dimMaskX * dimMaskY * maskBytCt; // Slice number x next z
                    int zOffsetInput = z * dimBeanX * dimBeanY * maskBytCt;
                    for ( int y = 0; y < dimBeanY; y++ ) {
                        int yOffsetOutput = zOffsetOutput + ( ( (dimMaskY - y - 1) * dimMaskX ) * maskBytCt );
                        int yOffsetInput = zOffsetInput + ( ( (dimBeanY - y - 1) * dimBeanX ) * maskBytCt );
                        for ( int x = 0; x < dimBeanX; x++ ) {
                            int outputOffset = yOffsetOutput + x*maskBytCt;
                            int inputOffset = yOffsetInput + x*maskBytCt;

                            // The voxel value may be a multi-byte value.
                            int voxelVal = 0;
                            for ( int mi = 0; mi < maskBytCt; mi++ ) {
                                try {
                                    byte nextVoxelByte = maskData[ inputOffset + mi ];
                                    voxelVal += nextVoxelByte << (mi * 8);
                                } catch ( RuntimeException ex ) {
                                    System.out.println( ex.getMessage() + " offset=" + inputOffset +
                                    " mi=" + mi + " zOffset=" + zOffsetInput + " yOffset=" + yOffsetInput +
                                            " yOffs x maskByteCt=" + yOffsetInput*maskBytCt + " x*maskByteCt=" + x * maskBytCt + " x,y,z="+x+","+y+","+z);
                                    throw ex;
                                }
                            }

                            int newVal = voxelVal;
                            if ( voxelVal > 0 ) {
                                values.add( voxelVal );
                                if ( fragmentBeans != null ) {
                                    // This set-only technique will merely _set_ the value to the latest loaded mask's
                                    // value at this location.  There is no overlap taken into account here.
                                    // LAST PRECEDENT STRATEGY
                                    newVal = 0;
                                    for ( FragmentBean fragmentBean: fragmentBeans ) {
                                        if ( (fragmentBean.getLabelFileNum()) == voxelVal ) {
                                            newVal = fragmentBean.getTranslatedNum();
                                        }
                                    }
                                }

                            }

                            // Either a new value will be set in the mask, or the original value
                            // will wind up there.
                            if ( newVal > 0 ) {
                                for ( int mi = 0; mi < maskBytCt; mi ++ ) {
                                    byte mByte = (byte)(newVal >>> (mi * 8) & 0x000000ff);
                                    rtnValue[ outputOffset + mi ] = mByte;
                                }
                            }
                        }
                    }
                }

                System.out.println("Values seen in file " + texBean.getFilename() + " in volume mask builder.");
                for ( Integer nextVal: values ) {
                    System.out.print( nextVal + "," );
                }
                System.out.println();
            }

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

    public void setFragments(List<FragmentBean> fragments) {
        this.fragments = fragments;
    }
}
