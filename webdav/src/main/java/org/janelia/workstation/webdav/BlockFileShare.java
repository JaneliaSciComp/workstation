package org.janelia.workstation.webdav;

import java.io.OutputStream;

/**
 * Created by schauderd on 6/26/15.
 */
public class BlockFileShare extends FileShare {
    @Override
    public void getFile (OutputStream response, String qualifiedFilename) {
        // check path mapping and stream resource out
    }
}
