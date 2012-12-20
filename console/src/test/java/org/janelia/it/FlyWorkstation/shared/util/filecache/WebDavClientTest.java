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
    private URL testUrl;

    public WebDavClientTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(WebDavClientTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        client = new WebDavClient(100, 100);
        testUrl = new URL("http://jacs.int.janelia.org/WebDAV/opt/jacs-webdav-test/");
    }

    public void testWithoutCredentials() throws Exception {
        try {
            client.findImmediateInternalFiles(testUrl);
            Assert.fail("request for " + testUrl +
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
        //     /opt/jacs-webdav-test/.htpasswd
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

        List<WebDavFile> fileList =
                client.findImmediateInternalFiles(testUrl);
        final int immediateSize = fileList.size();

        Assert.assertTrue("no immediate files found for " + testUrl,
                          immediateSize > 0);

        fileList = client.findAllInternalFiles(testUrl);
        final int allSize = fileList.size();

        Assert.assertTrue("all file count (" + allSize +
                          ") is not greater than immediate file count (" +
                          immediateSize + ") for " + testUrl,
                          allSize > immediateSize);
    }

}
