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
        String testStorageTags = "t1, t2";
        String testUploadContext = "WorkstationFileUpload";
        String testStorageUrl = "http://teststorage/" + testStorageName;
        File testFile = testFiles.get(0);
        
        Mockito.when(webDavClientMgr.createStorage(testStorageName, testUploadContext, testStorageTags))
                .thenReturn(testStorageUrl);
        Mockito.when(webDavClientMgr.urlEncodeComp(ArgumentMatchers.anyString()))
                .thenCallRealMethod();
        Mockito.when(webDavClientMgr.uploadFile(testFile, testStorageUrl, testFile.getName()))
                .then(invocation -> {
                    RemoteLocation remoteFile = new RemoteLocation(testFile.getName(), testFile.getAbsolutePath(), testStorageUrl + "/file/" + testFile.getName());
                    remoteFile.setStorageURL(invocation.getArgument(1));
                    return remoteFile;
                });
        RemoteLocation remoteFile = uploader.uploadFile(testStorageName, testUploadContext, testStorageTags, testFile);
        assertNotNull("null path returned for file upload", remoteFile);
        assertEquals(testStorageUrl, remoteFile.getStorageURL());
    }

    @Test
    public void uploadMultipleFiles() throws Exception {
        String testStorageName = "f1";
        String testStorageTags = "t1, t2";
        String testUploadContext = "WorkstationFileUpload";
        String testStorageUrl = "http://teststorage/" + testStorageName;
        Mockito.when(webDavClientMgr.createStorage(testStorageName, testUploadContext, testStorageTags))
                .thenReturn(testStorageUrl);
        Mockito.when(webDavClientMgr.urlEncodeComp(ArgumentMatchers.anyString()))
                .thenCallRealMethod();
        Mockito.when(webDavClientMgr.urlEncodeComps(ArgumentMatchers.anyString()))
                .thenCallRealMethod();
        Mockito.when(webDavClientMgr.uploadFile(ArgumentMatchers.any(File.class), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .then(invocation -> {
                    RemoteLocation remoteFile = new RemoteLocation(
                            ((File) invocation.getArgument(0)).getAbsolutePath(),
                            ((File) invocation.getArgument(0)).getAbsolutePath(),
                            invocation.getArgument(2));
                    remoteFile.setStorageURL(invocation.getArgument(1));
                    return remoteFile;
                });

        List<RemoteLocation> remoteFiles = uploader.uploadFiles(
                testStorageName,
                testUploadContext,
                testStorageTags,
                testFiles,
                testRootParentDirectory);

        assertNotNull("null path returned for file upload", remoteFiles);
        for (RemoteLocation rf : remoteFiles) {
            assertEquals(testStorageUrl, rf.getStorageURL());
        }

        Mockito.verify(webDavClientMgr).createDirectory(testStorageUrl, testNestedDirectory.getName());
        for (File f : testFiles) {
            String encodedUrl = webDavClientMgr.urlEncodeComps(testRootParentDirectory.toPath().relativize(f.toPath()).toString());
            Mockito.verify(webDavClientMgr).uploadFile(f, testStorageUrl, encodedUrl);
        }   
    }

}
