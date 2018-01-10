package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.BorderLayout;
import java.awt.Dimension;
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
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.events.Events;
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
import org.janelia.it.workstation.browser.util.ConcurrentUtils;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.gui.colordepth.ColorDepthMask;
import org.janelia.model.domain.gui.colordepth.ColorDepthResult;
import org.janelia.model.domain.gui.colordepth.ColorDepthSearch;
import org.janelia.model.domain.sample.DataSet;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

    // Utilities
    private final Debouncer debouncer = new Debouncer();
    
    // UI Components
    private final ConfigPanel configPanel;
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
        thresholdSlider = new JSlider(0, 255);
        thresholdSlider.putClientProperty("Slider.paintThumbArrowShape", Boolean.TRUE);
        thresholdSlider.setMaximumSize(new Dimension(120, Integer.MAX_VALUE));
        setThreshold(DEFAULT_THRESHOLD_VALUE);
        thresholdSlider.addChangeListener((ChangeEvent e) -> {
            setThreshold(thresholdSlider.getValue());
        });
        thresholdPanel = new JPanel(new BorderLayout());
        thresholdPanel.add(thresholdLabel, BorderLayout.NORTH);
        thresholdPanel.add(thresholdSlider, BorderLayout.CENTER);
        
        pctPxField = new JTextField("10.00");
        pctPxPanel = new JPanel(new BorderLayout());
        pctPxPanel.add(new JLabel("% of Positive PX Threshold"), BorderLayout.NORTH);
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

        searchButton = new JButton("Run Search");
        
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
//            
//            @Override
//            protected void popupTriggered(MouseEvent e, SelectablePanel resultPanel) {
//                if (resultPanel instanceof PipelineResultPanel) {
//                    SampleResultContextMenu popupMenu = new SampleResultContextMenu(((PipelineResultPanel)resultPanel).getResult());
//                    popupMenu.addMenuItems();
//                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
//                }
//                else if (resultPanel instanceof PipelineErrorPanel) {
//                    SampleErrorContextMenu popupMenu = new SampleErrorContextMenu(((PipelineErrorPanel)resultPanel).getRun());
//                    popupMenu.addMenuItems();
//                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
//                }
//            }
//            
//            @Override
//            protected void doubleLeftClicked(MouseEvent e, SelectablePanel resultPanel) {
//                if (resultPanel instanceof PipelineResultPanel) {
//                    SampleResultContextMenu popupMenu = new SampleResultContextMenu(((PipelineResultPanel)resultPanel).getResult());
//                    popupMenu.runDefaultAction();
//                }
//                else if (resultPanel instanceof PipelineErrorPanel) {
//                    SampleErrorContextMenu popupMenu = new SampleErrorContextMenu(((PipelineErrorPanel)resultPanel).getRun());
//                    popupMenu.runDefaultAction();
//                }
//            }
            
        };

        maskScrollPane = new JScrollPane();
        maskScrollPane.setBorder(BorderFactory.createEmptyBorder());
        maskScrollPane.setViewportView(maskPanel);
        maskScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        maskScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS); 
        
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, maskScrollPane, resultPanel);
        splitPane.setDividerLocation(0.25);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        
        Dimension minimumSize = new Dimension(200, 0);
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
        
        if (colorDepthSearch.getPctPositivePixels()!=null) {
            pctPxField.setText(PX_FORMATTER.format(colorDepthSearch.getPctPositivePixels()));
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
        thresholdLabel.setText(THRESHOLD_LABEL_PREFIX+threshold);
    }
    
    @Override
    public DomainObjectSelectionModel getSelectionModel() {
        return selectionModel;
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
    }


//    @Subscribe
//    public void domainObjectInvalidated(DomainObjectInvalidationEvent event) {
//        try {
//		    if (sample==null) return;
//            if (event.isTotalInvalidation()) {
//                log.info("total invalidation, reloading...");
//                Sample updatedSample = DomainMgr.getDomainMgr().getModel().getDomainObject(sample);
//                if (updatedSample!=null) {
//                    loadDomainObject(updatedSample, false, null);
//                }
//            }
//            else {
//                for (DomainObject domainObject : event.getDomainObjects()) {
//                    if (StringUtils.areEqual(domainObject.getId(), sample.getId())) {
//                        log.info("Sample invalidated, reloading...");
//                        Sample updatedSample = DomainMgr.getDomainMgr().getModel().getDomainObject(sample);
//                        if (updatedSample!=null) {
//                            loadDomainObject(updatedSample, false, null);
//                        }
//                        break;
//                    }
//                    else if (lsms!=null) {
//                        for(LSMImage lsm : lsms) {
//                            if (StringUtils.areEqual(domainObject.getId(), lsm.getId())) {
//                                log.info("LSM invalidated, reloading...");
//                                Sample updatedSample = DomainMgr.getDomainMgr().getModel().getDomainObject(sample);
//                                if (updatedSample!=null) {
//                                    loadDomainObject(updatedSample, false, null);
//                                }
//                                break;
//                            }
//                        }
//                    }
//                }
//            }
//        }  catch (Exception e) {
//            ConsoleApp.handleException(e);
//        }
//    }

//    @Subscribe
//    public void domainObjectRemoved(DomainObjectRemoveEvent event) {
//        if (sample==null) return;
//        if (StringUtils.areEqual(event.getDomainObject().getId(), sample.getId())) {
//            this.sample = null;
//            if (currRunMap!=null) currRunMap.clear();
//            if (lsms!=null) lsms.clear();
//            if (lsmAnnotations!=null) lsmAnnotations.clear();
//            showNothing();
//        }
//    }
//        
//    @Subscribe
//    public void domainObjectSelected(DomainObjectSelectionEvent event) {
//        // Forward to LSM panel
//        if (lsmPanel!=null) {
//            lsmPanel.domainObjectSelected(event);
//        }
//    }
//
//    @Subscribe
//    public void domainObjectChanged(DomainObjectChangeEvent event) {
//        // Forward to LSM panel
//        if (lsmPanel!=null) {
//            lsmPanel.domainObjectChanged(event);
//        }
//    }
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
