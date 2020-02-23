package org.janelia.workstation.core.filecache;

import java.io.FileNotFoundException;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.janelia.filecacheutils.FileKeyToProxyMapper;
import org.janelia.filecacheutils.FileProxy;
import org.janelia.filecacheutils.HttpFileProxy;
import org.janelia.filecacheutils.LocalFileProxy;
import org.janelia.workstation.core.api.http.HttpClientProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebDavFileKeyProxyMapper implements FileKeyToProxyMapper<WebdavCachedFileKey> {

    private static final Logger LOG = LoggerFactory.getLogger(WebDavFileKeyProxyMapper.class);

    private final HttpClientProxy httpClient;
    private final StorageClientMgr storageClientMgr;

    public WebDavFileKeyProxyMapper(HttpClientProxy httpClient, StorageClientMgr storageClientMgr) {
        this.httpClient = httpClient;
        this.storageClientMgr = storageClientMgr;
    }

    @Override
    public FileProxy getProxyFromKey(WebdavCachedFileKey fileKey) throws FileNotFoundException {
        switch(fileKey.getRemoteFileScheme()) {
            case "file":
                return new LocalFileProxy(fileKey.getRemoteFileName());
            case "http":
                return new HttpFileProxy(
                        fileKey.getRemoteFileName(),
                        url -> {
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
                        },
                        url -> {
                            try {
                                HeadMethod httpHead = new HeadMethod(url);
                                int responseCode = httpClient.executeMethod(httpHead);
                                if (responseCode > 200) {
                                    LOG.info("HEAD {} from {}", responseCode, url);
                                    return false;
                                } else {
                                    LOG.debug("HEAD {} from {}", responseCode, url);
                                    return true;
                                }
                            } catch (Exception e) {
                                throw new IllegalStateException(e);
                            }
                        });
            default:
                return getWebDavFileProxy(fileKey.getRemoteFileName());
        }
    }

    private FileProxy getWebDavFileProxy(String remoteFileName) throws FileNotFoundException {
        WebDavFile webDavFile;
        try {
            webDavFile = storageClientMgr.findFile(remoteFileName);
        } catch (FileNotFoundException e) {
            throw e;
        }
        LOG.debug("Loading {} from {}", webDavFile.isDirectory() ? "dialog" : "file", webDavFile.getRemoteFileUrl());
        return new WebDavFileProxy(httpClient, webDavFile);
    }

}
