package org.janelia.it.workstation.browser.gui.colordepth;

import static org.janelia.it.workstation.browser.api.DomainMgr.getDomainMgr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.api.FileMgr;
import org.janelia.it.workstation.browser.api.web.AsyncServiceClient;
import org.janelia.it.workstation.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.model.DomainObjectChangeEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectRemoveEvent;
import org.janelia.it.workstation.browser.events.selection.DomainObjectSelectionEvent;
import org.janelia.it.workstation.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.browser.gui.editor.ConfigPanel;
import org.janelia.it.workstation.browser.gui.editor.DomainObjectEditor;
import org.janelia.it.workstation.browser.gui.editor.DomainObjectEditorState;
import org.janelia.it.workstation.browser.gui.editor.DomainObjectNodeSelectionEditor;
import org.janelia.it.workstation.browser.gui.editor.SelectionButton;
import org.janelia.it.workstation.browser.gui.hud.Hud;
import org.janelia.it.workstation.browser.gui.listview.PaginatedResultsPanel;
import org.janelia.it.workstation.browser.gui.support.Debouncer;
import org.janelia.it.workstation.browser.gui.support.LoadedImagePanel;
import org.janelia.it.workstation.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.browser.gui.support.SelectablePanel;
import org.janelia.it.workstation.browser.gui.support.SelectablePanelListPanel;
import org.janelia.it.workstation.browser.model.search.ResultPage;
import org.janelia.it.workstation.browser.model.search.SearchResults;
import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;
import org.janelia.it.workstation.browser.nodes.NodeUtils;
import org.janelia.it.workstation.browser.util.ConcurrentUtils;
import org.janelia.it.workstation.browser.workers.AsyncServiceMonitoringWorker;
import org.janelia.it.workstation.browser.workers.BackgroundWorker;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.colordepth.ColorDepthMask;
import org.janelia.model.domain.gui.colordepth.ColorDepthResult;
import org.janelia.model.domain.gui.colordepth.ColorDepthSearch;
import org.janelia.model.domain.sample.DataSet;
import org.janelia.model.domain.workspace.TreeNode;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.Subscribe;


/**
 * Specialized component for executing color depth searches on the cluster and viewing their results.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthSearchEditorPanel extends JPanel implements DomainObjectEditor<ColorDepthSearch>, DomainObjectNodeSelectionEditor<ColorDepthSearch>, SearchProvider {

    private final static Logger log = LoggerFactory.getLogger(ColorDepthSearchEditorPanel.class);

    // Constants
    private static final String PREFERENCE_KEY = "ColorDepthSearchEditor";
    private static final String THRESHOLD_LABEL_PREFIX = "Data Threshold: ";
    private static final int DEFAULT_THRESHOLD_VALUE = 100;
    private static final NumberFormat PX_FORMATTER = new DecimalFormat("#0.00");

    private static final String PCT_POSITIVE_THRESHOLD = "% of Positive PX Threshold";

    private static final String DEFAULT_PCT_PC = "10.00";

    // Utilities
    private final Debouncer debouncer = new Debouncer();
    private AsyncServiceClient asyncServiceClient = new AsyncServiceClient();
    
    // UI Components
    private final ConfigPanel configPanel;
    // TODO: IMPLEMENT THESE FEATURES
//    private final JButton saveButton;
//    private final JButton saveAsButton;
    private final JSplitPane splitPane;
    private final JPanel dataSetPanel;
    private final JPanel pctPxPanel;
    private final JPanel thresholdPanel;
    private final JSlider thresholdSlider;
    private final JTextField pctPxField;
    private final JLabel thresholdLabel;
    private final SelectionButton<DataSet> dataSetButton;
    private final JButton searchButton;
    private final SelectablePanelListPanel maskPanel;
    private final JScrollPane maskScrollPane;
    private final Set<LoadedImagePanel> lips = new HashSet<>();
    private final PaginatedResultsPanel resultPanel;
    
    // Results
    private SearchResults searchResults;
    private final DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();
    
    // State
    private boolean dirty = false;
    private ColorDepthSearch search;
    private List<ColorDepthMask> masks;
    private List<ColorDepthResult> results;
    private List<DataSet> dataSets;
    private String sortCriteria;
    
    public ColorDepthSearchEditorPanel() {

        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());
        setFocusable(true);

        configPanel = new ConfigPanel(true, 15, 10) {
            @Override
            protected void titleClicked(MouseEvent e) {
                Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(this, Arrays.asList(search), true, true, true));
            }
        };
        
        thresholdLabel = new JLabel();
        thresholdSlider = new JSlider(1, 255);
        thresholdSlider.putClientProperty("Slider.paintThumbArrowShape", Boolean.TRUE);
        thresholdSlider.setMaximumSize(new Dimension(120, Integer.MAX_VALUE));
        thresholdSlider.addChangeListener((ChangeEvent e) -> {
            setThreshold(thresholdSlider.getValue());
        });
        thresholdPanel = new JPanel(new BorderLayout());
        thresholdPanel.add(thresholdLabel, BorderLayout.NORTH);
        thresholdPanel.add(thresholdSlider, BorderLayout.CENTER);
        
        pctPxField = new JTextField(DEFAULT_PCT_PC);
        pctPxPanel = new JPanel(new BorderLayout());
        pctPxPanel.add(new JLabel(PCT_POSITIVE_THRESHOLD), BorderLayout.NORTH);
        pctPxPanel.add(pctPxField, BorderLayout.CENTER);

        dataSetButton = new SelectionButton<DataSet>("Data Sets") {

            @Override
            protected Collection<DataSet> getValues() {
                return dataSets.stream().collect(Collectors.toSet());
            }

            @Override
            protected Set<String> getSelectedValueNames() {
                return search.getDataSets().stream().collect(Collectors.toSet());
            }

            @Override
            protected String getName(DataSet value) {
                return value.getIdentifier();
            }

            @Override
            protected void clearSelected() {
                search.getDataSets().clear();
                dirty = true;
            }

            @Override
            protected void updateSelection(DataSet dataSet, boolean selected) {
                if (selected) {
                    search.getDataSets().add(dataSet.getIdentifier());
                }
                else {
                    search.getDataSets().remove(dataSet.getIdentifier());
                }
                dirty = true;
            }
            
        };
        
        dataSetPanel = new JPanel(new BorderLayout());
        dataSetPanel.add(new JLabel("Data sets to search:"), BorderLayout.NORTH);
        dataSetPanel.add(dataSetButton, BorderLayout.CENTER);

        searchButton = new JButton("Execute Search");
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                if (search.getMasks().isEmpty()) {
                    JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), "You need to select some masks to search on.");
                    return;
                }
                
                if (search.getDataSets().isEmpty()) {
                    JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), "You need to select some data sets to search against.");
                    return;
                }
                
                int result = JOptionPane.showConfirmDialog(ColorDepthSearchEditorPanel.this, 
                        "Are you sure you want to queue this search to run on the compute cluster?");
                if (result != 0) return;
                
                SimpleWorker worker = new SimpleWorker() {
                        
                    @Override
                    protected void doStuff() throws Exception {
                        populateSearchFromForm();
                        DomainModel model = getDomainMgr().getModel();
                        search = model.save(search);
                        executeSearch();
                    }

                    @Override
                    protected void hadSuccess() {

                    }

                    @Override
                    protected void hadError(Throwable error) {
                        ConsoleApp.handleException(error);
                    }
                };

                worker.execute();
                
            }
        });
        
        
        resultPanel = new PaginatedResultsPanel(selectionModel, this) {
            @Override
            protected ResultPage getPage(SearchResults searchResults, int page) throws Exception {
                return searchResults.getPage(page);
            }
        };

        maskPanel = new SelectablePanelListPanel() {

            @Override
            protected void panelSelected(SelectablePanel resultPanel, boolean isUserDriven) {
                if (resultPanel instanceof MaskPanel) {
                    ColorDepthMask mask = ((MaskPanel)resultPanel).getMask();
                    loadMaskResults(mask, true);
                }
            }
            
            @Override
            protected void updateHud(SelectablePanel resultPanel, boolean toggle) {
                if (resultPanel instanceof MaskPanel) {
                    ColorDepthMask mask = ((MaskPanel)resultPanel).getMask();
                    Hud hud = Hud.getSingletonInstance();
                    if (toggle) {
                        hud.setObjectAndToggleDialog(mask, null, null);
                    }
                    else {
                        hud.setObject(mask, null, null, true);
                    }
                }
            }
            
            @Override
            protected void popupTriggered(MouseEvent e, SelectablePanel resultPanel) {
                MaskContextMenu popupMenu = new MaskContextMenu(search, ((MaskPanel)resultPanel).getMask());
                popupMenu.addMenuItems();
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
            
        };

        maskScrollPane = new JScrollPane();
        maskScrollPane.setBorder(BorderFactory.createEmptyBorder());
        maskScrollPane.setViewportView(maskPanel);
        maskScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        maskScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS); 
        
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, maskScrollPane, resultPanel);
        splitPane.setDividerLocation(0.30);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        
        Dimension minimumSize = new Dimension(100, 0);
        maskScrollPane.setMinimumSize(minimumSize);
        resultPanel.setMinimumSize(minimumSize);
        
        maskScrollPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                for(LoadedImagePanel image : lips) {
                    rescaleImage(image);
                    image.invalidate();
                }
            }
        });
    }
    
    private void populateSearchFromForm() {

        Double pctPositivePixels;
        try {
            pctPositivePixels = new Double(pctPxField.getText());
            if (pctPositivePixels<1 || pctPositivePixels>100) {
                throw new NumberFormatException();
            }
            search.setPctPositivePixels(pctPositivePixels);
        }
        catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    ColorDepthSearchEditorPanel.this,
                    PCT_POSITIVE_THRESHOLD+" must be a percentage between 1 and 100",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        
        search.setDataThreshold(thresholdSlider.getValue());
    }
    
    @Override
    public String getSortField() {
        return sortCriteria;
    }

    @Override
    public void setSortField(final String sortCriteria) {
        this.sortCriteria = sortCriteria;
        savePreferences();
    }
    
    @Override
    public void search() {

//        SimpleWorker worker = new SimpleWorker() {
//
//            @Override
//            protected void doStuff() throws Exception {
//                loadPreferences();
//                prepareLsmResults();
//            }
//
//            @Override
//            protected void hadSuccess() {
//                showResults(true);
//            }
//
//            @Override
//            protected void hadError(Throwable error) {
//                showNothing();
//                ConsoleApp.handleException(error);
//            }
//        };
//
//        worker.execute();
    }
    
    @Override
    public void export() {
//        DomainObjectTableViewer viewer = null;
//        if (lsmPanel.getViewer() instanceof DomainObjectTableViewer) {
//            viewer = (DomainObjectTableViewer)lsmPanel.getViewer();
//        }
//        ExportResultsAction<DomainObject> action = new ExportResultsAction<>(lsmSearchResults, viewer);
//        action.actionPerformed(null);
    }

    private void loadPreferences() {
        if (search.getId()==null) return;
        try {
            sortCriteria = FrameworkImplProvider.getRemotePreferenceValue(DomainConstants.PREFERENCE_CATEGORY_SORT_CRITERIA, PREFERENCE_KEY, null);
        }
        catch (Exception e) {
            log.error("Could not load sort criteria",e);
        }
    }

    private void savePreferences() {
        if (StringUtils.isEmpty(sortCriteria)) return;
        try {
            FrameworkImplProvider.setRemotePreferenceValue(DomainConstants.PREFERENCE_CATEGORY_SORT_CRITERIA, PREFERENCE_KEY, sortCriteria);
        }
        catch (Exception e) {
            log.error("Could not save sort criteria",e);
        }
    }

    private void reload() throws Exception {
        
        if (search==null) {
            // Nothing to reload
            return;
        }
        
        ColorDepthSearch updatedSearch = DomainMgr.getDomainMgr().getModel().getDomainObject(search.getClass(), search.getId());
        if (updatedSearch!=null) {
//            if (treeNodeNode!=null && !treeNodeNode.getTreeNode().equals(updatedTreeNode)) {
//                treeNodeNode.update(updatedTreeNode);
//            }
//            this.treeNode = updatedTreeNode;
//            restoreState(saveState());
            loadDomainObject(updatedSearch, false, null);
        }
        else {
            // The folder no longer exists, or we no longer have access to it (perhaps running as a different user?) 
            // Either way, there's nothing to show. 
            showNothing();
        }
    }
    
    @Override
    public String getName() {
        if (search==null) {
            return "Color Depth Search";
        }
        return StringUtils.abbreviate(search.getName(), 15);
    }
    
    @Override
    public Object getEventBusListener() {
        return this;
    }

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }

    @Override
    public void loadDomainObjectNode(AbstractDomainObjectNode<ColorDepthSearch> domainObjectNode, boolean isUserDriven, Callable<Void> success) {
        loadDomainObject(domainObjectNode.getDomainObject(), isUserDriven, success);
    }

    @Override
    public void loadDomainObject(final ColorDepthSearch colorDepthSearch, final boolean isUserDriven, final Callable<Void> success) {

        if (colorDepthSearch==null) return;
        
        if (!debouncer.queue(success)) {
            log.info("Skipping load, since there is one already in progress");
            return;
        }
        
        log.info("loadDomainObject({},isUserDriven={})",colorDepthSearch.getName(),isUserDriven);
        final StopWatch w = new StopWatch();

        configPanel.setTitle(colorDepthSearch.getName()+" ("+colorDepthSearch.getAlignmentSpace()+")");
        
        if (colorDepthSearch.getDataThreshold()!=null) {
            setThreshold(colorDepthSearch.getDataThreshold());
        }
        else {
            setThreshold(DEFAULT_THRESHOLD_VALUE);
        }
        
        if (colorDepthSearch.getPctPositivePixels()!=null) {
            pctPxField.setText(PX_FORMATTER.format(colorDepthSearch.getPctPositivePixels()));
        }
        else {
            pctPxField.setText(DEFAULT_PCT_PC);
        }
        
        selectionModel.setParentObject(colorDepthSearch);
        this.dirty = false;
        
        this.search = colorDepthSearch;
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                masks = model.getDomainObjectsAs(ColorDepthMask.class, colorDepthSearch.getMasks());
                results = model.getDomainObjectsAs(ColorDepthResult.class, colorDepthSearch.getResults());
                dataSets = model.getColorDepthDataSets(colorDepthSearch.getAlignmentSpace());
                loadPreferences();
//                prepareResults();
            }
            
            @Override
            protected void hadSuccess() {
                showUI(isUserDriven);
                ConcurrentUtils.invokeAndHandleExceptions(success);
                debouncer.success();
                ActivityLogHelper.logElapsed("ColorDepthSearchEditorPanel.loadDomainObject", search, w);
            }
            
            @Override
            protected void hadError(Throwable error) {
                showNothing();
                debouncer.failure();
                ConsoleApp.handleException(error);
            }
        };
        worker.execute();
    }
    
    public void showNothing() {
        removeAll();
        updateUI();
    }
    
    public void showUI(boolean isUserDriven) {
        showSearchView(isUserDriven);
        updateUI();
    }
    
    private void loadMaskResults(ColorDepthMask mask, boolean isUserDriven) {
    
        // results.get(0);
        
        List<DomainObject> filteredMatches = new ArrayList<>();

        DomainUtils.sortDomainObjects(filteredMatches, sortCriteria);
        searchResults = SearchResults.paginate(filteredMatches, Collections.emptyList());
        
        resultPanel.showSearchResults(searchResults, isUserDriven, null);
        
    }
    
//
//    private void showLsmView(boolean isUserDriven) {
//
//    	configPanel.removeAllConfigComponents();
//        configPanel.addConfigComponent(objectiveButton);
//        configPanel.addConfigComponent(areaButton);
//    	
//        if (lsms!=null) {
//            Set<String> objectiveSet = new LinkedHashSet<>(sample.getObjectives());
//            Set<String> areaSet = new LinkedHashSet<>();
//        	for(LSMImage lsm : lsms) {
//        		objectiveSet.add(lsm.getObjective());
//                    String area = lsm.getAnatomicalArea();
//                    if (area==null) {
//                        area = "Unknown";
//                    }
//                    areaSet.add(area);
//        	}
//        	
//        	List<String> objectives = new ArrayList<>(objectiveSet);
//            objectives.add(0, ALL_VALUE);
//            populateObjectiveButton(objectives);
//            
//            List<String> areas = new ArrayList<>(areaSet);
//            areas.add(0, ALL_VALUE);
//            populateAreaButton(areas);
//    
//            lsmPanel.showSearchResults(lsmSearchResults, isUserDriven, null);
//        }
//        else {
//            lsmPanel.showNothing();
//        }
//        
//        removeAll();
//        add(configPanel, BorderLayout.NORTH);
//        add(lsmPanel, BorderLayout.CENTER);
//    }
//
        
    private void showSearchView(boolean isUserDriven) {
        
        dataSetButton.update();
        
        lips.clear();
        maskPanel.clearPanels();
        configPanel.removeAllConfigComponents();
        configPanel.addConfigComponent(thresholdPanel);
        configPanel.addConfigComponent(pctPxPanel);
        configPanel.addConfigComponent(dataSetPanel);
        configPanel.addConfigComponent(searchButton);
        
        JLabel titleLabel = new JLabel("Search Masks:");
        titleLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
     
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        maskPanel.add(titlePanel);
        
        for(ColorDepthMask mask : masks) {
            maskPanel.addPanel(new MaskPanel(mask));
        }
        
        removeAll();
        add(configPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private void rescaleImage(LoadedImagePanel image) {
        double width = image.getParent()==null?0:image.getParent().getSize().getWidth()-18;
        if (width==0) {
            width = maskScrollPane.getViewport().getSize().getWidth()-18;
        }
        if (width==0) {
            log.warn("Could not get width from parent or viewport");
            return;
        }
        image.scaleImage((int)Math.ceil(width));
    }

    private void setThreshold(int threshold) {
        thresholdSlider.setValue(threshold);
        thresholdLabel.setText(THRESHOLD_LABEL_PREFIX+threshold);
    }
    
    @Override
    public DomainObjectSelectionModel getSelectionModel() {
        return selectionModel;
    }

    private void executeSearch() {

        StringBuilder maskFilepaths = new StringBuilder();
        for (ColorDepthMask colorDepthMask : masks) {
            if (maskFilepaths.length()>0) maskFilepaths.append(',');
            maskFilepaths.append(colorDepthMask.getFilepath());
        }
        
        StringBuilder searchDirs = new StringBuilder();
        for (String dataSetIdentifier : search.getDataSets()) {
            // TODO: search should take care of this
            String filepath = "/nrs/jacs/jacsData/filestore/system/ColorDepthMIPs/"+search.getAlignmentSpace()+"/"+dataSetIdentifier;
            if (searchDirs.length()>0) searchDirs.append(',');
            searchDirs.append(filepath);
        }
        
        ImmutableList.Builder<String> serviceArgsBuilder = ImmutableList.<String>builder()
                .add("-inputFiles", maskFilepaths.toString())
                .add("-searchDirs", searchDirs.toString());
        
        Long serviceId = asyncServiceClient.invokeService("colorDepthSearch",
                serviceArgsBuilder.build(),
                FileMgr.getFileMgr().getSubjectKey(),
                null,
                ImmutableMap.of());
        
        BackgroundWorker executeWorker = new AsyncServiceMonitoringWorker(FileMgr.getFileMgr().getSubjectKey(), serviceId) {
            
            @Override
            public String getName() {
                return "Executing "+search.getName();
            }

            @Override
            public Callable<Void> getSuccessCallback() {
                return new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        return null;
                    }
                };
            }
        };

        executeWorker.executeWithEvents();
    }
    
    @Override
    public void resetState() {
    }

    @Override
    public DomainObjectEditorState<ColorDepthSearch> saveState() {
        return null;
    }

    @Override
    public void restoreState(DomainObjectEditorState<ColorDepthSearch> state) {
        if (state==null) {
            log.warn("Cannot restore null state");
            return;
        }
        
        log.info("Restoring state: {}", state);
//        if (state.getListViewerState()!=null) {
//            getResultsPanel().setViewerType(state.getListViewerState().getType());
//        }

        // Prepare to restore the selection
//        List<Reference> selected = getResultsPanel().getViewer().getSelectionModel().getSelectedIds();
//        selected.clear();
//        selected.addAll(state.getSelectedIds());
        
        // Prepare to restore the page
//        getResultsPanel().setCurrPage(state.getPage());
//
//        getResultsPanel().getViewer().restoreState(state.getListViewerState());
        
        // Prepare to restore viewer state, after the reload
        Callable<Void> success = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
//                if (state.getListViewerState()!=null) {
//                    log.info("State load completed, restoring viewer state {}", state.getListViewerState());
////                    getResultsPanel().getViewer().restoreState(state.getListViewerState());
//                }
                return null;
            }
        };
                
        if (state.getDomainObjectNode()==null) {
            loadDomainObject(state.getDomainObject(), true, success);
        }
        else {
            loadDomainObjectNode(state.getDomainObjectNode(), true, success);
        }
    }


    @Subscribe
    public void domainObjectInvalidated(DomainObjectInvalidationEvent event) {
        try {
		    if (search==null) return;
		    boolean affected = false;
            if (event.isTotalInvalidation()) {
                log.info("total invalidation, reloading...");
                affected = true;
            }
            else {
                for (DomainObject domainObject : event.getDomainObjects()) {
                    if (StringUtils.areEqual(domainObject.getId(), search.getId())) {
                        log.info("Search invalidated, reloading...");
                        affected = true;
                        break;
                    }
                    else if (masks!=null) {
                        for(ColorDepthMask mask : masks) {
                            if (StringUtils.areEqual(domainObject.getId(), mask.getId())) {
                                log.info("Mask invalidated, reloading...");
                                affected = true;
                                break;
                            }
                        }
                    }
                }
            }
        
            if (affected) reload();
        }  
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
    }

    @Subscribe
    public void domainObjectRemoved(DomainObjectRemoveEvent event) {
        if (search==null) return;
        if (StringUtils.areEqual(event.getDomainObject().getId(), search.getId())) {
            this.search = null;
            showNothing();
        }
        else {
            for (ColorDepthMask colorDepthMask : masks) {
                if (StringUtils.areEqual(event.getDomainObject().getId(), search.getId())) {
                    // Refresh
                    loadDomainObject(search, false, null);
                }
            }
        }
    }
//        
//    @Subscribe
//    public void domainObjectSelected(DomainObjectSelectionEvent event) {
//        // Forward to LSM panel
//        if (lsmPanel!=null) {
//            lsmPanel.domainObjectSelected(event);
//        }
//    }
//
    @Subscribe
    public void domainObjectChanged(DomainObjectChangeEvent event) {
        if (search==null) return;
        try {
            if (event.getDomainObject().getId().equals(search)) {
                // Refresh
                reload();
            }
        }  
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
    }
//    
//    @Subscribe
//    public void annotationsChanged(DomainObjectAnnotationChangeEvent event) {
//        // Forward to LSM panel
//        if (lsmPanel!=null) {
//            lsmPanel.annotationsChanged(event);
//        }
//    }
    

    private class MaskPanel extends SelectablePanel {
        
        private final ColorDepthMask mask;
        
        private MaskPanel(ColorDepthMask mask) {
            
            this.mask = mask;
            
            int b = SelectablePanel.BORDER_WIDTH;
            setBorder(BorderFactory.createEmptyBorder(b, b, b, b));
            setLayout(new BorderLayout());
            
            JLabel label = new JLabel();
            label.setText(mask.getName());
            label.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 4));
            add(label, BorderLayout.NORTH);
            
            add(getImagePanel(mask.getFilepath()), BorderLayout.CENTER);

            setFocusTraversalKeysEnabled(false);
        }

        public ColorDepthMask getMask() {
            return mask;
        }
        
        private JPanel getImagePanel(String filepath) {
            LoadedImagePanel lip = new LoadedImagePanel(filepath) {
                @Override
                protected void doneLoading() {
                    rescaleImage(this);
                    revalidate();
                    repaint();
                }
            };
            rescaleImage(lip);
            lip.addMouseListener(new MouseForwarder(this, "LoadedImagePanel->PipelineResultPanel"));
            lips.add(lip);
            return lip;
        }
    }
}
