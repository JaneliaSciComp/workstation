
package src.org.janelia.it.jacs.compute.service.search;

import org.hibernate.SQLQuery;
import org.hibernate.Session;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Oct 30, 2007
 * Time: 3:30:30 PM
 */
class SequenceAccessionSearchResultBuilder extends AccessionSearchResultBuilder {

    SequenceAccessionSearchResultBuilder() {
    }

    protected SQLQuery createSearchQuery(Object[] accessionQueryParams, Session session) {
        String searchedAcc = (String) accessionQueryParams[0];
        String sql = "select " +
                "accession as accession," +
                "et.name," +
                "accession as docAccessionName " +
                "from " +
                "sequence_entity se, entity_type et " +
                "where upper(accession) = :accession  " +
                "and se.entity_type_code = et.code ";
        SQLQuery sqlQuery = session.createSQLQuery(sql);
        _logger.debug("sequence accession search sql: " + sql + " for " + searchedAcc);
        sqlQuery.setString("accession", searchedAcc.toUpperCase());
        return sqlQuery;
    }

}
