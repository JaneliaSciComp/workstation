package org.janelia.it.workstation.gui.dialogs.search.alignment_board;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.model.domain.Sample;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentContext;
import org.janelia.it.jacs.shared.solr.SolrQueryBuilder;
import org.janelia.it.jacs.shared.solr.SolrResults;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;
import org.janelia.it.workstation.gui.browser.exchange.DomainObjectReceiver;
import org.janelia.it.workstation.gui.framework.viewer.search.SolrResultsMetaData;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This "fields" requests to search, and deposits the results back into the param's receiver.
 *
 * Created by fosterl on 1/30/14.
 */
public class SearchWorker extends SimpleWorker {

    public static final String SEARCH_HISTORY_MDL_PROP = "ABTargetedSearchDialog_SEARCH_HISTORY";
    private static final int MAX_RESULT_ROWS = 200;
    private static final int MAX_QUERY_ROWS = 3500; //20000;

    private Logger logger = LoggerFactory.getLogger(ABTargetedSearchDialog.class);
    private SearchWorkerParam param;
    private List<DomainObject> rootedResults;
    private SolrResultsMetaData resultsMetaData;
    private SolrQueryBuilder queryBuilder;
    private AlignmentBoardContext context;

    private int maxQueryRows = MAX_QUERY_ROWS;

    public SearchWorker( SearchWorkerParam param, SolrQueryBuilder queryBuilder, AlignmentBoardContext context ) {
        this.param = param;
        this.queryBuilder = queryBuilder;
        this.context = context;
    }

    public void setMaxQueryRows( int maxQueryRows ) {
        this.maxQueryRows = maxQueryRows;
    }

    @Override
    protected void doStuff() throws Exception {
        if ( param.getSearchRootId() != null ) {
            queryBuilder.setRootId( param.getSearchRootId() );
        }

        Map<String,Set<String>> filters = new HashMap<>();
        Set<String> filterValues = new HashSet<>();
        filterValues.add( "Sample" );
        filterValues.add( "Neuron Fragment" );
        filters.put( "entity_type", filterValues );

        queryBuilder.setFilters( filters );
        SolrQuery query = queryBuilder.getQuery();
        query.setStart( param.getStartingRow() );
        query.setRows(maxQueryRows);

        SolrResults results = ModelMgr.getModelMgr().searchSolr( query );
        List<DomainObject> resultList = null; //results.getResultList();
        rootedResults = getCompatibleRootedEntities( resultList );

        resultsMetaData = new SolrResultsMetaData();
        resultsMetaData.setNumHits( rootedResults.size() );
        resultsMetaData.setRawNumHits( resultList.size() );
        resultsMetaData.setSearchDuration(
                results.getResponse().getElapsedTime()
        );
    }

    @Override
    protected void hadSuccess() {
        // Accept results and populate.
        DomainObjectReceiver receiver = param.getReceiver();
        receiver.setDomainObjects(
                rootedResults,
                resultsMetaData
        );
    }

    @Override
    protected void hadError(Throwable error) {
        param.getErrorHandler().handleError( error );
        FrameworkImplProvider.handleException(error);
    }

    /**
     * Finds only the results that can be added to the context provided.  Also, moves up the hierarchy
     * from raw entities to their rooted entities.
     *
     * @param domainObjects from possibly many alignment contexts
     * @return those from specific context.
     */
    private List<DomainObject> getCompatibleRootedEntities( Collection<DomainObject> domainObjects ) throws Exception {
        logger.info("Found {} raw entities.", domainObjects.size());
        List<DomainObject> rtnVal = new ArrayList<>();

        List<Long> guids = new ArrayList<>();
        for ( DomainObject entity: domainObjects ) {
            guids.add( entity.getId() );
        }
        String opticalRes = context.getAlignmentContext().getOpticalResolution();
        String pixelRes = context.getAlignmentContext().getImageSize();
        List<Long> compatibleList = ModelMgr.getModelMgr().getEntityIdsInAlignmentSpace(opticalRes, pixelRes, guids);

        int nonCompatibleNeuronCount = 0;
        int nonCompatibleSampleCount = 0;
        int incorrectTypeCount = 0;
        Set<String> incorrectTypes = new HashSet<>();
        // Next, walk each entity's tree looking for proper info.
        for ( DomainObject domainObject: domainObjects ) {
            try {
                // Now, to "prowl" the trees of the result list, to find out what can be added, here.
                switch (domainObject.getType()) {
//                    case EntityConstants.TYPE_SAMPLE:
//                        Entity childEntity = ModelMgrUtils.getAccessibleChildren(domainObject).iterator().next();
//                        rootedEntity =
//                                new RootedEntity(ModelMgr.getModelMgr().getAncestorWithType(childEntity, EntityConstants.TYPE_SAMPLE));
//
//                        if (rootedEntity == null) {
//                            logger.warn("Did not find child/parent.  Instead wrapping with new rooted entity.");
//                            rootedEntity = new RootedEntity(domainObject);
//                        }
//
//                        if (isSampleCompatible(param.getContext(), rootedEntity)) {
//                            rtnVal.add(rootedEntity);
//                        } else {
//                            nonCompatibleSampleCount++;
//                        }
//                        break;
//                    case EntityConstants.TYPE_NEURON_FRAGMENT:
//                        // Find ancestor to figure out if it is compatible.
//                        if (isNeuronCompatible(domainObject, compatibleList)) {
//                            rtnVal.add(new RootedEntity(domainObject));
//                        } else {
//                            nonCompatibleNeuronCount++;
//                        }
//
//                        break;
//                    default:
//                        incorrectTypes.add(domainObject.getEntityTypeName());
//                        incorrectTypeCount++;
//                        break;
                }

            } catch ( Exception ex ) {
                ex.printStackTrace();
                throw new RuntimeException( ex );
            }

            if ( rtnVal.size() >= MAX_RESULT_ROWS ) {
                logger.info("Hit maximum of {}.", MAX_RESULT_ROWS);
                break;
            }
        }

        logger.info( "Filtered to {} entities.", rtnVal.size() );
        logger.info(
                "Non-compatible neurons: {}, non-compatible samples: {}.",
                nonCompatibleNeuronCount,
                nonCompatibleSampleCount
        );
        StringBuilder incorrectTypeBuf = new StringBuilder();
        for ( String type: incorrectTypes ) {
            incorrectTypeBuf.append( type ).append( " " );
        }
        logger.info( "Found {} instances of these non-filtered types /{}/.", incorrectTypeCount, incorrectTypeBuf );
        return rtnVal;
    }

    private boolean isSampleCompatible(AlignmentContext standardContext, DomainObject domainObject) throws Exception {
        boolean rtnVal;
        boolean foundMatch = false;
        Sample sample = (Sample)domainObject;
        List<AlignmentContext> contexts = Collections.EMPTY_LIST;
        
//TODO: get the alignment contexts for the sample...

        //List<AlignmentContext> contexts = wrapper.getAvailableAlignmentContexts();
        Iterator<AlignmentContext> contextIterator = contexts.iterator();

        while ( contextIterator.hasNext() && (! foundMatch) ) {
            AlignmentContext nextContext = contextIterator.next();
            if ( standardContext.equals( nextContext ) ) {
                foundMatch = true;
            }

        }

        rtnVal = foundMatch;
        return rtnVal;
    }

    private boolean isNeuronCompatible(DomainObject domainObject, List<Long> compatibleList) throws Exception {
        return ( compatibleList.contains( domainObject.getId() ) );
    }

    public static class SearchWorkerParam {
        private DomainObjectReceiver receiver;
        private Long searchRootId;
        private AlignmentContext context;
        private SearchErrorHandler errorHandler;
        private int startingRow;

        public DomainObjectReceiver getReceiver() {
            return receiver;
        }

        public void setReceiver(DomainObjectReceiver receiver) {
            this.receiver = receiver;
        }

        public Long getSearchRootId() {
            return searchRootId;
        }

        public void setSearchRootId(Long searchRootId) {
            this.searchRootId = searchRootId;
        }

        public AlignmentContext getContext() {
            return context;
        }

        public void setContext(AlignmentContext context) {
            this.context = context;
        }

        public void setErrorHandler( SearchErrorHandler errorHandler ) {
            this.errorHandler = errorHandler;
        }

        public SearchErrorHandler getErrorHandler() {
            return errorHandler;
        }

        public int getStartingRow() {
            return startingRow;
        }

        public void setStartingRow(int startingRow) {
            this.startingRow = startingRow;
        }
    }

    public static interface SearchErrorHandler {
        void handleError( Throwable th );
    }

}
