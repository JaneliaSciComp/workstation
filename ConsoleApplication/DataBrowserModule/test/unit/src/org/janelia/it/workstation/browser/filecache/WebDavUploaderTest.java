package org.janelia.it.workstation.browser.filecache;

import org.janelia.it.jacs.model.TestCategories;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests the {@link WebDavUploader} class.
 *
 * @author Eric Trautman
 */
@Category(TestCategories.FastTests.class)
public class WebDavUploaderTest {
    private static final Logger LOG = LoggerFactory.getLogger(WebDavUploaderTest.class);

    private WebDavClientMgr webDavClientMgr;
    private WebDavUploader uploader;
    private File testRootParentDirectory;
    private File testNestedDirectory;
    private List<File> testFiles;

    @Before
    public void setUp() throws Exception {
        webDavClientMgr = Mockito.mock(WebDavClientMgr.class);
        uploader = new WebDavUploader(webDavClientMgr);

        final String ts = TestFileUtils.buildTimestampName();
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

        testFiles = new ArrayList<>();
        testFiles.add(TestFileUtils.createFile(testRootParentDirectory, 1));
        testFiles.add(TestFileUtils.createFile(testRootParentDirectory, 1));
        testFiles.add(TestFileUtils.createFile(testNestedDirectory, 1));
    }

    @After
    public void tearDown() {
        for (File file : testFiles) {
            TestFileUtils.deleteFile(file);
        }

        TestFileUtils.deleteFile(testNestedDirectory);
        TestFileUtils.deleteFile(testRootParentDirectory);
    }

    @Test
    public void uploadASingleFile() throws Exception {
        String testStorageName = "f1";
        String testStorageUrl = "http://teststorage/" + testStorageName;
        File testFile = testFiles.get(0);
        
        Mockito.when(webDavClientMgr.createStorageFolder(testStorageName))
                .thenReturn(testStorageUrl);
        Mockito.when(webDavClientMgr.uploadFile(testFile, testStorageUrl, ""))
                .thenReturn(testStorageUrl + "/" + testFile.getName());
        
        String remoteUrl = uploader.uploadFile(testStorageName, testFile);
        assertNotNull("null path returned for file upload", remoteUrl);
        assertEquals(testStorageUrl + "/" + testFile.getName(), remoteUrl);
    }

    @Test
    public void uploadMultipleFiles() throws Exception {
        String testStorageName = "f1";
        String testStorageUrl = "http://teststorage/" + testStorageName;
        Mockito.when(webDavClientMgr.createStorageFolder(testStorageName))
                .thenReturn(testStorageUrl);

        Mockito.when(webDavClientMgr.urlEncodeComp(ArgumentMatchers.anyString()))
                .then(invocation -> invocation.getArgument(0));
        Mockito.when(webDavClientMgr.urlEncodeComps(ArgumentMatchers.anyString()))
                .then(invocation -> invocation.getArgument(0));

        String remoteUrl = uploader.uploadFiles(
                testStorageName, 
                testFiles,
                testRootParentDirectory);

        assertNotNull("null path returned for file upload", remoteUrl);
        assertEquals(testStorageUrl, remoteUrl);
        
        Mockito.verify(webDavClientMgr).createDirectory(testStorageUrl, testNestedDirectory.getName());
        for (File f : testFiles) {
            Mockito.verify(webDavClientMgr).uploadFile(f, testStorageUrl, 
                    testRootParentDirectory.toPath().relativize(f.toPath()).toString());
            
        }
        
    }

//
//        final String[] expectedOrderedDirPaths = {
//                "/remote/upload/123/d/",
//                "/remote/upload/123/d/e/",
//                "/remote/upload/123/g/"
//        };
//
//        List<String> relativePaths = new ArrayList<>();
//        for (String unixPath : relativeUnixPaths) {
//            if (isUnix) {
//                relativePaths.add(unixPath);
//            } else {
//                relativePaths.add(unixPath.replace('/', '\\'));
//            }
//        }
//
//        List<String> localFilePaths = new ArrayList<>();
//        for (String relativePath : relativePaths) {
//            localFilePaths.add(localRoot + relativePath);
//        }
//
//        // use LHM to preserve order
//        Map<String, File> remotePathToFileMap = new LinkedHashMap<>();
//        List<String> orderedDirectoryPaths = new ArrayList<>();
//
//        uploader.derivePaths(localRootPathWithTrailingSeparator,
//                             localFilePaths,
//                             remoteRootWithTrailingSeparator,
//                             orderedDirectoryPaths,
//                             remotePathToFileMap);
//
//        assertEquals("incorrect remote path map size, map=" + remotePathToFileMap,
//                localFilePaths.size(),
//                remotePathToFileMap.size());
//
//        int index = 0;
//        String expectedPath;
//        for (String remotePath : remotePathToFileMap.keySet()) {
//            expectedPath = remoteRoot + relativePaths.get(index);
//            expectedPath = expectedPath.replace('\\', '/');
//            assertEquals("incorrect file path derived", expectedPath, remotePath);
//            index++;
//        }
//
//        assertEquals("incorrect directory list size, list=" + orderedDirectoryPaths,
//                expectedOrderedDirPaths.length,
//                orderedDirectoryPaths.size());
//
//        for (int i = 0; i < expectedOrderedDirPaths.length; i++) {
//            assertEquals("incorrect dir path " + i + " derived",
//                    expectedOrderedDirPaths[i], orderedDirectoryPaths.get(i));
//        }
//    }
//
//    @Test
//    @Category(TestCategories.SlowIntegrationTests.class)
//    public void testUploadFilesWithoutRelativePath() throws Exception {
//        commonTestUploadFiles(null, 1);
//    }
//
//    @Test
//    @Category(TestCategories.SlowIntegrationTests.class)
//    public void testUploadFilesWithRelativePath() throws Exception {
//        commonTestUploadFiles(testRootParentDirectory, 2);
//    }
//
//    private void commonTestUploadFiles(File localRootDirectory,
//                                       int expectedDistinctParentPaths) throws Exception {
//        String remotePath = uploader.uploadFiles(testFiles, localRootDirectory);
//        assertNotNull("null path returned for upload", remotePath);
//
//        URL url = client.getWebDavUrl(remotePath);
//
//        List<WebDavFile> list = client.findAllInternalFiles(url);
//
//        assertEquals("invalid number of files created under " + url, testFiles.size(), list.size());
//
//        Set<String> parentPathSet = new HashSet<>();
//        File file;
//        for (WebDavFile webDavFile : list) {
//            file = new File(webDavFile.getUrl().toURI().getPath());
//            parentPathSet.add(file.getParent());
//        }
//
//        assertEquals("invalid number of distinct parent paths created " + parentPathSet,
//                expectedDistinctParentPaths, parentPathSet.size());
//    }
//
//    private static final Logger LOG = LoggerFactory.getLogger(WebDavUploaderTest.class);
}
