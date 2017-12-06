package org.janelia.it.workstation.browser.filecache;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.janelia.it.jacs.model.TestCategories;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Tests the {@link CachedFile} class.
 *
 * @author Eric Trautman
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({RemoteFileCacheLoader.class})
@Category(TestCategories.FastTests.class)
public class RemoteFileCacheLoaderTest {

    private static int fileCount = 0;

    private HttpClient httpClient;
    private WebDavClientMgr webDavClientMgr;
    private RemoteFileCacheLoader remoteFileCacheLoader;
    private File testCacheRootDirectory;
    private File testCacheTempDirectory;
    private File testCacheActiveDirectory;
    private File testRemoteDirectory;
    private File testRemoteFile;

    @Before
    public void setUp() throws Exception {
        final File parentDirectory = (new File(".")).getCanonicalFile();
        final String rootName = "test-cache-" + buildTimestampName();
        testCacheRootDirectory = createDirectory(parentDirectory, rootName);
        testCacheTempDirectory = createDirectory(testCacheRootDirectory, "temp");
        testCacheActiveDirectory = createDirectory(testCacheRootDirectory, "active");
        testRemoteDirectory = createDirectory(parentDirectory, rootName + "-remote");
        testRemoteFile = createFile(testRemoteDirectory, 1);
        httpClient = Mockito.mock(HttpClient.class);
        webDavClientMgr = Mockito.mock(WebDavClientMgr.class);
        remoteFileCacheLoader = new RemoteFileCacheLoader(httpClient, webDavClientMgr,
                100,
                testCacheActiveDirectory,
                testCacheTempDirectory);
    }

    @After
    public void tearDown() throws Exception {
        deleteFile(testCacheActiveDirectory);
        deleteFile(testCacheTempDirectory);
        deleteFile(testCacheRootDirectory);
        deleteFile(testRemoteFile);
        deleteFile(testRemoteDirectory);
    }

    @Test
    public void testLoadAndDelete() throws Exception {
        String testRemoteFileName = testRemoteFile.getAbsolutePath();
        MultiStatusResponse multiStatusResponse = new MultiStatusResponse("http://test", 200, "desc");
        WebDavFile testWebDavFile = new WebDavFile(testRemoteFileName, multiStatusResponse);
        GetMethod testMethod = Mockito.mock(GetMethod.class);

        PowerMockito.whenNew(GetMethod.class).withAnyArguments().thenReturn(testMethod);
        Mockito.when(testMethod.getResponseBodyAsStream()).thenReturn(new FileInputStream(testRemoteFile));
        Mockito.when(webDavClientMgr.findFile(testRemoteFileName)).thenReturn(testWebDavFile);
        Mockito.when(httpClient.executeMethod(testMethod)).thenReturn(200);



//        WebDavFile webDavFile = new WebDavFile(null, testRemoteFile);
//        String urlPath = webDavFile.getUrl().getPath();
//        File activeFile = new File(testCacheActiveDirectory, urlPath);
//        File tempFile = new File(testCacheTempDirectory, "test-temp-file");

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

        validateDirectoryFileCount("after load", testCacheActiveDirectory, 1);
        validateDirectoryFileCount("after load", testCacheTempDirectory, 0);

        cachedFile.remove(testCacheActiveDirectory);

        validateDirectoryFileCount("after remove", testCacheActiveDirectory, 0);

//        webDavFile = new WebDavFile(null, testRemoteDirectory);
//        urlPath = webDavFile.getUrl().getPath();
//        activeFile = new File(testCacheActiveDirectory, urlPath);
//        tempFile = new File(testCacheTempDirectory, "test-temp-dir");
//        try {
//            RemoteFileCacheLoader.loadRemoteFile(webDavFile, tempFile, activeFile, mockWebDavClient);
//            fail("attempt to load directory should have caused exception");
//        } catch (IllegalArgumentException e) {
//            LOG.debug("attempt to load directory correctly caused exception", e);
//        }

    }

    private File createDirectory(File parent,
                                 String name) throws IllegalStateException {
        File directory = new File(parent, name);
        if (! directory.mkdir()) {
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

    /**
     * Utility to create a uniquely named test file with the specified length.
     *
     * @param  numberOfKilobytes  size of file in kilobytes.
     *
     * @return new file of specified size.
     *
     * @throws IOException
     *   if the file cannot be created.
     */
    public static File createFile(File parentDirectory,
                                  long numberOfKilobytes) throws IOException {
        final long numberOfBytes = numberOfKilobytes * 1024;
        final String name = "test-" + buildTimestampName() + ".txt";
        File file = new File(parentDirectory, name);

        final int lineLength = name.length() + 1;
        FileWriter out = null;
        try {
            out = new FileWriter(file);
            long i = lineLength;
            for (; i < numberOfBytes; i += lineLength) {
                out.write(name);
                out.write('\n');
            }
            long remainder = numberOfBytes % lineLength;
            for (i = 1; i < remainder; i++) {
                out.write('.');
            }
            if (remainder > 0) {
                out.write('\n');
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }

        LOG.info("createFile: created " + numberOfKilobytes +
                 "Kb file " + file.getAbsolutePath());

        return file;
    }

    /**
     * Utility to delete the specified file.
     *
     * @param  file  file to delete.
     */
    public static void deleteFile(File file) {
        if (file.exists()) {
            if (file.delete()) {
                LOG.info("deleteFile: deleted " + file.getAbsolutePath());
            } else {
                LOG.info("deleteFile: failed to delete " + file.getAbsolutePath());
            }
        }
    }

    /**
     * @return a new timestamp directory name based on the current time.
     */
    public synchronized static String buildTimestampName() {
        fileCount++;
        return TIMESTAMP_FORMAT.format(new Date()) + "-" + fileCount;
    }

    private static final Logger LOG = LoggerFactory.getLogger(RemoteFileCacheLoaderTest.class);

    private static final String TIMESTAMP_PATTERN =
            "yyyyMMdd-HHmmssSSS";
    private static final SimpleDateFormat TIMESTAMP_FORMAT =
            new SimpleDateFormat(TIMESTAMP_PATTERN);
}
