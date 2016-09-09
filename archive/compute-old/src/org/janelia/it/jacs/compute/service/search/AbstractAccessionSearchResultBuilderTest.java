
package src.org.janelia.it.jacs.compute.service.search;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 12, 2007
 * Time: 1:01:36 PM
 *
 */
abstract public class AbstractAccessionSearchResultBuilderTest extends AbstractBaseAccessionSearchTest  {

    public AbstractAccessionSearchResultBuilderTest(String name) {
        super(name);
    }

    abstract protected AccessionSearchResultBuilder getCurrentAccessionResultBuilder();

    protected AccessionSearchResultBuilder.AccessionSearchResult retrieveAccessionSearchResult(String acc)
            throws Exception {
        List<AccessionSearchResultBuilder.AccessionSearchResult> accResults =
                getCurrentAccessionResultBuilder().retrieveAccessionSearchResult(acc,new Long(-1),getCurrentSession());
        if(accResults == null) {
            return null;
        } else {
            if(accResults.size() > 0) {
                return accResults.get(0);
            } else {
                return new AccessionSearchResultBuilder.AccessionSearchResult();
            }
        }
    }

}
