package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.MaskTextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteOrder;
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

    private static final int SHADER_FRIENDLY_BYTE_COUNT = 2;
    private static final long UNSET_MASK_UID = -1L;
    private static final int GPU_MULTIBYTE_DIVISIBILITY_VALUE = 4;

    private List<TextureDataI> maskingDataBeans = new ArrayList<TextureDataI>();
    private ByteOrder consensusByteOrder;
    //TODO revisit below at optimization time.
    // NOTE: force the consensus (output) byte count to 2, because the fragment shader can easily handle that,
    // even though, for all-8-bit masks, some time and space are wasted.
    private int consensusByteCount = SHADER_FRIENDLY_BYTE_COUNT;
    private int consensusChannelCount;
    private float[] coordCoverage;
    private Collection<RenderableBean> renderables;

    private String firstFileName = null;

    private Logger logger = LoggerFactory.getLogger( VolumeDataAcceptor.class );

    public VolumeMaskBuilder() {
    }

    public byte[] getVolumeMask() {
//        if ( 0 == 0 )
//            return ((MaskTextureDataBean)maskingDataBeans.get(0)).getTextureBytes();

        if ( renderables == null ) {
            return null;
        }

//        Map<Long,Set<RenderableBean>> labelUidToRenderable = new HashMap<Long,Set<RenderableBean>>();
//        for ( RenderableBean bean: renderables) {
//            Set<RenderableBean> beans = labelUidToRenderable.get( bean.getLabelUid() );
//            if ( beans == null ) {
//                beans = new HashSet<RenderableBean>();
//                labelUidToRenderable.put(bean.getLabelUid(), beans);
//            }
//            beans.add(bean);
//        }

        Integer[] volumeMaskVoxels = getVolumeMaskVoxels();

        // Build a volume big enough to hold them all.  The volume mask voxels array tells
        // the maximum number of voxels from any direction from all input masks.

        // This terribly naive first cut will make a huge box big enough to hold any direction
        // of any input.  Then the individual masks will be thrown into slots with an assumption
        // (even though wrong) that their voxels are the same size as all other voxels of
        // any other mask.
        int bufferSizeBytes = (volumeMaskVoxels[0] * consensusByteCount) * volumeMaskVoxels[1] * volumeMaskVoxels[2];
        byte[] rtnValue = new byte[ bufferSizeBytes ];
        int dimMaskX = volumeMaskVoxels[ X_INX ];
        int dimMaskY = volumeMaskVoxels[ Y_INX ];

        // Shortcut bypass
        if ( maskingDataBeans.size() == 1  &&  maskingDataBeans.get(0).getRenderables() == null ) {
            rtnValue = ((MaskTextureDataBean)maskingDataBeans.get(0)).getTextureBytes();
        }
        else {
            for ( TextureDataI texBean: maskingDataBeans ) {
                int maskBytCt = texBean.getPixelByteCount();  // This is allowed to be non-consensus.
                Set<Integer> values = new TreeSet<Integer>();

                int dimBeanX = texBean.getSx();
                int dimBeanY = texBean.getSy();
                int dimBeanZ = texBean.getSz();

                Collection<RenderableBean> renderableBeans = texBean.getRenderables();

                byte[] maskData = ((MaskTextureDataBean)texBean).getTextureBytes();

                for ( int z = 0; z < dimBeanZ; z++ ) {
                    int zOffsetOutput = z * dimMaskX * dimMaskY * consensusByteCount; // Slice number * next z
                    int zOffsetInput = z * dimBeanX * dimBeanY * maskBytCt;
                    for ( int y = 0; y < dimBeanY; y++ ) {
                        int yOffsetOutput = calcYOffset( consensusByteCount, dimMaskX, dimMaskY, zOffsetOutput, y, false );    // TEMP
                        int yOffsetInput = calcYOffset( maskBytCt, dimBeanX, dimBeanY, zOffsetInput, y, texBean.isInverted() );
                        for ( int x = 0; x < dimBeanX; x++ ) {
                            int outputOffset = yOffsetOutput + x*consensusByteCount;
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
                                            " yOffs x consensusByteCt=" + yOffsetInput*maskBytCt + " x*consensusByteCount=" + x * consensusByteCount + " x,y,z="+x+","+y+","+z);
                                    throw ex;
                                }
                            }

                            int newVal = 0;
                            if ( voxelVal > 0 ) {
                                values.add( voxelVal );
                                if ( renderableBeans != null ) {
                                    // This set-only technique will merely _set_ the value to the latest loaded mask's
                                    // value at this location.  There is no overlap taken into account here.
                                    // LAST PRECEDENT STRATEGY
                                    for ( RenderableBean renderableBean : renderableBeans ) {
                                        // Use only masks settings FROM the latest texture file.
                                        if ( renderableBean.getLabelFileNum() == voxelVal ) {
                                            newVal = renderableBean.getTranslatedNum();
                                        }
                                    }
                                }

                            }

                            // Either a new value will be set in the mask, or the original value
                            // will wind up there.
                            if ( newVal > 0 ) {
                                // NOTE: consensus byte count may be larger than regular byte count.
                                // If so, using the full consensus count for output size, will zero
                                // the higher-order bytes, as required.
                                for ( int mi = 0; mi < consensusByteCount; mi ++ ) {
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

    public ByteOrder getPixelByteOrder() {
        return consensusByteOrder;
    }

    public int getPixelByteCount() {
        return consensusByteCount;
    }

    @Override
    public void setTextureData(TextureDataI textureData) {
        //zeroCheckDebug(textureData);
        int maskPixelByteCount = textureData.getPixelByteCount();
        if ( consensusByteCount == 0 ) {
            consensusByteCount = maskPixelByteCount;
        }
        else if ( consensusByteCount != maskPixelByteCount ) {
            //todo watch this output; consider how serious this mismatch becomes, and how frequent.
            logger.warn(
                    "Mismatch in pixel byte count.  Previously saw {}, now seeing {}.  Sticking with higher value.",
                    consensusByteCount, maskPixelByteCount
            );
            if ( maskPixelByteCount > consensusByteCount ) {
                consensusByteCount = maskPixelByteCount;
            }
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
        textureData.setRenderables( renderables );
    }

    /**
     * This builds up a finalized texture data object for external use, combined from all texture data
     * objects set on this builder.
     *
     * @return "consensus" texture data object.
     */
    public TextureDataI getCombinedTextureData() {
        TextureDataI rtnVal = new MaskTextureDataBean( getVolumeMask(), getVolumeMaskVoxels() );
        rtnVal.setByteOrder(getPixelByteOrder());
        rtnVal.setPixelByteCount(getPixelByteCount());
        rtnVal.setHeader("Accumulated");
        rtnVal.setColorSpace(getTextureColorSpace());
        rtnVal.setVoxelMicrometers(new Double[]{1.0, 1.0, 1.0});
        rtnVal.setFilename(firstFileName);
        rtnVal.setChannelCount( consensusChannelCount );
        rtnVal.setLoaded(false);
        rtnVal.setCoordCoverage( coordCoverage );

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

    public void setRenderables(Collection<RenderableBean> renderables) {
        this.renderables = renderables;
    }

    private<T extends Comparable> void adjustMaxValues(T[] maxValues, T[] values) {
        for ( int dim = 0; dim < 3; dim ++ ) {
            if ( values[ dim ].compareTo( maxValues[ dim ] ) > 0 ) {
                maxValues[ dim ] = values[ dim ];
            }
        }
    }

    private int calcYOffset(int maskBytCt, int dimBeanX, int dimBeanY, int zOffsetInput, int y, boolean invert) {
        if ( invert ) {
            return zOffsetInput + ( ( (dimBeanY - y - 1) * dimBeanX ) * maskBytCt );
        }
        else {
            return zOffsetInput + ( ( (y               ) * dimBeanX ) * maskBytCt );
        }
    }

    private void zeroCheckDebug(TextureDataI textureData) {
        // QUICK CHECK
        int nonZeroCount = 0;
        for ( int i = 0; i < textureData.getTextureData().length; i++ ) {
            if ( textureData.getTextureData()[i] != 0 ) {
                nonZeroCount ++;
            }
        }
        if ( nonZeroCount > 0 ) {
            System.out.println("In Texture " + textureData.getFilename() + ", found " + nonZeroCount + " non-zero values.");
        }
        else {
            System.out.println("All-zero texture " + textureData.getFilename());
        }
        // END QUICK CHECK
    }

}
