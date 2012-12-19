package org.janelia.it.FlyWorkstation.shared.util.filecache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility to map file share prefixes to WebDAV (HTTP) URLs.
 *
 * @author Eric Trautman
 */
public class WebDavPathMap {

    private Map<String, String> fileSharePrefixToWebDavRoot;

    /**
     * Constructs an empty map.
     */
    public WebDavPathMap() {
        // use linked map to allow for search order optimization
        // (e.g. adding jacsData first to map should reduce looping)
        fileSharePrefixToWebDavRoot = new LinkedHashMap<String, String>();
    }

    /**
     * Adds a new mapping.
     *
     * @param  fileSharePrefix  file share prefix to be mapped.
     * @param  webDavRoot       corresponding WebDAV root URL.
     */
    public void addFileShare(String fileSharePrefix,
                             URL webDavRoot) {
        final String prefix = trimTrailingSlash(fileSharePrefix);
        final String root = trimTrailingSlash(String.valueOf(webDavRoot));
        fileSharePrefixToWebDavRoot.put(prefix, root);
    }

    /**
     * @param  fileSharePath  file share path to be mapped.
     *
     * @return corresponding WebDAV URL.
     */
    public URL getUrl(String fileSharePath) {

        URL url = null;

        for (String prefix : fileSharePrefixToWebDavRoot.keySet()) {
            if (fileSharePath.startsWith(prefix)) {
                final int relativeIndex = prefix.length();
                if (fileSharePath.length() > relativeIndex) {
                    final String relativePath =
                            fileSharePath.substring(relativeIndex);
                    final String webDavRoot =
                            fileSharePrefixToWebDavRoot.get(prefix);
                    try {
                        url = new URL(webDavRoot + relativePath);
                    } catch (MalformedURLException e) {
                        LOG.error("failed to create URL " +
                                webDavRoot + relativePath +
                                " for " + fileSharePath, e);
                    }
                }
            }
        }

        return url;
    }

    private String trimTrailingSlash(String value) {
        String trimmedValue = value;
        if (value.endsWith("/")) {
            trimmedValue = value.substring(0, value.length() - 1);
        }
        return trimmedValue;
    }

    private static final Logger LOG =
            LoggerFactory.getLogger(WebDavPathMap.class);
}
