package org.janelia.it.workstation.shared.util.filecache;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.http.HttpStatus;
import org.janelia.it.jacs.model.TestCategories;
import org.janelia.it.workstation.shared.util.filecache.WebDavException;
import org.janelia.it.workstation.shared.util.filecache.WebDavFile;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests the {@link WebDavClient} class.
 *
 * @author Eric Trautman
 */
public class WebDavClientTest {

    public static final String JACS_WEBDAV_TEST_ROOT_PATH = "/opt/jacs-webdav-test";
    public static final String JACS_WEBDAV_TEST_WRITE_ROOT_PATH = JACS_WEBDAV_TEST_ROOT_PATH + "/test-write";

    // --------------------------------------------------------
    // NOTE:
    //
    //   These credentials are maintained on the jacs-webdav server in
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

    @Before
    public void setUp() throws Exception {
        client = new WebDavClient(WebDavClient.JACS_WEBDAV_BASE_URL, 100, 100);
        final String testHref = JACS_WEBDAV_TEST_ROOT_PATH + "/test-read/unit-test-files";
        testUrlWithoutSlash = client.getWebDavUrl(testHref);
        testUrlWithSlash = client.getWebDavUrl(testHref + "/");
    }

    @Test
    @Category(TestCategories.FastIntegrationTests.class)
    public void testWithoutCredentials() throws Exception {
        try {
            client.findImmediateInternalFiles(testUrlWithSlash);
            fail("request for " + testUrlWithSlash + " should have failed without credentials");
        } catch (WebDavException e) {
            assertEquals("invalid status code returned for unauthorized request",
                         new Integer(HttpStatus.SC_UNAUTHORIZED),
                         e.getStatusCode());
        }
    }

    @Test
    @Category(TestCategories.SlowIntegrationTests.class)
    public void testWithCredentials() throws Exception {

        client.setCredentials(WEBDAV_TEST_USER_CREDENTIALS);

        WebDavFile webDavFile = client.findFile(testUrlWithSlash);
        assertTrue("base directory should be identified as a directory", webDavFile.isDirectory());

        assertEquals("invalid URL saved for base directory", testUrlWithSlash, webDavFile.getUrl());

        final String etag = webDavFile.getEtag();
        assertNotNull("etag missing for base directory", etag);

        List<WebDavFile> fileList =
                client.findImmediateInternalFiles(testUrlWithSlash);
        final int immediateSize = fileList.size();

        assertTrue("no immediate files found for " + testUrlWithSlash, immediateSize > 0);

        fileList = client.findAllInternalFiles(testUrlWithSlash);
        final int allSize = fileList.size();

        assertTrue("all file count (" + allSize + ") is not greater than immediate file count (" +
                        immediateSize + ") for " + testUrlWithSlash,
                allSize > immediateSize
        );

        assertTrue("base directory is not readable", client.canReadDirectory(testUrlWithSlash));

        webDavFile = client.findFile(testUrlWithoutSlash);
        assertTrue("base directory (without slash) should be identified as a directory",
                webDavFile.isDirectory());

        assertTrue("isDirectory convenience method should have returned true for " + testUrlWithoutSlash,
                client.isDirectory(testUrlWithoutSlash));

        assertTrue("isDirectory convenience method should have returned true for " + testUrlWithSlash,
                client.isDirectory(testUrlWithSlash));

        // ----------------------------------------
        // Test available check ...

        assertTrue(testUrlWithSlash + " should be available", client.isAvailable(testUrlWithSlash));

        assertTrue(testUrlWithoutSlash + " should be available", client.isAvailable(testUrlWithoutSlash));

        final URL missingUrl = new URL(testUrlWithSlash, "missing");
        assertFalse(missingUrl + " should NOT be available", client.isAvailable(missingUrl));

        // ----------------------------------------
        // Test MKCOL and PUT ...

        Date now = new Date();
        final String contentsString = "This test was run on " + now + ".\n";
        ByteArrayInputStream testFileStream = new ByteArrayInputStream(contentsString.getBytes());
        final String rootUploadPath = JACS_WEBDAV_TEST_WRITE_ROOT_PATH;
        final String uploadDirectoryPath = client.getUniqueUploadDirectoryPath(rootUploadPath);
        final URL uploadDirectoryUrl = client.getWebDavUrl(uploadDirectoryPath);

        client.createDirectory(uploadDirectoryUrl);
        assertTrue("isDirectory convenience method should have returned true for " + uploadDirectoryUrl,
                client.isDirectory(uploadDirectoryUrl));

        final URL writeTestUrl = client.getWebDavUrl(uploadDirectoryPath + "test.txt");
        client.saveFile(writeTestUrl, testFileStream);

        webDavFile = client.findFile(writeTestUrl);
        assertEquals("invalid kilobyte size returned for " + writeTestUrl, 1, webDavFile.getKilobytes());
    }

    @Test
    @Category(TestCategories.FastTests.class)
    public void testGetWebDavUrl() throws Exception {
        final String path = "/path with 25% /bad/ \\chars\\.txt";
        final String expectedUrlString = WebDavClient.JACS_WEBDAV_BASE_URL +
                                         "/path%20with%2025%25%20/bad/%20%5Cchars%5C.txt";
        final URL url = client.getWebDavUrl(path);
        assertEquals("invalid URL string returned for path '" + path + "'", expectedUrlString, url.toString());
    }

}
