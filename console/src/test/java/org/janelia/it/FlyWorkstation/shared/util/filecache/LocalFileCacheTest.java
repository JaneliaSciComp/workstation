package org.janelia.it.FlyWorkstation.shared.util.filecache;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the {@link LocalFileCache} class.
 *
 * @author Eric Trautman
 */
public class LocalFileCacheTest extends TestCase {

    private List<File> testRemoteFiles;
    private int singleFileKilobytes;
    private int maxNumberOfCachedFiles;
    private File remoteTestDirectory;
    private List<File> filesToDeleteDuringTearDown;

    private LocalFileCache cache;

    public LocalFileCacheTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(LocalFileCacheTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        LOG.info("setUp: entry ----------------------------------------");
        final String ts = LocalFileCache.buildTimestampName();
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
        testRemoteFiles = new ArrayList<File>();

        testRemoteFiles.add(
                CachedFileTest.createFile(nestedRemoteDirectory,
                                          singleFileKilobytes));

        for (int i = 0; i < 2; i++) {
            testRemoteFiles.add(
                    CachedFileTest.createFile(remoteTestDirectory,
                                              singleFileKilobytes));
        }

        maxNumberOfCachedFiles = testRemoteFiles.size() - 1;
        // adding last file should force removal of first file
        final int cacheKilobytes =
                (singleFileKilobytes + 1) * maxNumberOfCachedFiles;
        cache = new LocalFileCache(cacheRootParentDirectory, cacheKilobytes);

        filesToDeleteDuringTearDown = new ArrayList<File>();
        filesToDeleteDuringTearDown.add(cacheRootParentDirectory);
        filesToDeleteDuringTearDown.add(cache.getRootDirectory());
        filesToDeleteDuringTearDown.add(remoteTestDirectory);
        filesToDeleteDuringTearDown.add(nestedRemoteDirectory);
        filesToDeleteDuringTearDown.addAll(testRemoteFiles);

        LOG.info("setUp: exit ----------------------------------------");
    }

    @Override
    protected void tearDown() throws Exception {

        LOG.info("tearDown: entry --------------------------------------");

        try {
            if (cache.getNumberOfFiles() > 0) {
                LOG.info("tearDown: clearing non-empty test cache instance");
                cache.clear();
            }
        } catch (Throwable t) {
            LOG.warn("tearDown: failed to clear test cache instance", t);
        }

        // remove in reverse order so that directories are empty before removal
        for (int i = filesToDeleteDuringTearDown.size(); i > 0; i--) {
            CachedFileTest.deleteFile(filesToDeleteDuringTearDown.get(i-1));
        }

        LOG.info("tearDown: exit --------------------------------------");
    }

    public void testGetFile() throws Exception {

        final long singleFileBytes = singleFileKilobytes * 1024;

        File testRemoteFile;
        List<File> localFiles = new ArrayList<File>();
        File localFile;

        int numberOfAdds;
        for (int i = 0; i < maxNumberOfCachedFiles; i++) {
            testRemoteFile = testRemoteFiles.get(i);
            localFile = cache.getFile(testRemoteFile.toURI().toURL());
            localFiles.add(localFile);
            numberOfAdds = localFiles.size();
            assertEquals("cached file " + numberOfAdds + " has invalid length",
                         singleFileBytes, localFile.length());
            assertEquals("invalid number of files in cache after " +
                         numberOfAdds + " addition(s)",
                         numberOfAdds, cache.getNumberOfFiles());
        }

        testRemoteFile = testRemoteFiles.get(maxNumberOfCachedFiles);
        localFile = cache.getFile(testRemoteFile.toURI().toURL());
        localFiles.add(localFile);
        assertEquals("cached file has invalid length",
                singleFileBytes, localFile.length());

        // give removal a chance to complete
        Thread.sleep(500);

        assertEquals("invalid number of files in cache",
                maxNumberOfCachedFiles, cache.getNumberOfFiles());

        File removedLocalFile = localFiles.remove(0);
        assertFalse(removedLocalFile.getAbsolutePath() +
                " should have been removed after expiration",
                removedLocalFile.exists());

        cache.clear();

        assertEquals("invalid number of files in cache after clear",
                     0, cache.getNumberOfFiles());

        for (File file : localFiles) {
            assertFalse(file.getAbsolutePath() +
                        " should have been removed after clear",
                        file.exists());
        }

        testRemoteFile =
                CachedFileTest.createFile(remoteTestDirectory,
                                          cache.getKilobyteCapacity() + 1);
        filesToDeleteDuringTearDown.add(testRemoteFile);
        try {
            cache.getFile(testRemoteFile.toURI().toURL());
            fail("file larger than cache should have caused exception");
        } catch (FileNotCacheableException e) {
            Throwable cause = e.getCause();
            assertNotNull("exception cause is missing", cause);
            LOG.info("succesfully received exception, message is: " +
                    cause.getMessage());
        }

        // give removal a chance to complete
        Thread.sleep(500);
    }

    public void testGetDirectory() throws Exception {

        // increase capacity so that all original files are kept
        cache.setKilobyteCapacity(cache.getKilobyteCapacity() + singleFileKilobytes);

        // create one more file that is too big, which should be ignored
        final File remoteTooBigFile =
                CachedFileTest.createFile(remoteTestDirectory,
                                          cache.getKilobyteCapacity() + 1);
        filesToDeleteDuringTearDown.add(remoteTooBigFile);

        final URL remoteTestDirectoryUrl = remoteTestDirectory.toURI().toURL();
        MockWebDavClient mockClient = new MockWebDavClient();

        List<File> remoteFileList = new ArrayList<File>(testRemoteFiles);
        remoteFileList.add(remoteTooBigFile);

        mockClient.setFilesForUrl(remoteTestDirectoryUrl, remoteFileList);

        File remotePreCachedFile = testRemoteFiles.get(0);
        File localPreCachedFile =
                cache.getFile(remotePreCachedFile.toURI().toURL());
        Assert.assertTrue(localPreCachedFile.getAbsolutePath() +
                          " should exist after being (pre) cached",
                          localPreCachedFile.exists());

        File localDirectory = cache.getDirectory(remoteTestDirectoryUrl,
                                                 mockClient);

        Assert.assertTrue(localDirectory.getAbsolutePath() +
                          " should exist after being cached",
                          localDirectory.exists());

        // all original file entries + 1 additional directory entry
        final int expectedNumberOfFiles =
                testRemoteFiles.size() + 1;

        assertEquals("invalid number of files in cache",
                     expectedNumberOfFiles, cache.getNumberOfFiles());

        final int relativeStart =
                remoteTestDirectory.getAbsolutePath().length() + 1;
        for (File remoteFile : testRemoteFiles) {
            verifyCachedDirectoryFileExists(localDirectory,
                                            relativeStart,
                                            remoteFile);
        }

        File localTooBigFile = new File(localDirectory,
                                        remoteTooBigFile.getName());
        Assert.assertFalse(
                localTooBigFile.getAbsolutePath() +
                " should NOT exist in cache since it is too big",
                localTooBigFile.exists());

        Assert.assertFalse(
                localPreCachedFile.getAbsolutePath() +
                " should have been moved into directory",
                localPreCachedFile.exists());

        // reload the cache from the file system
        cache.loadCacheFromFilesystem();

        final int numberOfFilesMinusDirectory = expectedNumberOfFiles - 1;
        assertEquals("invalid number of files in cache after reload",
                     numberOfFilesMinusDirectory,
                     cache.getNumberOfFiles());

        localDirectory = cache.getDirectory(remoteTestDirectoryUrl,
                                            mockClient);

        Assert.assertTrue(localDirectory.getAbsolutePath() +
                          " should exist after being cached",
                          localDirectory.exists());

        cache.clear();

        assertEquals("invalid number of files in cache after clear",
                     0, cache.getNumberOfFiles());

        // reload the cache from the file system to clear empty directories
        cache.loadCacheFromFilesystem();

        assertEquals("invalid number of files in cache after post clear load",
                     0, cache.getNumberOfFiles());
    }

    private void verifyCachedDirectoryFileExists(File localDirectory,
                                                 int relativeStart,
                                                 File remoteFile) {
        final String absolutePath = remoteFile.getAbsolutePath();
        final String relativePath = absolutePath.substring(relativeStart);
        final File localFile = new File(localDirectory, relativePath);
        Assert.assertTrue(localFile.getAbsolutePath() +
                " should exist after being cached",
                localFile.exists());
    }

    private static final Logger LOG =
            LoggerFactory.getLogger(LocalFileCacheTest.class);
}
