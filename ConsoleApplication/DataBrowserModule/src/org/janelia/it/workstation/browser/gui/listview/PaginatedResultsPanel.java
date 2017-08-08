package org.janelia.it.workstation.browser.gui.listview;

import static org.janelia.it.workstation.browser.api.DomainMgr.getDomainMgr;
import static org.janelia.it.workstation.browser.model.search.SearchResults.PAGE_SIZE;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.text.Position;

import com.google.common.eventbus.Subscribe;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.model.DomainObjectAnnotationChangeEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectChangeEvent;
import org.janelia.it.workstation.browser.events.selection.DomainObjectSelectionEvent;
import org.janelia.it.workstation.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.browser.gui.find.FindContext;
import org.janelia.it.workstation.browser.gui.find.FindContextRegistration;
import org.janelia.it.workstation.browser.gui.find.FindToolbar;
import org.janelia.it.workstation.browser.gui.support.Debouncer;
import org.janelia.it.workstation.browser.gui.support.ScrollingDropDownButton;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.browser.model.search.ResultIterator;
import org.janelia.it.workstation.browser.model.search.ResultIteratorFind;
import org.janelia.it.workstation.browser.model.search.ResultPage;
import org.janelia.it.workstation.browser.model.search.SearchResults;
import org.janelia.it.workstation.browser.util.ConcurrentUtils;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A panel that displays a paginated result set inside of a user-configurable AnnotatedDomainObjectListViewer.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class PaginatedResultsPanel extends JPanel implements FindContext {

    private static final Logger log = LoggerFactory.getLogger(PaginatedResultsPanel.class);
    
    // Splash panel
    private final JLabel splashPanel;

    // Result view
    private AnnotatedDomainObjectListViewer resultsView;

    // Status bar
    private FindToolbar findToolbar;
    private final JPanel bottomBar;
    private final JPanel statusBar;
    private final JLabel statusLabel;
    private final JPanel selectionButtonContainer;
    private final JButton prevPageButton;
    private final JButton nextPageButton;
    private final JButton endPageButton;
    private final JButton startPageButton;
    private final JButton selectAllButton;
    private final JLabel pagingStatusLabel;
    private final ScrollingDropDownButton viewTypeButton;

    // Content
    private SearchResults searchResults;
    private ResultPage resultPage;
    private int numPages = 0;
    private int currPage = 0;
    
    // State
    protected DomainObjectSelectionModel selectionModel;
    protected SearchProvider searchProvider;
    	
    public PaginatedResultsPanel(DomainObjectSelectionModel selectionModel, SearchProvider searchProvider) {
                
        this.selectionModel = selectionModel;
        this.searchProvider = searchProvider;
        
        setLayout(new BorderLayout());
        
        splashPanel = new JLabel(Icons.getIcon("workstation_logo_white.png"));
        add(splashPanel);
        
        viewTypeButton = new ScrollingDropDownButton("Choose Viewer...");
        populateViewerPopupMenu(viewTypeButton.getPopupMenu());

        prevPageButton = new JButton(Icons.getIcon("arrow_back.gif"));
        prevPageButton.setToolTipText("Back a page");
        prevPageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goPrevPage();
            }
        });

        nextPageButton = new JButton(Icons.getIcon("arrow_forward.gif"));
        nextPageButton.setToolTipText("Forward a page");
        nextPageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goNextPage();
            }
        });

        startPageButton = new JButton(Icons.getIcon("arrow_double_left.png"));
        startPageButton.setToolTipText("Jump to start");
        startPageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goStartPage();
            }
        });

        endPageButton = new JButton(Icons.getIcon("arrow_double_right.png"));
        endPageButton.setToolTipText("Jump to end");
        endPageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goEndPage();
            }
        });

        selectAllButton = new JButton("Select All");
        selectAllButton.setToolTipText("Select all items on all pages");
        selectAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadAndSelectAll();
                ActivityLogHelper.logUserAction("PaginatedResultsPanel.loadAndSelectAll");
            }
        });

        statusLabel = new JLabel("");
        pagingStatusLabel = new JLabel("");

        selectionButtonContainer = new JPanel();
        selectionButtonContainer.setLayout(new BoxLayout(selectionButtonContainer, BoxLayout.LINE_AXIS));

        selectionButtonContainer.add(selectAllButton);
        selectionButtonContainer.add(Box.createRigidArea(new Dimension(10, 20)));
        selectionButtonContainer.setVisible(false);

        statusBar = new JPanel();
        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.LINE_AXIS));
        statusBar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, (Color) UIManager.get("ws.ComponentBorderColor")), BorderFactory.createEmptyBorder(0, 5, 2, 5)));

        statusBar.add(viewTypeButton);
        statusBar.add(Box.createHorizontalGlue());
        statusBar.add(new JSeparator(JSeparator.VERTICAL));
        statusBar.add(statusLabel);
        statusBar.add(Box.createRigidArea(new Dimension(10, 20)));
        statusBar.add(selectionButtonContainer);
        statusBar.add(new JSeparator(JSeparator.VERTICAL));
        statusBar.add(pagingStatusLabel);
        statusBar.add(Box.createRigidArea(new Dimension(10, 20)));
        statusBar.add(startPageButton);
        statusBar.add(prevPageButton);
        statusBar.add(nextPageButton);
        statusBar.add(endPageButton);

        findToolbar = new FindToolbar(this);
        findToolbar.addMouseListener(new MouseForwarder(this, "FindToolbar->PaginatedResultsPanel"));

        bottomBar = new JPanel();
        bottomBar.setLayout(new BorderLayout());
        bottomBar.add(findToolbar, BorderLayout.NORTH);
        bottomBar.add(statusBar, BorderLayout.CENTER);

        addHierarchyListener(new FindContextRegistration(this, this));

        setViewerType(ListViewerType.IconViewer);
    }
    
    private void populateViewerPopupMenu(JPopupMenu popupMenu) {
        for(final ListViewerType type : ListViewerType.values()) {
            JMenuItem viewItem = new JMenuItem(type.getName());
            viewItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    ActivityLogHelper.logUserAction("PaginatedResultsPanel.setViewerType", type.getName());
                    setViewerType(type);

                    final List<DomainObject> selectedDomainObjects = new ArrayList<>();
                    DomainModel model = getDomainMgr().getModel();
                    for(Reference id : selectionModel.getSelectedIds()) {
                        try {
                            DomainObject domainObject = model.getDomainObject(id);
                            if (domainObject!=null) {
                                selectedDomainObjects.add(domainObject);
                            }
                        }  catch (Exception e) {
                            ConsoleApp.handleException(e);
                        }
                    }

                    // Set user driven to false in order to avoid selecting the first item
                    updateResultsView(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            // Reselect the items that were selected
                            log.info("Reselecting {} domain objects in the {} viewer",selectedDomainObjects.size(),type.getName());
                            resultsView.selectDomainObjects(selectedDomainObjects, true, true, true);
                            return null;
                        }
                    });
                }
            });
            popupMenu.add(viewItem);
        }
    }
    
    public void setViewerType(final ListViewerType viewerType) {
        this.viewTypeButton.setText(viewerType.getName());
        try {
            if (viewerType.getViewerClass()==null) {
                setViewer(null);
            }
            else {
                AnnotatedDomainObjectListViewer viewer = viewerType.getViewerClass().newInstance();
                viewer.getPanel().addMouseListener(new MouseForwarder(this, "AnnotatedDomainObjectListViewer->PaginatedResultsPanel"));
                setViewer(viewer);
            }
        }
        catch (InstantiationException | IllegalAccessException e) {
            log.error("Error instantiating viewer class",e);
            setViewer(null);
        }
    }

    public void activate() {
        resultsView.activate();
    }

    public void deactivate() {
        resultsView.deactivate();
    }
    
    public AnnotatedDomainObjectListViewer getViewer() {
        return resultsView;
    }
    
    private void setViewer(AnnotatedDomainObjectListViewer viewer) {
        if (resultsView!=null) {
            Events.getInstance().unregisterOnEventBus(resultsView);
        }
        this.resultsView = viewer;
        Events.getInstance().registerOnEventBus(resultsView);
        resultsView.setSelectionModel(selectionModel);
        resultsView.setSearchProvider(searchProvider);
    }

    private void updatePagingStatus() {
        startPageButton.setEnabled(numPages>0 && currPage != 0);
        prevPageButton.setEnabled(numPages>0 && currPage > 0);
        nextPageButton.setEnabled(numPages>0 && currPage < numPages - 1);
        endPageButton.setEnabled(numPages>0 && currPage != numPages - 1);
    }
    
    @Subscribe
    public void domainObjectSelected(DomainObjectSelectionEvent event) {
        if (event.getSource()!=resultsView) return;
        updateStatusBar();
    }

    @Subscribe
    public void domainObjectChanged(DomainObjectChangeEvent event) {
        if (searchResults==null) return;
        if (searchResults.updateIfFound(event.getDomainObject())) {
            log.info("Updated search results with changed domain object: {}", event.getDomainObject());
            resultsView.refreshDomainObject(event.getDomainObject());
        }
    }
    
    @Subscribe
    public void annotationsChanged(DomainObjectAnnotationChangeEvent event) {
        if (searchResults==null) return;
        for(final ResultPage page : searchResults.getPages()) {
            if (page==null) continue; // Page not yet loaded
            final Long domainObjectId = event.getDomainObject().getId();
            final DomainObject pageObject = page.getDomainObject(domainObjectId);
            if (pageObject!=null) {

                SimpleWorker annotationUpdateWorker = new SimpleWorker() {

                    @Override
                    protected void doStuff() throws Exception {
                        DomainModel model = getDomainMgr().getModel();
                        page.updateAnnotations(domainObjectId, model.getAnnotations(Reference.createFor(pageObject)));
                    }

                    @Override
                    protected void hadSuccess() {
                        resultsView.refreshDomainObject(pageObject);
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        ConsoleApp.handleException(error);
                    }
                };

                annotationUpdateWorker.execute();
                break;
            }
        }
    }

    private void loadAndSelectAll() {
        
        if (!searchResults.isAllLoaded()) {
            int rv = JOptionPane.showConfirmDialog(ConsoleApp.getMainFrame(), 
                    "Load all "+searchResults.getNumTotalResults()+" results?",
                    "Load all?", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (rv == JOptionPane.YES_OPTION) {
                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        searchResults.loadAllResults();
                    }

                    @Override
                    protected void hadSuccess() {
                        selectAll();
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        ConsoleApp.handleException(error);
                    }
                };

                worker.setProgressMonitor(new IndeterminateProgressMonitor(ConsoleApp.getMainFrame(), "Loading...", ""));
                worker.execute();
                return;
            }
        }
        else {
            Utils.setWaitingCursor(ConsoleApp.getMainFrame());
            selectAll();
            Utils.setDefaultCursor(ConsoleApp.getMainFrame());
        }
    }
    
    private void selectAll() {
        boolean clearAll = true;
        List<DomainObject> domainObjects = new ArrayList<>();
        for(ResultPage page : searchResults.getPages()) {
            if (page==null) continue; // Page not yet loaded
            domainObjects.addAll(page.getDomainObjects());
        }
        selectionModel.select(domainObjects, clearAll, true);
    }
    
    private void updateStatusBar() {
        int s = selectionModel.getSelectedIds().size();
        if (resultPage==null) {
            statusLabel.setText("");
            selectionButtonContainer.setVisible(false);
        }
        else {
            long pn = resultPage.getNumPageResults();
            long tn = resultPage.getNumTotalResults();
            statusLabel.setText(s + " of " + tn + " selected");
            selectionButtonContainer.setVisible(s==pn && tn>pn);
        }
    }

    public void setCurrPage(int currPage) {
        this.currPage = currPage;
    }

    public int getCurrPage() {
        return currPage;
    }

    private synchronized void goPrevPage() {
        this.currPage -= 1;
        if (currPage < 0) {
            currPage = 0;
        }
        showCurrPage();
    }

    private synchronized void goNextPage() {
        this.currPage += 1;
        if (currPage >= numPages) {
            currPage = numPages-1;
        }
        showCurrPage();
    }

    private synchronized void goStartPage() {
        this.currPage = 0;
        showCurrPage();
    }

    private synchronized void goEndPage() {
        this.currPage = numPages - 1;
        showCurrPage();
    }

    /**
     * This is a non-user driven, direct to page navigation.
     * @param page
     * @param success
     */
    public synchronized void goToPage(int page, Callable<Void> success) {
        this.currPage = page;
        showCurrPage(success);
    }
    
    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()));
        updateUI();
    }
    
    public void showResultsView() {
        if (resultsView==null) {
            showNothing();
            return;
        }
        removeAll();
        add(resultsView.getPanel(), BorderLayout.CENTER);
        if (numPages==0) {
            pagingStatusLabel.setText("Page 0 of 0");    
        }
        else {
            pagingStatusLabel.setText("Page " + (currPage + 1) + " of " + numPages);
        }
        updateStatusBar();
        add(bottomBar, BorderLayout.SOUTH);
        updateUI();
    }
    
    public void showNothing() {
        removeAll();
        add(splashPanel, BorderLayout.CENTER);
        updateUI();
    }
    
    public void showSearchResults(SearchResults searchResults, boolean isUserDriven, Callable<Void> success) {

        if (searchResults==null) {
            // First load into this panel, so we need a top-level loading indicator, since the resultsView isn't active yet
            showLoadingIndicator();
        }

        this.searchResults = searchResults;
        this.numPages = searchResults.getNumTotalPages();
        
        showCurrPage(isUserDriven, success);
    }

    public void reset() {
        selectionModel.reset();
        this.currPage = 0;
    }
    
    protected void showCurrPage() {
        showCurrPage(true, null);
    }
    
    protected void showCurrPage(Callable<Void> success) {
        showCurrPage(false, success);
    }

    protected void showCurrPage(final boolean isUserDriven) {
        showCurrPage(isUserDriven, null);
    }
    
    protected void showCurrPage(final boolean isUserDriven, final Callable<Void> success) {

        log.debug("showCurrPage(isUserDriven={})",isUserDriven);

        resultsView.showLoadingIndicator();
        updatePagingStatus();
                
        if (searchResults==null) {
            throw new IllegalStateException("Cannot show page when there are no search results");
        }

        SimpleWorker worker = new SimpleWorker() {
        
            @Override
            protected void doStuff() throws Exception {
                resultPage = getPage(searchResults, currPage);
            }

            @Override
            protected void hadSuccess() {
                log.debug("Got results, updating view");
                final ArrayList<Reference> selectedRefs = new ArrayList<>(selectionModel.getSelectedIds());
                log.debug("Got selected refs: {}",selectedRefs);
                updateResultsView(new Callable<Void>() {   
                    @Override
                    public Void call() throws Exception {
                        log.info("updateResultsView complete, restoring selection");
                        if (selectedRefs.isEmpty()) {
                            // If the selection model is empty, just select the first item to make it appear in the inspector
                            List<DomainObject> objects = resultPage.getDomainObjects();
                            if (!objects.isEmpty()) {
                                log.debug("Auto-selecting first object");
                                resultsView.selectDomainObjects(Arrays.asList(objects.get(0)), true, true, false);
                            }
                        }
                        else {
                            // There's already something in the selection model, so we should attempt to reselect it
                            log.debug("Reselecting {} objects",selectedRefs.size());
                            List<DomainObject> domainObjects = DomainMgr.getDomainMgr().getModel().getDomainObjects(selectedRefs);
                            resultsView.selectDomainObjects(domainObjects, true, true, false);
                        }
                        resultsView.refreshEditMode();
                        ConcurrentUtils.invokeAndHandleExceptions(success);
                        return null;
                    }
                });
            }

            @Override
            protected void hadError(Throwable error) {
                showNothing();
                ConsoleApp.handleException(error);
            }
        };
        
        worker.execute();
    }
    
    private void updateResultsView(final Callable<Void> success) {
        selectionModel.reset();
        if (resultPage!=null) {
            resultsView.showDomainObjects(resultPage, new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    showResultsView();
                    ConcurrentUtils.invokeAndHandleExceptions(success);
                    return null;
                }
            });
        }
        else {
            showNothing();
        }
    }
    
    protected abstract ResultPage getPage(SearchResults searchResults, int page) throws Exception;

    @Override
    public void showFindUI() {
        findToolbar.open();
    }

    @Override
    public void hideFindUI() {
        findToolbar.close();
    }

    private final Debouncer matchDebouncer = new Debouncer();

    @Override
    public void findMatch(String text, Position.Bias bias, boolean skipStartingNode, Callable<Void> success) {

        Map<String,Object> params = new HashMap<>();
        params.put("text", text);
        params.put("bias", bias);
        params.put("skipStartingNode", skipStartingNode);

        Utils.setWaitingCursor(ConsoleApp.getMainFrame());
        if (!matchDebouncer.queueWithParameters(success, params)) {
            return;
        }

        SimpleWorker worker = createFindWorker(text, bias, skipStartingNode);
        worker.execute();
    }

    private SimpleWorker createFindWorker(final String text, final Position.Bias bias, final boolean skipStartingNode) {

        return new SimpleWorker() {

            private ResultIteratorFind searcher;
            private DomainObject match;
            private Integer matchPage;

            @Override
            protected void doStuff() throws Exception {
                System.currentTimeMillis();
                DomainObject startObject;
                Reference lastSelectedRef = selectionModel.getLastSelectedId();
                log.info("lastSelectedId={}", lastSelectedRef);
                if (lastSelectedRef!=null) {
                    startObject = DomainMgr.getDomainMgr().getModel().getDomainObject(lastSelectedRef);
                }
                else {
                    // This is generally unexpected, because the viewer should
                    // select the first item automatically, and there is no way to deselect everything.
                    log.warn("No 'last selected object' in selection model! Defaulting to first object...");
                    startObject = resultPage.getDomainObjects().get(0);
                }

                int index = 0;
                Integer foundIndex = null;
                for (DomainObject domainObject : resultPage.getDomainObjects()) {
                    if (DomainUtils.equals(domainObject, startObject)) {
                        foundIndex = index;
                        break;
                    }
                    index++;
                }
                
                if (foundIndex==null) {
                    log.warn("Last selected object no longer exists");
                    foundIndex = 0;
                }
                
                log.debug("currPage={}",currPage);
                int globalStartIndex = currPage*PAGE_SIZE + foundIndex;
                log.debug("globalStartIndex={}",globalStartIndex);
                ResultIterator resultIterator = new ResultIterator(searchResults, globalStartIndex, bias, skipStartingNode);
                searcher = new ResultIteratorFind(resultIterator) {
                    @Override
                    protected boolean matches(ResultPage resultPage, DomainObject currObject) {
                        return resultsView.matches(resultPage, currObject, text);
                    }
                };
                match = searcher.find();
                matchPage = resultIterator.getCurrPage();
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(ConsoleApp.getMainFrame());
                if (match != null) {
                    log.info("Found match for '{}': {}",text,match.getId());
                    if (matchPage!=null && matchPage!=currPage) {
                        log.trace("Match page ({}) differs from current page ({})",matchPage,currPage);
                        currPage = matchPage;
                        selectionModel.select(Arrays.asList(match), true, true);
                        showCurrPage(false); // isUserDriven=false in order to "reselect" the match
                    }
                    else {
                        resultsView.selectDomainObjects(Arrays.asList(match), true, true, true);
                    }
                }
                else {
                    log.info("No match found for '{}'",text);
                }
                Map<String,Object> params = matchDebouncer.drainToLastParameters();
                if (params!=null) {
                    log.info("Re-doing find because there were other find requests in the meantime");
                    // This is where a functional approach would be nice... instead we get this mess
                    createFindWorker((String)params.get("text"), (Position.Bias)params.get("bias"), (Boolean)params.get("skipStartingNode")).execute();
                }
                else {
                    matchDebouncer.success();
                }
            }

            @Override
            protected void hadError(Throwable error) {
                Utils.setDefaultCursor(ConsoleApp.getMainFrame());
                matchDebouncer.failure();
                ConsoleApp.handleException(error);
            }
        };
    }

    @Override
    public void openMatch() {
    }
}
