
package src.org.janelia.it.jacs.compute.service.search;

import org.hibernate.SQLQuery;
import org.hibernate.Session;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Oct 30, 2007
 * Time: 3:30:30 PM
 */
class PublicationAccessionSearchResultBuilder extends AccessionSearchResultBuilder {

    PublicationAccessionSearchResultBuilder() {
    }

    protected SQLQuery createSearchQuery(Object[] accessionQueryParams, Session session) {
        String searchedAcc = (String) accessionQueryParams[0];
        String sql = "select " +
                "publication_acc," +
                ":accessionType," +
                "title " +
                "from " +
                "publication pub " +
                "where upper(publication_acc) = :accession ";
        SQLQuery sqlQuery = session.createSQLQuery(sql);
        _logger.debug("Publication accession search sql: " + sql + " for " + searchedAcc);
        sqlQuery.setString("accessionType", "Publication");
        sqlQuery.setString("accession", searchedAcc.toUpperCase());
        return sqlQuery;
    }

}
