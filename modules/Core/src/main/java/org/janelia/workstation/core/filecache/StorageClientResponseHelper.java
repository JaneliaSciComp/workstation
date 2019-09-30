package org.janelia.workstation.core.filecache;

import java.io.FileNotFoundException;

import org.apache.commons.httpclient.Header;
import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.janelia.workstation.core.api.http.HttpClientProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StorageClientResponseHelper {

    private static final Logger LOG = LoggerFactory.getLogger(StorageClientResponseHelper.class);

    static MultiStatusResponse[] getResponses(HttpClientProxy httpClient, String href, int depth, int callCount)
            throws WebDavException, FileNotFoundException {
        MultiStatusResponse[] multiStatusResponses;
        PropFindMethod method = null;
        try {
            LOG.debug("WebDAV property lookup - {}", href);
            method = new PropFindMethod(href, WebDavFile.PROPERTY_NAMES, depth);
            method.addRequestHeader("Accept", "application/xml");
            method.addRequestHeader("Content-Type", "application/xml");
            final int responseCode = httpClient.executeMethod(method);
            LOG.trace("getResponses: {} returned for PROPFIND {}", responseCode, href);

            if (responseCode == HttpStatus.SC_MULTI_STATUS) {
                final MultiStatus multiStatus = method.getResponseBodyAsMultiStatus();
                multiStatusResponses = multiStatus.getResponses();
            }
            else if (responseCode == HttpStatus.SC_MOVED_PERMANENTLY) {
                final Header locationHeader = method.getResponseHeader("Location");
                if (locationHeader != null) {
                    final String movedHref = locationHeader.getValue();
                    if (callCount == 0) {
                        return getResponses(httpClient, movedHref, depth, 1);
                    }
                }
                throw new WebDavException(responseCode + " response code returned for " + href, responseCode);
            }
            else if (responseCode == HttpStatus.SC_NOT_FOUND) {
                throw new FileNotFoundException("Resource " + href + " not found (" + responseCode + ")");
            }
            else {
                throw new WebDavException(responseCode + " response code returned for " + href, responseCode);
            }
        }
        catch (WebDavException | FileNotFoundException e) {
            throw e;
        }
        catch (Exception e) {
            throw new WebDavException("Failed to retrieve WebDAV information from " + href, e);
        }
        finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
        if ((multiStatusResponses == null) || (multiStatusResponses.length == 0)) {
            throw new WebDavException("empty response returned for " + href);
        }
        return multiStatusResponses;
    }

    static String getStorageLookupURL(String baseUrl, String context, String remoteFileName) {
        if (baseUrl == null) {
            return null;
        }
        else {
            return baseUrl + "/" + context + "/" + remoteFileName;
        }
    }

}
