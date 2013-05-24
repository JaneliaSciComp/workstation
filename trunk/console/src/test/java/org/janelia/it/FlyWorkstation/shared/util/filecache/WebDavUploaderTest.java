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


        List<File> fileList = new ArrayList<File>();
        final String[] clientPaths = {
                "/a/b/c/root/d/e/f1.mip",
                "/a/b/c/root/d/e/f2.mip",
                "/a/b/c/root/d/f3.mip",
                "/a/b/c/root/f4.mip",
                "/a/b/c/root/g/f5.mip"
        };
        for (String path : clientPaths) {
            fileList.add(new File(path));
        }

        final String localPathRoot = "/a/b/c/root";
        final String remoteUploadDirectoryPath = "/remote/upload/123";
        Map<String, File> remotePathToFileMap = new HashMap<String, File>();
        List<String> orderedDirectoryPaths = new ArrayList<String>(fileList.size());

        uploader.derivePaths(fileList,
                             new File(localPathRoot),
                             remoteUploadDirectoryPath,
                             orderedDirectoryPaths,
                             remotePathToFileMap);

        Assert.assertEquals("incorrect remote path map size, map=" + remotePathToFileMap,
                            clientPaths.length,
                            remotePathToFileMap.size());

        File localFile;
        String expectedPath;
        for (String remotePath : remotePathToFileMap.keySet()) {
            localFile = remotePathToFileMap.get(remotePath);
            expectedPath = remoteUploadDirectoryPath + "/" +
                           localFile.getAbsolutePath().substring(localPathRoot.length() + 1);
            Assert.assertEquals("incorrect file path derived", expectedPath, remotePath);
        }

        final String[] expectedOrderedDirPaths = {
                "/remote/upload/123/",
                "/remote/upload/123/d/",
                "/remote/upload/123/d/e/",
                "/remote/upload/123/g/"
        };

        Assert.assertEquals("incorrect directory list size, list=" + orderedDirectoryPaths,
                            expectedOrderedDirPaths.length,
                            orderedDirectoryPaths.size());

        for (int i = 0; i < expectedOrderedDirPaths.length; i++) {
            Assert.assertEquals("incorrect dir path " + i + " derived",
                                expectedOrderedDirPaths[i], orderedDirectoryPaths.get(i));
        }
    }

    public void testDerivePathsForWindows() throws Exception {

        final String[] relativePaths = {
                "\\d\\e\\f1.mip",
                "\\d\\e\\f2.mip",
                "\\d\\f3.mip",
                "\\f4.mip",
                "\\g\\f5.mip"
        };

        final String[] expectedOrderedDirPaths = {
                "/remote/upload/123/",
                "/remote/upload/123/d/",
                "/remote/upload/123/d/e/",
                "/remote/upload/123/g/"
        };

        commonTestDerivePaths("C:\\a\\b\\c\\root",
                              relativePaths,
                              "/remote/upload/123",
                              expectedOrderedDirPaths);

    }

    private void commonTestDerivePaths(String localRoot,
                                       String[] relativePaths,
                                       String remoteRoot,
                                       String[] expectedOrderedDirPaths) throws Exception {


        List<File> fileList = new ArrayList<File>();

        for (String relativePath : relativePaths) {
            fileList.add(new File(localRoot + relativePath));
        }

        // use LHM to preserve order
        Map<String, File> remotePathToFileMap = new LinkedHashMap<String, File>();
        List<String> orderedDirectoryPaths = new ArrayList<String>(fileList.size());

        uploader.derivePaths(fileList,
                             new File(localRoot),
                             remoteRoot,
                             orderedDirectoryPaths,
                             remotePathToFileMap);

        Assert.assertEquals("incorrect remote path map size, map=" + remotePathToFileMap,
                            fileList.size(),
                            remotePathToFileMap.size());

        int index = 0;
        String expectedPath;
        for (String remotePath : remotePathToFileMap.keySet()) {
            expectedPath = remoteRoot + relativePaths[index];
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
