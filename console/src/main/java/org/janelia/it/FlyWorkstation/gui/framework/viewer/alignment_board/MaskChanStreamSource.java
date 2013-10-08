package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 10/8/13
 * Time: 1:41 PM
 *
 * Encapsulate the pair of mask and channel streams, so they can be passed around as a unit, and so that the
 * streams may be re-created for the same renderable as many times as needed.
 */

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.MaskChanRenderableData;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MaskChanStreamSource {
    private MaskChanRenderableData maskChanRenderableData;
    private boolean useChannelData;
    private FileResolver resolver;
    private Logger logger = LoggerFactory.getLogger(MaskChanStreamSource.class);
    public MaskChanStreamSource( MaskChanRenderableData renderableData, FileResolver resolver, boolean useChannelData ) {
        this.maskChanRenderableData = renderableData;
        this.useChannelData = useChannelData;
        this.resolver = resolver;
    }

    /** Carry out a sanity check against inputs. */
    public StreamSourceSanity getSanity() {
        String message = "";
        boolean isSane = true;

        // Mask file is always needed.
        if ( maskChanRenderableData.getMaskPath() == null ) {
            int translatedNum = maskChanRenderableData.getBean().getTranslatedNum();
            if ( translatedNum != 0 ) {
                message += String.format(
                        "Renderable %s has a missing mask file. ID is %s.  ",
                        maskChanRenderableData.getBean().getTranslatedNum(),
                        maskChanRenderableData.getBean().getRenderableEntity().getId()
                );
            }
            isSane = false;
        }

        // Channel file is optional; presence implies channel data must be shown.
        if ( useChannelData  &&  maskChanRenderableData.getChannelPath() == null ) {
            message += String.format(
                    "Renderable %s has a missing channel file -- %s.",
                    maskChanRenderableData.getBean().getTranslatedNum(),
                    maskChanRenderableData.getMaskPath() + maskChanRenderableData.getChannelPath()
            );
            isSane = false;
        }

        return new StreamSourceSanity( message, isSane );
    }

    public InputStream getMaskInputStream() throws IOException {
        //  The mask stream is required in all cases.  But the channel path is optional.
        String resolvedFilename;
        try {
            resolvedFilename = resolver.getResolvedFilename(maskChanRenderableData.getMaskPath());
        } catch ( Throwable ex ) {
            logger.warn(ex.getMessage());
            resolvedFilename = maskChanRenderableData.getMaskPath(); // Non-cached.
        }

        InputStream maskStream =
                new BufferedInputStream(
                        new FileInputStream(resolvedFilename)
                );
        return maskStream;
    }

    public InputStream getChannelInputStream() throws IOException {
        InputStream chanStream = null;
        String resolvedFilename;
        try {
            resolvedFilename = resolver.getResolvedFilename( maskChanRenderableData.getChannelPath() );
        } catch ( Throwable ex ) {
            logger.warn(ex.getMessage());
            resolvedFilename = maskChanRenderableData.getChannelPath(); // Non-cached.
        }
        if ( useChannelData ) {
            chanStream =
                    new BufferedInputStream(
                            new FileInputStream( resolvedFilename )
                    );
        }
        return chanStream;
    }

    /** Simple "bean of sanity", suitable for single return value, to eliminate side-effects. */
    public static class StreamSourceSanity {
        private boolean sane;
        private String message;
        public StreamSourceSanity( String message, boolean sane ) {
            this.message = message;
            this.sane = sane;
        }
        public boolean isSane() {
            return sane;
        }
        public String getMessage() {
            return message;
        }

    }
}

