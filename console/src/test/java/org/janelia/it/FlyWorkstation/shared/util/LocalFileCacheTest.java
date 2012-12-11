package org.janelia.it.FlyWorkstation.shared.util;

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

    private File cacheRootParentDirectory;
    private List<File> testRemoteFiles;
    private int singleFileKilobytes;
    private int maxNumberOfCachedFiles;

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
        final String ts = CachedFile.buildTimestampName();
        cacheRootParentDirectory = new File("test-cache-" + ts);
        final String path = cacheRootParentDirectory.getAbsolutePath();
        if (cacheRootParentDirectory.mkdir()) {
            LOG.info("setUp: created " + path);
        } else {
            throw new IllegalStateException("failed to create " + path);
        }

        singleFileKilobytes = 50;
        testRemoteFiles = new ArrayList<File>();
        for (int i = 0; i < 3; i++) {
            testRemoteFiles.add(
                    CachedFileTest.createFile(singleFileKilobytes));
        }

        maxNumberOfCachedFiles = testRemoteFiles.size() - 1;
        // adding last file should force removal of first file
        final int cacheKilobytes =
                (singleFileKilobytes + 1) * maxNumberOfCachedFiles;
        cache = new LocalFileCache(cacheRootParentDirectory, cacheKilobytes);
    }

    @Override
    protected void tearDown() throws Exception {

        for (File testRemoteFile : testRemoteFiles) {
            CachedFileTest.deleteFile(testRemoteFile);
        }

        try {
            if (cache.getNumberOfFiles() > 0) {
                LOG.info("tearDown: clearing non-empty test cache instance");
                cache.clear();
            }
        } catch (Throwable t) {
            LOG.warn("tearDown: failed to clear test cache instance", t);
        }

        CachedFileTest.deleteFile(cache.getRootDirectory());
        CachedFileTest.deleteFile(cacheRootParentDirectory);

        LOG.info("tearDown: exit --------------------------------------");
    }

    public void testCache() throws Exception {

        final long singleFileBytes = singleFileKilobytes * 1024;

        File testRemoteFile;
        List<File> localFiles = new ArrayList<File>();
        File localFile;

        int numberOfAdds;
        for (int i = 0; i < maxNumberOfCachedFiles; i++) {
            testRemoteFile = testRemoteFiles.get(i);
            localFile = cache.get(testRemoteFile.toURI().toURL());
            localFiles.add(localFile);
            numberOfAdds = localFiles.size();
            assertEquals("cached file " + numberOfAdds + " has invalid length",
                         singleFileBytes, localFile.length());
            assertEquals("invalid number of files in cache after " +
                         numberOfAdds + " addition(s)",
                         numberOfAdds, cache.getNumberOfFiles());
        }

        testRemoteFile = testRemoteFiles.get(maxNumberOfCachedFiles);
        localFile = cache.get(testRemoteFile.toURI().toURL());
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
                CachedFileTest.createFile(cache.getKilobyteCapacity() + 1);
        testRemoteFiles.add(testRemoteFile); // ensures clean-up after test
        try {
            cache.get(testRemoteFile.toURI().toURL());
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

    private static final Logger LOG =
            LoggerFactory.getLogger(LocalFileCacheTest.class);
}
