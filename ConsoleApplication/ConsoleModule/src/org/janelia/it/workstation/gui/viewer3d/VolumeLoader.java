package org.janelia.it.workstation.gui.viewer3d;

import org.janelia.it.jacs.shared.img_3d_loader.H264FileLoader;
import org.janelia.it.jacs.shared.img_3d_loader.MpegFileLoader;
import org.janelia.it.jacs.shared.img_3d_loader.LsmFileLoader;
import org.janelia.it.workstation.gui.viewer3d.loader.TifTextureBuilder;
import org.janelia.it.workstation.gui.viewer3d.loader.TextureDataBuilder;
import org.janelia.it.jacs.shared.img_3d_loader.V3dSignalFileLoader;
import org.janelia.it.jacs.shared.img_3d_loader.V3dMaskFileLoader;
import org.apache.commons.io.FilenameUtils;
import org.janelia.it.workstation.gui.viewer3d.VolumeDataAcceptor.TextureColorSpace;
import org.janelia.it.jacs.shared.img_3d_loader.VolumeFileLoaderI;
import org.janelia.it.workstation.gui.viewer3d.loader.VolumeLoaderI;
import org.janelia.it.workstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import javax.media.opengl.GL2;
import org.janelia.it.jacs.shared.img_3d_loader.H265FileLoader;
import org.janelia.it.workstation.gui.viewer3d.loader.LociTextureBuilder;
import org.janelia.it.jacs.shared.img_3d_loader.TifVolumeFileLoader;

public class VolumeLoader implements VolumeLoaderI {

    private static final int MAX_FILE_LOAD_RETRY = 3;
    private static final int WAIT_BETWEEN_FILE_LOAD_RETRIES = 1000;

    private FileResolver resolver;
    private boolean isLuminance = false;
    private TextureDataI textureData;

    private Logger logger;

    public VolumeLoader( FileResolver resolver ) {
        logger = LoggerFactory.getLogger( VolumeLoader.class );
        this.resolver = resolver;
    }

    public boolean loadVolume(String unCachedFileName)
    {
        System.out.println("Start load volume: " + new java.util.Date());
        try {
            String localFileName = resolver.getResolvedFilename( unCachedFileName );
            if ( localFileName == null ) {
                logger.error( "Cannot resolve {} to a local file name.", unCachedFileName );
                return false;
            }

            String extension = FilenameUtils.getExtension(localFileName).toUpperCase();
            logger.debug("FILENAME: {}", localFileName);
            String baseName = FilenameUtils.getBaseName(localFileName);

            VolumeFileLoaderI fileLoader = null;
            TextureDataBuilder textureDataBuilder = null;
            switch ( getFileType( localFileName, baseName, extension ) ) {
                case TIF: {
                    TifTextureBuilder tifTextureBuilder = new TifTextureBuilder();
                    TifVolumeFileLoader tifVolumeFileLoader = new TifVolumeFileLoader();
                    tifTextureBuilder.setVolumeFileLoader(tifVolumeFileLoader);
                    fileLoader = tifVolumeFileLoader;
                    textureDataBuilder = tifTextureBuilder;
                    break;
                }
                case LSM: {
                    LsmFileLoader lsmFileLoader = new LsmFileLoader();
                    fileLoader = lsmFileLoader;
                    textureDataBuilder = new LociTextureBuilder();
                    textureDataBuilder.setVolumeFileLoader(lsmFileLoader);
                    break;
                }
                case V3DSIGNAL: {
                    V3dSignalFileLoader v3dFileLoader = new V3dSignalFileLoader();
                    fileLoader = v3dFileLoader;
                    textureDataBuilder = new LociTextureBuilder();
                    textureDataBuilder.setVolumeFileLoader(v3dFileLoader);
                    break;
                }
                case V3DMASK: {
                    V3dMaskFileLoader maskFileLoader = new V3dMaskFileLoader();
                    fileLoader = maskFileLoader;
                    textureDataBuilder = new LociTextureBuilder();
                    textureDataBuilder.setVolumeFileLoader(maskFileLoader);
                    isLuminance = true;
                    break;
                }
                case H264: {
                    // Extension can contain .mp4.  Need see this case first.
                    H264FileLoader h264FileLoader = new H264FileLoader();
                    fileLoader = h264FileLoader;
                    textureDataBuilder = new LociTextureBuilder();
                    textureDataBuilder.setVolumeFileLoader(h264FileLoader);
                    break;
                }
                case H265: {
                    // Extension can contain .mp4.  Need see this case first.
                    H265FileLoader h265FileLoader = new H265FileLoader();
                    fileLoader = h265FileLoader;
                    textureDataBuilder = new LociTextureBuilder();
                    textureDataBuilder.setVolumeFileLoader(h265FileLoader);
                    break;
                }
                case MP4: {
                    MpegFileLoader mpegFileLoader = new MpegFileLoader();
                    fileLoader = mpegFileLoader;
                    textureDataBuilder = new LociTextureBuilder();
                    textureDataBuilder.setVolumeFileLoader(mpegFileLoader);
                    break;
                }
                default:
                    break;
                    //throw new IllegalArgumentException("Unknown filename/extension combination " + baseName + "/" + extension);
            }

            if ( textureDataBuilder != null  &&  fileLoader != null ) {
                textureDataBuilder.setColorSpace( resolveColorSpace(baseName, extension) );
            }
            else {
                return true;
            }

            // Attempt to load the file.  After the max attempt, pass through the exception.
            int tryCount = 0;
            while ( true ) {
                try {
                    fileLoader.loadVolumeFile( localFileName );
                    break;

                } catch ( IOException ioe ) {
                    try {
                        logger.warn( "Exception " + ioe.getMessage() + " during file-load attempt of {}. ", localFileName );
                        Thread.sleep( WAIT_BETWEEN_FILE_LOAD_RETRIES );
                    } catch ( Exception sleepEx ) {
                        // Will basically ignore this, since it is in a retry loop.
                        logger.error( "Interrupted during volume-load retry.  Continuing..." );
                        sleepEx.printStackTrace();
                    }
                    tryCount ++;
                    if ( tryCount > MAX_FILE_LOAD_RETRY ) {
                        throw ioe;
                    }
                }
            }

            textureData = textureDataBuilder.buildTextureData( isLuminance );            
            final FileType fileType = getFileType( localFileName, baseName, extension );
            if ( FileType.TIF.equals( fileType)  &&
                 localFileName.contains("tiff_mousebrain") ) {
                textureData.setExplicitInternalFormat( GL2.GL_LUMINANCE16 );
                textureData.setExplicitVoxelComponentOrder( GL2.GL_LUMINANCE );
                textureData.setExplicitVoxelComponentType( GL2.GL_UNSIGNED_SHORT );
            }
            else if ( FileType.TIF.equals( fileType ) ) {
                textureData.setExplicitInternalFormat( GL2.GL_RGBA );
                textureData.setExplicitVoxelComponentOrder( GL2.GL_RGBA );
                textureData.setExplicitVoxelComponentType( GL2.GL_UNSIGNED_BYTE );
            }
            else if ( FileType.H264.equals( fileType ) ) {
                textureData.setExplicitInternalFormat(GL2.GL_LUMINANCE8);
                textureData.setExplicitVoxelComponentOrder(GL2.GL_LUMINANCE);
                textureData.setExplicitVoxelComponentType(GL2.GL_UNSIGNED_BYTE);
            }
            else if ( FileType.H265.equals( fileType ) ) {
//                textureData.setExplicitInternalFormat( GL2.GL_RGB );
//                textureData.setExplicitVoxelComponentOrder( GL2.GL_RGB );
//                textureData.setExplicitVoxelComponentType( GL2.GL_UNSIGNED_BYTE );
// Best yet.
                textureData.setExplicitInternalFormat(GL2.GL_LUMINANCE8);
                textureData.setExplicitVoxelComponentOrder(GL2.GL_RGB);
                textureData.setExplicitVoxelComponentType(GL2.GL_UNSIGNED_BYTE);

// Fluffy overlap effect.                
//                textureData.setExplicitInternalFormat(GL2.GL_LUMINANCE8);
//                textureData.setExplicitVoxelComponentOrder(GL2.GL_LUMINANCE);
//                textureData.setExplicitVoxelComponentType(GL2.GL_UNSIGNED_BYTE);
            }

            System.out.println("End load volume: " + new java.util.Date());
            return true;
        }
        catch (Exception exc) {
            exc.printStackTrace();
        }

        return false;
    }

    /** This picks up the result of the build process carried out above. */
    @Override
    public void populateVolumeAcceptor(VolumeDataAcceptor dataAcceptor) {
        dataAcceptor.setPrimaryTextureData( textureData );
    }

    private FileType getFileType( String filename, String baseName, String extension ) {
        logger.debug("FILENAME: {}", filename);

        if (extension.startsWith(VolumeFileLoaderI.TIF_EXT)) {
            return FileType.TIF;
        }
        else if (extension.startsWith(VolumeFileLoaderI.LSM_EXT)) {
            return FileType.LSM;
        }
        else if (extension.startsWith(VolumeFileLoaderI.H264_EXT) || filename.contains(VolumeFileLoaderI.H264_EXT)) {
            return FileType.H264;
        }
        else if (extension.startsWith(VolumeFileLoaderI.H265_EXT) || filename.contains(VolumeFileLoaderI.H265_EXT)  ||  extension.equals(VolumeFileLoaderI.H5J_EXT)) {
            return FileType.H265;
        }
        else if (extension.startsWith(VolumeFileLoaderI.MP4_EXT)) {
            return FileType.MP4;
        }
        else if (extension.startsWith(VolumeFileLoaderI.V3D_EXT) &&
                 ( baseName.startsWith( V3dMaskFileLoader.CONSOLIDATED_LABEL_MASK ) ||
                   baseName.startsWith( V3dMaskFileLoader.COMPARTMENT_MASK_INDEX ) ) ) {
            return FileType.V3DMASK;
        }
        else if (extension.startsWith(VolumeFileLoaderI.V3D_EXT)) {
            return FileType.V3DSIGNAL;
        }
        else {
            return FileType.UNKNOWN;
        }
    }

    private TextureColorSpace resolveColorSpace(String baseName, String extension) {
        // Default to linear color space
        // But look for some exceptions we know about
        TextureColorSpace colorSpace = TextureColorSpace.COLOR_SPACE_LINEAR;
        if (baseName.startsWith(VolumeFileLoaderI.CONSOLIDATED_SIGNAL_FILE))
            colorSpace = TextureColorSpace.COLOR_SPACE_SRGB;
        if (baseName.startsWith("Aligned20xScale"))
            colorSpace = TextureColorSpace.COLOR_SPACE_SRGB;
        if (baseName.startsWith(VolumeFileLoaderI.REFERENCE_FILE))
            colorSpace = TextureColorSpace.COLOR_SPACE_SRGB;
        // assume all mpegs are in sRGB color space
        if (extension.startsWith(VolumeFileLoaderI.MP4_EXT))
            colorSpace = TextureColorSpace.COLOR_SPACE_SRGB;

        return colorSpace;
    }

}
