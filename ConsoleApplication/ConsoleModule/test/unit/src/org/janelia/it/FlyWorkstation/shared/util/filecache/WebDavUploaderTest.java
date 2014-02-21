package org.janelia.it.FlyWorkstation.shared.util.filecache;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * Tests the {@link WebDavUploader} class.
 *
 * @author Eric Trautman
 */
public class WebDavUploaderTest extends TestCase {

    private WebDavClient client;
    private WebDavUploader uploader;
    private File testRootParentDirectory;
    private File testNestedDirectory;
    private List<File> testFiles;

    public WebDavUploaderTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(WebDavUploaderTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        client = new WebDavClient(WebDavClient.JACS_WEBDAV_BASE_URL, 100, 100);
        client.setCredentials(WebDavClientTest.WEBDAV_TEST_USER_CREDENTIALS);
        uploader = new WebDavUploader(client, WebDavClientTest.JACS_WEBDAV_TEST_WRITE_ROOT_PATH);

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

    @Override
    protected void tearDown() throws Exception {

        LOG.info("tearDown: entry --------------------------------------");

        for (File file : testFiles) {
            CachedFileTest.deleteFile(file);
        }

        CachedFileTest.deleteFile(testNestedDirectory);
        CachedFileTest.deleteFile(testRootParentDirectory);

        LOG.info("tearDown: exit --------------------------------------");
    }

    public void testUploadFile() throws Exception {

        String remotePath = uploader.uploadFile(testFiles.get(0));
        Assert.assertNotNull("null path returned for file upload", remotePath);

        URL url = client.getWebDavUrl(remotePath);

        WebDavFile webDavFile = client.findFile(url);
        Assert.assertNotNull("uploaded file " + url + " missing from server", webDavFile);
    }

    public void testDerivePathsForUnix() throws Exception {
        commonTestDerivePaths("/a/b/c/root", true);
    }

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

        Assert.assertEquals("incorrect remote path map size, map=" + remotePathToFileMap,
                            localFilePaths.size(),
                            remotePathToFileMap.size());

        int index = 0;
        String expectedPath;
        for (String remotePath : remotePathToFileMap.keySet()) {
            expectedPath = remoteRoot + relativePaths.get(index);
            expectedPath = expectedPath.replace('\\', '/');
            Assert.assertEquals("incorrect file path derived", expectedPath, remotePath);
            index++;
        }

        Assert.assertEquals("incorrect directory list size, list=" + orderedDirectoryPaths,
                            expectedOrderedDirPaths.length,
                            orderedDirectoryPaths.size());

        for (int i = 0; i < expectedOrderedDirPaths.length; i++) {
            Assert.assertEquals("incorrect dir path " + i + " derived",
                                expectedOrderedDirPaths[i], orderedDirectoryPaths.get(i));
        }
    }

    public void testUploadFilesWithoutRelativePath() throws Exception {
        commonTestUploadFiles(null, 1);
    }

    public void testUploadFilesWithRelativePath() throws Exception {
        commonTestUploadFiles(testRootParentDirectory, 2);
    }

    private void commonTestUploadFiles(File localRootDirectory,
                                       int expectedDistinctParentPaths) throws Exception {
        String remotePath = uploader.uploadFiles(testFiles, localRootDirectory);
        Assert.assertNotNull("null path returned for upload", remotePath);

        URL url = client.getWebDavUrl(remotePath);

        List<WebDavFile> list = client.findAllInternalFiles(url);

        Assert.assertEquals("invalid number of files created under " + url,
                            testFiles.size(), list.size());

        Set<String> parentPathSet = new HashSet<String>();
        File file;
        for (WebDavFile webDavFile : list) {
            file = new File(webDavFile.getUrl().toURI().getPath());
            parentPathSet.add(file.getParent());
        }

        Assert.assertEquals("invalid number of distinct parent paths created " + parentPathSet,
                            expectedDistinctParentPaths, parentPathSet.size());
    }

    private static final Logger LOG = LoggerFactory.getLogger(WebDavUploaderTest.class);
}
