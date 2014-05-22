package org.janelia.it.workstation.shared.util.filecache;

import org.janelia.it.jacs.model.TestCategories;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests the {@link org.janelia.it.workstation.shared.util.filecache.WebDavUploader} class.
 *
 * @author Eric Trautman
 */
public class WebDavUploaderTest {

    private org.janelia.it.workstation.shared.util.filecache.WebDavClient client;
    private org.janelia.it.workstation.shared.util.filecache.WebDavUploader uploader;
    private File testRootParentDirectory;
    private File testNestedDirectory;
    private List<File> testFiles;

    @Before
    public void setUp() throws Exception {
        client = new org.janelia.it.workstation.shared.util.filecache.WebDavClient(WebDavClient.JACS_WEBDAV_BASE_URL, 100, 100);
        client.setCredentials(WebDavClientTest.WEBDAV_TEST_USER_CREDENTIALS);
        uploader = new org.janelia.it.workstation.shared.util.filecache.WebDavUploader(client, WebDavClientTest.JACS_WEBDAV_TEST_WRITE_ROOT_PATH);

        final String ts = CachedFileTest.buildTimestampName();
        testRootParentDirectory = new File("test-upload-" + ts);
        String path = testRootParentDirectory.getAbsolutePath();
        if (testRootParentDirectory.mkdir()) {
            LOG.info("setUp: created " + path);
        } else {
            throw new IllegalStateException("failed to create " + path);
        }

        testNestedDirectory = new File(testRootParentDirectory, "nestedDir");
        path = testNestedDirectory.getAbsolutePath();
        if (testNestedDirectory.mkdir()) {
            LOG.info("setUp: created " + path);
        } else {
            throw new IllegalStateException("failed to create " + path);
        }

        testFiles = new ArrayList<File>();
        testFiles.add(CachedFileTest.createFile(testRootParentDirectory, 1));
        testFiles.add(CachedFileTest.createFile(testRootParentDirectory, 1));
        testFiles.add(CachedFileTest.createFile(testNestedDirectory, 1));
    }

    @After
    public void tearDown() throws Exception {

        LOG.info("tearDown: entry --------------------------------------");

        for (File file : testFiles) {
            CachedFileTest.deleteFile(file);
        }

        CachedFileTest.deleteFile(testNestedDirectory);
        CachedFileTest.deleteFile(testRootParentDirectory);

        LOG.info("tearDown: exit --------------------------------------");
    }

    @Test
    @Category(TestCategories.SlowIntegrationTests.class)
    public void testUploadFile() throws Exception {

        String remotePath = uploader.uploadFile(testFiles.get(0));
        assertNotNull("null path returned for file upload", remotePath);

        URL url = client.getWebDavUrl(remotePath);

        org.janelia.it.workstation.shared.util.filecache.WebDavFile webDavFile = client.findFile(url);
        assertNotNull("uploaded file " + url + " missing from server", webDavFile);
    }

    @Test
    @Category(TestCategories.FastTests.class)
    public void testDerivePathsForUnix() throws Exception {
        commonTestDerivePaths("/a/b/c/root", true);
    }

    @Test
    @Category(TestCategories.FastTests.class)
    public void testDerivePathsForWindows() throws Exception {
        commonTestDerivePaths("C:\\a\\b\\c\\root", false);
    }

    private void commonTestDerivePaths(String localRoot,
                                       boolean isUnix) throws Exception {

        String localRootPathWithTrailingSeparator;
        if (isUnix) {
            localRootPathWithTrailingSeparator = localRoot + "/";
        } else {
            localRootPathWithTrailingSeparator = localRoot + "\\";
        }

        final String remoteRoot = "/remote/upload/123";
        final String remoteRootWithTrailingSeparator = remoteRoot + "/";

        final String[] relativeUnixPaths = {
                "/d/e/f1.mip",
                "/d/e/f2.mip",
                "/d/f3.mip",
                "/f4.mip",
                "/g/f5.mip",
        };

        final String[] expectedOrderedDirPaths = {
                "/remote/upload/123/d/",
                "/remote/upload/123/d/e/",
                "/remote/upload/123/g/"
        };

        List<String> relativePaths = new ArrayList<String>();
        for (String unixPath : relativeUnixPaths) {
            if (isUnix) {
                relativePaths.add(unixPath);
            } else {
                relativePaths.add(unixPath.replace('/', '\\'));
            }
        }

        List<String> localFilePaths = new ArrayList<String>();
        for (String relativePath : relativePaths) {
            localFilePaths.add(localRoot + relativePath);
        }

        // use LHM to preserve order
        Map<String, File> remotePathToFileMap = new LinkedHashMap<String, File>();
        List<String> orderedDirectoryPaths = new ArrayList<String>();

        uploader.derivePaths(localRootPathWithTrailingSeparator,
                             localFilePaths,
                             remoteRootWithTrailingSeparator,
                             orderedDirectoryPaths,
                             remotePathToFileMap);

        assertEquals("incorrect remote path map size, map=" + remotePathToFileMap,
                localFilePaths.size(),
                remotePathToFileMap.size());

        int index = 0;
        String expectedPath;
        for (String remotePath : remotePathToFileMap.keySet()) {
            expectedPath = remoteRoot + relativePaths.get(index);
            expectedPath = expectedPath.replace('\\', '/');
            assertEquals("incorrect file path derived", expectedPath, remotePath);
            index++;
        }

        assertEquals("incorrect directory list size, list=" + orderedDirectoryPaths,
                expectedOrderedDirPaths.length,
                orderedDirectoryPaths.size());

        for (int i = 0; i < expectedOrderedDirPaths.length; i++) {
            assertEquals("incorrect dir path " + i + " derived",
                    expectedOrderedDirPaths[i], orderedDirectoryPaths.get(i));
        }
    }

    @Test
    @Category(TestCategories.SlowIntegrationTests.class)
    public void testUploadFilesWithoutRelativePath() throws Exception {
        commonTestUploadFiles(null, 1);
    }

    @Test
    @Category(TestCategories.SlowIntegrationTests.class)
    public void testUploadFilesWithRelativePath() throws Exception {
        commonTestUploadFiles(testRootParentDirectory, 2);
    }

    private void commonTestUploadFiles(File localRootDirectory,
                                       int expectedDistinctParentPaths) throws Exception {
        String remotePath = uploader.uploadFiles(testFiles, localRootDirectory);
        assertNotNull("null path returned for upload", remotePath);

        URL url = client.getWebDavUrl(remotePath);

        List<org.janelia.it.workstation.shared.util.filecache.WebDavFile> list = client.findAllInternalFiles(url);

        assertEquals("invalid number of files created under " + url, testFiles.size(), list.size());

        Set<String> parentPathSet = new HashSet<String>();
        File file;
        for (WebDavFile webDavFile : list) {
            file = new File(webDavFile.getUrl().toURI().getPath());
            parentPathSet.add(file.getParent());
        }

        assertEquals("invalid number of distinct parent paths created " + parentPathSet,
                expectedDistinctParentPaths, parentPathSet.size());
    }

    private static final Logger LOG = LoggerFactory.getLogger(WebDavUploaderTest.class);
}
