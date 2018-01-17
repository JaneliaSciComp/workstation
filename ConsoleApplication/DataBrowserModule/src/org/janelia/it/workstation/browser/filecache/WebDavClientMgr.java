package org.janelia.it.workstation.browser.filecache;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * {@link WebDavClient} manager.
 */
public class WebDavClientMgr {

    private static final int STORAGE_PATH_PREFIX_COMPS_COUNT = 2;

    private static final Cache<String, WebDavClient> WEBDAV_AGENTS_CACHE = CacheBuilder.newBuilder()
            .maximumSize(10)
            .build();

    private final HttpClient httpClient;
    private final WebDavClient masterWebDavInstance;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a client with default authentication credentials.
     *
     * @param  baseUrl base URL
     * @param  httpClient http client
     *
     * @throws IllegalArgumentException
     *   if the baseUrl cannot be parsed.
     */
    public WebDavClientMgr(String baseUrl, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.masterWebDavInstance = new WebDavClient(validateUrl(baseUrl), httpClient, objectMapper);
    }

    private String validateUrl(String urlString) {
        try {
            final URL url = new URL(urlString);
            return urlString;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("failed to parse URL: " + urlString, e);
        }
    }

    public URL getDownloadFileURL(String standardPathName) {
        WebDavClient webDavClient = getWebDavClientForStandardPath(standardPathName);
        return webDavClient.getDownloadFileURL(standardPathName);
    }

    private WebDavClient getWebDavClientForStandardPath(String standardPathName) {
        Path standardPath = Paths.get(standardPathName);
        int nPathComponents = standardPath.getNameCount();
        List<String> storagePathPrefixCandidates = new LinkedList<>();
        IntStream.range(1, nPathComponents)
                .mapToObj(pathIndex -> {
                    if (standardPath.getRoot() == null) {
                        return standardPath.subpath(0, pathIndex).toString();
                    } else {
                        return standardPath.getRoot().resolve(standardPath.subpath(0, pathIndex)).toString();
                    }
                })
                .forEach(p -> storagePathPrefixCandidates.add(0, p));
        WebDavClient webDavClient;
        for (String pathPrefix : storagePathPrefixCandidates) {
            webDavClient = WEBDAV_AGENTS_CACHE.getIfPresent(pathPrefix);
            if (webDavClient != null) {
                return webDavClient;
            }
        }
        WebDavFile webDavFile = findWebDavFileStorage(standardPathName);
        webDavClient = new WebDavClient(webDavFile.getRemoteFileUrl().toString(), httpClient, objectMapper);
        WEBDAV_AGENTS_CACHE.put(webDavFile.getEtag(), webDavClient);
        return webDavClient;
    }

    private WebDavFile findWebDavFileStorage(String storagePath) throws WebDavException {
        return masterWebDavInstance.findStorage(storagePath);
    }

    /**
     * Finds information about the specified file.
     *
     * @param  remoteFileName  file's remote reference name.
     *
     * @return WebDAV information for the specified file.
     *
     * @throws WebDavException
     *   if the file information cannot be retrieved.
     */
    WebDavFile findFile(String remoteFileName)
            throws WebDavException {
        WebDavClient webDavClient = getWebDavClientForStandardPath(remoteFileName);
        return webDavClient.findFile(remoteFileName);
    }

    String createStorage(String storageName, String storageContext, String storageTags) {
        return masterWebDavInstance.createStorage(storageName, storageContext, storageTags);
    }

    RemoteLocation uploadFile(File file, String storageURL, String storageLocation) {
        try {
            String uploadFileUrl = storageURL + "/file/" + (StringUtils.isBlank(storageLocation) ? "" : storageLocation);
            RemoteLocation remoteFile = masterWebDavInstance.saveFile(new URL(uploadFileUrl), file);
            remoteFile.setRemoteStorageURL(storageURL);
            return remoteFile;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    RemoteLocation createDirectory(String storageURL, String storageLocation) {
        try {
            String createDirUrl = storageURL + "/directory/" + (StringUtils.isBlank(storageLocation) ? "" : storageLocation);
            RemoteLocation remoteDirectory = masterWebDavInstance.createDirectory(new URL(createDirUrl));
            remoteDirectory.setRemoteStorageURL(storageURL);
            return remoteDirectory;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    String urlEncodeComp(String pathComp) {
        if (StringUtils.isBlank(pathComp)) {
            return "";
        } else {
            try {
                return URLEncoder.encode(pathComp, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    String urlEncodeComps(String pathComps) {
        return StringUtils.isBlank(pathComps)
                ? ""
                : StreamSupport.stream(Splitter.on(File.separatorChar).split(pathComps).spliterator(), false)
                    .map(pc -> urlEncodeComp(pc))
                    .reduce(null, (c1, c2) -> c1 == null ? c2 : c1 + File.separatorChar + c2);
    }
}
