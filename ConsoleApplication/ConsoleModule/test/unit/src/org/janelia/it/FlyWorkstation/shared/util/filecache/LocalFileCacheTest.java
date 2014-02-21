package org.janelia.it.FlyWorkstation.shared.util.filecache;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
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
    private int immediateFileCount;
    private int maxNumberOfCachedFiles;
    private File remoteTestDirectory;
    private List<File> filesToDeleteDuringTearDown;
    private List<File> directoriesToDeleteDuringTearDown;

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

        immediateFileCount = 3;
        for (int i = 0; i < immediateFileCount; i++) {
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
        cache = new LocalFileCache(cacheRootParentDirectory, cacheKilobytes, mockClient, null);

        filesToDeleteDuringTearDown = new ArrayList<File>();
        filesToDeleteDuringTearDown.addAll(testRemoteFiles);

        directoriesToDeleteDuringTearDown = new ArrayList<File>();
        directoriesToDeleteDuringTearDown.add(cacheRootParentDirectory);
        directoriesToDeleteDuringTearDown.add(cache.getRootDirectory());
        directoriesToDeleteDuringTearDown.add(cache.getActiveDirectory());
        directoriesToDeleteDuringTearDown.add(cache.getTempDirectory());
        directoriesToDeleteDuringTearDown.add(remoteTestDirectory);
        directoriesToDeleteDuringTearDown.add(nestedRemoteDirectory);

        LOG.info("setUp: exit ----------------------------------------");
    }

    @Override
    protected void tearDown() throws Exception {

        LOG.info("tearDown: entry --------------------------------------");

        for (File file : filesToDeleteDuringTearDown) {
            CachedFileTest.deleteFile(file);
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
            CachedFileTest.deleteFile(directoriesToDeleteDuringTearDown.get(i-1));
        }

        LOG.info("tearDown: exit --------------------------------------");
    }

    public void testRetrieveFile() throws Exception {

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

    public void testGetEffectiveUrl() throws Exception {
        File remoteFile = testRemoteFiles.get(0);
        Assert.assertEquals("should not be any cached files before first call",
                            0, cache.getNumberOfFiles());
        final URL remoteUrl = remoteFile.toURI().toURL();
        URL effectiveUrl = cache.getEffectiveUrl(remoteUrl);
        final long numberOfFiles = cache.getNumberOfFiles();

        Assert.assertEquals("remote and effective URLs should be the same after first call",
                            remoteUrl, effectiveUrl);
        Assert.assertEquals("should not be any cached files immediately after first call",
                0, numberOfFiles);

        // give async load a chance to complete
        Thread.sleep(500);

        effectiveUrl = cache.getEffectiveUrl(remoteUrl);

        Assert.assertEquals("the requested file should be cached after a short wait",
                            1, cache.getNumberOfFiles());

        Assert.assertFalse("remote and effective URLs should differ after file is cached",
                remoteUrl.equals(effectiveUrl));
    }

    public void testGetImmediateDirectory() throws Exception {
        // double capacity to ensure that limit is based upon immediate check
        cache.setKilobyteCapacity(cache.getKilobyteCapacity() * 2);

        final URL remoteDirectoryUrl = remoteTestDirectory.toURI().toURL();
        File localDirectory  = cache.getDirectory(remoteDirectoryUrl, false, false);

        Assert.assertTrue("local directory for " + remoteDirectoryUrl + " does not exist",
                          localDirectory.exists());

        final long numberOfFiles = cache.getNumberOfFiles();

        Assert.assertEquals("cache contains incorrect number of files",
                            immediateFileCount, numberOfFiles);
    }

    public void testGetAllDirectory() throws Exception {
        // double capacity so that all files can be cached
        cache.setKilobyteCapacity(cache.getKilobyteCapacity() * 2);

        final URL remoteDirectoryUrl = remoteTestDirectory.toURI().toURL();
        File localDirectory  = cache.getDirectory(remoteDirectoryUrl, true, false);

        Assert.assertTrue("local directory for " + remoteDirectoryUrl + " does not exist",
                localDirectory.exists());

        final long numberOfFiles = cache.getNumberOfFiles();

        Assert.assertEquals("cache contains incorrect number of files",
                            testRemoteFiles.size(), numberOfFiles);
    }

    public void testCleanUpInconsistentData() throws Exception {

        // ---------------------------------------
        // special set-up for this test

        final File firstNonNestedRemoteFile = testRemoteFiles.get(1);
        final File cachedFileWithoutMeta = cache.getFile(firstNonNestedRemoteFile.toURI().toURL());

        final File secondNonNestedRemoteFile = testRemoteFiles.get(2);
        File deletedCachedFile = cache.getFile(secondNonNestedRemoteFile.toURI().toURL());

        final File thirdNonNestedRemoteFile = testRemoteFiles.get(3);
        final File cachedFileWithCorruptedMeta =
                cache.getFile(thirdNonNestedRemoteFile.toURI().toURL());

        Assert.assertEquals("should be three cached files at start",
                            3, cache.getNumberOfFiles());

        final File parentDirectory = cachedFileWithoutMeta.getParentFile();

        File deletedMetaFile = new File(parentDirectory,
                                        CachedFile.getMetaFileName(cachedFileWithoutMeta));
        if (! deletedMetaFile.delete()) {
            Assert.fail("failed to remove cachedFileWithoutMeta meta file " +
                        deletedMetaFile.getAbsolutePath());
        }

        final File orphanedMetaFile = new File(parentDirectory,
                                               CachedFile.getMetaFileName(deletedCachedFile));
        if (! deletedCachedFile.delete()) {
            Assert.fail("failed to remove cached file " + deletedCachedFile.getAbsolutePath());
        }
        Assert.assertTrue("meta file " + orphanedMetaFile.getAbsolutePath() +
                          " is missing before starting test",
                          orphanedMetaFile.exists());

        File corruptMetaWithCacheFile =
                new File(parentDirectory,
                         CachedFile.getMetaFileName(cachedFileWithCorruptedMeta));
        Assert.assertTrue("meta file " + corruptMetaWithCacheFile.getAbsolutePath() +
                          " is missing",
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
        Thread.sleep(500);

        // ---------------------------------------
        // verify results ...

        Assert.assertFalse("meta file " + deletedMetaFile.getAbsolutePath() +
                          " should NOT have been restored for orphaned cache file",
                          deletedMetaFile.exists());

        Assert.assertFalse("valid but orphaned meta file " + orphanedMetaFile.getAbsolutePath() +
                           " was not removed",
                           orphanedMetaFile.exists());

        Assert.assertFalse("corrupted meta file " + corruptMetaWithCacheFile.getAbsolutePath() +
                          " should have been removed",
                          corruptMetaWithCacheFile.exists());

        Assert.assertFalse("corrupted and orphaned meta file " +
                           corruptMetaWithoutCacheFile.getAbsolutePath() +
                          " was not removed",
                           corruptMetaWithoutCacheFile.exists());

        Assert.assertFalse("file without meta data " + fileToRemove.getAbsolutePath() +
                           " should have been removed",
                          fileToRemove.exists());
    }

    private static final Logger LOG = LoggerFactory.getLogger(LocalFileCacheTest.class);
}
