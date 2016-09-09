
package src.org.janelia.it.jacs.compute.service.search;

import org.hibernate.SQLQuery;
import org.hibernate.Session;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Oct 30, 2007
 * Time: 3:30:30 PM
 */
class NCBIGIAccessionSearchResultBuilder extends AccessionSearchResultBuilder {

    NCBIGIAccessionSearchResultBuilder() {
    }

    List<AccessionSearchResult> retrieveAccessionSearchResult(String acc,
                                                              Long searchResultNodeId,
                                                              Session session)
            throws Exception {
        Integer ncbiGINumber = getGINumber(acc);
        if (ncbiGINumber == null) {
            return null;
        }
        SQLQuery sqlQuery = createSearchQuery(new Object[]{ncbiGINumber}, session);
        return querySearchResult(acc, searchResultNodeId, sqlQuery);
    }

    protected SQLQuery createSearchQuery(Object[] accessionQueryParams, Session session) {
        Integer searchedAcc = (Integer) accessionQueryParams[0];
        String sql = "select " +
                "accession as accession," +
                "et.name," +
                "cast(ncbi_gi_number as varchar) as docAccessionName " +
                "from " +
                "sequence_entity se, entity_type et " +
                "where se.ncbi_gi_number = :accession  " +
                "and se.entity_type_code = et.code ";
        SQLQuery sqlQuery = session.createSQLQuery(sql);
        _logger.debug("NCBI GI accession search sql: " + sql + " for " + searchedAcc);
        sqlQuery.setInteger("accession", searchedAcc);
        sqlQuery.setMaxResults(1); // interested only in 1 match
        return sqlQuery;
    }

    /**
     * An accession is consider a valid NCBI GI number if it starts with "gi|" or "ti|" and
     * all the other characters are digits or if it contains only digit characters
     *
     * @param acc accession's value
     * @return true if the accession is a valid NCBI GI number
     */
    private Integer getGINumber(String acc) {
        Integer giNumber = null;
        if (acc.toLowerCase().startsWith("gi|") ||
                acc.toLowerCase().startsWith("ti|")) {
            acc = acc.substring(3);
            if (acc.length() == 0) {
                // if it only contains "gi|" or "ti|" we consider that invalid for searches
                return giNumber;
            }
        }
        int accLength = acc.length();
        for (int i = 0; i < accLength; i++) {
            if (!Character.isDigit(acc.charAt(i))) {
                return giNumber;
            }
        }
        try {
            giNumber = Integer.valueOf(acc);
        }
        catch (NumberFormatException e) {
            // cannot be converted to integer - too large to be GI number
            giNumber = null;
        }
        return giNumber;
    }

}
