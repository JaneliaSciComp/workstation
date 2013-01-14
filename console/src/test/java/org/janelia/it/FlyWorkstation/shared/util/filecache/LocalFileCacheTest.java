package org.janelia.it.FlyWorkstation.shared.util.filecache;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the {@link LocalFileCache} class.
 *
 * @author Eric Trautman
 */
public class LocalFileCacheTest extends TestCase {

    private MockWebDavClient mockClient;
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
        final String ts = CachedFileTest.buildTimestampName();
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

        for (int i = 0; i < 3; i++) {
            testRemoteFiles.add(
                    CachedFileTest.createFile(remoteTestDirectory,
                                              singleFileKilobytes));
        }

        mockClient = new MockWebDavClient();
        mockClient.mapFilesUsingDefaultUrl(testRemoteFiles);

        maxNumberOfCachedFiles = testRemoteFiles.size() - 1;
        // adding last file should force removal of first file
        final int cacheKilobytes =
                (singleFileKilobytes + 1) * maxNumberOfCachedFiles;
        cache = new LocalFileCache(cacheRootParentDirectory, cacheKilobytes, mockClient);

        filesToDeleteDuringTearDown = new ArrayList<File>();
        filesToDeleteDuringTearDown.add(cacheRootParentDirectory);
        filesToDeleteDuringTearDown.add(cache.getRootDirectory());
        filesToDeleteDuringTearDown.add(cache.getActiveDirectory());
        filesToDeleteDuringTearDown.add(cache.getTempDirectory());
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

        assertEquals("invalid number of files in cache after max capacity reached",
                     maxNumberOfCachedFiles, cache.getNumberOfFiles());

        File removedLocalFile = localFiles.remove(0);
        assertFalse(removedLocalFile.getAbsolutePath() +
                    " should have been removed after max capacity reached",
                    removedLocalFile.exists());

        // decrease capacity so that another file is dropped
        cache.setKilobyteCapacity(cache.getKilobyteCapacity() - singleFileKilobytes);

        // give removal a chance to complete
        Thread.sleep(500);

        assertEquals("invalid number of files in cache after reducing capacity",
                     (maxNumberOfCachedFiles - 1), cache.getNumberOfFiles());

        removedLocalFile = localFiles.remove(0);
        assertFalse(removedLocalFile.getAbsolutePath() +
                    " should have been removed after reducing capacity",
                    removedLocalFile.exists());

        // clear everything else
        cache.clear();

        // give removal a chance to complete
        Thread.sleep(500);

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
        mockClient.mapFileUsingDefaultUrl(testRemoteFile);

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
    }

    private static final Logger LOG =
            LoggerFactory.getLogger(LocalFileCacheTest.class);
}
