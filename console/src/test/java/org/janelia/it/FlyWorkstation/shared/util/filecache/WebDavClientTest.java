package org.janelia.it.FlyWorkstation.shared.util.filecache;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * Tests the {@link WebDavClient} class.
 *
 * @author Eric Trautman
 */
public class WebDavClientTest extends TestCase {

    private UsernamePasswordCredentials credentials;

    public WebDavClientTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(WebDavClientTest.class);
    }

    @Override
    protected void setUp() throws Exception {

        // TODO: set up test WebDAV directory that does not require authentication

        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(new File("/tmp/creds")));
            final String username = in.readLine();
            final String password = in.readLine();
            this.credentials = new UsernamePasswordCredentials(username,
                                                               password);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testFind() throws Exception {

        WebDavClient client = new WebDavClient(100, 100);
        client.setCredentials(credentials);

        URL url = new URL("http://jacs.int.janelia.org/jacsData/bin/");

        List<WebDavFile> fileList = client.findImmediateInternalFiles(url);
        Collections.sort(fileList, WebDavFile.LENGTH_COMPARATOR);
        LOG.info("fileList: {}", fileList);

        fileList = client.findAllInternalFiles(url);
        LOG.info("fileList: {}", fileList);
    }

    private static final Logger LOG =
            LoggerFactory.getLogger(WebDavClientTest.class);
}
