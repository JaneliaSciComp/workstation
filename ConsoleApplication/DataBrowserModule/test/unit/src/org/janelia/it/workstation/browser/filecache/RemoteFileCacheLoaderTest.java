package org.janelia.it.workstation.browser.filecache;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.Arrays;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.janelia.it.jacs.model.TestCategories;
import org.janelia.it.workstation.browser.api.http.HttpClientProxy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Tests the {@link RemoteFileCacheLoader} class.
 *
 * @author Eric Trautman
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({RemoteFileCacheLoader.class})
@Category(TestCategories.FastTests.class)
public class RemoteFileCacheLoaderTest {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteFileCacheLoaderTest.class);

    private HttpClient httpClient;
    private StorageClientMgr storageClientMgr;
    private LocalFileCache testCache;
    private RemoteFileCacheLoader remoteFileCacheLoader;
    private File testCacheRootDirectory;
    private File testRemoteDirectory;
    private File testRemoteFile;

    @Before
    public void setUp() throws Exception {
        final File parentDirectory = (new File(".")).getCanonicalFile();
        final String rootName = "test-cache-" + TestFileUtils.buildTimestampName();
        testCacheRootDirectory = createDirectory(parentDirectory, rootName);
        testRemoteDirectory = createDirectory(parentDirectory, rootName + "-remote");
        testRemoteFile = TestFileUtils.createFile(testRemoteDirectory, 1);
        httpClient = Mockito.mock(HttpClient.class);
        HttpClientProxy httpClientProxy = new HttpClientProxy(httpClient);
        storageClientMgr = Mockito.mock(StorageClientMgr.class);
        testCache = new LocalFileCache(testCacheRootDirectory, 100, null, httpClientProxy, storageClientMgr);
        remoteFileCacheLoader = new RemoteFileCacheLoader(httpClientProxy, storageClientMgr, testCache);
    }

    @After
    public void tearDown() {
        TestFileUtils.deleteFile(testCacheRootDirectory);
        TestFileUtils.deleteFile(testRemoteFile);
        TestFileUtils.deleteFile(testRemoteDirectory);
    }

    @Test
    public void testLoadAndDelete() throws Exception {
        String testRemoteFileName = testRemoteFile.getAbsolutePath();
        MultiStatusResponse multiStatusResponse = new MultiStatusResponse("http://test", "desc");
        WebDavFile testWebDavFile = Mockito.spy(new WebDavFile(testRemoteFileName, multiStatusResponse, (t) -> {}));
        GetMethod testMethod = Mockito.mock(GetMethod.class);

        PowerMockito.whenNew(GetMethod.class).withAnyArguments().thenReturn(testMethod);
        Mockito.when(testMethod.getResponseBodyAsStream()).thenReturn(new FileInputStream(testRemoteFile));
        Mockito.when(storageClientMgr.findFile(testRemoteFileName)).thenReturn(testWebDavFile);
        Mockito.when(httpClient.executeMethod(ArgumentMatchers.any(HttpMethod.class))).thenReturn(200);

        CachedFile cachedFile = remoteFileCacheLoader.load(testRemoteFile.getAbsolutePath());

        File localFile = cachedFile.getLocalFile();
        assertNotNull("local file is missing", localFile);
        assertEquals("remote and local file lengths differ", testRemoteFile.length(), localFile.length());

        File metaFile = cachedFile.getMetaFile();
        assertNotNull("meta file is missing", metaFile);

        CachedFile reloadedCachedFile = CachedFile.loadPreviouslyCachedFile(metaFile);
        assertEquals("reloaded URL value differs", cachedFile.getRemoteFileName(), reloadedCachedFile.getRemoteFileName());

        File beforeFile = cachedFile.getLocalFile();
        File afterFile = reloadedCachedFile.getLocalFile();
        URI beforeURI = beforeFile.toURI();
        URI afterURI = afterFile.toURI();
        assertEquals("reloaded local file URL value differs",
                     beforeURI.toURL(),
                     afterURI.toURL());

        validateDirectoryFileCount("after load", testCache.getActiveDirectory(), 1);
        validateDirectoryFileCount("after load", testCache.getTempDirectory(), 0);

        cachedFile.remove(testCache.getActiveDirectory());

        validateDirectoryFileCount("after remove", testCache.getActiveDirectory(), 0);
    }

    private File createDirectory(File parent,
                                 String name) throws IllegalStateException {
        File directory = new File(parent, name);
        if (! directory.mkdirs()) {
            throw new IllegalStateException("failed to create " + directory.getAbsolutePath());
        }
        LOG.info("created " + directory.getAbsolutePath());
        return directory;
    }

    private void validateDirectoryFileCount(String context,
                                            File directory,
                                            int expectedCount) {
        File[] subDirectories = directory.listFiles();
        if (subDirectories == null) {
            fail(directory.getAbsolutePath() + " does not exist");
        } else {
            assertEquals(context + ", invalid number of sub directories in " +
                         directory.getAbsolutePath() +  ", found: " +
                         Arrays.asList(subDirectories),
                         expectedCount, subDirectories.length);
        }
    }

}
