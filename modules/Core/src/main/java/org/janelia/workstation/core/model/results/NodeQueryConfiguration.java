package org.janelia.workstation.core.model.results;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.workspace.Node;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.model.search.DomainObjectResultPage;
import org.perf4j.LoggingStopWatch;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

/**
 * A configuration for querying a node's children with pagination.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NodeQueryConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NodeQueryConfiguration.class);

    private static final boolean LOG_TIME_ELAPSED = false;

    // Source state
    private final Node node;
    private final int pageSize;
    private String sortCriteria = "+id";

    // Options
    private boolean fetchAnnotations = true;

    public NodeQueryConfiguration(Node node, int pageSize) {
        this.node = node;
        this.pageSize = pageSize;
    }

    public Node getNode() {
        return node;
    }

    public int getPageSize() {
        return pageSize;
    }

    public String getSortCriteria() {
        return sortCriteria;
    }

    public void setSortCriteria(String sortCriteria) {
        this.sortCriteria = sortCriteria;
    }

    public boolean isFetchAnnotations() {
        return fetchAnnotations;
    }

    public void setFetchAnnotations(boolean fetchAnnotations) {
        this.fetchAnnotations = fetchAnnotations;
    }

    /**
     * Perform the initial query and return a control object which can be used to fetch additional pages.
     * @return
     * @throws Exception
     */
    public NodeQueryResults performSearch() throws Exception {
        DomainObjectResultPage firstPage = performSearch(0);
        return new NodeQueryResults(this, firstPage);
    }

    /**
     * Perform a query for a given page.
     * @param page the number of the page to return (zero-indexed)
     * @return the result page
     * @throws Exception
     */
    DomainObjectResultPage performSearch(int page) throws Exception {

        StopWatch stopWatch = new LoggingStopWatch();

        if (SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("SearchConfiguration.performSearch called in the EDT");
        }

        if (LOG_TIME_ELAPSED) stopWatch.lap("performNodeQuery");

        DomainModel model = DomainMgr.getDomainMgr().getModel();
        List<DomainObject> pageObjects = model.getChildren(node, sortCriteria, page, pageSize);
        List<Annotation> annotations = fetchAnnotations
                ? model.getAnnotations(DomainUtils.getReferences(pageObjects))
                : Collections.emptyList();

        log.info("Node query found {} objects. Current page ({}) includes {} objects and {} annotations.",
                node.getNumChildren(), page, pageObjects.size(), annotations.size());

        if (LOG_TIME_ELAPSED) stopWatch.stop("performNodeQuery");

        return new DomainObjectResultPage(pageObjects, annotations, node.getNumChildren());
    }



}
