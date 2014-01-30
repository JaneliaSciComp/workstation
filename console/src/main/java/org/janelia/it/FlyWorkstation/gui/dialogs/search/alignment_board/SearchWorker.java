package org.janelia.it.FlyWorkstation.gui.dialogs.search.alignment_board;

import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntityReceiver;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.search.SolrResultsMetaData;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentContext;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapperFactory;
import org.janelia.it.FlyWorkstation.model.domain.Sample;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.compute.api.support.SolrQueryBuilder;
import org.janelia.it.jacs.compute.api.support.SolrResults;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
    private List<RootedEntity> rootedResults;
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

        Map<String,Set<String>> filters = new HashMap<String,Set<String>>();
        Set<String> filterValues = new HashSet<String>();
        filterValues.add( EntityConstants.TYPE_SAMPLE );
        filterValues.add( EntityConstants.TYPE_NEURON_FRAGMENT );
        filters.put( "entity_type", filterValues );

        queryBuilder.setFilters( filters );
        SolrQuery query = queryBuilder.getQuery();
        query.setStart( param.getStartingRow() );
        query.setRows(maxQueryRows);

        SolrResults results = ModelMgr.getModelMgr().searchSolr( query );
        List<Entity> resultList = results.getResultList();
        rootedResults = getCompatibleRootedEntities( resultList );

        resultsMetaData = new SolrResultsMetaData();
        resultsMetaData.setNumHits( rootedResults.size() );
        resultsMetaData.setRawNumHits( resultList.size() );
        resultsMetaData.setSearchDuration(
                results.getResponse().getElapsedTime()
        );

        // Update search history.
        String queryStr = queryBuilder.getSearchString();

        if ( !StringUtils.isEmpty(queryStr) ) {
            resultsMetaData.setQueryStr( queryStr );
            List<String> searchHistory = (List<String>)
                    SessionMgr.getSessionMgr().getModelProperty(SEARCH_HISTORY_MDL_PROP);
            if ( searchHistory == null ) {
                searchHistory = new ArrayList<String>();
            }
            if ( ! searchHistory.contains( queryStr ) ) {
                searchHistory.add( queryStr );
                // To preserve history, must push it into the model.
                SessionMgr.getSessionMgr().setModelProperty( SEARCH_HISTORY_MDL_PROP, searchHistory );
            }
        }
    }

    @Override
    protected void hadSuccess() {
        // Accept results and populate.
        RootedEntityReceiver receiver = param.getReceiver();
        receiver.setRootedEntities(
                rootedResults,
                resultsMetaData
        );
    }

    @Override
    protected void hadError(Throwable error) {
        param.getErrorHandler().handleError( error );
        SessionMgr.getSessionMgr().handleException(error);
    }

    /**
     * Finds only the results that can be added to the context provided.  Also, moves up the hierarchy
     * from raw entities to their rooted entities.
     *
     * @param entities from possibly many alingment contexts
     * @return those from specific context.
     */
    private List<RootedEntity> getCompatibleRootedEntities( Collection<Entity> entities ) {
        logger.info("Found {} raw entities.", entities.size());
        List<RootedEntity> rtnVal = new ArrayList<RootedEntity>();

        int nonCompatibleNeuronCount = 0;
        int nonCompatibleSampleCount = 0;
        // Next, walk each entity's tree looking for proper info.
        MAX_OUT:
        for ( Entity entity: entities ) {
            try {
                // Now, to "prowl" the trees of the result list, to find out what can be added, here.
                if ( entity.getEntityTypeName().equals( EntityConstants.TYPE_SAMPLE ) ) {
                    RootedEntity rootedEntity = null;
                    Entity childEntity = entity.getChildren().iterator().next();
                    rootedEntity =
                            new RootedEntity( ModelMgr.getModelMgr().getAncestorWithType( childEntity, EntityConstants.TYPE_SAMPLE ) );

                    if ( rootedEntity == null ) {
                        logger.warn( "Did not find child/parent.  Instead wrapping with new rooted entity.");
                        rootedEntity = new RootedEntity( entity );
                    }

                    if ( isSampleCompatible( param.getContext(), rootedEntity) ) {
                        rtnVal.add( rootedEntity );
                    }
                    else {
                        nonCompatibleSampleCount ++;
                    }
                }
                else {
                    // Find ancestor to figure out if it is compatible.
                    if ( isNeuronCompatible(entity) ) {
                        rtnVal.add( new RootedEntity( entity ) );
                    }
                    else {
                        nonCompatibleNeuronCount ++;
                    }

                }

            } catch ( Exception ex ) {
                ex.printStackTrace();
                throw new RuntimeException( ex );
            }

            if ( rtnVal.size() >= MAX_RESULT_ROWS ) {
                logger.info("Hit maximum of {}.", MAX_RESULT_ROWS);
                break MAX_OUT;
            }
        }

        logger.info( "Filtered to {} entities.", rtnVal.size() );
        logger.info(
                "Non-compatible neurons: {}, non-compatible samples: {}.",
                nonCompatibleNeuronCount,
                nonCompatibleSampleCount
        );
        return rtnVal;
    }

    private boolean isSampleCompatible(AlignmentContext standardContext, RootedEntity entity) throws Exception {
        boolean rtnVal;
        boolean foundMatch = false;
        Sample wrapper = (Sample) EntityWrapperFactory.wrap(entity);
        List< AlignmentContext> contexts = wrapper.getAvailableAlignmentContexts();
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

    private boolean isNeuronCompatible(Entity entity) throws Exception {

        Entity separationEntity = ModelMgr.getModelMgr().getAncestorWithType( entity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT );
        if ( separationEntity == null ) {
            return false;
        }
        Entity alignmentEntity = ModelMgr.getModelMgr().getAncestorWithType( separationEntity, EntityConstants.TYPE_ALIGNMENT_RESULT );
        if ( alignmentEntity == null ) {
            return false;
        }
        return context.isCompatibleAlignmentSpace( new RootedEntity( entity ), separationEntity, alignmentEntity, false );
    }

    public static class SearchWorkerParam {
        private RootedEntityReceiver receiver;
        private Long searchRootId;
        private AlignmentContext context;
        private SearchErrorHandler errorHandler;
        private int startingRow;

        public RootedEntityReceiver getReceiver() {
            return receiver;
        }

        public void setReceiver(RootedEntityReceiver receiver) {
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
