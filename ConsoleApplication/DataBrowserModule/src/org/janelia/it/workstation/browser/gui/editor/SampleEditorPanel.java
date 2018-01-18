package org.janelia.it.workstation.browser.gui.editor;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.actions.ExportResultsAction;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.model.DomainObjectAnnotationChangeEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectChangeEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectRemoveEvent;
import org.janelia.it.workstation.browser.events.selection.DomainObjectSelectionEvent;
import org.janelia.it.workstation.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.browser.events.selection.PipelineErrorSelectionEvent;
import org.janelia.it.workstation.browser.events.selection.PipelineResultSelectionEvent;
import org.janelia.it.workstation.browser.gui.hud.Hud;
import org.janelia.it.workstation.browser.gui.listview.ListViewerState;
import org.janelia.it.workstation.browser.gui.listview.PaginatedDomainResultsPanel;
import org.janelia.it.workstation.browser.gui.listview.table.DomainObjectTableViewer;
import org.janelia.it.workstation.browser.gui.support.Debouncer;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.LoadedImagePanel;
import org.janelia.it.workstation.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.browser.gui.support.SelectablePanel;
import org.janelia.it.workstation.browser.gui.support.SelectablePanelListPanel;
import org.janelia.it.workstation.browser.gui.support.buttons.DropDownButton;
import org.janelia.it.workstation.browser.model.DomainModelViewUtils;
import org.janelia.it.workstation.browser.model.ImageDecorator;
import org.janelia.it.workstation.browser.model.descriptors.ArtifactDescriptor;
import org.janelia.it.workstation.browser.model.descriptors.ResultArtifactDescriptor;
import org.janelia.it.workstation.browser.model.search.DomainObjectSearchResults;
import org.janelia.it.workstation.browser.model.search.ResultPage;
import org.janelia.it.workstation.browser.model.search.SearchResults;
import org.janelia.it.workstation.browser.util.ConcurrentUtils;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.enums.ErrorType;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.interfaces.HasAnatomicalArea;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.ObjectiveSample;
import org.janelia.model.domain.sample.PipelineError;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SamplePipelineRun;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * Specialized component for viewing information about Samples, including their LSMs and processing results.  
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleEditorPanel extends JPanel implements DomainObjectEditor<Sample>, SearchProvider {

    private final static Logger log = LoggerFactory.getLogger(SampleEditorPanel.class);

    // Constants
    private final static String PREFERENCE_KEY = "SampleEditor";
    private final static String MODE_LSMS = "LSMs";
    private final static String MODE_RESULTS = "Results";
    private final static String ALL_VALUE = "all";

    // Utilities
    private final Debouncer debouncer = new Debouncer();
    
    // UI Components
    private final ConfigPanel configPanel;
    private final DropDownButton viewButton;
    private final DropDownButton objectiveButton;
    private final DropDownButton areaButton;
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
    private String sortCriteria;
    private String currMode = MODE_RESULTS;
    private String currObjective = ALL_VALUE;
    private String currArea = ALL_VALUE;
    
    public SampleEditorPanel() {

        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());
        setFocusable(true);
        
        viewButton = new DropDownButton("View: ");
        populateViewButton();
        objectiveButton = new DropDownButton("Objective: "+currObjective);
        areaButton = new DropDownButton("Area: "+currArea);

        configPanel = new ConfigPanel(true) {
            @Override
            protected void titleClicked(MouseEvent e) {
                Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(this, Arrays.asList(sample), true, true, true));
            }
        };
        configPanel.addTitleComponent(viewButton, true, true);
        
        lsmPanel = new PaginatedDomainResultsPanel(selectionModel, this) {
            @Override
            protected ResultPage<DomainObject, Reference> getPage(SearchResults<DomainObject, Reference> searchResults, int page) throws Exception {
                return searchResults.getPage(page);
            }
            @Override
            public Reference getId(DomainObject object) {
                return Reference.createFor(object);
            }
        };

        mainPanel = new SelectablePanelListPanel() {

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
            protected void updateHud(SelectablePanel resultPanel, boolean toggle) {
                if (resultPanel instanceof PipelineResultPanel) {
                    ArtifactDescriptor resultDescriptor = ((PipelineResultPanel)resultPanel).getResultDescriptor();
                    Hud hud = Hud.getSingletonInstance();
                    if (toggle) {
                        hud.setObjectAndToggleDialog(sample, resultDescriptor, null);
                    }
                    else {
                        hud.setObject(sample, resultDescriptor, null, true);
                    }
                }
            }
            
            @Override
            protected void popupTriggered(MouseEvent e, SelectablePanel resultPanel) {
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
            }
            
        };

        scrollPane = new JScrollPane();
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportView(mainPanel);

        this.addComponentListener(new ComponentAdapter() {
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

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                loadPreferences();
                prepareLsmResults();
            }

            @Override
            protected void hadSuccess() {
                showResults(true);
            }

            @Override
            protected void hadError(Throwable error) {
                showNothing();
                ConsoleApp.handleException(error);
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
        ExportResultsAction<DomainObject> action = new ExportResultsAction<>(lsmSearchResults, viewer);
        action.actionPerformed(null);
    }

    private void loadPreferences() {
        if (sample.getId()==null) return;
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
                else if (MODE_RESULTS.equals(currMode))  {
                    // Everything is already in memory
                }
            }
            
            @Override
            protected void hadSuccess() {
                showResults(isUserDriven);
                
                if (MODE_RESULTS.equals(currMode))  {
                    mainPanel.selectFirst(isUserDriven);
                }
                else {
                    lsmPanel.getViewer().restoreState(viewerState);
                }
                
                ConcurrentUtils.invokeAndHandleExceptions(success);
                debouncer.success();
                ActivityLogHelper.logElapsed("SampleEditorPanel.loadDomainObject", sample, w);
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
    
    public void showResults(boolean isUserDriven) {
        if (MODE_LSMS.equals(currMode))  {
            showLsmView(isUserDriven);
        }
        else if (MODE_RESULTS.equals(currMode)) {
            showResultView(isUserDriven);
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
                    String area = null;
                    if (result instanceof HasAnatomicalArea) {
                        area = ((HasAnatomicalArea)result).getAnatomicalArea();
                    }
                    if (area==null) {
                        area = "Unknown";
                    }
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
            
            for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
                
                String objective = objectiveSample.getObjective();
                if (!StringUtils.areEqual(currObjective, ALL_VALUE) && !StringUtils.areEqual(currObjective, objective)) {
                    continue;
                }
    
                SamplePipelineRun run = currRunMap.get(objective);
                if (run==null || run.getResults()==null) continue;
                
                for(PipelineResult result : run.getResults()) {
    
                    String area = null;
                    if (result instanceof HasAnatomicalArea) {
                        area = ((HasAnatomicalArea)result).getAnatomicalArea();
                    }
                    
                    if (area==null) {
                        area = "Unknown";
                    }
                    
                    if (!StringUtils.areEqual(currArea, ALL_VALUE) && !areEqualOrEmpty(currArea, area)) {
                        continue;
                    }
                    
                    mainPanel.addPanel(new PipelineResultPanel(result));
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
	
    private void populateViewButton() {
        viewButton.setText(currMode);
        viewButton.removeAll();
        ButtonGroup group = new ButtonGroup();
        for (final String mode : Arrays.asList(MODE_LSMS, MODE_RESULTS)) {
            JMenuItem menuItem = new JMenuItem(mode);
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    viewButton.setText(mode);
                    setViewMode(mode);
                }
            });
            group.add(menuItem);
            viewButton.addMenuItem(menuItem);
        }
    }

    private void setViewMode(String currMode) {
        ActivityLogHelper.logUserAction("SampleEditorPanel.setViewMode", currMode);
        this.currMode = currMode;
        loadDomainObject(sample, true, null);
    }
    
    private void populateObjectiveButton(List<String> objectives) {
        objectiveButton.setText("Objective: "+currObjective);
        objectiveButton.removeAll();
        ButtonGroup group = new ButtonGroup();
        for (final String objective : objectives) {
            JMenuItem menuItem = new JRadioButtonMenuItem(objective, StringUtils.areEqual(objective, currObjective));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setObjective(objective);
                }
            });
            group.add(menuItem);
            objectiveButton.addMenuItem(menuItem);
        }
    }
    
    private void setObjective(String objective) {
        this.currObjective = objective;
        loadDomainObject(sample, true, null);
    }
    
    private void populateAreaButton(List<String> areas) {
        areaButton.setText("Area: "+currArea);
        areaButton.removeAll();
        ButtonGroup group = new ButtonGroup();
        for (final String area : areas) {
            JMenuItem menuItem = new JRadioButtonMenuItem(area, StringUtils.areEqual(area, currArea));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setArea(area);
                }
            });
            group.add(menuItem);
            areaButton.addMenuItem(menuItem);
        }
    }
    
    private void setArea(String area) {
        this.currArea = area;
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
    
    private void rescaleImage(LoadedImagePanel image) {
        double width = image.getParent()==null?0:image.getParent().getSize().getWidth();
        if (width==0) {
            width = scrollPane.getViewport().getSize().getWidth() - 20;
        }
        if (width==0) {
            log.warn("Could not get width from parent or viewport");
            return;
        }
        image.scaleImage((int)Math.ceil(width/2));
    }

    private class PipelineResultPanel extends SelectablePanel {
        
        private final ArtifactDescriptor resultDescriptor;
        private final PipelineResult result;
        private JLabel label = new JLabel();
        private JLabel subLabel1 = new JLabel();
        private JLabel subLabel2 = new JLabel();
        
        private PipelineResultPanel(PipelineResult result) {
            
            this.result = result;
            
            int b = SelectablePanel.BORDER_WIDTH;
            setBorder(BorderFactory.createEmptyBorder(b, b, b, b));
            setLayout(new BorderLayout());
            
            JPanel imagePanel = new JPanel();
            imagePanel.setLayout(new GridLayout(1, 2, 5, 0));

            if (result!=null) {
                this.resultDescriptor = new ResultArtifactDescriptor(result);
                label.setText(resultDescriptor.toString());
                subLabel1.setText(DomainModelViewUtils.getDateString(result.getCreationDate()));
                if (!StringUtils.isBlank(result.getMessage())) {
                    subLabel2.setText(result.getMessage());
                }
                
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
                
                List<ImageDecorator> decorators = ClientDomainUtils.getDecorators(result);
                if (signalMip!=null || refMip!=null) {
                    imagePanel.add(getImagePanel(signalMip, decorators));
                    imagePanel.add(getImagePanel(refMip, decorators));
                }
                
                JPanel titlePanel = new JPanel(new BorderLayout());
                titlePanel.add(label, BorderLayout.PAGE_START);
                titlePanel.add(subLabel1, BorderLayout.CENTER);
                titlePanel.add(subLabel2, BorderLayout.PAGE_END);
                
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
    
        private JPanel getImagePanel(String filepath, List<ImageDecorator> decorators) {
            LoadedImagePanel lip = new LoadedImagePanel(filepath, decorators) {
                @Override
                protected void doneLoading() {
                    rescaleImage(this);
                    invalidate();
                }
            };
            rescaleImage(lip);
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
            ConsoleApp.handleException(e);
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
}
