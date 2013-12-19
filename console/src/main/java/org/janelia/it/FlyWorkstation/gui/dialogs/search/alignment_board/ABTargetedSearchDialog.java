package org.janelia.it.FlyWorkstation.gui.dialogs.search.alignment_board;

import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.ModalDialog;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchConfiguration;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchParametersPanel;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.BaseballCardPanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntityReceiver;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentContext;
import org.janelia.it.FlyWorkstation.model.domain.Sample;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.compute.api.support.SolrQueryBuilder;
import org.janelia.it.jacs.compute.api.support.SolrResults;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 12/13/13
 * Time: 9:53 AM
 *
 * This specialized search dialog's output will be targeted at the alignment board.
 */
public class ABTargetedSearchDialog extends ModalDialog {

    private static final int DEFAULT_ROWS_PER_PAGE = 10;

    private AlignmentBoardContext context;
    private Entity searchRoot;
    private SearchParametersPanel searchParamsPanel;
    private BaseballCardPanel baseballCardPanel;

    private int dialogWidth;

    /**
     * Must always launch this with an alignment board context, even though this is modal.  Wish to make certain
     * that the target board does not dynamically change after launch. Therefore passing it in, rather than
     * fetching it from the session.
     *
     * @param context active alignment board at time of construction.
     */
    public ABTargetedSearchDialog( AlignmentBoardContext context ) {
        this.context = context;
        if ( context == null ) {
            throw new RuntimeException("Cannot launch without context");
        }
        initGeneralGui();
        baseballCardPanel = initResultsGui();
        JPanel queryPanel = initParamGui();
        layoutGeneralGui(queryPanel, baseballCardPanel);
    }

    /** Launch with/without search-here starting point. */
    public void showDialog( Entity searchRoot ) {
        this.searchRoot = searchRoot;
        packAndShow();
    }

    public void showDialog() {
        this.showDialog(null);
    }

    //------------------------------------------------GUI elements for the search inputs.
    private void initGeneralGui() {
        Browser browser = SessionMgr.getBrowser();
        setLayout( new BorderLayout() );
        setPreferredSize(new Dimension((int)(browser.getWidth()*0.8),(int)(browser.getHeight()*0.8)));
        dialogWidth = getPreferredSize().width;
    }

    private void layoutGeneralGui( JPanel queryPanel, JPanel resultsPanel ) {
        add( queryPanel, BorderLayout.NORTH );
        add( resultsPanel, BorderLayout.CENTER );
    }

    /** Simple parameter GUI. */
    private JPanel initParamGui() {
        searchParamsPanel = new SearchParametersPanel();
        SearchConfiguration searchConfig = new SearchConfiguration();
        searchConfig.load();
        searchConfig.addConfigurationChangeListener(searchParamsPanel);
        searchParamsPanel.init(searchConfig);
        SearchErrorHandler errorHandler = new SearchErrorHandler() {
            @Override
            public void handleError(Throwable th) {
                ABTargetedSearchDialog.this.setVisible(false);
                SessionMgr.getSessionMgr().handleException( th );
            }
        };
        searchParamsPanel.getSearchButton().addActionListener(
            new QueryLaunchAction(
                errorHandler,
                searchParamsPanel,
                "Search",
                baseballCardPanel,
                context,
                searchRoot == null ? null : searchRoot.getId()
            )
        );
        List<String> searchHistory = (List<String>) SessionMgr.getSessionMgr().getModelProperty(Browser.SEARCH_HISTORY);
        searchParamsPanel.setSearchHistory( searchHistory );
        return searchParamsPanel;

    }

    private BaseballCardPanel initResultsGui() {
        return new BaseballCardPanel( false, dialogWidth, DEFAULT_ROWS_PER_PAGE );
    }

    private static class QueryLaunchAction extends AbstractAction {
        private BaseballCardPanel baseballCardPanel;
        private Long searchRootId;
        private AlignmentBoardContext context;
        private SearchParametersPanel queryBuilderSource;
        private SearchErrorHandler errorHandler;

        public QueryLaunchAction(
                SearchErrorHandler errorHandler,
                SearchParametersPanel queryBuilderSource,
                String actionName,
                BaseballCardPanel baseballCardPanel,
                AlignmentBoardContext context,
                Long searchRootId
        ) {
            super( actionName );
            this.queryBuilderSource = queryBuilderSource;
            this.baseballCardPanel = baseballCardPanel;
            this.searchRootId = searchRootId;
            this.context = context;
            this.errorHandler = errorHandler;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // set the thing busy.
            showLoadingIndicator();
            SearchWorker.SearchWorkerParam param = new SearchWorker.SearchWorkerParam();
            param.setReceiver(baseballCardPanel);
            param.setContext(context.getAlignmentContext());
            param.setSearchRootId(searchRootId);
            param.setErrorHandler(errorHandler);
            param.setStartingRow( 0 );
            SimpleWorker worker = new SearchWorker( param, queryBuilderSource.getQueryBuilder() );
            worker.execute();
        }
        private void showLoadingIndicator() {
            baseballCardPanel.showLoadingIndicator();
        }

    }

    static interface SearchErrorHandler {
        void handleError( Throwable th );
    }

    private static class SearchWorker extends SimpleWorker {

        private Logger logger = LoggerFactory.getLogger(ABTargetedSearchDialog.class);
        private SearchWorkerParam param;
        private List<RootedEntity> rootedResults;
        private SolrQueryBuilder queryBuilder;

        public SearchWorker( SearchWorkerParam param, SolrQueryBuilder queryBuilder ) {
            this.param = param;
            this.queryBuilder = queryBuilder;
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
            query.setRows( 200 ); //MAX_VALUE

            SolrResults results = ModelMgr.getModelMgr().searchSolr(query);
            List<Entity> resultList = results.getResultList();
            List<Entity> filteredList = new ArrayList<Entity>();
            for ( Entity result: resultList ) {
                if ( EntityConstants.TYPE_SAMPLE.equals( result.getEntityTypeName() )   ||
                     EntityConstants.TYPE_NEURON_FRAGMENT.equals( result.getEntityTypeName() ) ) {
                    filteredList.add( result );
                }
            }

            rootedResults = getCompatibleRootedEntities( filteredList );
        }

        @Override
        protected void hadSuccess() {
            // Accept results and populate.
            param.getReceiver().setRootedEntities( rootedResults );
        }

        @Override
        protected void hadError(Throwable error) {
            param.getErrorHandler().handleError( error );
            SessionMgr.getSessionMgr().handleException( error );
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

            // Next, walk each entity's tree looking for proper info.
            for ( Entity entity: entities ) {
                try {
                    // Now, to "prowl" the trees of the result list, to find out what can be added, here.
                    RootedEntity rootedEntity = null;
                    if ( entity.getEntityTypeName().equals( EntityConstants.TYPE_SAMPLE ) ) {
                        rootedEntity = new RootedEntity( entity );
                    }
                    else {
                        Entity sampleEntity = ModelMgr.getModelMgr().getAncestorWithType(entity, EntityConstants.TYPE_SAMPLE);
                        rootedEntity = new RootedEntity( sampleEntity );
                    }

                    if ( isSampleCompatible( param.getContext(), rootedEntity) ) {
                        rtnVal.add( rootedEntity );
                    }
                } catch ( Exception ex ) {
                    ex.printStackTrace();
                    throw new RuntimeException( ex );
                }
            }

            logger.info("Filtered to {} entities.", rtnVal.size());
            return rtnVal;
        }

        private boolean isSampleCompatible(AlignmentContext standardContext, RootedEntity entity) throws Exception {
            boolean rtnVal;
            boolean foundMatch = false;
            Sample wrapper = new Sample( entity );
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

        public static class SearchWorkerParam {
            private String query;
            private RootedEntityReceiver receiver;
            private Long searchRootId;
            private AlignmentContext context;
            private SearchErrorHandler errorHandler;
            private int startingRow;

            public String getQuery() {
                return query;
            }

            public void setQuery(String query) {
                this.query = query;
            }

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
    }
}
