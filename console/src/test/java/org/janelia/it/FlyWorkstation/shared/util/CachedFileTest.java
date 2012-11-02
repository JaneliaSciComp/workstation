package org.janelia.it.FlyWorkstation.shared.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * Tests the {@link CachedFile} class.
 *
 * @author Eric Trautman
 */
public class CachedFileTest extends TestCase {

    private File testCacheRootDirectory;
    private String rootPath;
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
        final String ts = CachedFile.buildTimestampName();
        testCacheRootDirectory = new File("test-cache-" + ts);
        rootPath = testCacheRootDirectory.getAbsolutePath();
        if (testCacheRootDirectory.mkdir()) {
            LOG.info("setUp: created " + rootPath);
        } else {
            throw new IllegalStateException("failed to create " + rootPath);
        }
        testRemoteFile = createFile(1);
    }

    @Override
    protected void tearDown() throws Exception {
        deleteFile(testCacheRootDirectory);
        deleteFile(testRemoteFile);
        LOG.info("tearDown: exit --------------------------------------");
    }

    public void testRemoteLoadAndDelete() throws Exception {
        final URL remoteFileUrl = testRemoteFile.toURI().toURL();
        CachedFile cachedFile = new CachedFile(testCacheRootDirectory,
                                               remoteFileUrl);

        File localFile = cachedFile.getLocalFile();
        assertNotNull("local file is missing",
                      localFile);
        assertEquals("remote and local file lengths differ",
                     testRemoteFile.length(), localFile.length());

        File[] cacheSubDirectories = testCacheRootDirectory.listFiles();
        if (cacheSubDirectories == null) {
            fail(rootPath + " is not a directory");
        } else {
            assertEquals("invalid number of sub directories after add in " +
                         rootPath +  ", found " +
                         Arrays.asList(cacheSubDirectories),
                         1, cacheSubDirectories.length);
        }

        Callable<Void> removalTask = cachedFile.getRemovalTask();
        removalTask.call();
        cacheSubDirectories = testCacheRootDirectory.listFiles();
        if (cacheSubDirectories == null) {
            fail(rootPath + " is not a directory");
        } else {
            assertEquals("invalid number of sub directories after remove in " +
                    rootPath + ", found " +
                    Arrays.asList(cacheSubDirectories),
                    0, cacheSubDirectories.length);
        }
    }

    public void testLocalLoadAndDelete() throws Exception {
        File tsDirectory = new File(testCacheRootDirectory,
                                    CachedFile.buildTimestampName());
        if (! tsDirectory.mkdir()) {
            fail("failed to create " + tsDirectory.getAbsolutePath());
        }

        final String cachedLocalFileName = "foo.txt";
        File localFile = new File(tsDirectory, cachedLocalFileName);
        copyFile(testRemoteFile, localFile);

        CachedFile cachedFile = new CachedFile(testCacheRootDirectory,
                                               localFile);

        File cachedLocalFile = cachedFile.getLocalFile();
        assertNotNull("cached local file is missing",
                      cachedLocalFile);
        assertEquals("cached local file does not match source",
                localFile, cachedLocalFile);
        assertEquals("invalid relative path derived",
                     "/" + cachedLocalFileName,
                     cachedFile.getRelativePath());

        Callable<Void> removalTask = cachedFile.getRemovalTask();
        removalTask.call();
        File[] cacheSubDirectories = testCacheRootDirectory.listFiles();
        if (cacheSubDirectories == null) {
            fail(rootPath + " is not a directory");
        } else {
            assertEquals("invalid number of sub directories after remove in " +
                    rootPath +  ", found " +
                    Arrays.asList(cacheSubDirectories),
                    0, cacheSubDirectories.length);
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
    public static File createFile(int numberOfKilobytes) throws IOException {
        final long numberOfBytes = numberOfKilobytes * 1024;
        final String name = "test-" + CachedFile.buildTimestampName() + ".txt";
        File file = new File(name);

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
     * Utility to copy one file to another.
     *
     * @param  from  source file.
     * @param  to    target file.
     *
     * @throws IOException
     *   if the file cannot be copied.
     */
    public static void copyFile(File from,
                                File to) throws IOException {
        FileChannel fromChannel = new FileInputStream(from).getChannel();
        FileChannel toChannel = new FileOutputStream(to).getChannel();
        final long totalBytes = from.length();
        int bytesWritten = 0;
        while (bytesWritten < totalBytes) {
            bytesWritten += fromChannel.transferTo(bytesWritten,
                                                   totalBytes,
                                                   toChannel);
        }

        LOG.info("copyFile: copied " + from.getAbsolutePath() +
                 " to " + to.getAbsolutePath());
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

    private static final Logger LOG = Logger.getLogger(CachedFileTest.class);
}
