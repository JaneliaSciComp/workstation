package org.janelia.workstation.gui.viewer3d.loader;

import org.janelia.workstation.img_3d_loader.V3dMaskFileLoader;
import org.janelia.workstation.img_3d_loader.AbstractVolumeFileLoader;
import org.janelia.workstation.gui.viewer3d.VolumeDataAcceptor;
import org.janelia.workstation.gui.viewer3d.texture.TextureDataI;

import javax.media.opengl.GL2;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/6/13
 * Time: 2:31 PM
 *
 * Extend this with something that can supply the data array, x, y, and z sizes.
 */
public abstract class TextureDataBuilder {

    protected VolumeDataAcceptor.TextureColorSpace colorSpace =
            VolumeDataAcceptor.TextureColorSpace.COLOR_SPACE_LINEAR;
    private AbstractVolumeFileLoader volumeFileLoader;

    public void setVolumeFileLoader( AbstractVolumeFileLoader volumeFileLoader ) {
        this.volumeFileLoader = volumeFileLoader;
    }
    
    public AbstractVolumeFileLoader getVolumeFileLoader() {
        return volumeFileLoader;
    }

    public void setColorSpace( VolumeDataAcceptor.TextureColorSpace colorSpace ) {
        this.colorSpace = colorSpace;
    }

    public TextureDataI buildTextureData(boolean isLuminance ) {
        TextureDataI textureData = createTextureDataBean();
        if ( textureData == null ) {
            throw new IllegalArgumentException("Failed to create texture data bean.");
        }

        textureData.setSx(volumeFileLoader.getSx());
        textureData.setSy(volumeFileLoader.getSy());
        textureData.setSz(volumeFileLoader.getSz());

        textureData.setColorSpace(colorSpace);
        textureData.setVolumeMicrometers(new Double[]{(double) volumeFileLoader.getSx(), (double) volumeFileLoader.getSy(), (double) volumeFileLoader.getSz()});
        textureData.setVoxelMicrometers(new Double[]{1.0, 1.0, 1.0});
        if ( volumeFileLoader.getHeader() != null ) {
            textureData.setHeader( volumeFileLoader.getHeader() );
        }
        textureData.setByteOrder(volumeFileLoader.getPixelByteOrder());
        if ( textureData.getPixelByteCount() <= 0  ||  textureData.getPixelByteCount() < volumeFileLoader.getPixelBytes() ) {
            textureData.setPixelByteCount(volumeFileLoader.getPixelBytes());
        }
        else {
            volumeFileLoader.setPixelBytes(textureData.getPixelByteCount());
        }
        textureData.setFilename(volumeFileLoader.getUnCachedFileName());
        textureData.setChannelCount(volumeFileLoader.getChannelCount());

        if (! isLuminance  &&  (volumeFileLoader.getChannelCount() == 4)  &&  volumeFileLoader.getArgbTextureIntArray() != null ) {
            setAlphaToSaturateColors( colorSpace );

            textureData.setExplicitVoxelComponentOrder( GL2.GL_RGBA );
            textureData.setExplicitInternalFormat( GL2.GL_RGBA );
            textureData.setExplicitVoxelComponentType( GL2.GL_UNSIGNED_INT_8_8_8_8_REV );
        }
		else if (isLuminance  &&  (volumeFileLoader.getChannelCount() == 1)  &&  volumeFileLoader.getPixelBytes() == 2) {
            //  Theory: I have two-byte masks -> just a two-byte alpha.
            //GL_RGBA, GL_BGRA, GL_RED, GL_RG, GL_RGB, GL_BGR
            textureData.setExplicitVoxelComponentOrder( GL2.GL_RED );
            textureData.setExplicitInternalFormat( GL2.GL_INTENSITY16 );
            textureData.setExplicitVoxelComponentType( GL2.GL_UNSIGNED_SHORT );
            /*
			//  Theory: I have two-byte masks -> just a two-byte alpha.
            //GL_RGBA, GL_BGRA, GL_RED, GL_RG, GL_RGB, GL_BGR
            // Black hole with one-sided line segment.  Also error java.lang.IndexOutOfBoundsException: Required 572896800 remaining bytes in buffer, only had 286448400
            textureData.setExplicitVoxelComponentOrder( GL2.GL_RG );
            textureData.setExplicitInternalFormat( GL2.GL_INTENSITY16 );
            textureData.setExplicitVoxelComponentType( GL2.GL_UNSIGNED_SHORT );
            */

            /*
            //  Theory: I have two-byte masks -> just a two-byte alpha.
            //GL_RGBA, GL_BGRA, GL_RED, GL_RG, GL_RGB, GL_BGR
            // Black hole.
            textureData.setExplicitVoxelComponentOrder( GL2.GL_RED );
            textureData.setExplicitInternalFormat( GL2.GL_ALPHA16 );
            textureData.setExplicitVoxelComponentType( GL2.GL_UNSIGNED_SHORT );
            */

            /*
			//  Theory: I have two-byte masks -> just a two-byte luminance, and no alpha.
			textureData.setExplicitVoxelComponentOrder( GL2.GL_RED );
            textureData.setExplicitInternalFormat( GL2.GL_LUMINANCE16 );
            textureData.setExplicitVoxelComponentType( GL2.GL_UNSIGNED_SHORT );
            */

			/*  Black hole
			// Setup for SHORT luminance
			//  Theory: I have two-byte masks -> just a two-byte luminance, and no alpha.
			textureData.setExplicitVoxelComponentOrder( GL2.GL_LUMINANCE );
            textureData.setExplicitInternalFormat( GL2.GL_LUMINANCE );
            textureData.setExplicitVoxelComponentType( GL2.GL_UNSIGNED_SHORT );
			*/
			
			/*
			   Black hole
			
			// Theory: red because one color needed.  Luminance because: masks;
			// short, because: two-byte voxels.			
            textureData.setExplicitVoxelComponentOrder( GL2.GL_RED );
            textureData.setExplicitInternalFormat( GL2.GL_LUMINANCE );
            textureData.setExplicitVoxelComponentType( GL2.GL_UNSIGNED_SHORT );
			*/
			
			/*
			  This triggers exception: expecting twice as many bytes.
			// Theory: need luminance/alpha everywhere.
			// Problem: I think Luminance_Alpha implies two channels (4 bytes).
			textureData.setExplicitVoxelComponentOrder( GL2.GL_LUMINANCE_ALPHA );
            textureData.setExplicitInternalFormat( GL2.GL_LUMINANCE_ALPHA );
            textureData.setExplicitVoxelComponentType( GL2.GL_UNSIGNED_SHORT );
			*/
		}
		else {
            if ( volumeFileLoader.getUnCachedFileName().contains( V3dMaskFileLoader.COMPARTMENT_MASK_INDEX ) ) {
                textureData.setInverted( false );  // Do not invert the compartment mask.
            }
        }

        return textureData;
    }

    /**
     * Set alpha component of each voxel assuming that R,G,B
     * values represent a saturated color with premultiplied alpha.
     * Similar to Vaa3D.  In other words, alpha = max(R,G,B)
     */
    private void setAlphaToSaturateColors(VolumeDataAcceptor.TextureColorSpace space) {
        if ( space == null )
            return;

        // Use modified alpha value for sRGB textures
        int[] alphaMap = new int[256];
        double exponent = 1.0;
        if (space == VolumeDataAcceptor.TextureColorSpace.COLOR_SPACE_SRGB)
            exponent  = 2.2;
        for (int i = 0; i < 256; ++i) {
            double i0 = i / 255.0;
            double i1 = Math.pow(i0, exponent);
            alphaMap[i] = (int)(i1 * 255.0 + 0.5);
        }
        int[] argbTexIntArr = volumeFileLoader.getArgbTextureIntArray();
        int numVoxels = argbTexIntArr.length;
        for (int v = 0; v < numVoxels; ++v) {
            int argb = argbTexIntArr[v];
            int red   = (argb & 0x00ff0000) >>> 16;
            int green = (argb & 0x0000ff00) >>> 8;
            int blue  = (argb & 0x000000ff);
            int alpha = Math.max(red, Math.max(green, blue));
            alpha = alphaMap[alpha];
            argb = (argb & 0x00ffffff) | (alpha << 24);
            argbTexIntArr[v] = argb;
        }
    }

    abstract protected TextureDataI createTextureDataBean();

}
