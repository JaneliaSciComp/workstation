package org.janelia.workstation.core.filecache;

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

    private StorageClientMgr storageClientMgr;
    private WebDavUploader uploader;
    private File testRootParentDirectory;
    private File testNestedDirectory;
    private List<File> testFiles;

    @Before
    public void setUp() throws Exception {
        storageClientMgr = Mockito.mock(StorageClientMgr.class);
        uploader = new WebDavUploader(storageClientMgr);

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
        
        Mockito.when(storageClientMgr.createStorage(testStorageName, testUploadContext, testStorageTags))
                .thenReturn(testStorageUrl);
        Mockito.when(storageClientMgr.urlEncodeComp(ArgumentMatchers.anyString()))
                .thenCallRealMethod();
        Mockito.when(storageClientMgr.uploadFile(testFile, testStorageUrl, testFile.getName()))
                .then(invocation -> {
                    RemoteLocation remoteFile = new RemoteLocation(testFile.getName(), testFile.getAbsolutePath(), testStorageUrl + "/file/" + testFile.getName());
                    remoteFile.setStorageURL(invocation.getArgument(1));
                    return remoteFile;
                });
        RemoteLocation remoteFile = uploader.uploadFile(testStorageName, testUploadContext, testStorageTags, testFile);
        assertNotNull("null path returned for file upload", remoteFile);
        assertEquals(testStorageUrl, remoteFile.getStorageURL());
    }

    // TODO: test is out of date, with syntax error
//    @Test
//    public void uploadMultipleFiles() throws Exception {
//        String testStorageName = "f1";
//        String testStorageTags = "t1, t2";
//        String testUploadContext = "WorkstationFileUpload";
//        String testStorageUrl = "http://teststorage/" + testStorageName;
//        Mockito.when(storageClientMgr.createStorage(testStorageName, testUploadContext, testStorageTags))
//                .thenReturn(testStorageUrl);
//        Mockito.when(storageClientMgr.urlEncodeComp(ArgumentMatchers.anyString()))
//                .thenCallRealMethod();
//        Mockito.when(storageClientMgr.urlEncodeComps(ArgumentMatchers.anyString()))
//                .thenCallRealMethod();
//        Mockito.when(storageClientMgr.uploadFile(ArgumentMatchers.any(File.class), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
//                .then(invocation -> {
//                    RemoteLocation remoteFile = new RemoteLocation(
//                            ((File) invocation.getArgument(0)).getAbsolutePath(),
//                            ((File) invocation.getArgument(0)).getAbsolutePath(),
//                            invocation.getArgument(2));
//                    remoteFile.setStorageURL(invocation.getArgument(1));
//                    return remoteFile;
//                });
//
//        List<RemoteLocation> remoteFiles = uploader.uploadFiles(
//                testStorageName,
//                testUploadContext,
//                testStorageTags,
//                testFiles,
//                testRootParentDirectory);
//
//        assertNotNull("null path returned for file upload", remoteFiles);
//        for (RemoteLocation rf : remoteFiles) {
//            assertEquals(testStorageUrl, rf.getStorageURL());
//        }
//
//        Mockito.verify(storageClientMgr).createDirectory(testStorageUrl, testNestedDirectory.getName());
//        for (File f : testFiles) {
//            String encodedUrl = storageClientMgr.urlEncodeComps(testRootParentDirectory.toPath().relativize(f.toPath()).toString());
//            Mockito.verify(storageClientMgr).uploadFile(f, testStorageUrl, encodedUrl);
//        }
//    }

}
