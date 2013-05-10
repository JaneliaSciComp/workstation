package org.janelia.it.FlyWorkstation.shared.util.filecache;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the {@link WebDavUploader} class.
 *
 * @author Eric Trautman
 */
public class WebDavUploaderTest extends TestCase {

    private WebDavClient client;
    private WebDavUploader uploader;
    private File testRootParentDirectory;
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
        final String path = testRootParentDirectory.getAbsolutePath();
        if (testRootParentDirectory.mkdir()) {
            LOG.info("setUp: created " + path);
        } else {
            throw new IllegalStateException("failed to create " + path);
        }

        testFiles = new ArrayList<File>();
        testFiles.add(CachedFileTest.createFile(testRootParentDirectory, 1));
    }

    @Override
    protected void tearDown() throws Exception {

        LOG.info("tearDown: entry --------------------------------------");

        for (File file : testFiles) {
            CachedFileTest.deleteFile(file);
        }

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

    private static final Logger LOG = LoggerFactory.getLogger(WebDavUploaderTest.class);
}
