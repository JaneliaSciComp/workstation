
package src.org.janelia.it.jacs.compute.service.search;

import org.hibernate.SQLQuery;
import org.hibernate.Session;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Oct 30, 2007
 * Time: 3:30:30 PM
 */
class CoreClusterAccessionSearchResultBuilder extends AccessionSearchResultBuilder {

    CoreClusterAccessionSearchResultBuilder() {
    }

    protected SQLQuery createSearchQuery(Object[] accessionQueryParams, Session session) {
        String searchedAcc = (String) accessionQueryParams[0];
        String sql = "select " +
                "core_cluster_acc," +
                ":accessionType," +
                "core_cluster_acc as docAccessionName " +
                "from " +
                "core_cluster cc " +
                "where core_cluster_acc = :accession ";
        SQLQuery sqlQuery = session.createSQLQuery(sql);
        _logger.debug("Core cluster accession search sql: " + sql + " for " + searchedAcc);
        sqlQuery.setString("accessionType", "Core cluster");
        sqlQuery.setString("accession", searchedAcc.toUpperCase());
        return sqlQuery;
    }

}
