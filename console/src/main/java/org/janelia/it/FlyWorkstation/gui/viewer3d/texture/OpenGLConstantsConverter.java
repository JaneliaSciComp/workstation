package org.janelia.it.FlyWorkstation.gui.viewer3d.texture;

import java.util.HashMap;
import java.util.Map;

import org.janelia.it.jacs.shared.loader.texture.TextureDataI;

import javax.media.opengl.GL2;

/**
 * Convert between OpenGL constants and internally-used enums.  This helps insulate lower layers from
 * view code.
 *
 * Created by fosterl on 5/2/14.
 */
public class OpenGLConstantsConverter {

    private static OpenGLConstantsConverter instance;

    private OpenGLConstantsConverter() {}

    public static OpenGLConstantsConverter getInstance() {
        if ( instance == null ) {
            synchronized( OpenGLConstantsConverter.class ) {
                // Double-if-check: if we got this lock after a wait, the thing is no longer null.
                if ( instance == null )
                    instance = new OpenGLConstantsConverter();
            }
        }
        return instance;
    }

    /*
        //  Explicit enumerations to be mapped to values in the next lower/higher layer.
    public enum VoxelComponentOrder {
        RED, RG, RGB, BGR, RGBA, BGRA,
        UNSET_VALUE
    }

    // See https://www.opengl.org/wiki/GLAPI/glTexImage2D
    public enum InternalFormat {
        RGBA,R8,R8_SNORM,R16,R16_SNORM,RG8,RG8_SNORM,RG16,RG16_SNORM,R3_G3_B2,RGB4,RGB5,
        RGB8,RGB8_SNORM,RGB10,RGB12,RGB16_SNORM,RGBA2,RGBA4,RGB5_A1,RGBA8,RGBA8_SNORM,
        RGB10_A2,RGB10_A2UI,RGBA12,RGBA16,SRGB8,SRGB8_ALPHA8,R16F,RG16F,RGB16F,RGBA16F,
        R32F,RG32F,RGB32F,RGBA32F,R11F_G11F_B10F,RGB9_E5,R8I,R8UI,R16I,R16UI,R32I,
        R32UI,RG8I,RG8UI,RG16I,RG16UI,RG32I,RG32UI,RGB8I,RGB8UI,RGB16I,RGB16UI,
        RGB32I,RGB32UI,RGBA8I,RGBA8UI,RGBA16I,RGBA16UI,RGBA32I,RGBA32UI,
        COMPRESSED_RED,COMPRESSED_RG,COMPRESSED_RGB,COMPRESSED_RGBA,COMPRESSED_SRGB,COMPRESSED_SRGB_ALPHA,
        COMPRESSED_RED_RGTC1,COMPRESSED_SIGNED_RED_RGTC1,COMPRESSED_RG_RGTC2,COMPRESSED_SIGNED_RG_RGTC2,
        COMPRESSED_RGBA_BPTC_UNORM,COMPRESSED_SRGB_ALPHA_BPTC_UNORM,COMPRESSED_RGB_BPTC_SIGNED_FLOAT,
        COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT,COMPRESSED_RGB_S3TC_DXT1_EXT,COMPRESSED_SRGB_S3TC_DXT1_EXT,
        COMPRESSED_RGBA_S3TC_DXT1_EXT,COMPRESSED_SRGB_ALPHA_S3TC_DXT1_EXT,COMPRESSED_RGBA_S3TC_DXT3_EXT,
        COMPRESSED_SRGB_ALPHA_S3TC_DXT3_EXT,COMPRESSED_RGBA_S3TC_DXT5_EXT,COMPRESSED_SRGB_ALPHA_S3TC_DXT5_EXT,
        UNSET_VALUE
    }

    public enum VoxelComponentType {
        UNSIGNED_BYTE, BYTE, UNSIGNED_SHORT, SHORT, UNSIGNED_INT, INT, FLOAT, UNSIGNED_BYTE_3_3_2,
        UNSIGNED_BYTE_2_3_3_REV, UNSIGNED_SHORT_5_6_5, UNSIGNED_SHORT_5_6_5_REV, UNSIGNED_SHORT_4_4_4_4,
        UNSIGNED_SHORT_4_4_4_4_REV, UNSIGNED_SHORT_5_5_5_1, UNSIGNED_SHORT_1_5_5_5_REV, UNSIGNED_INT_8_8_8_8,
        UNSIGNED_INT_8_8_8_8_REV, UNSIGNED_INT_10_10_10_2, UNSIGNED_INT_2_10_10_10_REV,UNSET_VALUE
    }


     */

    private Map<TextureDataI.InternalFormat,Integer> internalFormatCode;
    private Map<TextureDataI.VoxelComponentType,Integer> voxelComponentTypeCode;
    private Map<TextureDataI.VoxelComponentOrder,Integer> voxelComponentOrderCode;

    private Map<Integer, TextureDataI.InternalFormat> codeToInternalFormat;
    private Map<Integer, TextureDataI.VoxelComponentType> codeToVoxelType;
    private Map<Integer, TextureDataI.VoxelComponentOrder> codeToVoxelOrder;

    public Integer convertFromInternalFormat( TextureDataI.InternalFormat format ) {
        initInternalFormatCode();
        return internalFormatCode.get( format );
    }

    public Integer convertFromVoxelComponentType( TextureDataI.VoxelComponentType type ) {
        initVoxelComponentTypeCode();
        return voxelComponentTypeCode.get( type );
    }

    public Integer convertFromVoxelComponentOrder( TextureDataI.VoxelComponentOrder order ) {
        initVoxelComponentOrderCode();
        return voxelComponentOrderCode.get( order );
    }

    /** Back-converters seldom if ever used. */
    public synchronized TextureDataI.InternalFormat backConvertToInternalFormat( Integer glCode ) {
        initBackConverters();
        return codeToInternalFormat.get( glCode );
    }

    public synchronized TextureDataI.VoxelComponentOrder backConvertToVoxelOrder( Integer glCode ) {
        initBackConverters();
        return codeToVoxelOrder.get( glCode );
    }

    public TextureDataI.VoxelComponentType backConvertToVoxelType( Integer glCode ) {
        initBackConverters();
        return codeToVoxelType.get( glCode );
    }

    /** Setup the map lazily. */
    private void initInternalFormatCode() {
        if ( internalFormatCode == null ) {
            synchronized( this ) {
                // Double-if-check: if we got this lock after a wait, the thing is no longer null.
                if ( internalFormatCode != null )
                    return;

                internalFormatCode = new HashMap<TextureDataI.InternalFormat,Integer>();
                internalFormatCode.put( TextureDataI.InternalFormat.RGBA, GL2.GL_RGBA);
                internalFormatCode.put( TextureDataI.InternalFormat.R8, GL2.GL_R8);
                internalFormatCode.put( TextureDataI.InternalFormat.R8_SNORM, GL2.GL_R8_SNORM);
                internalFormatCode.put( TextureDataI.InternalFormat.R16, GL2.GL_R16);
                internalFormatCode.put( TextureDataI.InternalFormat.R16_SNORM, GL2.GL_R16_SNORM);
                internalFormatCode.put( TextureDataI.InternalFormat.RG8, GL2.GL_RG8);
                internalFormatCode.put( TextureDataI.InternalFormat.RG8_SNORM, GL2.GL_RG8_SNORM);
                internalFormatCode.put( TextureDataI.InternalFormat.RG16, GL2.GL_RG16);
                internalFormatCode.put( TextureDataI.InternalFormat.RG16_SNORM, GL2.GL_RG16_SNORM);
                internalFormatCode.put( TextureDataI.InternalFormat.R3_G3_B2, GL2.GL_R3_G3_B2);
                internalFormatCode.put( TextureDataI.InternalFormat.RGB4, GL2.GL_RGB4);
                internalFormatCode.put( TextureDataI.InternalFormat.RGB5, GL2.GL_RGB5);
                internalFormatCode.put( TextureDataI.InternalFormat.RGB8, GL2.GL_RGB8);
                internalFormatCode.put( TextureDataI.InternalFormat.RGB8_SNORM, GL2.GL_RGB8_SNORM);
                internalFormatCode.put( TextureDataI.InternalFormat.RGB10, GL2.GL_RGB10);
                internalFormatCode.put( TextureDataI.InternalFormat.RGB12, GL2.GL_RGB12);
                internalFormatCode.put( TextureDataI.InternalFormat.RGB16_SNORM, GL2.GL_RGB16_SNORM);
                internalFormatCode.put( TextureDataI.InternalFormat.RGBA2, GL2.GL_RGBA2);
                internalFormatCode.put( TextureDataI.InternalFormat.RGBA4, GL2.GL_RGBA4);
                internalFormatCode.put( TextureDataI.InternalFormat.RGB5_A1, GL2.GL_RGB5_A1);
                internalFormatCode.put( TextureDataI.InternalFormat.RGBA8, GL2.GL_RGBA8);
                internalFormatCode.put( TextureDataI.InternalFormat.RGBA8_SNORM, GL2.GL_RGBA8_SNORM);
                internalFormatCode.put( TextureDataI.InternalFormat.RGB10_A2, GL2.GL_RGB10_A2);
                internalFormatCode.put( TextureDataI.InternalFormat.RGBA12, GL2.GL_RGBA12);
                internalFormatCode.put( TextureDataI.InternalFormat.RGBA16, GL2.GL_RGBA16);
                internalFormatCode.put( TextureDataI.InternalFormat.SRGB8, GL2.GL_SRGB8);
                internalFormatCode.put( TextureDataI.InternalFormat.SRGB8_ALPHA8, GL2.GL_SRGB8_ALPHA8);
                internalFormatCode.put( TextureDataI.InternalFormat.R16F, GL2.GL_R16F);
                internalFormatCode.put( TextureDataI.InternalFormat.RG16F, GL2.GL_RG16F);
                internalFormatCode.put( TextureDataI.InternalFormat.RGB16F, GL2.GL_RGB16F);
                internalFormatCode.put( TextureDataI.InternalFormat.RGBA16F, GL2.GL_RGBA16F);
                internalFormatCode.put( TextureDataI.InternalFormat.R32F, GL2.GL_R32F);
                internalFormatCode.put( TextureDataI.InternalFormat.RG32F, GL2.GL_RG32F);
                internalFormatCode.put( TextureDataI.InternalFormat.RGB32F, GL2.GL_RGB32F);
                internalFormatCode.put( TextureDataI.InternalFormat.RGBA32F, GL2.GL_RGBA32F);
                internalFormatCode.put( TextureDataI.InternalFormat.R11F_G11F_B10F, GL2.GL_R11F_G11F_B10F);
                internalFormatCode.put( TextureDataI.InternalFormat.RGB9_E5, GL2.GL_RGB9_E5);
                internalFormatCode.put( TextureDataI.InternalFormat.R8I, GL2.GL_R8I);
                internalFormatCode.put( TextureDataI.InternalFormat.R8UI, GL2.GL_R8UI);
                internalFormatCode.put( TextureDataI.InternalFormat.R16I, GL2.GL_R16I);
                internalFormatCode.put( TextureDataI.InternalFormat.R16UI, GL2.GL_R16UI);
                internalFormatCode.put( TextureDataI.InternalFormat.R32I, GL2.GL_R32I);
                internalFormatCode.put( TextureDataI.InternalFormat.R32UI, GL2.GL_R32UI);
                internalFormatCode.put( TextureDataI.InternalFormat.RG8I, GL2.GL_RG8I);
                internalFormatCode.put( TextureDataI.InternalFormat.RG8UI, GL2.GL_RG8UI);
                internalFormatCode.put( TextureDataI.InternalFormat.RG16I, GL2.GL_RG16I);
                internalFormatCode.put( TextureDataI.InternalFormat.RG16UI, GL2.GL_RG16UI);
                internalFormatCode.put( TextureDataI.InternalFormat.RG32I, GL2.GL_RG32I);
                internalFormatCode.put( TextureDataI.InternalFormat.RG32UI, GL2.GL_RG32UI);
                internalFormatCode.put( TextureDataI.InternalFormat.RGB8I, GL2.GL_RGB8I);
                internalFormatCode.put( TextureDataI.InternalFormat.RGB8UI, GL2.GL_RGB8UI);
                internalFormatCode.put( TextureDataI.InternalFormat.RGB16I, GL2.GL_RGB16I);
                internalFormatCode.put( TextureDataI.InternalFormat.RGB16UI, GL2.GL_RGB16UI);
                internalFormatCode.put( TextureDataI.InternalFormat.RGB32I, GL2.GL_RGB32I);
                internalFormatCode.put( TextureDataI.InternalFormat.RGB32UI, GL2.GL_RGB32UI);
                internalFormatCode.put( TextureDataI.InternalFormat.RGBA8I, GL2.GL_RGBA8I);
                internalFormatCode.put( TextureDataI.InternalFormat.RGBA8UI, GL2.GL_RGBA8UI);
                internalFormatCode.put( TextureDataI.InternalFormat.RGBA16I, GL2.GL_RGBA16I);
                internalFormatCode.put( TextureDataI.InternalFormat.RGBA16UI, GL2.GL_RGBA16UI);
                internalFormatCode.put( TextureDataI.InternalFormat.RGBA32I, GL2.GL_RGBA32I);
                internalFormatCode.put( TextureDataI.InternalFormat.RGBA32UI, GL2.GL_RGBA32UI);
                internalFormatCode.put( TextureDataI.InternalFormat.COMPRESSED_RED, GL2.GL_COMPRESSED_RED);
                internalFormatCode.put( TextureDataI.InternalFormat.COMPRESSED_RG, GL2.GL_COMPRESSED_RG);
                internalFormatCode.put( TextureDataI.InternalFormat.COMPRESSED_RGB, GL2.GL_COMPRESSED_RGB);
                internalFormatCode.put( TextureDataI.InternalFormat.COMPRESSED_RGBA, GL2.GL_COMPRESSED_RGBA);
                internalFormatCode.put( TextureDataI.InternalFormat.COMPRESSED_SRGB, GL2.GL_COMPRESSED_SRGB);
                internalFormatCode.put( TextureDataI.InternalFormat.COMPRESSED_SRGB_ALPHA, GL2.GL_COMPRESSED_SRGB_ALPHA);
                internalFormatCode.put( TextureDataI.InternalFormat.COMPRESSED_RED_RGTC1, GL2.GL_COMPRESSED_RED_RGTC1);
                internalFormatCode.put( TextureDataI.InternalFormat.COMPRESSED_SIGNED_RED_RGTC1, GL2.GL_COMPRESSED_SIGNED_RED_RGTC1);
                internalFormatCode.put( TextureDataI.InternalFormat.COMPRESSED_RG_RGTC2, GL2.GL_COMPRESSED_RG_RGTC2);
                internalFormatCode.put( TextureDataI.InternalFormat.COMPRESSED_SIGNED_RG_RGTC2, GL2.GL_COMPRESSED_SIGNED_RG_RGTC2);
                internalFormatCode.put( TextureDataI.InternalFormat.COMPRESSED_RGB_S3TC_DXT1_EXT, GL2.GL_COMPRESSED_RGB_S3TC_DXT1_EXT);
                internalFormatCode.put( TextureDataI.InternalFormat.COMPRESSED_RGBA_S3TC_DXT1_EXT, GL2.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT);
                internalFormatCode.put( TextureDataI.InternalFormat.COMPRESSED_RGBA_S3TC_DXT3_EXT, GL2.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT);
                internalFormatCode.put( TextureDataI.InternalFormat.COMPRESSED_RGBA_S3TC_DXT5_EXT, GL2.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT);
                internalFormatCode.put( TextureDataI.InternalFormat.UNSET_VALUE, TextureDataI.UNSET_VALUE);
            }
        }

    }

    private void initBackConverters() {
        initInternalFormatCode();
        initVoxelComponentOrderCode();
        initVoxelComponentTypeCode();
        if ( codeToInternalFormat == null ) {
            codeToInternalFormat = new HashMap<Integer,TextureDataI.InternalFormat>();
            for ( TextureDataI.InternalFormat key: internalFormatCode.keySet() ) {
                codeToInternalFormat.put( internalFormatCode.get( key ), key );
            }
        }
        if ( codeToVoxelType == null ) {
            codeToVoxelType = new HashMap<Integer,TextureDataI.VoxelComponentType>();
            for (TextureDataI.VoxelComponentType key: voxelComponentTypeCode.keySet() ) {
                codeToVoxelType.put( voxelComponentTypeCode.get( key ), key );
            }
        }
        if ( codeToVoxelOrder == null ) {
            codeToVoxelOrder = new HashMap<Integer,TextureDataI.VoxelComponentOrder>();
            for ( TextureDataI.VoxelComponentOrder key: voxelComponentOrderCode.keySet() ) {
                codeToVoxelOrder.put( voxelComponentOrderCode.get( key ), key );
            }
        }
    }

    private void initVoxelComponentTypeCode() {
        if ( voxelComponentTypeCode == null ) {
            synchronized (this) {
                // Double-if-check: if we got this lock after a wait, the thing is no longer null.
                if ( voxelComponentTypeCode != null )
                    return;

                voxelComponentTypeCode = new HashMap<TextureDataI.VoxelComponentType,Integer>();
                voxelComponentTypeCode.put( TextureDataI.VoxelComponentType.UNSIGNED_BYTE, GL2.GL_UNSIGNED_BYTE);
                voxelComponentTypeCode.put( TextureDataI.VoxelComponentType.BYTE, GL2.GL_BYTE);
                voxelComponentTypeCode.put( TextureDataI.VoxelComponentType.UNSIGNED_SHORT, GL2.GL_UNSIGNED_SHORT);
                voxelComponentTypeCode.put( TextureDataI.VoxelComponentType.SHORT, GL2.GL_SHORT);
                voxelComponentTypeCode.put( TextureDataI.VoxelComponentType.UNSIGNED_INT, GL2.GL_UNSIGNED_INT);
                voxelComponentTypeCode.put( TextureDataI.VoxelComponentType.INT, GL2.GL_INT);
                voxelComponentTypeCode.put( TextureDataI.VoxelComponentType.FLOAT, GL2.GL_FLOAT);
                voxelComponentTypeCode.put( TextureDataI.VoxelComponentType.UNSIGNED_BYTE_3_3_2, GL2.GL_UNSIGNED_BYTE_3_3_2);
                voxelComponentTypeCode.put( TextureDataI.VoxelComponentType.UNSIGNED_BYTE_2_3_3_REV, GL2.GL_UNSIGNED_BYTE_2_3_3_REV);
                voxelComponentTypeCode.put( TextureDataI.VoxelComponentType.UNSIGNED_SHORT_5_6_5, GL2.GL_UNSIGNED_SHORT_5_6_5);
                voxelComponentTypeCode.put( TextureDataI.VoxelComponentType.UNSIGNED_SHORT_5_6_5_REV, GL2.GL_UNSIGNED_SHORT_5_6_5_REV);
                voxelComponentTypeCode.put( TextureDataI.VoxelComponentType.UNSIGNED_SHORT_4_4_4_4, GL2.GL_UNSIGNED_SHORT_4_4_4_4);
                voxelComponentTypeCode.put( TextureDataI.VoxelComponentType.UNSIGNED_SHORT_4_4_4_4_REV, GL2.GL_UNSIGNED_SHORT_4_4_4_4_REV);
                voxelComponentTypeCode.put( TextureDataI.VoxelComponentType.UNSIGNED_SHORT_5_5_5_1, GL2.GL_UNSIGNED_SHORT_5_5_5_1);
                voxelComponentTypeCode.put( TextureDataI.VoxelComponentType.UNSIGNED_SHORT_1_5_5_5_REV, GL2.GL_UNSIGNED_SHORT_1_5_5_5_REV);
                voxelComponentTypeCode.put( TextureDataI.VoxelComponentType.UNSIGNED_INT_8_8_8_8, GL2.GL_UNSIGNED_INT_8_8_8_8);
                voxelComponentTypeCode.put( TextureDataI.VoxelComponentType.UNSIGNED_INT_8_8_8_8_REV, GL2.GL_UNSIGNED_INT_8_8_8_8_REV);
                voxelComponentTypeCode.put( TextureDataI.VoxelComponentType.UNSIGNED_INT_10_10_10_2, GL2.GL_UNSIGNED_INT_10_10_10_2);
                voxelComponentTypeCode.put( TextureDataI.VoxelComponentType.UNSIGNED_INT_2_10_10_10_REV, GL2.GL_UNSIGNED_INT_2_10_10_10_REV);
                voxelComponentTypeCode.put( TextureDataI.VoxelComponentType.UNSET_VALUE, TextureDataI.UNSET_VALUE);
            }
        }

    }

    private void initVoxelComponentOrderCode() {
        if ( voxelComponentOrderCode == null ) {
            synchronized( this ) {
                // Double-if-check: if we got this lock after a wait, the thing is no longer null.
                if ( voxelComponentOrderCode != null )
                    return;

                voxelComponentOrderCode  = new HashMap<TextureDataI.VoxelComponentOrder, Integer>();
                voxelComponentOrderCode.put( TextureDataI.VoxelComponentOrder.RED, GL2.GL_RED);
                voxelComponentOrderCode.put( TextureDataI.VoxelComponentOrder.RG, GL2.GL_RG);
                voxelComponentOrderCode.put( TextureDataI.VoxelComponentOrder.RGB, GL2.GL_RGB);
                voxelComponentOrderCode.put( TextureDataI.VoxelComponentOrder.BGR, GL2.GL_BGR);
                voxelComponentOrderCode.put( TextureDataI.VoxelComponentOrder.RGBA, GL2.GL_RGBA);
                voxelComponentOrderCode.put( TextureDataI.VoxelComponentOrder.BGRA, GL2.GL_BGRA);
                voxelComponentOrderCode.put( TextureDataI.VoxelComponentOrder.UNSET_VALUE, TextureDataI.UNSET_VALUE);
            }
        }
    }

}
