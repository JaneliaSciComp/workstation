package org.janelia.it.FlyWorkstation.gui.dialogs.search.alignment_board;

import org.janelia.it.FlyWorkstation.gui.dialogs.ModalDialog;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchConfiguration;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchParametersPanel;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.BaseballCardPanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.baseball_card.BaseballCard;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
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
        Dimension preferredSize = new Dimension((int) (browser.getWidth() * 0.5), (int) (browser.getHeight() * 0.8));
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
        searchParamsPanel = new SearchParametersPanel();
        SearchConfiguration searchConfig = new SearchConfiguration();
        searchConfig.load();
        searchConfig.addConfigurationChangeListener(searchParamsPanel);
        searchParamsPanel.init(searchConfig);
        SearchWorker.SearchErrorHandler errorHandler = new SearchWorker.SearchErrorHandler() {
            @Override
            public void handleError(Throwable th) {
                ABTargetedSearchDialog.this.setVisible(false);
                SessionMgr.getSessionMgr().handleException( th );
            }
        };
        QueryLaunchAction searchAction = new QueryLaunchAction(
                errorHandler,
                searchParamsPanel,
                "Search",
                baseballCardPanel,
                context,
                searchRoot == null ? null : searchRoot.getId()
        );
        searchParamsPanel.getSearchButton().addActionListener(
                searchAction
        );

        searchParamsPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0,true),"enterAction");
        searchParamsPanel.getActionMap().put("enterAction", searchAction);


        List<String> searchHistory = (List<String>) SessionMgr.getSessionMgr().getModelProperty( SearchWorker.SEARCH_HISTORY_MDL_PROP );
        if ( searchHistory == null ) {
            searchHistory = new ArrayList<String>();
        }
        searchParamsPanel.setSearchHistory( searchHistory );
        return searchParamsPanel;

    }

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
                        AlignmentBoardContext context = SessionMgr.getBrowser().getLayersPanel().getAlignmentBoardContext();
                        for ( BaseballCard bbc: selected ) {
                            logger.info("Adding entity {}.", bbc.toString());
                            try {
                                context.addRootedEntity( new RootedEntity( bbc.getEntity() ) );
                            } catch ( Exception ex ) {
                                logger.error(
                                        "Failed to add entity {} to alignment board context {}.",
                                        bbc.getEntity(),
                                        context.getName()
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

    private static class QueryLaunchAction extends AbstractAction {
        private BaseballCardPanel baseballCardPanel;
        private Long searchRootId;
        private AlignmentBoardContext context;
        private SearchParametersPanel queryBuilderSource;
        private SearchWorker.SearchErrorHandler errorHandler;

        public QueryLaunchAction(
                SearchWorker.SearchErrorHandler errorHandler,
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

            // Two searches: first gets the whole set of all things that can be matched.  On completion, it first up
            // the second search.
            SearchWorker.SearchWorkerParam param = new SearchWorker.SearchWorkerParam();
            param.setReceiver( baseballCardPanel );
            param.setContext(context.getAlignmentContext());
            param.setSearchRootId(searchRootId);
            param.setErrorHandler(errorHandler);
            param.setStartingRow( 0 );
            SimpleWorker worker = new SearchWorker( param, queryBuilderSource.getQueryBuilder(), context );
            worker.execute();

        }
        private void showLoadingIndicator() {
            baseballCardPanel.showLoadingIndicator();
        }

    }


}
