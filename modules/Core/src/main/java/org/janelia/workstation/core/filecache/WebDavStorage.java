package org.janelia.workstation.core.filecache;

import org.apache.jackrabbit.webdav.MultiStatusResponse;

/**
 * Encapsulates minimum amount of information for a remote storage
 * through WebDAV.
 *
 * @author Eric Trautman
 */
class WebDavStorage extends AbstractWebDav {

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
    WebDavStorage(String webdavFileKey, MultiStatusResponse multiStatusResponse)
            throws IllegalArgumentException {
        super(webdavFileKey, multiStatusResponse);
    }

}
