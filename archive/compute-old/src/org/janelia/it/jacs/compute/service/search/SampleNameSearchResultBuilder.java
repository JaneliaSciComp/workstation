
package src.org.janelia.it.jacs.compute.service.search;

import org.hibernate.SQLQuery;
import org.hibernate.Session;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Oct 30, 2007
 * Time: 3:30:30 PM
 */
class SampleNameSearchResultBuilder extends AccessionSearchResultBuilder {

    SampleNameSearchResultBuilder() {
    }

    protected SQLQuery createSearchQuery(Object[] accessionQueryParams, Session session) {
        String searchedAcc = (String) accessionQueryParams[0];
        String sql = "select " +
                "sample_acc," +
                ":accessionType," +
                "sample_name " +
                "from " +
                "bio_sample sample " +
                "where upper(sample_name) = :accession ";
        SQLQuery sqlQuery = session.createSQLQuery(sql);
        _logger.debug("Sample name search sql: " + sql + " for " + searchedAcc);
        sqlQuery.setString("accessionType", "Sample");
        sqlQuery.setString("accession", searchedAcc.toUpperCase());
        return sqlQuery;
    }

}
