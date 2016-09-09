
package src.org.janelia.it.jacs.compute.service.search;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Apr 19, 2007
 * Time: 2:54:23 PM
 *
 */
public class SearchTests extends SearchTestBase {

    public SearchTests() {
        super();
    }

    public SearchTests(String name) {
        super(name);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(SubmitSearchTest.class);
        return suite;
    }

}
