package org.janelia.it.FlyWorkstation.shared.util.filecache;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.http.HttpStatus;

import java.net.URL;
import java.util.List;

/**
 * Tests the {@link WebDavClient} class.
 *
 * @author Eric Trautman
 */
public class WebDavClientTest extends TestCase {

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
        client = new WebDavClient("http://jacs.int.janelia.org/WebDAV", 100, 100);
        final String testHref = "/opt/jacs-webdav-test/unit-test-files";
        testUrlWithoutSlash = client.getWebDavUrl(testHref);
        testUrlWithSlash = client.getWebDavUrl(testHref + "/");
    }

    public void testWithoutCredentials() throws Exception {
        try {
            client.findImmediateInternalFiles(testUrlWithSlash);
            Assert.fail("request for " + testUrlWithSlash +
                        " should have failed without credentials");
        } catch (WebDavRetrievalException e) {
            Assert.assertEquals(
                    "invalid status code returned for unauthorized request",
                    new Integer(HttpStatus.SC_UNAUTHORIZED),
                    e.getStatusCode());
        }
    }

    public void testWithCredentials() throws Exception {

        // --------------------------------------------------------
        // NOTE:
        //
        //   These credentials are maintained on the jacs server in
        //     /opt/jacs-webdav-test-auth/.htpasswd
        //
        //   The user file was created using the htpasswd utility.
        //   The Apache server configuration in /etc/httpd/conf.d/
        //   references the user file to limit access to the
        //   /opt/jacs-webdav-test/ directory.
        //
        //   This allows us to hard code the password here without
        //   worrying about security risks.
        // --------------------------------------------------------

        final UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials("testuser",
                                                "testuser");
        client.setCredentials(credentials);

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
    }

}
