package org.janelia.workstation.webdav;

import java.io.OutputStream;

/**
 * Created by schauderd on 6/26/15.
 * For now, this works only with Scality.  At some point, we can abstract it to work with any object storage
 */
public class ObjectFileShare extends FileShare {
    @Override
    public void getFile (OutputStream response, String qualifiedFilename) {
        // check path mapping and stream resource out
    }
}
