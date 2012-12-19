package org.janelia.it.FlyWorkstation.shared.util.filecache;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.net.URL;

/**
 * Tests the {@link WebDavPathMap} class.
 *
 * @author Eric Trautman
 */
public class WebDavPathMapTest extends TestCase {

    public WebDavPathMapTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(WebDavPathMapTest.class);
    }

    public void testGetUrl() throws Exception {

        final String fileSharePrefix1 = "/groups/scicomp/jacsData";
        final String rootUrlName1 = "http://jacs.int.janelia.org/remote/jacsData";
        final String fileSharePrefix2 = "/groups/flylight/flylight/polarity";
        final String rootUrlName2 = "http://jacs.int.janelia.org/remote/polarity";
        final String fileSharePrefix3 = "/groups/flylight/flylight/flip";
        final String rootUrlName3 = "http://jacs.int.janelia.org/remote/flip";

        WebDavPathMap map = new WebDavPathMap();
        map.addFileShare(fileSharePrefix1, new URL(rootUrlName1));
        map.addFileShare(fileSharePrefix2, new URL(rootUrlName2));
        map.addFileShare(fileSharePrefix3 + "/", new URL(rootUrlName3 + "/"));

        validatePath(fileSharePrefix1,
                     "/one/two/three.jpg",
                     rootUrlName1,
                     map);

        validatePath(fileSharePrefix2,
                     "/aaa/bbb/ccc/ddd.jpg",
                     rootUrlName2,
                     map);

        validatePath(fileSharePrefix3,
                     "/q",
                     rootUrlName3,
                     map);
    }

    private void validatePath(String prefix,
                              String relativePath,
                              String rootUrlName,
                              WebDavPathMap map ) {

        String testPath = prefix + relativePath;
        String expectedUrlString = rootUrlName + relativePath;

        URL url = map.getUrl(testPath);
        Assert.assertEquals("invalid URL returned for " + testPath,
                expectedUrlString, String.valueOf(url));
    }
}
