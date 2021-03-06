package org.janelia.workstation.core.model.results;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.janelia.workstation.core.model.search.DomainObjectResultPage;
import org.janelia.workstation.core.model.search.DomainObjectSearchResults;

/**
 * Domain objects backed by a node children query.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NodeQueryResults extends DomainObjectSearchResults {

    private final NodeQueryConfiguration searchConfig;

    public NodeQueryResults(NodeQueryConfiguration queryConfiguration, DomainObjectResultPage firstPage) {
        super(firstPage);
        this.searchConfig = queryConfiguration;
    }

    @Override
    public DomainObjectResultPage getPage(int page) throws Exception {
        DomainObjectResultPage resultPage = super.getPage(page);
        if (resultPage==null) {
            resultPage = searchConfig.performSearch(page);
            setPage(page, resultPage);
        }
        return resultPage;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("nodeId", searchConfig.getNode() == null ? null : searchConfig.getNode().getId())
                .append("pages", pages)
                .append("loadedPages", loadedPages)
                .append("numTotalResults", numTotalResults)
                .append("numLoadedResults", numLoadedResults)
                .toString();
    }
}
