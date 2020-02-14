package org.janelia.workstation.core.filecache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.methods.GetMethod;
import org.janelia.filecacheutils.FileProxy;
import org.janelia.workstation.core.api.http.HttpClientProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebDavFileProxy implements FileProxy {
    private static final int WBEDAV_SO_TIMEOUT_INMILLIS = 180000;
    private static final Logger LOG = LoggerFactory.getLogger(WebDavFileProxy.class);

    private final HttpClientProxy httpClientProxy;
    private final WebDavFile webDavFile;

    WebDavFileProxy(HttpClientProxy httpClientProxy, WebDavFile webDavFile) {
        this.httpClientProxy = httpClientProxy;
        this.webDavFile = webDavFile;
    }

    @Override
    public String getFileId() {
        return webDavFile.getWebdavFileKey();
    }

    @Override
    public Long estimateSizeInBytes() {
        return webDavFile.getSizeInBytes();
    }

    @Override
    public InputStream openContentStream() throws FileNotFoundException {
        GetMethod httpGet;
        try {
            httpGet = new GetMethod(webDavFile.getRemoteFileUrl());
            httpGet.getParams().setSoTimeout(WBEDAV_SO_TIMEOUT_INMILLIS);
        } catch (Exception e) {
            LOG.error("Could not create GET method for {}", webDavFile.getRemoteFileUrl(), e);
            webDavFile.handleError(e);
            throw new IllegalStateException(e);
        }
        try {
            final int responseCode = httpClientProxy.executeMethod(httpGet);
            if (responseCode != HttpServletResponse.SC_OK) {
                LOG.error("GET {} returned {}", webDavFile.getRemoteFileUrl(), responseCode);
                throw new WebDavException("GET " + webDavFile.getRemoteFileUrl(), responseCode);
            }
            LOG.trace("GET {} returned {}", webDavFile.getRemoteFileUrl(), responseCode);
            return httpGet.getResponseBodyAsStream();
        } catch (WebDavException e) {
            webDavFile.handleError(e);
            httpGet.releaseConnection();
            throw e;
        } catch (Exception e) {
            LOG.error("GET {} error", webDavFile.getRemoteFileUrl(), e);
            webDavFile.handleError(e);
            httpGet.releaseConnection();
            throw new WebDavException("failed to open " + webDavFile.getRemoteFileUrl(), e);
        }
    }

    @Override
    public File getLocalFile() {
        return null;
    }

    @Override
    public boolean exists() {
        return webDavFile != null;
    }

    @Override
    public boolean deleteProxy() {
        return false;
    }
}
