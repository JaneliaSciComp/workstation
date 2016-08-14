package org.janelia.it.workstation.gui.dialogs.search.alignment_board;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;
import org.janelia.it.workstation.gui.alignment_board.ab_mgr.AlignmentBoardMgr;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.gui.baseball_card.BaseballCard;
import org.janelia.it.workstation.gui.browser.gui.baseball_card.BaseballCardPanel;
import org.janelia.it.workstation.gui.dialogs.ModalDialog;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.model.domain.Sample;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private DomainObject searchRoot;
//    private SearchParametersPanel searchParamsPanel;
    private BaseballCardPanel baseballCardPanel;
    private Logger logger = LoggerFactory.getLogger( ABTargetedSearchDialog.class );

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
        JPanel disposePanel = initDisposeGui();
        layoutGeneralGui(queryPanel, baseballCardPanel, disposePanel);
    }

    /** Launch with/without search-here starting point. */
    public void showDialog( DomainObject searchRoot ) {
        this.searchRoot = searchRoot;
        packAndShow();
    }

    public void showDialog() {
        this.showDialog(null);
    }

    //------------------------------------------------GUI elements for the search inputs.
    private void initGeneralGui() {
        Component mainFrame = SessionMgr.getMainFrame();
        setLayout( new BorderLayout() );
        Dimension preferredSize = new Dimension((int) (mainFrame.getWidth() * 0.5), (int) (mainFrame.getHeight() * 0.8));
        setPreferredSize( preferredSize );
        dialogWidth = preferredSize.width;
    }

    private void layoutGeneralGui( JPanel queryPanel, JPanel resultsPanel, JPanel disposePanel ) {
        add( queryPanel, BorderLayout.NORTH );
        add( resultsPanel, BorderLayout.CENTER );
        add( disposePanel, BorderLayout.SOUTH );
    }

    /** Simple parameter GUI. */
    private JPanel initParamGui() {
        SearchWorker.SearchErrorHandler errorHandler = new SearchWorker.SearchErrorHandler() {
            @Override
            public void handleError(Throwable th) {
                ABTargetedSearchDialog.this.setVisible(false);
                SessionMgr.getSessionMgr().handleException( th );
            }
        };
        final QueryLaunchAction searchAction = new QueryLaunchAction(
                errorHandler,
                "Search",
                baseballCardPanel,
                context,
                searchRoot == null ? null : searchRoot.getId()
        );
        throw new IllegalStateException("Search is not currently supported");
//        searchParamsPanel = new SearchParametersPanel() {
//            @Override
//            public void performSearch(boolean clear) {
//                searchAction.actionPerformed(null);
//            }
//            @Override
//            protected List<String> getSearchHistory() {
//                return (List<String>) SessionMgr.getSessionMgr().getModelProperty(SearchWorker.SEARCH_HISTORY_MDL_PROP);
//            }
//            @Override
//            protected void setSearchHistory(List<String> searchHistory) {
//                SessionMgr.getSessionMgr().setModelProperty(SEARCH_HISTORY_MDL_PROP, searchHistory);
//            }
//        };
//        SearchConfiguration searchConfig = new SearchConfiguration();
//        searchConfig.load();
//        searchConfig.addConfigurationChangeListener(searchParamsPanel);
//        searchParamsPanel.init(searchConfig);
//        return searchParamsPanel;
    }

	/**
	 * @todo fix references to entities.
	 */
    private JPanel initDisposeGui() {
        JButton addToBoardBtn = new JButton("Add to Alignment Board");
        addToBoardBtn.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible( false );

                SimpleWorker addToBoardWorker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        List<BaseballCard> selected = baseballCardPanel.getSelectedCards();
                        // Let's add these to the alignment board.
                        AlignmentBoardContext context = AlignmentBoardMgr.getInstance().getLayersPanel().getAlignmentBoardContext();
                        for ( BaseballCard bbc: selected ) {
                            logger.info("Adding entity {}.", bbc.toString());
                            try {
                                DomainModel model = DomainMgr.getDomainMgr().getModel();
                                final String type = bbc.getDomainObject().getType();
                                String domainObjectClass = null;
                                if (type.equals("Neuron Fragment")) {
                                    domainObjectClass = NeuronFragment.class.getSimpleName();
                                }
                                else if (type.equals("Sample")) {
                                    domainObjectClass = Sample.class.getSimpleName();
                                }
                                DomainObject dobj = model.getDomainObject(domainObjectClass, bbc.getDomainObject().getId());
                                context.addDomainObject(dobj);
                                //context.addRootedEntity( new RootedEntity( bbc.getEntity() ) );
                            } catch ( Exception ex ) {
                                logger.error(
                                        "Failed to add entity {} to alignment board context {}.",
                                        bbc.getDomainObject(),
                                        context.getAlignmentBoard().getName()
                                );
                            }
                        }
                    }

                    @Override
                    protected void hadSuccess() {
                        // Need to nada.
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        throw new RuntimeException( error );
                    }
                };

                addToBoardWorker.execute();

            }
        });

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener( new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible( false );
            }
        });

        // Layout the add-to-board button.
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout( new BorderLayout() );
        buttonPanel.add(addToBoardBtn, BorderLayout.WEST);
        buttonPanel.add( closeButton, BorderLayout.EAST );
        return buttonPanel;

    }

    private BaseballCardPanel initResultsGui() {
        BaseballCardPanel.ControlCallback controlCallback = new BaseballCardPanel.ControlCallback() {
            @Override
            public void callerRequiresFocus() {
                ABTargetedSearchDialog.this.setVisible( false );
            }
        };
        return new BaseballCardPanel( true, dialogWidth, DEFAULT_ROWS_PER_PAGE, controlCallback );
    }

    private class QueryLaunchAction extends AbstractAction {
        private BaseballCardPanel baseballCardPanel;
        private Long searchRootId;
        private AlignmentBoardContext context;
        private SearchWorker.SearchErrorHandler errorHandler;

        public QueryLaunchAction(
                SearchWorker.SearchErrorHandler errorHandler,
                String actionName,
                BaseballCardPanel baseballCardPanel,
                AlignmentBoardContext context,
                Long searchRootId
        ) {
            super( actionName );
            this.baseballCardPanel = baseballCardPanel;
            this.searchRootId = searchRootId;
            this.context = context;
            this.errorHandler = errorHandler;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // set the thing busy.
            showLoadingIndicator();

            // Two searches: first gets the whole set of all things that can be matched.  On completion, it first up
            // the second search.
            SearchWorker.SearchWorkerParam param = new SearchWorker.SearchWorkerParam();
            param.setReceiver( baseballCardPanel );
            param.setContext(context.getAlignmentContext());
            param.setSearchRootId(searchRootId);
            param.setErrorHandler(errorHandler);
            param.setStartingRow( 0 );
            throw new IllegalStateException("Search is not currently supported");
//            SimpleWorker worker = new SearchWorker( param, searchParamsPanel.getQueryBuilder(), context );
//            worker.execute();

        }
        private void showLoadingIndicator() {
            baseballCardPanel.showLoadingIndicator();
        }

    }


}
