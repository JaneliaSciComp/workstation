package org.janelia.workstation.core.filecache;

import org.apache.commons.httpclient.methods.GetMethod;
import org.janelia.filecacheutils.FileProxy;
import org.janelia.filecacheutils.HttpFileProxy;
import org.janelia.filecacheutils.LocalFileProxy;
import org.janelia.filecacheutils.RemoteFileRetriever;
import org.janelia.workstation.core.api.http.HttpClientProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebDavRemoteFileRetriever implements RemoteFileRetriever<WebdavCachedFileKey> {

    private static final Logger LOG = LoggerFactory.getLogger(WebDavRemoteFileRetriever.class);

    private final HttpClientProxy httpClient;
    private final StorageClientMgr storageClientMgr;

    public WebDavRemoteFileRetriever(HttpClientProxy httpClient, StorageClientMgr storageClientMgr) {
        this.httpClient = httpClient;
        this.storageClientMgr = storageClientMgr;
    }

    @Override
    public FileProxy retrieve(WebdavCachedFileKey fileKey) {
        switch(fileKey.getRemoteFileScheme()) {
            case "file":
                return new LocalFileProxy(fileKey.getRemoteFileName());
            case "http":
                return new HttpFileProxy(
                    fileKey.getRemoteFileName(),
                    (String url) -> {
                        try {
                            GetMethod httpGet = new GetMethod(url);
                            int responseCode = httpClient.executeMethod(httpGet);
                            LOG.trace("GET {} from {}", responseCode, url);
                            if (responseCode != 200) {
                                throw new IllegalStateException("Response code "+responseCode+" returned for call to "+url);
                            }
                            return httpGet.getResponseBodyAsStream();
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    });
            default:
                return getWebDavFileProxy(fileKey.getRemoteFileName());
        }
    }

    private FileProxy getWebDavFileProxy(String remoteFileName) {
        WebDavFile webDavFile;
        try {
            webDavFile = storageClientMgr.findFile(remoteFileName);
        } catch (Exception e) {
            throw new IllegalStateException("Error retrieving " + remoteFileName, e);
        }
        if (webDavFile.isDirectory()) {
            throw new IllegalArgumentException(
                    "Requested load of directory " + webDavFile.getRemoteFileUrl() + ".  Only files may be requested.");
        }
        return new WebDavFileProxy(httpClient, webDavFile);
    }

}
