package org.janelia.it.FlyWorkstation.gui.viewer3d;

import org.apache.commons.io.FilenameUtils;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor.TextureColorSpace;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.*;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

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
                case TIF:
                    TifFileLoader tifFileLoader = new TifFileLoader();
                    textureDataBuilder = tifFileLoader;
                    fileLoader = tifFileLoader;
                    break;
                case LSM:
                    LsmFileLoader lsmFileLoader = new LsmFileLoader();
                    fileLoader = lsmFileLoader;
                    textureDataBuilder = lsmFileLoader;
                    break;
                case V3DSIGNAL:
                    V3dSignalFileLoader v3dFileLoader = new V3dSignalFileLoader();
                    fileLoader = v3dFileLoader;
                    textureDataBuilder = v3dFileLoader;
                    break;
                case V3DMASK:
                    V3dMaskFileLoader maskFileLoader = new V3dMaskFileLoader();
                    fileLoader = maskFileLoader;
                    textureDataBuilder = maskFileLoader;
                    isLuminance = true;
                    break;
                case MP4:
                    MpegFileLoader mpegFileLoader = new MpegFileLoader();
                    fileLoader = mpegFileLoader;
                    textureDataBuilder = mpegFileLoader;
                    break;
                default:
                    break;
                    //throw new IllegalArgumentException("Unknown filename/extension combination " + baseName + "/" + extension);
            }

            if ( textureDataBuilder != null ) {
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

            return true;
        }
        catch (Exception exc) {
            exc.printStackTrace();
        }
        return false;
    }

    /** This picks up the result of the build process carried out above. */
    public void populateVolumeAcceptor(VolumeDataAcceptor dataAcceptor) {
        dataAcceptor.setTextureData( textureData );
    }

    private FileType getFileType( String filename, String baseName, String extension ) {
        logger.debug("FILENAME: {}", filename);

        if (extension.startsWith(VolumeFileLoaderI.TIF_EXT)) {
            return FileType.TIF;
        }
        else if (extension.startsWith(VolumeFileLoaderI.LSM_EXT)) {
            return FileType.LSM;
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
