package org.janelia.workstation.browser.gui.listview;

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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.ProgressMonitor;
import javax.swing.UIManager;
import javax.swing.text.Position;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.workstation.browser.gui.find.FindContext;
import org.janelia.workstation.browser.gui.find.FindContextRegistration;
import org.janelia.workstation.browser.gui.find.FindToolbar;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.janelia.workstation.common.gui.listview.ListViewer;
import org.janelia.workstation.common.gui.listview.ListViewerActionListener;
import org.janelia.workstation.common.gui.listview.ListViewerClassProvider;
import org.janelia.workstation.common.gui.model.ImageModel;
import org.janelia.workstation.common.gui.support.Debouncer;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.support.MouseForwarder;
import org.janelia.workstation.common.gui.support.buttons.DropDownButton;
import org.janelia.workstation.common.gui.util.UIUtils;
import org.janelia.workstation.core.model.search.ResultIterator;
import org.janelia.workstation.core.model.search.ResultIteratorFind;
import org.janelia.workstation.core.model.search.ResultPage;
import org.janelia.workstation.core.model.search.SearchResults;
import org.janelia.workstation.core.util.ConcurrentUtils;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.common.gui.support.PreferenceSupport;
import org.janelia.workstation.common.gui.support.SearchProvider;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.openide.windows.TopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A panel that displays a paginated result set inside of a user-selectable ListViewer.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class PaginatedResultsPanel<T,S> extends JPanel implements FindContext {

    private static final Logger log = LoggerFactory.getLogger(PaginatedResultsPanel.class);
    
    // Splash panel
    protected final JLabel splashPanel;

    // Result view
    protected ListViewer<T,S> resultsView;

    // Status bar
    protected FindToolbar findToolbar;
    protected final JPanel bottomBar;
    protected final JPanel statusBar;
    protected final JLabel statusLabel;
    protected final JPanel selectionButtonContainer;
    protected final JButton prevPageButton;
    protected final JButton nextPageButton;
    protected final JButton endPageButton;
    protected final JButton startPageButton;
    protected final JButton selectAllButton;
    protected final JLabel pagingStatusLabel;
    protected final DropDownButton viewTypeButton;

    // Content
    protected SearchResults<T,S> searchResults;
    protected ResultPage<T,S> resultPage;
    protected int numPages = 0;
    protected int currPage = 0;
    
    // State
    protected ChildSelectionModel<T,S> selectionModel;
    protected ChildSelectionModel<T,S> editSelectionModel;
    protected PreferenceSupport preferenceSupport;
    protected SearchProvider searchProvider;
    protected List<? extends ListViewerClassProvider> validViewerTypes;
    protected ImageModel<T, S> imageModel;
    
    public PaginatedResultsPanel(
            ChildSelectionModel<T,S> selectionModel,
            ChildSelectionModel<T,S> editSelectionModel,
            PreferenceSupport preferenceSupport, 
            SearchProvider searchProvider, 
            List<? extends ListViewerClassProvider> validViewerTypes) {
        this(selectionModel, editSelectionModel, preferenceSupport, searchProvider, validViewerTypes, null);
    }
    
    public PaginatedResultsPanel(
            ChildSelectionModel<T,S> selectionModel, 
            ChildSelectionModel<T,S> editSelectionModel,
            PreferenceSupport preferenceSupport, 
            SearchProvider searchProvider, 
            List<? extends ListViewerClassProvider> validViewerTypes,
            ImageModel<T, S> imageModel) {
               
        this.selectionModel = selectionModel;
        this.editSelectionModel = editSelectionModel;
        this.preferenceSupport = preferenceSupport;
        this.searchProvider = searchProvider;
        this.validViewerTypes = validViewerTypes;
        this.imageModel = imageModel;
        
        if (validViewerTypes==null || validViewerTypes.isEmpty()) {
            throw new IllegalArgumentException("PaginatedResultsPanel needs at least one valid viewer type");
        }
        
        setLayout(new BorderLayout());
        
        splashPanel = new JLabel(Icons.getIcon("workstation_logo_white.png"));
        add(splashPanel);
        
        viewTypeButton = new DropDownButton("Choose Viewer...");
        populateViewerPopupMenu(viewTypeButton);

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

        setViewerType(validViewerTypes.get(0));
    }

    private void populateViewerPopupMenu(DropDownButton button) {
        for(final ListViewerClassProvider type : validViewerTypes) {
            JMenuItem viewItem = new JMenuItem(type.getName());
            viewItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    ActivityLogHelper.logUserAction("PaginatedResultsPanel.setViewerType", type.getName());
                    setViewerType(type);

                    final List<T> selectedObjects = getPageObjects(selectionModel.getSelectedIds());

                    updateResultsView(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            // Reselect the items that were selected
                            log.info("Reselecting {} objects in the {} viewer",selectedObjects.size(),type.getName());
                            resultsView.select(selectedObjects, true, true, true, false);
                            return null;
                        }
                    });
                }
            });
            button.addMenuItem(viewItem);
        }
    }
    
    public void setViewerType(final ListViewerClassProvider viewerType) {
        
        if (!validViewerTypes.contains(viewerType)) {
            throw new IllegalArgumentException("Viewer type is not in valid list: "+viewerType);
        }
        
        this.viewTypeButton.setText(viewerType.getName());
        try {
            if (viewerType.getViewerClass()==null) {
                setViewer(null);
            }
            else {
                @SuppressWarnings("unchecked")
                ListViewer<T,S> viewer = (ListViewer<T, S>) viewerType.getViewerClass().newInstance();
                setViewer(viewer);
            }
        }
        catch (InstantiationException | IllegalAccessException e) {
            log.error("Error instantiating viewer class",e);
            setViewer(null);
        }
    }

    public void activate() {
        if (resultsView!=null) {
            resultsView.activate();
            Events.getInstance().registerOnEventBus(resultsView);
        }
    }

    public void deactivate() {
        if (resultsView!=null) {
            resultsView.deactivate();
            Events.getInstance().unregisterOnEventBus(resultsView);
        }
    }
    
    public ListViewer<T,S> getViewer() {
        return resultsView;
    }
    
    private void setViewer(ListViewer<T,S> viewer) {
        if (resultsView!=null) {
            Events.getInstance().unregisterOnEventBus(resultsView);
        }
        this.resultsView = viewer;
        if (resultsView != null) {
            resultsView.getPanel().addMouseListener(new MouseForwarder(this, "ListViewer->PaginatedResultsPanel"));
            resultsView.setActionListener(new ListViewerActionListener() {
                @Override
                public void visibleObjectsChanged() {
                    updateStatusBar();
                }
            });
            Events.getInstance().registerOnEventBus(resultsView);
            resultsView.setSelectionModel(selectionModel);
            resultsView.setEditSelectionModel(editSelectionModel);
            resultsView.setPreferenceSupport(preferenceSupport);
            resultsView.setSearchProvider(searchProvider);
            if (imageModel != null) {
                resultsView.setImageModel(imageModel);
            }
        }
    }

    public ImageModel<T, S> getImageModel() {
        return imageModel;
    }

    public void setImageModel(ImageModel<T, S> imageModel) {
        this.imageModel = imageModel;
        if (resultsView != null) {
            resultsView.setImageModel(imageModel);
        }
    }

    private void loadAndSelectAll() {
        
        if (!searchResults.isAllLoaded()) {
            int rv = JOptionPane.showConfirmDialog(FrameworkImplProvider.getMainFrame(),
                    "Load all "+searchResults.getNumTotalResults()+" results?",
                    "Load all?", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (rv == JOptionPane.YES_OPTION) {
                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        for(int i=0; i<searchResults.getNumTotalPages(); i++) {
                            searchResults.getPage(i);
                            setProgress(i, searchResults.getNumTotalPages());
                        }
                    }

                    @Override
                    protected void hadSuccess() {
                        selectAll();
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        FrameworkImplProvider.handleException(error);
                    }
                };

                worker.setProgressMonitor(new ProgressMonitor(FrameworkImplProvider.getMainFrame(), "Loading result pages...", "", 0, 100));
                worker.execute();
            }
        }
        else {
            UIUtils.setWaitingCursor(FrameworkImplProvider.getMainFrame());
            selectAll();
            UIUtils.setDefaultCursor(FrameworkImplProvider.getMainFrame());
        }
    }
    
    private void selectAll() {
        boolean clearAll = true;
        List<T> allObjects = new ArrayList<>();
        for(ResultPage<T,S> page : searchResults.getPages()) {
            if (page==null) continue; // Page not yet loaded
            allObjects.addAll(page.getObjects());
        }
        selectionModel.select(allObjects, clearAll, true);
    }

    protected void updatePagingStatus() {
        startPageButton.setEnabled(numPages>0 && currPage != 0);
        prevPageButton.setEnabled(numPages>0 && currPage > 0);
        nextPageButton.setEnabled(numPages>0 && currPage < numPages - 1);
        endPageButton.setEnabled(numPages>0 && currPage != numPages - 1);
    }
    
    public void updateStatusBar() {
        if (resultPage==null || resultsView==null) {
            statusLabel.setText("");
            selectionButtonContainer.setVisible(false);
        }
        else {
            long pn = resultPage.getNumPageResults();
            long tn = resultPage.getNumTotalResults();
            long numItemsHidden = resultsView.getNumItemsHidden();
            long numItemsVisible = pn - numItemsHidden;
            long numItemsSelected = selectionModel.getSelectedIds().size();
            String status = numItemsSelected + " of " + tn + " selected";
            if (numItemsHidden > 0) {
                status += " ("+numItemsHidden+" hidden)";
            }
            statusLabel.setText(status);
            selectionButtonContainer.setVisible(numItemsSelected==numItemsVisible && tn>pn);
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
    
    public void showSearchResults(SearchResults<T,S> searchResults, boolean isUserDriven, Callable<Void> success) {

        if (this.searchResults==null) {
            // First load into this panel, so we need a top-level loading indicator, since the resultsView isn't active yet
            showLoadingIndicator();
        }

        this.searchResults = searchResults;
        this.numPages = searchResults.getNumTotalPages();
        
        showCurrPage(isUserDriven, success);
    }

    public void reset() {
        selectionModel.reset();
        if (editSelectionModel != null) {
            editSelectionModel.reset();
        }
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
        
        log.info("showCurrPage(isUserDriven={}, currPage={})",isUserDriven, currPage);

        if (currPage>0 && currPage >= numPages) {
            log.warn("currPage {} is outside of page count ({}), resetting to first page", currPage, numPages);
            currPage = 0;
        }
        
        resultsView.showLoadingIndicator();
        updatePagingStatus();
                
        if (searchResults==null) {
            throw new IllegalStateException("Cannot show page when there are no search results");
        }

        SimpleWorker worker = new SimpleWorker() {
        
            @Override
            protected void doStuff() throws Exception {
                resultPage = getPage(searchResults, currPage);
                log.info("Got page {} with {} results", currPage, resultPage.getNumPageResults());
            }

            @Override
            protected void hadSuccess() {
                final ArrayList<S> selectedRefs = new ArrayList<>(selectionModel.getSelectedIds());
                log.info("Got selected refs: {}",selectedRefs);
                updateResultsView(new Callable<Void>() {   
                    @Override
                    public Void call() throws Exception {

                        TopComponent topComponent = UIUtils.getAncestorWithType(PaginatedResultsPanel.this, TopComponent.class);
                        boolean notifyModel = topComponent==null || topComponent.isVisible();
                    
                        log.info("updateResultsView complete, restoring selection");
                        if (selectedRefs.isEmpty()) {
                            // If the selection model is empty, just select the first item to make it appear in the inspector
                            List<T> objects = resultPage.getObjects();
                            if (!objects.isEmpty()) {
                                log.debug("Auto-selecting first object");
                                resultsView.select(Arrays.asList(objects.get(0)), true, true, true, notifyModel);
                            }
                        }
                        else {
                            // There's already something in the selection model, so we should attempt to reselect it
                            log.debug("Reselecting {} objects",selectedRefs.size());
                            List<T> objects = getPageObjects(selectedRefs);
                            resultsView.select(objects, true, true, false, notifyModel);
                        }
                            
                        resultsView.refreshEditMode();
                        
                        updateStatusBar();
                        
                        ConcurrentUtils.invokeAndHandleExceptions(success);
                        return null;
                    }
                });
            }

            @Override
            protected void hadError(Throwable error) {
                showNothing();
                FrameworkImplProvider.handleException(error);
            }
        };
        
        worker.execute();
    }
    
    protected List<T> getPageObjects(List<S> selectedRefs) {
        final List<T> objects = new ArrayList<>();
        for(S id : selectionModel.getSelectedIds()) {
            try {
                T object = resultPage.getObjectById(id);
                if (object!=null) {
                    objects.add(object);
                }
            }  catch (Exception e) {
                FrameworkImplProvider.handleException(e);
            }
        }
        
        return objects;
    }

    
    private void updateResultsView(final Callable<Void> success) {
        selectionModel.reset();
        if (resultPage!=null) {
            resultsView.show(resultPage, new Callable<Void>() {
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
    
    protected abstract ResultPage<T,S> getPage(SearchResults<T,S> searchResults, int page) throws Exception;

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

        UIUtils.setWaitingCursor(FrameworkImplProvider.getMainFrame());
        if (!matchDebouncer.queueWithParameters(success, params)) {
            return;
        }

        SimpleWorker worker = createFindWorker(text, bias, skipStartingNode);
        worker.execute();
    }
    
    private SimpleWorker createFindWorker(final String text, final Position.Bias bias, final boolean skipStartingNode) {

        return new SimpleWorker() {

            private ResultIteratorFind<T,S> searcher;
            private T match;
            private Integer matchPage;

            @Override
            protected void doStuff() throws Exception {
                System.currentTimeMillis();
                T startObject = null;
                S lastSelectedRef = selectionModel.getLastSelectedId();
                log.info("lastSelectedId={}", lastSelectedRef);
                if (lastSelectedRef!=null) {
                    startObject = resultPage.getObjectById(lastSelectedRef);
                }
                else {
                    // This is generally unexpected, because the viewer should
                    // select the first item automatically, and there is no way to deselect everything.
                    if (resultPage.getObjects()!=null && !resultPage.getObjects().isEmpty()) {
                        log.warn("No 'last selected object' in selection model! Defaulting to first object...");
                        startObject = resultPage.getObjects().get(0);
                    }
                }

                int index = 0;
                Integer foundIndex = null;
                for (T object : resultPage.getObjects()) {
                    if (areEqual(object, startObject)) {
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
                int globalStartIndex = currPage* SearchResults.PAGE_SIZE + foundIndex;
                log.debug("globalStartIndex={}",globalStartIndex);
                ResultIterator<T,S> resultIterator = new ResultIterator<T,S>(searchResults, globalStartIndex, bias, skipStartingNode);
                searcher = new ResultIteratorFind<T,S>(resultIterator) {
                    @Override
                    protected boolean matches(ResultPage<T, S> resultPage, T object) {
                        return resultsView.matches(resultPage, object, text);
                    }
                };
                match = searcher.find();
                matchPage = resultIterator.getCurrPage();
            }

            @Override
            protected void hadSuccess() {
                UIUtils.setDefaultCursor(FrameworkImplProvider.getMainFrame());
                if (match != null) {
                    log.info("Found match for '{}': {}", text, match);
                    if (matchPage!=null && matchPage!=currPage) {
                        log.trace("Match page ({}) differs from current page ({})", matchPage, currPage);
                        currPage = matchPage;
                        selectionModel.select(Arrays.asList(match), true, true);
                        showCurrPage(false); // isUserDriven=false in order to "reselect" the match
                    }
                    else {
                        resultsView.select(Arrays.asList(match), true, true, true, true);
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
                UIUtils.setDefaultCursor(FrameworkImplProvider.getMainFrame());
                matchDebouncer.failure();
                FrameworkImplProvider.handleException(error);
            }
        };
    }

    @Override
    public void openMatch() {
    }

    /**
     * Returns the id for the given object.
     * TODO: merge with getId in SelectionModel
     * @param object
     * @return
     */
    public abstract S getId(T object);
    
    public T getObject(S id) {
        T object = null;
        for (ResultPage<T, S> page : searchResults.getPages()) {
            object = page.getObjectById(id);
            if (object!=null) return object;
        }
        return object;
    }

    private boolean areEqual(T o1, T o2) {
        if (o1==null || o2==null) return false;
        S id1 = getId(o1);
        S id2 = getId(o2);
        if (id1==null || id2==null) return false;
        return id1.equals(id2);
    }
}
