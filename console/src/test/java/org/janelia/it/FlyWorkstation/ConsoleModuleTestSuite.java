
package org.janelia.it.FlyWorkstation;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.janelia.it.FlyWorkstation.shared.util.filecache.CachedFileTest;
import org.janelia.it.FlyWorkstation.shared.util.filecache.LocalFileCacheTest;
import org.janelia.it.FlyWorkstation.shared.util.filecache.WebDavClientTest;

public class ConsoleModuleTestSuite extends TestSuite {

    /**
     * Developers are supposed to add their unit tests to this suite() method
     * in order to be automatically executed by CruiseControl
     * @return suite of all test classes.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(CachedFileTest.class);
        suite.addTestSuite(LocalFileCacheTest.class);
        suite.addTestSuite(WebDavClientTest.class);
        return suite;
    }


    /**
     * Main method which gets executed by CruiseControl
     * @param args not used.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}
