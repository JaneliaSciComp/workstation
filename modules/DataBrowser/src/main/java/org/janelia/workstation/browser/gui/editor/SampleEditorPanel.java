package org.janelia.workstation.browser.gui.editor;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.eventbus.Subscribe;
import net.miginfocom.swing.MigLayout;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainObjectAttribute;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.SampleUtils;
import org.janelia.model.domain.compute.ContainerizedService;
import org.janelia.model.domain.enums.AlignmentScoreType;
import org.janelia.model.domain.enums.ErrorType;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.interfaces.HasAnatomicalArea;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.interfaces.HasIdentifier;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.NeuronSeparation;
import org.janelia.model.domain.sample.ObjectiveSample;
import org.janelia.model.domain.sample.PipelineError;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SampleAlignmentResult;
import org.janelia.model.domain.sample.SamplePipelineRun;
import org.janelia.model.domain.sample.SampleProcessingResult;
import org.janelia.workstation.browser.actions.ExportResultsAction;
import org.janelia.workstation.browser.gui.hud.Hud;
import org.janelia.workstation.browser.gui.listview.PaginatedDomainResultsPanel;
import org.janelia.workstation.browser.gui.listview.table.DomainObjectTableViewer;
import org.janelia.workstation.browser.gui.support.LoadedImagePanel;
import org.janelia.workstation.browser.gui.support.SampleUIUtils;
import org.janelia.workstation.browser.gui.support.SelectablePanel;
import org.janelia.workstation.browser.gui.support.SelectablePanelListPanel;
import org.janelia.workstation.browser.selection.PipelineErrorSelectionEvent;
import org.janelia.workstation.browser.selection.PipelineResultSelectionEvent;
import org.janelia.workstation.common.gui.editor.DomainObjectEditor;
import org.janelia.workstation.common.gui.editor.ViewerContextProvider;
import org.janelia.workstation.common.gui.listview.ListViewerState;
import org.janelia.workstation.common.gui.support.Debouncer;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.support.MouseForwarder;
import org.janelia.workstation.common.gui.support.PreferenceSupport;
import org.janelia.workstation.common.gui.support.SearchProvider;
import org.janelia.workstation.common.gui.support.buttons.DropDownButton;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.model.DomainObjectAnnotationChangeEvent;
import org.janelia.workstation.core.events.model.DomainObjectChangeEvent;
import org.janelia.workstation.core.events.model.DomainObjectInvalidationEvent;
import org.janelia.workstation.core.events.model.DomainObjectRemoveEvent;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.janelia.workstation.core.events.selection.DomainObjectSelectionEvent;
import org.janelia.workstation.core.events.selection.DomainObjectSelectionModel;
import org.janelia.workstation.core.events.selection.ViewerContextChangeEvent;
import org.janelia.workstation.core.model.Decorator;
import org.janelia.workstation.core.model.DomainModelViewUtils;
import org.janelia.workstation.core.model.ImageModel;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.core.model.descriptors.ResultArtifactDescriptor;
import org.janelia.workstation.core.model.search.DomainObjectSearchResults;
import org.janelia.workstation.core.model.search.ResultPage;
import org.janelia.workstation.core.model.search.SearchResults;
import org.janelia.workstation.core.util.ColorDepthUtils;
import org.janelia.workstation.core.util.ConcurrentUtils;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Specialized component for viewing information about Samples, including their LSMs and processing results.  
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleEditorPanel 
        extends JPanel 
        implements DomainObjectEditor<Sample>, SearchProvider, PreferenceSupport {

    private final static Logger log = LoggerFactory.getLogger(SampleEditorPanel.class);

    // Constants
    private final static String PREFERENCE_KEY = "SampleEditor";
    private final static String MODE_LSMS = "LSMs";
    private final static String MODE_RESULTS = "Results";
    private final static String MODE_COLOR_DEPTH = "Color Depth";
    private final static String ALL_VALUE = "all";
    private final static List<FileType> COLOR_DEPTH_TYPES = Arrays.asList(FileType.ColorDepthMip1, FileType.ColorDepthMip2, FileType.ColorDepthMip3, FileType.ColorDepthMip4);

    // Utilities
    private final Debouncer debouncer = new Debouncer();
    
    // UI Components
    private final ConfigPanel configPanel;
    private final SingleSelectionButton<String> viewButton;
    private final SingleSelectionButton<String> objectiveButton;
    private final SingleSelectionButton<String> areaButton;
    private final SingleSelectionButton<String> alignmentSpaceButton;
    private final Map<String,DropDownButton> historyButtonMap = new HashMap<>();
    private final SelectablePanelListPanel mainPanel;
    private final JScrollPane scrollPane;
    private final Set<LoadedImagePanel> lips = new HashSet<>();
    private final PaginatedDomainResultsPanel lsmPanel;
    
    // Results
    private DomainObjectSearchResults lsmSearchResults;
    private final DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();
    
    // State
    private Sample sample;
    private Map<String,SamplePipelineRun> currRunMap = new HashMap<>();
    private List<LSMImage> lsms;
    private List<Annotation> lsmAnnotations;
    private List<String> objectives;
    private List<String> areas;
    private List<String> alignmentSpaces;
    private String currMode = MODE_RESULTS;
    private String currObjective = ALL_VALUE;
    private String currArea = ALL_VALUE;
    private String currAlignmentSpace;
    private String sortCriteria;
    private Map<Long, ContainerizedService> containers = new HashMap<>();;
    
    public SampleEditorPanel() {

        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());
        setFocusable(true);
        
        this.viewButton = new SingleSelectionButton<String>("View") {
            
            @Override
            public Collection<String> getValues() {
                return Arrays.asList(MODE_LSMS, MODE_RESULTS, MODE_COLOR_DEPTH);
            }

            @Override
            public String getSelectedValue() {
                return currMode;
            }
            
            @Override
            public void updateSelection(String value) {
                setViewMode(value);
            }
        };
        viewButton.update();
        
        this.objectiveButton = new SingleSelectionButton<String>("Objective") {
            
            @Override
            public Collection<String> getValues() {
                return objectives;
            }

            @Override
            public String getSelectedValue() {
                return currObjective;
            }
            
            @Override
            public void updateSelection(String value) {
                setObjective(value);
            }
        };
        
        this.areaButton = new SingleSelectionButton<String>("Area") {
            
            @Override
            public Collection<String> getValues() {
                return areas;
            }

            @Override
            public String getSelectedValue() {
                return currArea;
            }
            
            @Override
            public void updateSelection(String value) {
                setArea(value);
            }
        };
        
        this.alignmentSpaceButton = new SingleSelectionButton<String>("Alignment Space") {
            
            @Override
            public Collection<String> getValues() {
                return alignmentSpaces;
            }

            @Override
            public String getSelectedValue() {
                return currAlignmentSpace;
            }
            
            @Override
            public void updateSelection(String value) {
                setAlignmentSpace(value);
            }
        };
        
        configPanel = new ConfigPanel(true) {
            @Override
            protected void titleClicked(MouseEvent e) {
                Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(this, Arrays.asList(sample), true, true, true));
            }
        };
        configPanel.addTitleComponent(viewButton, true, true);
        
        lsmPanel = new PaginatedDomainResultsPanel(selectionModel, null, this, this) {
            @Override
            protected ResultPage<DomainObject, Reference> getPage(SearchResults<DomainObject, Reference> searchResults, int page) throws Exception {
                return searchResults.getPage(page);
            }
            @Override
            public Reference getId(DomainObject object) {
                return Reference.createFor(object);
            }
            @Override
            protected void viewerContextChanged() {
                Events.getInstance().postOnEventBus(new ViewerContextChangeEvent(this, getViewerContext()));
            }
        };

        mainPanel = new SelectablePanelListPanel() {

            @Override
            protected void updateHud(SelectablePanel resultPanel, boolean toggle) {
                if (resultPanel instanceof PipelineResultPanel) {
                    ArtifactDescriptor resultDescriptor = ((PipelineResultPanel)resultPanel).getResultDescriptor();
                    Hud.getSingletonInstance().setObjectAndToggleDialog(sample, resultDescriptor, null, toggle, true);
                }
                else if (resultPanel instanceof ColorDepthPanel) {
                    ArtifactDescriptor resultDescriptor = ((ColorDepthPanel)resultPanel).getResultDescriptor();
                    FileType fileType = ((ColorDepthPanel)resultPanel).getType();
                    Hud.getSingletonInstance().setObjectAndToggleDialog(sample, resultDescriptor, fileType.toString(), toggle, true);
                }
            }
            
            @Override
            protected void panelSelected(SelectablePanel resultPanel, boolean isUserDriven) {
                if (resultPanel instanceof PipelineResultPanel) {
                    PipelineResultPanel resultPanel2 = (PipelineResultPanel)resultPanel;
                    Events.getInstance().postOnEventBus(new PipelineResultSelectionEvent(this, resultPanel2.getResult(), isUserDriven));
                }
                else if (resultPanel instanceof PipelineErrorPanel) {
                    PipelineErrorPanel resultPanel2 = (PipelineErrorPanel)resultPanel;
                    Events.getInstance().postOnEventBus(new PipelineErrorSelectionEvent(this, resultPanel2.getError(), isUserDriven));
                }
            }
            
            @Override
            protected void popupTriggered(MouseEvent e, SelectablePanel resultPanel) {
                if (e.isConsumed()) {
                    return;
                }
                // TODO: refactor this in the style of the IconGridViewerPanel's popupTriggered

                if (resultPanel instanceof PipelineResultPanel) {
                    SampleResultContextMenu popupMenu = new SampleResultContextMenu(((PipelineResultPanel)resultPanel).getResult());
                    popupMenu.addMenuItems();
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
                else if (resultPanel instanceof PipelineErrorPanel) {
                    SampleErrorContextMenu popupMenu = new SampleErrorContextMenu(((PipelineErrorPanel)resultPanel).getRun());
                    popupMenu.addMenuItems();
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
                else if (resultPanel instanceof ColorDepthPanel) {
                    ColorDepthPanel resultPanel2 = (ColorDepthPanel)resultPanel;
                    ColorDepthContextMenu popupMenu = new ColorDepthContextMenu(sample, resultPanel2.getResultDescriptor(), resultPanel2.getResult(), resultPanel2.getType());
                    popupMenu.addMenuItems();
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            
            @Override
            protected void doubleLeftClicked(MouseEvent e, SelectablePanel resultPanel) {
                if (resultPanel instanceof PipelineResultPanel) {
                    SampleResultContextMenu popupMenu = new SampleResultContextMenu(((PipelineResultPanel)resultPanel).getResult());
                    popupMenu.runDefaultAction();
                }
                else if (resultPanel instanceof PipelineErrorPanel) {
                    SampleErrorContextMenu popupMenu = new SampleErrorContextMenu(((PipelineErrorPanel)resultPanel).getRun());
                    popupMenu.runDefaultAction();
                }
                else if (resultPanel instanceof ColorDepthPanel) {
                    ColorDepthPanel resultPanel2 = (ColorDepthPanel)resultPanel;
                    ColorDepthContextMenu popupMenu = new ColorDepthContextMenu(sample, resultPanel2.getResultDescriptor(), resultPanel2.getResult(), resultPanel2.getType());
                    popupMenu.runDefaultAction();
                }
            }
            
        };

        scrollPane = new JScrollPane();
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportView(mainPanel);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                for(LoadedImagePanel image : lips) {
                    if (MODE_RESULTS.equals(currMode)) {
                        rescaleImage(image, 0.5f);
                    }
                    else if (MODE_COLOR_DEPTH.equals(currMode)) {
                        rescaleImage(image, 1f);
                    }
                    image.invalidate();
                }
            }
        });
    }

    @Override
    public ViewerContext<DomainObject, Reference> getViewerContext() {
        if (MODE_LSMS.equals(currMode)) {
            return new ViewerContext<DomainObject, Reference>() {
                @Override
                public ChildSelectionModel<DomainObject, Reference> getSelectionModel() {
                    return selectionModel;
                }

                @Override
                public ChildSelectionModel<DomainObject, Reference> getEditSelectionModel() {
                    return lsmPanel.isEditMode() ? lsmPanel.getViewer().getEditSelectionModel() : null;
                }

                @Override
                public ImageModel<DomainObject, Reference> getImageModel() {
                    return lsmPanel.getImageModel();
                }
            };
        }
        else {
            // TODO: implement viewer contexts for the other modes
            return null;
        }
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

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                loadPreferences();
                loadContainers();
                prepareLsmResults();
            }

            @Override
            protected void hadSuccess() {
                showResults(true);
            }

            @Override
            protected void hadError(Throwable error) {
                showNothing();
                FrameworkAccess.handleException(error);
            }
        };

        worker.execute();
    }

    @Override
    public void export() {
        DomainObjectTableViewer viewer = null;
        if (lsmPanel.getViewer() instanceof DomainObjectTableViewer) {
            viewer = (DomainObjectTableViewer)lsmPanel.getViewer();
        }
        ExportResultsAction<DomainObject, Reference> action = new ExportResultsAction<>(lsmSearchResults, viewer);
        action.actionPerformed(null);
    }

    private void loadPreferences() {
        if (sample.getId()==null) return;
        try {
            sortCriteria = FrameworkAccess.getRemotePreferenceValue(DomainConstants.PREFERENCE_CATEGORY_SORT_CRITERIA, PREFERENCE_KEY, null);
        }
        catch (Exception e) {
            log.error("Could not load sort criteria",e);
        }
    }

    private void savePreferences() {
        if (StringUtils.isEmpty(sortCriteria)) return;
        try {
            FrameworkAccess.setRemotePreferenceValue(DomainConstants.PREFERENCE_CATEGORY_SORT_CRITERIA, PREFERENCE_KEY, sortCriteria);
        }
        catch (Exception e) {
            log.error("Could not save sort criteria",e);
        }
    }
    
    private void loadContainers() {
        log.info("Loading containers");
        synchronized (this) {
            this.containers = new HashMap<>();
            for (ContainerizedService container : DomainMgr.getDomainMgr().getModel().getContainerizedServices()) {
                log.debug("Found service '{}' ({})", container.getName(), container.getId());
                containers.put(container.getId(), container);
            }
        }
    }
    
    @Override
    public String getName() {
        if (sample==null) {
            return "Sample Editor";
        }
        return "Sample: "+StringUtils.abbreviate(sample.getName(), 15);
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
    public void loadDomainObject(final Sample sample, final boolean isUserDriven, final Callable<Void> success) {

        if (sample==null) return;
        
        if (!debouncer.queue(success)) {
            log.info("Skipping load, since there is one already in progress");
            return;
        }
        
        log.info("loadDomainObject({},isUserDriven={})",sample.getName(),isUserDriven);
        final StopWatch w = new StopWatch();

        // Save the scroll horizontal position on the table, so that users can compare attributes more easily
        final ListViewerState viewerState = MODE_LSMS.equals(currMode) ? lsmPanel.getViewer().saveState() : null;

        currRunMap.clear();
        configPanel.setTitle(sample.getName());
        selectionModel.setParentObject(sample);
        
        this.sample = sample;
        this.lsms = null;
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                if (MODE_LSMS.equals(currMode))  {
                    DomainModel model = DomainMgr.getDomainMgr().getModel();
                    lsms = model.getLsmsForSample(sample);
                    lsmAnnotations = model.getAnnotations(DomainUtils.getReferences(lsms));
                    loadPreferences();
                    prepareLsmResults();
                }
                else {
                    loadContainers();
                }
            }
            
            @Override
            protected void hadSuccess() {
                showResults(isUserDriven);
                
                if (MODE_LSMS.equals(currMode))  {
                    lsmPanel.getViewer().restoreState(viewerState);
                }
                else {
                    mainPanel.selectFirst(isUserDriven);
                }
                
                ConcurrentUtils.invokeAndHandleExceptions(success);
                debouncer.success();
                Events.getInstance().postOnEventBus(new ViewerContextChangeEvent(lsmPanel, getViewerContext()));
                ActivityLogHelper.logElapsed("SampleEditorPanel.loadDomainObject", sample, w);
            }
            
            @Override
            protected void hadError(Throwable error) {
                showNothing();
                debouncer.failure();
                FrameworkAccess.handleException(error);
            }
        };
        worker.execute();
    }
    
    public void showNothing() {
        removeAll();
        updateUI();
    }
    
    public void showResults(boolean isUserDriven) {
        if (MODE_LSMS.equals(currMode))  {
            showLsmView(isUserDriven);
        }
        else if (MODE_RESULTS.equals(currMode)) {
            showResultView(isUserDriven);
        }
        else if (MODE_COLOR_DEPTH.equals(currMode)) {
            showColorDepthView(isUserDriven);
        }
        updateUI();
    }

    private void prepareLsmResults() {

        if (lsms==null) return;
        
        List<LSMImage> filteredLsms = new ArrayList<>();
        for(LSMImage lsm : lsms) {

            boolean display = true;

            if (!StringUtils.areEqual(currObjective, ALL_VALUE) && !areEqualOrEmpty(currObjective, lsm.getObjective())) {
                display = false;
            }

            if (!StringUtils.areEqual(currArea, ALL_VALUE) && !areEqualOrEmpty(currArea, lsm.getAnatomicalArea())) {
                display = false;
            }

            if (display) {
                filteredLsms.add(lsm);
            }
        }

        DomainUtils.sortDomainObjects(filteredLsms, sortCriteria);
        lsmSearchResults = new DomainObjectSearchResults(filteredLsms, lsmAnnotations);
    }

    private void showLsmView(boolean isUserDriven) {

    	configPanel.removeAllConfigComponents();
        configPanel.addConfigComponent(objectiveButton);
        configPanel.addConfigComponent(areaButton);
    	
        if (lsms!=null) {
            Set<String> objectiveSet = new LinkedHashSet<>(sample.getObjectives());
            Set<String> areaSet = new LinkedHashSet<>();
        	for(LSMImage lsm : lsms) {
        		objectiveSet.add(lsm.getObjective());
                    String area = lsm.getAnatomicalArea();
                    if (area==null) {
                        area = "Unknown";
                    }
                    areaSet.add(area);
        	}
        	
        	List<String> objectives = new ArrayList<>(objectiveSet);
            objectives.add(0, ALL_VALUE);
            populateObjectiveButton(objectives);
            
            List<String> areas = new ArrayList<>(areaSet);
            areas.add(0, ALL_VALUE);
            populateAreaButton(areas);
    
            lsmPanel.showSearchResults(lsmSearchResults, isUserDriven, null);
        }
        else {
            lsmPanel.showNothing();
        }
        
        removeAll();
        add(configPanel, BorderLayout.NORTH);
        add(lsmPanel, BorderLayout.CENTER);
    }

    private void showResultView(boolean isUserDriven) {

        lips.clear();
        mainPanel.clearPanels();
        configPanel.removeAllConfigComponents();
        configPanel.addConfigComponent(objectiveButton);
        configPanel.addConfigComponent(areaButton);
        
        if (sample!=null) {
            prepareSampleValues();
            
            for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
                
                String objective = objectiveSample.getObjective();
                if (!StringUtils.areEqual(currObjective, ALL_VALUE) && !StringUtils.areEqual(currObjective, objective)) {
                    continue;
                }
    
                SamplePipelineRun run = currRunMap.get(objective);
                if (run==null || run.getResults()==null) continue;
                
                for(PipelineResult result : run.getResults()) {
    
                    String area = getArea(result);
                    if (!StringUtils.areEqual(currArea, ALL_VALUE) && !areEqualOrEmpty(currArea, area)) {
                        continue;
                    }
                    
                    mainPanel.addPanel(new PipelineResultPanel(result));
                    
                    NeuronSeparation separation = result.getLatestSeparationResult();
                    if (separation!=null) {
                        if (!StringUtils.isBlank(separation.getMessage())) {
                            // Neuron separation error
                            mainPanel.addPanel(new PipelineResultPanel(separation));
                        }
                    }
                }
                
                if (run.hasError()) {
                    mainPanel.addPanel(new PipelineErrorPanel(run));
                }
                
            }
        }
        
        removeAll();
        add(configPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private String getArea(PipelineResult result) {

        String area = null;
        if (result instanceof HasAnatomicalArea) {
            area = ((HasAnatomicalArea)result).getAnatomicalArea();
        }
        
        if (area==null) {
            area = "Unknown";
        }
        
        return area;
    }
    
    private void showColorDepthView(boolean isUserDriven) {

        lips.clear();
        mainPanel.clearPanels();
        configPanel.removeAllConfigComponents();
        configPanel.addConfigComponent(objectiveButton);
        configPanel.addConfigComponent(areaButton);
        
        if (sample!=null) {
            // Populate drop down buttons
            prepareAlignmentSpaces();
            configPanel.addConfigComponent(alignmentSpaceButton);
            
            if (currAlignmentSpace != null) {
            
                for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {

                    String objective = objectiveSample.getObjective();
                    if (!StringUtils.areEqual(currObjective, ALL_VALUE) && !StringUtils.areEqual(currObjective, objective)) {
                        continue;
                    }
                    
                    RUNS : for(SamplePipelineRun run : Lists.reverse(objectiveSample.getPipelineRuns())) {
                        for (SampleAlignmentResult result : run.getAlignmentResults()) {

                            String area = getArea(result);
                            if (!StringUtils.areEqual(currArea, ALL_VALUE) && !areEqualOrEmpty(currArea, area)) {
                                continue;
                            }
                            
                            String alignmentSpace = result.getAlignmentSpace();
                            if (!StringUtils.areEqual(currAlignmentSpace, ALL_VALUE) && !StringUtils.areEqual(currAlignmentSpace, alignmentSpace)) {
                                continue;
                            }
                            
                            log.info("Found matching alignment for currObjective={}, currArea={}, alignmentSpace={}", currObjective, currArea, currAlignmentSpace);
                            for (FileType fileType : COLOR_DEPTH_TYPES) {
                                if (result.getFiles().containsKey(fileType)) {
                                    mainPanel.addPanel(new ColorDepthPanel(result, fileType));
                                }
                            }
                            
                            // Once a result for the current filter is found, there's no need to look in previous runs
                            break RUNS;
                        }
                    }
                }
            }
        }
        
        removeAll();
        add(configPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    
    private void prepareSampleValues() {

        Set<String> objectiveSet = new LinkedHashSet<>(sample.getObjectives());
        Set<String> areaSet = new LinkedHashSet<>();
        
        // Populate currRunMap
        for(String objective : objectiveSet) {
            
            ObjectiveSample objectiveSample = sample.getObjectiveSample(objective);
            if (objectiveSample==null) continue;
            
            // Is there a run already selected?
            SamplePipelineRun run = currRunMap.get(objective);
            
            if (run==null) {
                // If not, pick one as the default
                run = objectiveSample.getLatestRun();
                if (run!=null) {
                    currRunMap.put(objective, run);
                }
            }
        }
        
        // Populate drop down buttons
        for(String objective : objectiveSet) {
            
            ObjectiveSample objectiveSample = sample.getObjectiveSample(objective);
            if (objectiveSample==null) continue;
            SamplePipelineRun run = currRunMap.get(objective);
            if (run==null || run.getResults()==null) continue;

            for(PipelineResult result : run.getResults()) {
                String area = getArea(result);
                areaSet.add(area);
            }
            
            DropDownButton historyButton = new DropDownButton(objective+": "+getLabel(run));
            populateHistoryButton(historyButton, objectiveSample);
            historyButtonMap.put(objective, historyButton);
            configPanel.addConfigComponent(historyButton);
        }

        List<String> objectives = new ArrayList<>(objectiveSet);
        objectives.add(0, ALL_VALUE);
        populateObjectiveButton(objectives);
        
        List<String> areas = new ArrayList<>(areaSet);
        areas.add(0, ALL_VALUE);
        populateAreaButton(areas);
    }
    
    private void populateHistoryButton(DropDownButton button, final ObjectiveSample objectiveSample) {
    	final String objective = objectiveSample.getObjective();
    	button.removeAll();
        ButtonGroup group = new ButtonGroup();
        for(final SamplePipelineRun run : objectiveSample.getPipelineRuns()) {
            JMenuItem menuItem = new JRadioButtonMenuItem(getLabel(run), currRunMap.get(objective)==run);
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                	currRunMap.put(objective, run);
                	showResults(true);
                }
            });
            group.add(menuItem);
            button.addMenuItem(menuItem);
        }
    }

	private String getLabel(SamplePipelineRun run) {
	    if (run==null) return "";
	    return DomainModelViewUtils.getDateString(run.getCreationDate());
	}	
	
    private void setViewMode(String currMode) {
        ActivityLogHelper.logUserAction("SampleEditorPanel.setViewMode", currMode);
        this.currMode = currMode;
        loadDomainObject(sample, true, null);
    }
    
    private void populateObjectiveButton(List<String> objectives) {
        this.objectives = objectives;
        objectiveButton.update();
    }
    
    private void setObjective(String objective) {
        this.currObjective = objective;
        loadDomainObject(sample, true, null);
    }
    
    private void populateAreaButton(List<String> areas) {
        this.areas = areas;
        areaButton.update();
    }
    
    private void setArea(String area) {
        this.currArea = area;
        loadDomainObject(sample, true, null);
    }

    private void prepareAlignmentSpaces() {
        
        Set<String> alignmentSpaceSet = new TreeSet<>();
        
        for (ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {

            String objective = objectiveSample.getObjective();
            if (!StringUtils.areEqual(currObjective, ALL_VALUE) && !StringUtils.areEqual(currObjective, objective)) {
                continue;
            }

            for(SamplePipelineRun run : Lists.reverse(objectiveSample.getPipelineRuns())) {
                for (SampleAlignmentResult result : run.getAlignmentResults()) {
                    
                    String area = getArea(result);
                    if (!StringUtils.areEqual(currArea, ALL_VALUE) && !areEqualOrEmpty(currArea, area)) {
                        continue;
                    }
                    
                    if (result.getAlignmentSpace()!=null) {
                        
                        boolean hasColorDepthMips = false;
                        for (FileType fileType : COLOR_DEPTH_TYPES) {
                            if (result.getFiles().containsKey(fileType)) {
                                hasColorDepthMips = true;
                                break;
                            }
                        }
                        
                        if (hasColorDepthMips) {
                            String alignmentSpace = result.getAlignmentSpace();
                            if (ColorDepthUtils.isAlignmentSpaceVisible(alignmentSpace)) {
                                alignmentSpaceSet.add(alignmentSpace);
                            }
                        }
                    }
                }
            }
        }
        
        this.alignmentSpaces = new ArrayList<>(alignmentSpaceSet);

        if (currAlignmentSpace==null || !alignmentSpaces.contains(currAlignmentSpace)) {

            List<String> defaultAlignmentSpaces = new ArrayList<>(alignmentSpaceSet);
            // Make unisex/HR spaces default if possible, since they are the most useful for color depth searching
            alignmentSpaces.sort((o1, o2) -> {
                return ComparisonChain.start()
                        .compare(o1.endsWith("_HR"), o2.endsWith("_HR"), Ordering.natural().reversed())
                        .compare(o1.contains("Unisex"), o2.contains("Unisex"), Ordering.natural().reversed())
                        .compare(o1, o2)
                        .result();
            });

            // select first alignment space as default
            this.currAlignmentSpace = alignmentSpaces.isEmpty() ? null : alignmentSpaces.iterator().next();
        }
        alignmentSpaceButton.update();
    }
    
    private void setAlignmentSpace(String alignmentSpace) {
        this.currAlignmentSpace = alignmentSpace;
        loadDomainObject(sample, true, null);
    }
    
    private boolean areEqualOrEmpty(String value1, String value2) {
        if (value1==null || value1.equals("")) {
            return value2==null || value2.equals("");
        }
        if (value2==null || value2.equals("")) {
            return false;
        }
        return value1.equals(value2);
    }
    
    private void rescaleImage(LoadedImagePanel image, float factor) {
        double width = image.getParent()==null?0:image.getParent().getSize().getWidth();
        if (width==0) {
            width = scrollPane.getViewport().getSize().getWidth() - 20;
        }
        if (width==0) {
            log.warn("Could not get width from parent or viewport");
            return;
        }
        image.scaleImage((int)Math.ceil(width * factor));
    }

    private class PipelineResultPanel extends SelectablePanel {
        
        private final ArtifactDescriptor resultDescriptor;
        private final PipelineResult result;
        
        private PipelineResultPanel(PipelineResult result) {
            
            this.result = result;
            
            int b = SelectablePanel.BORDER_WIDTH;
            setBorder(BorderFactory.createEmptyBorder(b, b, b, b));
            setLayout(new BorderLayout());
            
            JPanel imagePanel = new JPanel();
            imagePanel.setLayout(new GridLayout(1, 2, 5, 0));

            if (result!=null) {
                
                String compressionLabel = result.getCompressionType()==null?"":SampleUtils.getCompressionLabel(result.getCompressionType());

                JLabel label = new JLabel();
                JLabel rightLabel = new JLabel();
                JLabel rightLabel2 = new JLabel();
                rightLabel2.setForeground(UIManager.getColor("textInactiveText"));
                JLabel subLabel1 = new JLabel();
                JLabel subLabel2 = new JLabel();
                
                this.resultDescriptor = new ResultArtifactDescriptor(result);
                label.setText(resultDescriptor.toString());
                subLabel1.setText(DomainModelViewUtils.getDateString(result.getCreationDate()));
                if (!StringUtils.isBlank(result.getMessage())) {
                    subLabel2.setText(result.getMessage());
                }
                rightLabel.setText(compressionLabel);
                //rightLabel2.setText(result.getName());
                
                HasFiles files = result;
                
                // Attempt to find a signal MIP to display
                String signalMip = DomainUtils.getFilepath(files, FileType.SignalMip);
                if (signalMip==null) {
                    signalMip = DomainUtils.getFilepath(files, FileType.AllMip);
                }
                if (signalMip==null) {
                    signalMip = DomainUtils.getFilepath(files, FileType.Signal1Mip);
                }
                
                String refMip = DomainUtils.getFilepath(files, FileType.ReferenceMip);
                
                List<Decorator> decorators = SampleUIUtils.getDecorators(result);
                if (signalMip!=null || refMip!=null) {
                    imagePanel.add(getImagePanel(signalMip, decorators));
                    imagePanel.add(getImagePanel(refMip, decorators));
                }

                JPanel titlePanel = new JPanel(new MigLayout("wrap 2, ins 0, fillx"));
                
                titlePanel.add(label, "");
                titlePanel.add(rightLabel, "align right");
                titlePanel.add(subLabel1, "");
                titlePanel.add(rightLabel2, "align right");
                titlePanel.add(subLabel2, "span 2");
                
                add(titlePanel, BorderLayout.NORTH);
                add(imagePanel, BorderLayout.CENTER);

                setFocusTraversalKeysEnabled(false);
            }
            else {
                this.resultDescriptor = null;
            }
        }

        public PipelineResult getResult() {
            return result;
        }
      
        public ArtifactDescriptor getResultDescriptor() {
            return resultDescriptor;
        }
    
        private JPanel getImagePanel(String filepath, List<Decorator> decorators) {
            LoadedImagePanel lip = new LoadedImagePanel(filepath, decorators) {
                @Override
                protected void doneLoading() {
                    rescaleImage(this, 0.5f);
                    invalidate();
                }
            };
            rescaleImage(lip, 0.5f);
            lip.addMouseListener(new MouseForwarder(this, "LoadedImagePanel->PipelineResultPanel"));
            lips.add(lip);
            return lip;
        }
    }
    
    private static final ImageIcon ERROR_ICON = Icons.getIcon("error_large.png");
    
    private class PipelineErrorPanel extends SelectablePanel {
        
        private SamplePipelineRun run;
        private JLabel label = new JLabel();
        private JLabel subLabel1 = new JLabel();
        private JLabel subLabel2 = new JLabel();
        
        private PipelineErrorPanel(SamplePipelineRun run) {
                        
            this.run = run;
            int b = SelectablePanel.BORDER_WIDTH;
            setBorder(BorderFactory.createEmptyBorder(b, b, b, b));
            setLayout(new BorderLayout());
            
            PipelineError error = run.getError();
            if (error==null) throw new IllegalStateException("Cannot create a PipelineErrorPanel for non-error run");

            ErrorType errorType = ErrorType.UnclassifiedError;
            if (error.getClassification()!=null) {
                errorType = ErrorType.valueOf(error.getClassification());
            }
            
            String title;
            String op = error.getOperation();
            if (StringUtils.isBlank(op)) {
                title = run.getParent().getObjective()+" "+errorType.getLabel();    
            }
            else {
                title = run.getParent().getObjective()+" "+op+" - "+errorType.getLabel();
            }
            
            label.setText(title);
            label.setToolTipText(errorType.getDescription());
            subLabel1.setText(DomainModelViewUtils.getDateString(error.getCreationDate()));
            subLabel2.setText("Error detail: "+error.getDescription());

            JPanel titlePanel = new JPanel(new BorderLayout());
            titlePanel.add(label, BorderLayout.PAGE_START);
            titlePanel.add(subLabel1, BorderLayout.CENTER);
            titlePanel.add(subLabel2, BorderLayout.PAGE_END);

            JPanel imagePanel = new JPanel();
            imagePanel.setLayout(new BorderLayout());
            imagePanel.add(new JLabel(ERROR_ICON), BorderLayout.CENTER);
            
            add(titlePanel, BorderLayout.NORTH);
            add(imagePanel, BorderLayout.CENTER);

            setFocusTraversalKeysEnabled(false);
        }

        public SamplePipelineRun getRun() {
            return run;
        }
        
        public PipelineError getError() {
            return run.getError();
        }
    }

    private class ColorDepthPanel extends SelectablePanel {

        private final ArtifactDescriptor resultDescriptor;
        private final SampleAlignmentResult result;
        private final FileType fileType;
        private final JLabel label = new JLabel();
        
        private ColorDepthPanel(SampleAlignmentResult result, FileType fileType) {
            
            this.result = result;
            this.fileType = fileType;
            
            int b = SelectablePanel.BORDER_WIDTH;
            setBorder(BorderFactory.createEmptyBorder(b, b, b, b));
            setLayout(new BorderLayout());
            
            JPanel imagePanel = new JPanel();
            imagePanel.setLayout(new GridLayout(1, 2, 5, 0));

            if (result!=null) {
                this.resultDescriptor = new ResultArtifactDescriptor(result);
                label.setText(resultDescriptor.toString());
                
                HasFiles files = result;
                String colorDepthMip = DomainUtils.getFilepath(files, fileType);
                if (colorDepthMip!=null) {
                    imagePanel.add(getImagePanel(colorDepthMip, null));
                }
                
                JPanel titlePanel = new JPanel(new BorderLayout());
                titlePanel.add(label, BorderLayout.PAGE_START);
                
                add(titlePanel, BorderLayout.NORTH);
                add(imagePanel, BorderLayout.CENTER);

                setFocusTraversalKeysEnabled(false);
            }
            else {
                this.resultDescriptor = null;
            }
        }

        public SampleAlignmentResult getResult() {
            return result;
        }
      
        public FileType getType() {
            return fileType;
        }

        public ArtifactDescriptor getResultDescriptor() {
            return resultDescriptor;
        }
    
        private JPanel getImagePanel(String filepath, List<Decorator> decorators) {
            LoadedImagePanel lip = new LoadedImagePanel(filepath, decorators) {
                @Override
                protected void doneLoading() {
                    rescaleImage(this, 1f);
                    invalidate();
                }
            };
            rescaleImage(lip, 1f);
            lip.addMouseListener(new MouseForwarder(this, "LoadedImagePanel->ColorDepthPanel"));
            lips.add(lip);
            return lip;
        }
    }
    
    @Subscribe
    public void domainObjectInvalidated(DomainObjectInvalidationEvent event) {
        try {
		    if (sample==null) return;
            if (event.isTotalInvalidation()) {
                log.info("total invalidation, reloading...");
                Sample updatedSample = DomainMgr.getDomainMgr().getModel().getDomainObject(sample);
                if (updatedSample!=null) {
                    loadDomainObject(updatedSample, false, null);
                }
            }
            else {
                for (DomainObject domainObject : event.getDomainObjects()) {
                    if (StringUtils.areEqual(domainObject.getId(), sample.getId())) {
                        log.info("Sample invalidated, reloading...");
                        Sample updatedSample = DomainMgr.getDomainMgr().getModel().getDomainObject(sample);
                        if (updatedSample!=null) {
                            loadDomainObject(updatedSample, false, null);
                        }
                        break;
                    }
                    else if (lsms!=null) {
                        for(LSMImage lsm : lsms) {
                            if (StringUtils.areEqual(domainObject.getId(), lsm.getId())) {
                                log.info("LSM invalidated, reloading...");
                                Sample updatedSample = DomainMgr.getDomainMgr().getModel().getDomainObject(sample);
                                if (updatedSample!=null) {
                                    loadDomainObject(updatedSample, false, null);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }  catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

    @Subscribe
    public void domainObjectRemoved(DomainObjectRemoveEvent event) {
        if (sample==null) return;
        if (StringUtils.areEqual(event.getDomainObject().getId(), sample.getId())) {
            this.sample = null;
            if (currRunMap!=null) currRunMap.clear();
            if (lsms!=null) lsms.clear();
            if (lsmAnnotations!=null) lsmAnnotations.clear();
            showNothing();
        }
    }
        
    @Subscribe
    public void domainObjectSelected(DomainObjectSelectionEvent event) {
        // Forward to LSM panel
        if (lsmPanel!=null) {
            lsmPanel.domainObjectSelected(event);
        }
    }

    @Subscribe
    public void domainObjectChanged(DomainObjectChangeEvent event) {
        // Forward to LSM panel
        if (lsmPanel!=null) {
            lsmPanel.domainObjectChanged(event);
        }
    }
    
    @Subscribe
    public void annotationsChanged(DomainObjectAnnotationChangeEvent event) {
        // Forward to LSM panel
        if (lsmPanel!=null) {
            lsmPanel.annotationsChanged(event);
        }
    }

    @Subscribe
    public void resultSelected(PipelineResultSelectionEvent event) {
        if (event.isUserDriven()) {
            PipelineResult result = event.getPipelineResult();
            log.info("resultSelected({})", result.getId());
            FrameworkAccess.getInspectionController().inspect(getProperties(result));
        }
    }

    @Subscribe
    public void errorSelected(PipelineErrorSelectionEvent event) {
        if (event.isUserDriven()) {
            PipelineError error = event.getPipelineError();
            log.info("errorSelected()");
            FrameworkAccess.getInspectionController().inspect(getProperties(error));
        }
    }

    private Map<String,Object> getProperties(PipelineResult result) {

        Map<String,Object> values = new HashMap<>();

        values.put("Creation Date", result.getCreationDate());
        values.put("Disk Space Usage (Bytes)", result.getDiskSpaceUsage());
        values.put("Disk Space Usage", result.getDiskSpaceUsageForHumans());
        values.put("Filepath", result.getFilepath());
        values.put("GUID", result.getId());
        values.put("Name", result.getName());
        values.put("Purged", result.getPurged());

        if (result.getCompressionType()!=null) {
            values.put("Compression Strategy", SampleUtils.getCompressionLabel(result.getCompressionType()));
        }
        
        if (result.getContainerRef()!=null) {
            Long containerId = result.getContainerRef().getTargetId();
            values.put("Plugin GUID", containerId);
            
            ContainerizedService container = containers==null?null:containers.get(containerId);
            if (container==null) {
                log.warn("Could not find container with id {}", containerId);
            }
            else {
                values.put("Plugin Name", container.getName());
                values.put("Plugin Description", container.getDescription());
                values.put("Plugin Version", container.getVersion());
                values.put("Plugin Harness", container.getHarnessClass());
            }
        }
        
        if (result.getContainerApp()!=null) {
            values.put("Plugin App", result.getContainerApp());
        }
        
        if (result instanceof HasAnatomicalArea) {
            HasAnatomicalArea hasAA = (HasAnatomicalArea) result;
            values.put("Anatomical Area", hasAA.getAnatomicalArea());
        }

        if (result instanceof SampleAlignmentResult) {
            SampleAlignmentResult alignment = (SampleAlignmentResult) result;
            values.put("Alignment Space", alignment.getAlignmentSpace());
            values.put("Bounding Box", alignment.getBoundingBox());
            values.put("Channel Colors", alignment.getChannelColors());
            values.put("Channel Spec", alignment.getChannelSpec());
            values.put("Image Size", alignment.getImageSize());
            values.put("Objective", alignment.getObjective());
            values.put("Optical Resolution", alignment.getOpticalResolution());
            values.put("Message", alignment.getMessage());

            if (alignment.getContainerRef()!=null) {
                values.put("Container Id", alignment.getContainerRef().getTargetId());
            }
            
            Long bridgeParentAlignmentId = alignment.getBridgeParentAlignmentId();
            if (bridgeParentAlignmentId!=null) {
                List<SampleAlignmentResult> bridges = alignment.getParentRun().getResultsById(SampleAlignmentResult.class, bridgeParentAlignmentId);
                if (!bridges.isEmpty()) {
                    SampleAlignmentResult sampleAlignmentResult = bridges.get(0);
                    values.put("Bridged From", sampleAlignmentResult.getAlignmentSpace());
                }
            }
            
            for (AlignmentScoreType scoretype : alignment.getScores().keySet()) {
                String score = alignment.getScores().get(scoretype);
                values.put(scoretype.getLabel(), score);
            }
        }
        else if (result instanceof SampleProcessingResult) {
            SampleProcessingResult spr = (SampleProcessingResult) result;
            values.put("Channel Colors", spr.getChannelColors());
            values.put("Channel Spec", spr.getChannelSpec());
            values.put("Image Size", spr.getImageSize());
            values.put("Optical Resolution", spr.getOpticalResolution());
            values.put("Distortion Corrected", spr.isDistortionCorrected());
        }
        else if (result instanceof NeuronSeparation) {
            NeuronSeparation ns = (NeuronSeparation) result;
            values.put("Number of Neurons", ns.getFragmentsReference().getCount());
            values.put("Neuron Weights", ns.getHasWeights());
        }
        return values;
    }

    private Map<String,Object> getProperties(PipelineError error) {

        Map<String,Object> values = new HashMap<>();

        values.put("Creation Date", error.getCreationDate());
        values.put("Filepath", error.getFilepath());
        values.put("Operation", error.getOperation());
        values.put("Classification", error.getClassification());
        values.put("Description", error.getDescription());

        return values;
    }

    @Override
    public Long getCurrentContextId() {
        Object parentObject = selectionModel.getParentObject();
        if (parentObject instanceof HasIdentifier) {
            return ((HasIdentifier)parentObject).getId();
        }
        throw new IllegalStateException("Parent object has no identifier: "+selectionModel.getParentObject());
    }
}
