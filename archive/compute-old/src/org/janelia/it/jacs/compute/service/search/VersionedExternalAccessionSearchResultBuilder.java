
package src.org.janelia.it.jacs.compute.service.search;

import org.hibernate.SQLQuery;
import org.hibernate.Session;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Oct 30, 2007
 * Time: 3:30:30 PM
 */
class VersionedExternalAccessionSearchResultBuilder extends AccessionSearchResultBuilder {

    VersionedExternalAccessionSearchResultBuilder() {
    }

    protected SQLQuery createSearchQuery(Object[] accessionQueryParams, Session session) {
        String searchedAcc = (String) accessionQueryParams[0];
        String sql = "select " +
                "accession as accession," +
                "et.name," +
                "external_acc as docAccessionName " +
                "from " +
                "sequence_entity se, entity_type et " +
                "where upper(se.external_acc) in (:accession ) " +
                "and se.entity_type_code = et.code " +
                "order by obs_flag desc, " +
                "externalAccSortValue(external_acc)";
        SQLQuery sqlQuery = session.createSQLQuery(sql);
        _logger.debug("Versioned external accession search sql: " + sql + " for " + searchedAcc);
        // add the first 100 accessions to searcheable versioned external accessions 
        ArrayList accessionVersions = new ArrayList();
        for (int i = 0; i < 100; i++) {
            accessionVersions.add(searchedAcc.toUpperCase() + "." + String.valueOf(i + 1));
        }
        sqlQuery.setParameterList("accession", accessionVersions);
        sqlQuery.setMaxResults(1); // interested only in 1 match
        return sqlQuery;
    }

}
