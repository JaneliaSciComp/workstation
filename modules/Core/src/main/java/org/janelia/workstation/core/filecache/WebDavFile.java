package org.janelia.workstation.core.filecache;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;

import org.apache.jackrabbit.webdav.MultiStatusResponse;

/**
 * Encapsulates minimum amount of information for a remote
 * file accesible through WebDAV.
 *
 * @author Eric Trautman
 */
class WebDavFile extends AbstractWebDav {

    private final Consumer<Throwable> connectionErrorHandler;

    /**
     * Parses the specified WebDAV PROPFIND response 'fragment' to
     * populate this file's attributes.
     *
     * @param  webdavFileKey        the webdav file key
     * @param  multiStatusResponse  the PROPFIND response for the file.
     *
     * @throws IllegalArgumentException
     *   if a file specific URL cannot be constructed.
     */
    WebDavFile(String webdavFileKey, MultiStatusResponse multiStatusResponse, Consumer<Throwable> connectionErrorHandler)
            throws IllegalArgumentException {
        super(webdavFileKey, multiStatusResponse);
        this.connectionErrorHandler = connectionErrorHandler;
    }

    void handleError(Throwable e) {
        this.connectionErrorHandler.accept(e);
    }

}
