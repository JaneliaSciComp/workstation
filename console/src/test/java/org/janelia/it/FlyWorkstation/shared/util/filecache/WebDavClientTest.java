package org.janelia.it.FlyWorkstation.shared.util.filecache;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;

/**
 * Tests the {@link WebDavClient} class.
 *
 * @author Eric Trautman
 */
public class WebDavClientTest extends TestCase {

    public static final String JACS_WEBDAV_TEST_ROOT_PATH = "/opt/jacs-webdav-test";
    public static final String JACS_WEBDAV_TEST_WRITE_ROOT_PATH = JACS_WEBDAV_TEST_ROOT_PATH + "/test-write";

    // --------------------------------------------------------
    // NOTE:
    //
    //   These credentials are maintained on the jacs server in
    //     /opt/jacs-webdav-test/auth/.htpasswd
    //
    //   The user file was created using the htpasswd utility.
    //   The Apache server configuration in /etc/httpd/conf.d/
    //   references the user file to limit access to the
    //   /opt/jacs-webdav-test/test-read and /opt/jacs-webdav-test/test-write
    //   directories.
    //
    //   This allows us to hard code the password here without
    //   worrying about security risks.
    // --------------------------------------------------------

    public static final UsernamePasswordCredentials WEBDAV_TEST_USER_CREDENTIALS =
            new UsernamePasswordCredentials("testuser", "testuser");

    private WebDavClient client;
    private URL testUrlWithoutSlash;
    private URL testUrlWithSlash;

    public WebDavClientTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(WebDavClientTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        client = new WebDavClient(WebDavClient.JACS_WEBDAV_BASE_URL, 100, 100);
        final String testHref = JACS_WEBDAV_TEST_ROOT_PATH + "/test-read/unit-test-files";
        testUrlWithoutSlash = client.getWebDavUrl(testHref);
        testUrlWithSlash = client.getWebDavUrl(testHref + "/");
    }

    public void testWithoutCredentials() throws Exception {
        try {
            client.findImmediateInternalFiles(testUrlWithSlash);
            Assert.fail("request for " + testUrlWithSlash +
                        " should have failed without credentials");
        } catch (WebDavException e) {
            Assert.assertEquals(
                    "invalid status code returned for unauthorized request",
                    new Integer(HttpStatus.SC_UNAUTHORIZED),
                    e.getStatusCode());
        }
    }

    public void testWithCredentials() throws Exception {

        client.setCredentials(WEBDAV_TEST_USER_CREDENTIALS);

        WebDavFile webDavFile = client.findFile(testUrlWithSlash);
        Assert.assertTrue("base directory should be identified as a directory",
                          webDavFile.isDirectory());

        Assert.assertEquals("invalid URL saved for base directory",
                            testUrlWithSlash, webDavFile.getUrl());

        final String etag = webDavFile.getEtag();
        Assert.assertNotNull("etag missing for base directory", etag);

        List<WebDavFile> fileList =
                client.findImmediateInternalFiles(testUrlWithSlash);
        final int immediateSize = fileList.size();

        Assert.assertTrue("no immediate files found for " + testUrlWithSlash,
                          immediateSize > 0);

        fileList = client.findAllInternalFiles(testUrlWithSlash);
        final int allSize = fileList.size();

        Assert.assertTrue("all file count (" + allSize +
                          ") is not greater than immediate file count (" +
                          immediateSize + ") for " + testUrlWithSlash,
                          allSize > immediateSize);

        Assert.assertTrue("base directory is not readable",
                          client.canReadDirectory(testUrlWithSlash));

        webDavFile = client.findFile(testUrlWithoutSlash);
        Assert.assertTrue(
                "base directory (without slash) should be identified as a directory",
                webDavFile.isDirectory());

        Assert.assertTrue(
                "isDirectory convenience method should have returned true for " + testUrlWithoutSlash,
                client.isDirectory(testUrlWithoutSlash));

        Assert.assertTrue(
                "isDirectory convenience method should have returned true for " + testUrlWithSlash,
                client.isDirectory(testUrlWithSlash));

        // ----------------------------------------
        // Test available check ...

        Assert.assertTrue(testUrlWithSlash + " should be available",
                          client.isAvailable(testUrlWithSlash));

        Assert.assertTrue(testUrlWithoutSlash + " should be available",
                          client.isAvailable(testUrlWithoutSlash));

        final URL missingUrl = new URL(testUrlWithSlash, "missing");
        Assert.assertFalse(missingUrl + " should NOT be available",
                           client.isAvailable(missingUrl));

        // ----------------------------------------
        // Test MKCOL and PUT ...

        Date now = new Date();
        final String contentsString = "This test was run on " + now + ".\n";
        ByteArrayInputStream testFileStream = new ByteArrayInputStream(contentsString.getBytes());
        final String rootUploadPath = JACS_WEBDAV_TEST_WRITE_ROOT_PATH;
        final String uploadDirectoryPath = client.getUniqueUploadDirectoryPath(rootUploadPath);
        final URL uploadDirectoryUrl = client.getWebDavUrl(uploadDirectoryPath);

        client.createDirectory(uploadDirectoryUrl);
        Assert.assertTrue(
                "isDirectory convenience method should have returned true for " + uploadDirectoryUrl,
                client.isDirectory(uploadDirectoryUrl));

        final URL writeTestUrl = client.getWebDavUrl(uploadDirectoryPath + "test.txt");
        client.saveFile(writeTestUrl, testFileStream);

        webDavFile = client.findFile(writeTestUrl);
        Assert.assertEquals(
                "invalid kilobyte size returned for " + writeTestUrl,
                1,
                webDavFile.getKilobytes());
    }

    public void testGetWebDavUrl() throws Exception {
        final String path = "/path with 25% /bad/ \\chars\\.txt";
        final String expectedUrlString =
                "http://jacs.int.janelia.org/WebDAV/path%20with%2025%25%20/bad/%20%5Cchars%5C.txt";
        final URL url = client.getWebDavUrl(path);
        Assert.assertEquals("invalid URL string returned for path '" + path + "'",
                            expectedUrlString,
                            url.toString());
    }

}
