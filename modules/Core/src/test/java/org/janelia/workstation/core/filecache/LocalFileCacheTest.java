package org.janelia.workstation.core.filecache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.janelia.it.jacs.model.TestCategories;
import org.janelia.workstation.core.api.http.HttpClientProxy;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;

import static org.junit.Assert.*;

/**
 * Tests the {@link LocalFileCache} class.
 *
 * @author Eric Trautman
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({StorageClientMgr.class, RemoteFileCacheLoader.class})
public class LocalFileCacheTest {
    private static final Logger LOG = LoggerFactory.getLogger(LocalFileCacheTest.class);

    private MasterStorageClient masterStorageClient;
    private AgentStorageClient agentStorageClient;
    private HttpClient httpClient;
    private GetMethod testGetMethod;
    private List<File> testRemoteFiles;
    private int singleFileKilobytes;
    private int immediateFileCount;
    private int maxNumberOfCachedFiles;
    private File remoteTestDirectory;
    private List<File> filesToDeleteDuringTearDown;
    private List<File> directoriesToDeleteDuringTearDown;

    private LocalFileCache cache;

    @Before
    public void setUp() throws Exception {
        final String ts = TestFileUtils.buildTimestampName();
        final File cacheRootParentDirectory = new File("test-cache-" + ts);
        final String path = cacheRootParentDirectory.getAbsolutePath();
        if (cacheRootParentDirectory.mkdir()) {
            LOG.info("setUp: created " + path);
        } else {
            throw new IllegalStateException("failed to create " + path);
        }

        final String remoteTestDirectoryName = "test-remote-dir-" + ts;
        remoteTestDirectory = new File(remoteTestDirectoryName);

        if (! remoteTestDirectory.mkdir()) {
            throw new IllegalStateException("failed to create directory " +
                                            remoteTestDirectory.getAbsolutePath());
        }

        final File nestedRemoteDirectory = new File(remoteTestDirectory, "nested-dir");
        if (! nestedRemoteDirectory.mkdir()) {
            throw new IllegalStateException("failed to create directory " +
                    nestedRemoteDirectory.getAbsolutePath());
        }

        singleFileKilobytes = 50;
        testRemoteFiles = new ArrayList<>();

        testRemoteFiles.add(TestFileUtils.createFile(nestedRemoteDirectory, singleFileKilobytes));

        immediateFileCount = 3;
        for (int i = 0; i < immediateFileCount; i++) {
            testRemoteFiles.add(TestFileUtils.createFile(remoteTestDirectory, singleFileKilobytes));
        }

        masterStorageClient = Mockito.mock(MasterStorageClient.class);
        agentStorageClient = Mockito.mock(AgentStorageClient.class);
        httpClient = Mockito.mock(HttpClient.class);
        HttpClientProxy httpClientProxy = new HttpClientProxy(httpClient);

        maxNumberOfCachedFiles = testRemoteFiles.size() - 1;
        // adding last file should force removal of first file
        final int cacheKilobytes =
                (singleFileKilobytes + 1) * maxNumberOfCachedFiles;
        PowerMockito.whenNew(MasterStorageClient.class)
                .withArguments(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpClientProxy.class), ArgumentMatchers.any(ObjectMapper.class))
                .thenReturn(masterStorageClient);
        PowerMockito.whenNew(AgentStorageClient.class)
                .withArguments(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpClientProxy.class), ArgumentMatchers.any(ObjectMapper.class))
                .thenReturn(agentStorageClient);

        Mockito.when(masterStorageClient.findStorage(ArgumentMatchers.anyString()))
                .then(invocation -> {
                    String storagePathName = invocation.getArgument(0);
                    Path storagePath = Paths.get(storagePathName);
                    Path storagePrefix;
                    if (storagePath.getRoot() == null) {
                        storagePrefix = storagePath.subpath(0, 3);
                    } else {
                        storagePrefix = storagePath.getRoot().resolve(storagePath.subpath(0, 3));
                    }
                    MultiStatusResponse multiStatusResponse = new MultiStatusResponse("http://test", "desc");
                    multiStatusResponse.add(new DefaultDavProperty<>(DavPropertyName.GETETAG, storagePrefix.toString()));
                    return new WebDavStorage(invocation.getArgument(0), multiStatusResponse);
                });
        Mockito.when(agentStorageClient.findFile(ArgumentMatchers.anyString()))
                .then(invocation -> {
                    String fileName = invocation.getArgument(0);
                    MultiStatusResponse multiStatusResponse = new MultiStatusResponse("http://test", "desc");
                    multiStatusResponse.add(new DefaultDavProperty<>(DavPropertyName.GETCONTENTLENGTH, String.valueOf(new File(fileName).length())), 200);
                    return new WebDavFile(fileName, multiStatusResponse, (err)->{});
                });
        Mockito.when(agentStorageClient.getDownloadFileURL(ArgumentMatchers.anyString()))
                .then(invocation -> {
                    return new URL("http://test/path" + invocation.getArgument(0));
                });
        testGetMethod = Mockito.mock(GetMethod.class);
        PowerMockito.whenNew(GetMethod.class).withAnyArguments().thenReturn(testGetMethod);
        Mockito.when(httpClient.executeMethod(ArgumentMatchers.any(HttpMethod.class))).thenReturn(200);

        cache = new LocalFileCache(cacheRootParentDirectory, cacheKilobytes, null, httpClientProxy, new StorageClientMgr("http://basewebdav", httpClientProxy));

        filesToDeleteDuringTearDown = new ArrayList<>();
        filesToDeleteDuringTearDown.addAll(testRemoteFiles);

        directoriesToDeleteDuringTearDown = new ArrayList<>();
        directoriesToDeleteDuringTearDown.add(cacheRootParentDirectory);
        directoriesToDeleteDuringTearDown.add(cache.getActiveDirectory());
        directoriesToDeleteDuringTearDown.add(cache.getTempDirectory());
        directoriesToDeleteDuringTearDown.add(remoteTestDirectory);
        directoriesToDeleteDuringTearDown.add(nestedRemoteDirectory);
    }

    @After
    public void tearDown() throws Exception {

        LOG.info("tearDown: entry --------------------------------------");

        for (File file : filesToDeleteDuringTearDown) {
            TestFileUtils.deleteFile(file);
        }

        try {
            if (cache.getNumberOfFiles() > 0) {
                LOG.info("tearDown: clearing non-empty test cache instance");
                cache.clear();
                // give removal a chance to complete
                Thread.sleep(500);
            }

            LOG.info("tearDown: clearing empty active cache directories");
            // HACK: reset capacity to force reload and delete of empty directories
            cache.setKilobyteCapacity(0);
            Thread.sleep(500); // give removal a chance to complete

        } catch (Throwable t) {
            LOG.warn("tearDown: failed to clear test cache instance", t);
        }

        // remove in reverse order so that directories are empty before removal
        for (int i = directoriesToDeleteDuringTearDown.size(); i > 0; i--) {
            TestFileUtils.deleteFile(directoriesToDeleteDuringTearDown.get(i-1));
        }

        LOG.info("tearDown: exit --------------------------------------");
    }

    @Test
    @Category(TestCategories.SlowTests.class)
    public void testRetrieveFile() throws Exception {

        final long singleFileBytes = singleFileKilobytes * 1024;

        File testRemoteFile;
        List<File> localFiles = new ArrayList<>();
        File localFile;

        int numberOfAdds;
        for (int i = 0; i < maxNumberOfCachedFiles; i++) {
            testRemoteFile = testRemoteFiles.get(i);
            Mockito.reset(testGetMethod);
            Mockito.when(testGetMethod.getResponseBodyAsStream()).thenReturn(new FileInputStream(testRemoteFile));

            localFile = cache.getFile(testRemoteFile.getAbsolutePath(), false);
            localFiles.add(localFile);
            numberOfAdds = localFiles.size();
            assertEquals("cached file " + numberOfAdds + " has invalid length",
                         singleFileBytes, localFile.length());
            assertEquals("invalid number of files in cache after " +
                         numberOfAdds + " addition(s)",
                         numberOfAdds, cache.getNumberOfFiles());
        }

        testRemoteFile = testRemoteFiles.get(maxNumberOfCachedFiles);
        Mockito.reset(testGetMethod);
        Mockito.when(testGetMethod.getResponseBodyAsStream()).thenReturn(new FileInputStream(testRemoteFile));
        localFile = cache.getFile(testRemoteFile.getAbsolutePath(), false);
        localFiles.add(localFile);
        assertEquals("cached file has invalid length",
                     singleFileBytes, localFile.length());

        // give removal a chance to complete
        Thread.sleep(1500);

        assertEquals("invalid number of files in cache after max capacity reached",
                     maxNumberOfCachedFiles, cache.getNumberOfFiles());

        File removedLocalFile = localFiles.remove(0);
        assertFalse(removedLocalFile.getAbsolutePath() +
                    " should have been removed after max capacity reached",
                    removedLocalFile.exists());

        // decrease capacity so that another file is dropped
        cache.setKilobyteCapacity(cache.getKilobyteCapacity() - singleFileKilobytes);

        // give removal a chance to complete
        Thread.sleep(1000);

        assertEquals("invalid number of files in cache after reducing capacity",
                     (maxNumberOfCachedFiles - 1), cache.getNumberOfFiles());

        removedLocalFile = localFiles.remove(0);
        assertFalse(removedLocalFile.getAbsolutePath() +
                    " should have been removed after reducing capacity",
                    removedLocalFile.exists());

        // clear everything else
        cache.clear();

        // give removal a chance to complete
        Thread.sleep(1000);

        assertEquals("invalid number of files in cache after clear",
                     0, cache.getNumberOfFiles());

        for (File file : localFiles) {
            assertFalse(file.getAbsolutePath() +
                        " should have been removed after clear",
                        file.exists());
        }

        testRemoteFile = TestFileUtils.createFile(remoteTestDirectory, cache.getKilobyteCapacity() + 1);

        filesToDeleteDuringTearDown.add(testRemoteFile);
        try {
            Mockito.reset(testGetMethod);
            Mockito.when(testGetMethod.getResponseBodyAsStream()).thenReturn(new FileInputStream(testRemoteFile));
            cache.getFile(testRemoteFile.getAbsolutePath(), false);
            fail("file larger than cache should have caused exception");
        } catch (FileNotCacheableException e) {
            Throwable cause = e.getCause();
            assertNotNull("exception cause is missing", cause);
            LOG.info("succesfully received exception, message is: " +
                    cause.getMessage());
        }
    }

    @Test
    @Category(TestCategories.SlowTests.class)
    public void testGetEffectiveUrl() throws Exception {
        File remoteFile = testRemoteFiles.get(0);
        assertEquals("should not be any cached files before first call", 0, cache.getNumberOfFiles());

        Mockito.reset(testGetMethod);
        Mockito.when(testGetMethod.getResponseBodyAsStream()).thenReturn(new FileInputStream(remoteFile));
        URLProxy effectiveUrl = cache.getEffectiveUrl(remoteFile.getAbsolutePath(), true);
        final long numberOfFiles = cache.getNumberOfFiles();

        assertNotNull(effectiveUrl);
        assertFalse("effective URL should not be a file URL", effectiveUrl.toString().startsWith("file:"));
        assertEquals("should not be any cached files immediately after first call", 0, numberOfFiles);

        // give async load a chance to complete
        Thread.sleep(1500);

        effectiveUrl = cache.getEffectiveUrl(remoteFile.getAbsolutePath(), false);

        assertEquals("the requested file should be cached after a short wait", 1, cache.getNumberOfFiles());
        assertTrue("effective URL should be a local file URL", effectiveUrl.toString().startsWith("file:/"));
    }

    @Test
    @Category(TestCategories.FastTests.class)
    public void testCleanUpInconsistentData() throws Exception {

        // ---------------------------------------
        // special set-up for this test

        final File firstNonNestedRemoteFile = testRemoteFiles.get(1);
        Mockito.reset(testGetMethod);
        Mockito.when(testGetMethod.getResponseBodyAsStream()).thenReturn(new FileInputStream(firstNonNestedRemoteFile));
        final File cachedFileWithoutMeta = cache.getFile(firstNonNestedRemoteFile.getAbsolutePath(), true);

        final File secondNonNestedRemoteFile = testRemoteFiles.get(2);
        Mockito.reset(testGetMethod);
        Mockito.when(testGetMethod.getResponseBodyAsStream()).thenReturn(new FileInputStream(secondNonNestedRemoteFile));
        File deletedCachedFile = cache.getFile(secondNonNestedRemoteFile.getAbsolutePath(), true);

        final File thirdNonNestedRemoteFile = testRemoteFiles.get(3);
        Mockito.reset(testGetMethod);
        Mockito.when(testGetMethod.getResponseBodyAsStream()).thenReturn(new FileInputStream(secondNonNestedRemoteFile));
        final File cachedFileWithCorruptedMeta = cache.getFile(thirdNonNestedRemoteFile.getAbsolutePath(), true);

        assertEquals("should be three cached files at start", 3, cache.getNumberOfFiles());

        final File parentDirectory = cachedFileWithoutMeta.getParentFile();

        File deletedMetaFile = new File(parentDirectory, CachedFile.getMetaFileName(cachedFileWithoutMeta));
        if (! deletedMetaFile.delete()) {
            fail("failed to remove cachedFileWithoutMeta meta file " +
                        deletedMetaFile.getAbsolutePath());
        }

        final File orphanedMetaFile = new File(parentDirectory, CachedFile.getMetaFileName(deletedCachedFile));
        if (! deletedCachedFile.delete()) {
            fail("failed to remove cached file " + deletedCachedFile.getAbsolutePath());
        }
        assertTrue("meta file " + orphanedMetaFile.getAbsolutePath() + " is missing before starting test",
                   orphanedMetaFile.exists());

        File corruptMetaWithCacheFile =
                new File(parentDirectory,
                         CachedFile.getMetaFileName(cachedFileWithCorruptedMeta));
        assertTrue("meta file " + corruptMetaWithCacheFile.getAbsolutePath() + " is missing",
                   corruptMetaWithCacheFile.exists());

        FileWriter writer = new FileWriter(corruptMetaWithCacheFile);
        writer.write("Power tends to corrupt,");
        writer.close();

        File corruptMetaWithoutCacheFile = new File(parentDirectory,
                                                    ".corrupt-meta-without-cache.jacs-cached-file");
        writer = new FileWriter(corruptMetaWithoutCacheFile);
        writer.write("and absolute power corrupts absolutely.");
        writer.close();

        File fileToRemove = new File(parentDirectory, "this-file-should-be-removed-by-load.txt");
        writer = new FileWriter(fileToRemove);
        writer.write("Nothing to see here, please move on.");
        writer.close();

        filesToDeleteDuringTearDown.add(fileToRemove);

        // ---------------------------------------
        // reset capacity to force cache reload and clean-up of inconsistent data

        cache.setKilobyteCapacity(cache.getKilobyteCapacity() * 2);

        // give async reload a chance to complete
        Thread.sleep(1500);

        // ---------------------------------------
        // verify results ...

        assertFalse("meta file " + deletedMetaFile.getAbsolutePath() +
                    " should NOT have been restored for orphaned cache file",
                    deletedMetaFile.exists());

        assertFalse("valid but orphaned meta file " + orphanedMetaFile.getAbsolutePath() + " was not removed",
                    orphanedMetaFile.exists());

        assertFalse("corrupted meta file " + corruptMetaWithCacheFile.getAbsolutePath() + " should have been removed",
                    corruptMetaWithCacheFile.exists());

        assertFalse("corrupted and orphaned meta file " + corruptMetaWithoutCacheFile.getAbsolutePath() +
                    " was not removed",
                    corruptMetaWithoutCacheFile.exists());

        assertFalse("file without meta data " + fileToRemove.getAbsolutePath() + " should have been removed",
                    fileToRemove.exists());
    }
}
