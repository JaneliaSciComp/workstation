package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder;

import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.MaskTextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataBean;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 6/21/13
 * Time: 1:50 PM
 *
 * Transposes volumes, represented as texture datas, so that their internal 1-dimensional array changes most quickly
 * along a different-than-original dimension.
 */
public class VolumeTransposer {

    private final TextureDataI signalTextureBean;
    private final TextureDataI maskTextureBean;

    private TextureDataI transposedSignalTextureBean;
    private TextureDataI transposedMaskTextureBean;

    public VolumeTransposer( TextureDataI signalTextureBean, TextureDataI maskTextureBean ) {
        this.signalTextureBean = signalTextureBean;
        this.maskTextureBean = maskTextureBean;
    }

    public void execute() {
        if ( transposedMaskTextureBean == null ) {
            transposedMaskTextureBean = new MaskTextureDataBean();
            transposedMaskTextureBean.setSx( maskTextureBean.getSy() );
            transposedMaskTextureBean.setSy( maskTextureBean.getSz() );
            transposedMaskTextureBean.setSz( maskTextureBean.getSx() );
            transposedMaskTextureBean.setCoordCoverage( transposeArr(maskTextureBean.getCoordCoverage()) );
            transposedMaskTextureBean.setVolumeMicrometers( transposeArr(maskTextureBean.getVolumeMicrometers()) );
            transposedMaskTextureBean.setVoxelMicrometers( transposeArr(maskTextureBean.getVoxelMicrometers()) );

            // Now, can copy the stuff that's always the same.
            copyBoilerPlate( maskTextureBean, transposedMaskTextureBean );
            byte[] textureData = transposeToY(
                    maskTextureBean.getTextureData(),
                    maskTextureBean.getSx(),
                    maskTextureBean.getSy(),
                    maskTextureBean.getSz()
            );
            transposedMaskTextureBean.setTextureData( textureData );
        }
        if ( transposedSignalTextureBean == null ) {
            transposedSignalTextureBean = null;

            byte[] textureData = transposeToY(
                    signalTextureBean.getTextureData(),
                    signalTextureBean.getSx(),
                    signalTextureBean.getSy(),
                    signalTextureBean.getSz()
            );
            transposedSignalTextureBean = new TextureDataBean( textureData, signalTextureBean.getSy(), signalTextureBean.getSz(), signalTextureBean.getSx() );
            transposedSignalTextureBean.setCoordCoverage( transposeArr( signalTextureBean.getCoordCoverage()) );
            transposedSignalTextureBean.setVoxelMicrometers( transposeArr( signalTextureBean.getVoxelMicrometers()) );
            transposedSignalTextureBean.setVolumeMicrometers( transposeArr( signalTextureBean.getVolumeMicrometers()) );

            // Now, can copy the stuff that's always the same.
            copyBoilerPlate( signalTextureBean, transposedSignalTextureBean );
        }
    }

    public TextureDataI getSignalYZXOrder() {
        execute();
        return transposedSignalTextureBean;
    }

    public TextureDataI getMaskYZXOrder() {
        execute();
        return transposedMaskTextureBean;
    }

    private float[] transposeArr(float[] input) {
        float[] output = new float[ 3 ];
        output[ 0 ] = input[ 1 ];
        output[ 1 ] = input[ 2 ];
        output[ 2 ] = input[ 0 ];
        return output;
    }

    private Double[] transposeArr( Double[] input ) {
        Double[] output = new Double[ 3 ];
        output[ 0 ] = input[ 1 ];
        output[ 1 ] = input[ 2 ];
        output[ 2 ] = input[ 0 ];
        return output;
    }

    private void copyBoilerPlate( TextureDataI source, TextureDataI target ) {
        target.setByteOrder( source.getByteOrder() );
        target.setChannelCount( source.getChannelCount() );
        target.setColorSpace( source.getColorSpace() );
        target.setExplicitInternalFormat( source.getExplicitInternalFormat() );
        target.setExplicitVoxelComponentOrder( source.getExplicitVoxelComponentOrder() );
        target.setExplicitVoxelComponentType( source.getExplicitVoxelComponentType() );
        target.setFilename( source.getFilename() );
        target.setHeader( source.getHeader() );
        target.setInterpolationMethod( source.getInterpolationMethod() );
        target.setInverted( source.isInverted() );
        target.setPixelByteCount( source.getPixelByteCount() );
        target.setRenderables(source.getRenderables());

    }

    private byte[] transposeToY( byte[] raw, int oldXDim, int oldYDim, int oldZDim ) {
        int bytesPerPosition = raw.length / oldXDim / oldYDim / oldZDim;
        byte[] rtnVal = new byte[ raw.length ];
        // For the entire line...
        int outputOffset = 0;
        for ( int i = 0; i < oldXDim; i++ ) {
            // For all lines...
            for ( int k = 0; k <(oldYDim * oldZDim); k++ ) {
                int inputOffset = i + k * oldXDim;
                if ( inputOffset > raw.length ) {
                    throw new RuntimeException( "Transpose calculations failed for input offset." );
                }
                if ( outputOffset > rtnVal.length ) {
                    throw new RuntimeException( "Transpose calculations failed for output offset." );
                }

                // Now, need to do an array copy of all required bytes.
                System.arraycopy(
                        raw, inputOffset * bytesPerPosition,
                        rtnVal, outputOffset * bytesPerPosition,
                        bytesPerPosition
                );
                outputOffset ++;
            }
        }
        return rtnVal;

    }
}
