package org.janelia.workstation.core.filecache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.methods.GetMethod;
import org.janelia.filecacheutils.FileProxy;
import org.janelia.workstation.core.api.http.HttpClientProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebDavFileProxy implements FileProxy {
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
        return webDavFile.isDirectory() ? null : webDavFile.getSizeInBytes();
    }

    @Override
    public InputStream openContentStream() throws FileNotFoundException {
        GetMethod httpGet;
        try {
            httpGet = new GetMethod(webDavFile.getRemoteFileUrl());
        } catch (Exception e) {
            LOG.error("Could not create GET method for {}", webDavFile.getRemoteFileUrl(), e);
            webDavFile.handleError(e);
            throw new IllegalStateException(e);
        }
        try {
            final int responseCode = httpClientProxy.executeMethod(httpGet);
            if (responseCode != HttpServletResponse.SC_OK) {
                throw new WebDavException(responseCode + " returned for GET " + webDavFile.getRemoteFileUrl(), responseCode);
            }
            LOG.trace("retrieveFile: {} returned for GET from {}", responseCode, webDavFile);
            return httpGet.getResponseBodyAsStream();
        } catch (WebDavException e) {
            webDavFile.handleError(e);
            httpGet.releaseConnection();
            throw e;
        } catch (Exception e) {
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
