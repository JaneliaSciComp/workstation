package org.janelia.it.FlyWorkstation.gui.dialogs.search.alignment_board;

import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.ModalDialog;
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

import javax.swing.*;
import javax.swing.border.TitledBorder;
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
    private static final int DIALOG_WIDTH = 800;
    private static final int DIALOG_HEIGHT = 800;
    public static final String SAMPLE_BUTTON_TXT = "Sample";
    public static final String NF_BUTTON_TXT = "Neuron Fragment";

    private AlignmentBoardContext context;
    private Entity searchRoot;
    private List<String> searchHistory;
    private JRadioButton sampleRB;
    private JRadioButton neuronFragmentRB;

    // Input controls
    private JComboBox queryTermComBox; // Not parameterized in Java6, where ant compile happens!
    private JButton queryButton;
    private BaseballCardPanel baseballCardPanel;

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
        layoutGeneralGui( queryPanel, baseballCardPanel );
    }

    /** Launch with/without search-here starting point. */
    public void showDialog( Entity searchRoot ) {
        this.searchRoot = searchRoot;
        packAndShow();
    }

    public void showDialog() {
        this.showDialog(null);
    }

    /** Optional search history, may be set prior to showing dialog */
    public void setSearchHistory(List<String> searchHistory) {
        this.searchHistory = searchHistory;
    }

    public List<String> getSearchHistory() {
        return searchHistory;
    }

    //------------------------------------------------GUI elements for the search inputs.
    private void initGeneralGui() {
        setLayout( new BorderLayout() );
        setPreferredSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
    }

    private void layoutGeneralGui( JPanel queryPanel, JPanel resultsPanel ) {
        add( queryPanel, BorderLayout.NORTH );
        add( resultsPanel, BorderLayout.CENTER );
    }

    /** Simple parameter GUI. */
    private JPanel initParamGui() {
        if ( searchHistory == null ) {
            queryTermComBox = new JComboBox();
            queryTermComBox.setToolTipText("Enter query.");
        }
        else {
            queryTermComBox = new JComboBox( new Vector<String>( searchHistory ) );
            queryTermComBox.setToolTipText("Enter query or select existing query.");
        }
        queryTermComBox.setPreferredSize( new Dimension( 200, 40 ) );
        queryTermComBox.setBorder( new TitledBorder( "Query" ) );
        queryTermComBox.setEditable( true );

        ButtonGroup entityTypeGroup = new ButtonGroup();
        sampleRB = new JRadioButton(SAMPLE_BUTTON_TXT);
        sampleRB.setActionCommand( SAMPLE_BUTTON_TXT );
        entityTypeGroup.add(sampleRB);
        neuronFragmentRB = new JRadioButton(NF_BUTTON_TXT);
        neuronFragmentRB.setActionCommand( NF_BUTTON_TXT );
        entityTypeGroup.add( neuronFragmentRB );
        sampleRB.setSelected( true );

        JPanel typeChoicePanel = new JPanel();
        typeChoicePanel.setLayout( new BorderLayout() );
        typeChoicePanel.add( sampleRB, BorderLayout.WEST );
        typeChoicePanel.add( neuronFragmentRB, BorderLayout.EAST );
        typeChoicePanel.setBorder( new TitledBorder( "Type" ) );

        queryButton = new JButton(
            new QueryLaunchAction(
                "Search",
                queryTermComBox,
                baseballCardPanel,
                entityTypeGroup,
                context,
                searchRoot == null ? null : searchRoot.getId()
            )
        );

        JPanel queryPanel = new JPanel();
        queryPanel.setLayout( new GridBagLayout() );

        Insets insets = new Insets( 1, 1, 1, 1 );
        GridBagConstraints qtermConstraints = new GridBagConstraints(
                0, 0, 4, 1, 2.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, insets, 0, 0
        );

        GridBagConstraints typeChoiceConstraints = new GridBagConstraints(
                0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, insets, 0, 0
        );

        GridBagConstraints launcherConstraints = new GridBagConstraints(
                0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, insets, 0, 0
        );

        queryPanel.add( queryTermComBox, qtermConstraints );
        queryPanel.add( typeChoicePanel, typeChoiceConstraints );
        queryPanel.add( queryButton, launcherConstraints );

        return queryPanel;
    }

    private BaseballCardPanel initResultsGui() {
        return new BaseballCardPanel( false, DIALOG_WIDTH );
    }

    private static class QueryLaunchAction extends AbstractAction {
        private BaseballCardPanel baseballCardPanel;
        private JComboBox queryTermBox;
        private ButtonGroup group;
        private Long searchRootId;
        private AlignmentBoardContext context;

        public QueryLaunchAction(
                String actionName,
                JComboBox queryTermBox,
                BaseballCardPanel baseballCardPanel,
                ButtonGroup group,
                AlignmentBoardContext context,
                Long searchRootId
        ) {
            super( actionName );
            this.baseballCardPanel = baseballCardPanel;
            this.queryTermBox = queryTermBox;
            this.group = group;
            this.searchRootId = searchRootId;
            this.context = context;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String selected = (String)queryTermBox.getSelectedItem();
            if ( selected != null ) {
                SearchWorker.SearchWorkerParam param = new SearchWorker.SearchWorkerParam();
                param.setReceiver( baseballCardPanel );
                param.setContext( context.getAlignmentContext() );
                param.setSearchTypeKey(group.getSelection().getActionCommand());
                param.setQuery( selected );
                param.setSearchRootId( searchRootId );
                SimpleWorker worker = new SearchWorker( param );
                worker.execute();
            }
        }
    }

    private static class SearchWorker extends SimpleWorker {

        private SearchWorkerParam param;
        private List<RootedEntity> rootedResults;

        public SearchWorker( SearchWorkerParam param ) {
            this.param = param;
        }
        @Override
        protected void doStuff() throws Exception {
            SolrQueryBuilder queryBuilder = new SolrQueryBuilder();
            for ( String subjectKey : SessionMgr.getSubjectKeys() ) {
                queryBuilder.addOwnerKey(subjectKey);
            }

            queryBuilder.setSearchString( param.getQuery() );
            if ( param.getSearchRootId() != null ) {
                queryBuilder.setRootId( param.getSearchRootId() );
            }
// todo use facets to filter vs only desired types
//            queryBuilder.setFilters();
            SolrQuery query = queryBuilder.getQuery();
            query.setRows( 50 );
            query.setStart( 0 );

            SolrResults results = ModelMgr.getModelMgr().searchSolr(query);
            List<Entity> resultList = results.getResultList();
            String targetFilterType = param.getSearchTypeKey();
            String targetEntityTypeName = EntityConstants.TYPE_NEURON_FRAGMENT;
            if ( targetFilterType.equals( SAMPLE_BUTTON_TXT ) ) {
                targetEntityTypeName = EntityConstants.TYPE_SAMPLE;
            }
            List<Entity> filteredList = new ArrayList<Entity>();
            for ( Entity result: resultList ) {
                if ( result.getEntityTypeName().equals( targetEntityTypeName ) ) {
                    filteredList.add( result );
                }
            }

            rootedResults = getCompatibleRootedEntities( filteredList, targetFilterType );
        }

        @Override
        protected void hadSuccess() {
            // Accept results and populate.
            param.getReceiver().setRootedEntities( rootedResults );
        }

        @Override
        protected void hadError(Throwable error) {
            SessionMgr.getSessionMgr().handleException( error );
        }

        /**
         * Finds only the results that can be added to the context provided.  Also, moves up the hierarchy
         * from raw entities to their rooted entities.
         *
         * @param entities from possibly many alingment contexts
         * @return those from specific context.
         */
        private List<RootedEntity> getCompatibleRootedEntities( Collection<Entity> entities, String targetEntityTypeName ) {
            List<RootedEntity> rtnVal = new ArrayList<RootedEntity>();

            // Next, walk each entity's tree looking for proper info.
            if ( targetEntityTypeName.equals( EntityConstants.TYPE_NEURON_FRAGMENT ) ) {
                for ( Entity entity: entities ) {
                    try {
                        // Now, to "prowl" the trees of the result list, to find out what can be added, here.
                        Entity sampleEntity = ModelMgr.getModelMgr().getAncestorWithType(entity, EntityConstants.TYPE_SAMPLE);
                        if ( sampleEntity != null ) {
                            RootedEntity rootedEntity = new RootedEntity( sampleEntity );
                            boolean compatible = isSampleCompatible( param.getContext(), rootedEntity );
                            if ( compatible ) {
                                rtnVal.add( rootedEntity );
                            }
                        }
                    } catch ( Exception ex ) {
                        ex.printStackTrace();
                        SessionMgr.getSessionMgr().handleException( ex );
                    }

                }
            }
            else if ( targetEntityTypeName.equals( EntityConstants.TYPE_SAMPLE ) ) {
                for ( Entity entity: entities ) {
                    try {
                        RootedEntity rootedEntity = new RootedEntity(entity);
                        if ( isSampleCompatible( param.getContext(), rootedEntity) ) {
                            rtnVal.add( rootedEntity );
                        }
                    } catch ( Exception ex ) {
                        ex.printStackTrace();
                        SessionMgr.getSessionMgr().handleException( ex );
                    }
                }
            }

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
            private String searchTypeKey;

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

            public String getSearchTypeKey() {
                return searchTypeKey;
            }

            public void setSearchTypeKey(String searchTypeKey) {
                this.searchTypeKey = searchTypeKey;
            }
        }
    }
}
