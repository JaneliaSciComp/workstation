package org.janelia.it.workstation.browser.filecache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.api.http.HttpClientProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * {@link AbstractStorageClient} manager.
 */
public class StorageClientMgr {

    private static final Logger LOG = LoggerFactory.getLogger(StorageClientMgr.class);
    
    private static final Cache<String, AgentStorageClient> STORAGE_WORKERS_CACHE = CacheBuilder.newBuilder()
            .maximumSize(100)
            .build();
    private static Consumer<Throwable> NOOP_ERROR_CONN_HANDLER = (t) -> {};

    private final HttpClientProxy httpClient;
    private final MasterStorageClient masterStorageClient;
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
    public StorageClientMgr(String baseUrl, HttpClientProxy httpClient) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.masterStorageClient = new MasterStorageClient(baseUrl, httpClient, objectMapper);
    }

    public URLProxy getDownloadFileURL(String standardPathName) throws FileNotFoundException {
        AgentStorageClient storageClient = getStorageClientForStandardPath(standardPathName);
        return storageClient.getDownloadFileURL(standardPathName);
    }

    private AgentStorageClient getStorageClientForStandardPath(String standardPathName) throws FileNotFoundException {
        Path standardPath = Paths.get(standardPathName.replaceFirst("^jade:\\/\\/", ""));
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
        LOG.debug("storagePathPrefixCandidates={}", storagePathPrefixCandidates);
        AgentStorageClient storageClient;
        synchronized(STORAGE_WORKERS_CACHE) {
            for (String pathPrefix : storagePathPrefixCandidates) {
                storageClient = STORAGE_WORKERS_CACHE.getIfPresent(pathPrefix);
                if (storageClient != null) {
                    LOG.debug("Found storage client for {} in cache", pathPrefix);
                    return storageClient;
                }
            }
            WebDavStorage storage = masterStorageClient.findStorage(standardPathName);
            String storageBindName = storage.getStorageBindName();
            String storageRootDir = storage.getStorageRootDir();
            String storageKey;
            Consumer<Throwable> agentErrorHandler;
            if  (storageBindName != null && standardPath.startsWith(storageBindName)) {
                storageKey = storageBindName;
                agentErrorHandler = t -> STORAGE_WORKERS_CACHE.invalidate(storageKey);
            } else if (storageRootDir != null && standardPath.startsWith(storageRootDir)) {
                storageKey = storageRootDir;
                agentErrorHandler = t -> STORAGE_WORKERS_CACHE.invalidate(storageKey);
            } else {
                storageKey = null;
                agentErrorHandler = t -> {};
            }
            storageClient = new AgentStorageClient(
                    storage.getRemoteFileUrl(),
                    httpClient,
                    objectMapper,
                    agentErrorHandler
            );
            if (storageKey != null) {
                STORAGE_WORKERS_CACHE.put(storageKey, storageClient);
                LOG.info("Created storage client for {}", storageKey);
            } else {
                LOG.warn("No storage agent cached for {}", standardPathName);
            }
        }
        return storageClient;
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
            throws WebDavException, FileNotFoundException {
        AgentStorageClient storageClient = getStorageClientForStandardPath(remoteFileName);
        return storageClient.findFile(remoteFileName);
    }

    String createStorage(String storageName, String storageContext, String storageTags) {
        return masterStorageClient.createStorage(storageName, storageContext, storageTags);
    }

    RemoteLocation uploadFile(File file, String storageURL, String storageLocation) {
        try {
            AgentStorageClient agentStorageClient = new AgentStorageClient(storageURL, httpClient, objectMapper, NOOP_ERROR_CONN_HANDLER);
            RemoteLocation remoteFile = agentStorageClient.saveFile(agentStorageClient.getUploadFileURL(storageLocation), file);
            remoteFile.setStorageURL(storageURL);
            return remoteFile;
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
