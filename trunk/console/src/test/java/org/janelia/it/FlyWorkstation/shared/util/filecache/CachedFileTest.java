package org.janelia.it.FlyWorkstation.shared.util.filecache;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Tests the {@link CachedFile} class.
 *
 * @author Eric Trautman
 */
public class CachedFileTest extends TestCase {

    private static int fileCount = 0;

    private File testCacheRootDirectory;
    private File testCacheTempDirectory;
    private File testCacheActiveDirectory;
    private File testRemoteDirectory;
    private File testRemoteFile;

    public CachedFileTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(CachedFileTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        LOG.info("setUp: entry ----------------------------------------");
        final File parentDirectory = (new File(".")).getCanonicalFile();
        final String rootName = "test-cache-" + buildTimestampName();
        testCacheRootDirectory = createDirectory(parentDirectory, rootName);
        testCacheTempDirectory = createDirectory(testCacheRootDirectory, "temp");
        testCacheActiveDirectory = createDirectory(testCacheRootDirectory, "active");
        testRemoteDirectory = createDirectory(parentDirectory, rootName + "-remote");
        testRemoteFile = createFile(testRemoteDirectory, 1);
        LOG.info("setUp: exit ----------------------------------------");
    }

    @Override
    protected void tearDown() throws Exception {
        LOG.info("tearDown: entry --------------------------------------");
        deleteFile(testCacheActiveDirectory);
        deleteFile(testCacheTempDirectory);
        deleteFile(testCacheRootDirectory);
        deleteFile(testRemoteFile);
        deleteFile(testRemoteDirectory);
        LOG.info("tearDown: exit --------------------------------------");
    }

    public void testLoadAndDelete() throws Exception {

        WebDavFile webDavFile = new WebDavFile(null, testRemoteFile);
        String urlPath = webDavFile.getUrl().getPath();
        File activeFile = new File(testCacheActiveDirectory, urlPath);
        File tempFile = new File(testCacheTempDirectory, "test-temp-file");

        CachedFile cachedFile = new CachedFile(webDavFile, activeFile);
        cachedFile.loadRemoteFile(tempFile);

        File localFile = cachedFile.getLocalFile();
        assertNotNull("local file is missing",
                      localFile);
        assertEquals("remote and local file lengths differ",
                     testRemoteFile.length(), localFile.length());

        File metaFile = cachedFile.getMetaFile();
        assertNotNull("meta file is missing",
                      metaFile);

        CachedFile reloadedCachedFile = CachedFile.loadPreviouslyCachedFile(metaFile);
        assertEquals("reloaded URL value differs",
                     cachedFile.getUrl(), reloadedCachedFile.getUrl());

        File beforeFile = cachedFile.getLocalFile();
        File afterFile = reloadedCachedFile.getLocalFile();
        URI beforeURI = beforeFile.toURI();
        URI afterURI = afterFile.toURI();
        assertEquals("reloaded local file URL value differs",
                     beforeURI.toURL(),
                     afterURI.toURL());

        validateDirectoryFileCount("after load", testCacheActiveDirectory, 1);
        validateDirectoryFileCount("after load", testCacheTempDirectory, 0);

        saveMetaDataInLegacyFormat(cachedFile, metaFile);
        reloadedCachedFile = CachedFile.loadPreviouslyCachedFile(metaFile);
        assertEquals("reloaded legacy URL value differs",
                     cachedFile.getUrl(), reloadedCachedFile.getUrl());

        cachedFile.remove(testCacheActiveDirectory);

        validateDirectoryFileCount("after remove", testCacheActiveDirectory, 0);

        webDavFile = new WebDavFile(null, testRemoteDirectory);
        urlPath = webDavFile.getUrl().getPath();
        activeFile = new File(testCacheActiveDirectory, urlPath);
        tempFile = new File(testCacheTempDirectory, "test-temp-dir");
        cachedFile = new CachedFile(webDavFile, activeFile);
        try {
            cachedFile.loadRemoteFile(tempFile);
            fail("attempt to load directory should have caused exception");
        } catch (IllegalArgumentException e) {
            LOG.debug("attempt to load directory correctly caused exception", e);
        }

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

    private void saveMetaDataInLegacyFormat(CachedFile cachedFile,
                                            File metaFile)
            throws Exception {

        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(metaFile));
            out.writeObject(cachedFile);
        } finally {
            if (out != null) {
                out.close();
            }
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

    private static final Logger LOG = LoggerFactory.getLogger(CachedFileTest.class);

    private static final String TIMESTAMP_PATTERN =
            "yyyyMMdd-HHmmssSSS";
    private static final SimpleDateFormat TIMESTAMP_FORMAT =
            new SimpleDateFormat(TIMESTAMP_PATTERN);
}
