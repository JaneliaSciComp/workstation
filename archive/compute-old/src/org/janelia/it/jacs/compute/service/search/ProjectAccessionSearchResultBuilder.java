
package src.org.janelia.it.jacs.compute.service.search;

import org.hibernate.SQLQuery;
import org.hibernate.Session;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Oct 30, 2007
 * Time: 3:30:30 PM
 */
class ProjectAccessionSearchResultBuilder extends AccessionSearchResultBuilder {
    ProjectAccessionSearchResultBuilder() {
    }

    protected SQLQuery createSearchQuery(Object[] accessionQueryParams, Session session) {
        String searchedAcc = (String) accessionQueryParams[0];
        String sql = "select " +
                "symbol," +
                ":accessionType," +
                "name " +
                "from " +
                "project proj " +
                "where :accession in (upper(symbol),upper(substring(symbol from 10)))";
        SQLQuery sqlQuery = session.createSQLQuery(sql);
        _logger.debug("Project accession search sql: " + sql + " for " + searchedAcc);
        sqlQuery.setString("accessionType", "Project");
        sqlQuery.setString("accession", searchedAcc.toUpperCase());
        return sqlQuery;
    }

}
