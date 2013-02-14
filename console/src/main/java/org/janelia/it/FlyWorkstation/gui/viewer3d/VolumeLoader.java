package org.janelia.it.FlyWorkstation.gui.viewer3d;

import org.apache.commons.io.FilenameUtils;
import org.apache.juli.JdkLoggerFormatter;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor.TextureColorSpace;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.*;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class VolumeLoader
{

    public enum FileType{
        TIF, LSM, V3DMASK, V3DSIGNAL, MP4, UNKNOWN
    };

    private static final String CONSOLIDATED_SIGNAL_FILE = "ConsolidatedSignal2";
    private static final String REFERENCE_FILE = "Reference2";
    private static final String TIF_EXT = "TIF";
    private static final String LSM_EXT = "LSM";
    private static final String V3D_EXT = "V3D";
    private static final String MP4_EXT = "MP4";

    private static final int MAX_FILE_LOAD_RETRY = 3;
    private static final int WAIT_BETWEEN_FILE_LOAD_RETRIES = 1000;

    private FileResolver resolver;
    private boolean isMask = false;
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

            String extension = FilenameUtils.getExtension(localFileName).toUpperCase();
            System.out.println("FILENAME: " + localFileName);
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
                    isMask = true;
                    break;
                case MP4:
                    MpegFileLoader mpegFileLoader = new MpegFileLoader();
                    fileLoader = mpegFileLoader;
                    textureDataBuilder = mpegFileLoader;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown filename/extension combination " + baseName + "/" + extension);
            }

            textureDataBuilder.setColorSpace( resolveColorSpace(baseName, extension) );

            // Attempt to load the file.  After the max attempt, pass through the exception.
            int tryCount = 0;
            try {
                while ( true ) {

                    fileLoader.loadVolumeFile( localFileName );
                    break;

                }
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

            textureData = textureDataBuilder.buildTextureData( isMask );

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
        System.out.println("FILENAME: " + filename);

        if (extension.startsWith(TIF_EXT)) {
            return FileType.TIF;
        }
        else if (extension.startsWith(LSM_EXT)) {
            return FileType.LSM;
        }
        else if (extension.startsWith(MP4_EXT)) {
            return FileType.MP4;
        }
        else if (extension.startsWith(V3D_EXT) &&
                 ( baseName.startsWith( V3dMaskFileLoader.CONSOLIDATED_LABEL_MASK ) ||
                   baseName.startsWith( V3dMaskFileLoader.COMPARTMENT_MASK_INDEX ) ) ) {
            return FileType.V3DMASK;
        }
        else if (extension.startsWith(V3D_EXT)) {
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
        if (baseName.startsWith(CONSOLIDATED_SIGNAL_FILE))
            colorSpace = TextureColorSpace.COLOR_SPACE_SRGB;
        if (baseName.startsWith(REFERENCE_FILE))
            colorSpace = TextureColorSpace.COLOR_SPACE_SRGB;
        // assume all mpegs are in sRGB color space
        if (extension.startsWith(MP4_EXT))
            colorSpace = TextureColorSpace.COLOR_SPACE_SRGB;

        return colorSpace;
    }

}
