package org.janelia.it.FlyWorkstation.gui.framework.viewer.search;

/**
 * Relevant data to pass around among search-related GUI classes.
 */
public class SolrResultsMetaData {
    private long searchDuration;
    private int numHits;
    private int rawNumHits;
    private String queryStr;

    public long getSearchDuration() {
        return searchDuration;
    }

    public void setSearchDuration(long searchDuration) {
        this.searchDuration = searchDuration;
    }

    public int getNumHits() {
        return numHits;
    }

    public void setNumHits(int numHits) {
        this.numHits = numHits;
    }

    public String getQueryStr() {
        return queryStr;
    }

    public void setQueryStr(String queryStr) {
        this.queryStr = queryStr;
    }

    public int getRawNumHits() {
        return rawNumHits;
    }

    public void setRawNumHits(int rawNumHits) {
        this.rawNumHits = rawNumHits;
    }
}


