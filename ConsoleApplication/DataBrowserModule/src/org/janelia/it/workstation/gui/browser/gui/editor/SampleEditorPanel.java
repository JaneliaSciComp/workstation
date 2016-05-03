package org.janelia.it.workstation.gui.browser.gui.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasAnatomicalArea;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineError;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.shared.utils.ReflectionUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.browser.actions.ExportResultsAction;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.events.selection.PipelineResultSelectionEvent;
import org.janelia.it.workstation.gui.browser.gui.hud.Hud;
import org.janelia.it.workstation.gui.browser.gui.listview.PaginatedResultsPanel;
import org.janelia.it.workstation.gui.browser.gui.listview.table.DomainObjectTableViewer;
import org.janelia.it.workstation.gui.browser.gui.support.Debouncer;
import org.janelia.it.workstation.gui.browser.gui.support.DropDownButton;
import org.janelia.it.workstation.gui.browser.gui.support.LoadedImagePanel;
import org.janelia.it.workstation.gui.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.browser.gui.support.SelectablePanel;
import org.janelia.it.workstation.gui.browser.model.DomainModelViewUtils;
import org.janelia.it.workstation.gui.browser.model.ResultDescriptor;
import org.janelia.it.workstation.gui.browser.model.search.ResultPage;
import org.janelia.it.workstation.gui.browser.model.search.SearchResults;
import org.janelia.it.workstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.gui.util.MouseHandler;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.common.eventbus.Subscribe;


/**
 * Specialized component for viewing information about Samples, including their LSMs and processing results.  
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleEditorPanel extends JPanel implements DomainObjectEditor<Sample>, SearchProvider {

    private final static Logger log = LoggerFactory.getLogger(SampleEditorPanel.class);
    
    // Constants
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
    private final JPanel mainPanel;
    private final PaginatedResultsPanel lsmPanel;
    private final JScrollPane scrollPane;
    private final JPanel dataPanel;
    private final List<SelectablePanel> resultPanels = new ArrayList<>();
    private final Set<LoadedImagePanel> lips = new HashSet<>();
    
    // Results
    private SearchResults lsmSearchResults;
    private final DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();
    
    // State
    private Sample sample;
    private Map<String,SamplePipelineRun> currRunMap = new HashMap<>();
    private List<LSMImage> lsms;
    private List<Annotation> lsmAnnotations;
    private String currMode = MODE_RESULTS;
    private String currObjective = ALL_VALUE;
    private String currArea = ALL_VALUE;
    private int currResultIndex = -1;

    // Listen for key strokes and execute the appropriate key bindings
    protected KeyListener keyListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {

            if (KeymapUtil.isModifier(e)) {
                return;
            }
            if (e.getID() != KeyEvent.KEY_PRESSED) {
                return;
            }
            
            // No keybinds matched, use the default behavior
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                updateHud(true);
                e.consume();
                return;
            }
            else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                enterKeyPressed();
                return;
            }
            else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                deleteKeyPressed();
                e.consume();
                return;
            }

            SelectablePanel object = null;
            if (e.getKeyCode() == KeyEvent.VK_TAB) {
                if (e.isShiftDown()) {
                    object = getPreviousObject();
                }
                else {
                    object = getNextObject();
                }
            }
            else {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    object = getPreviousObject();
                }
                else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    object = getNextObject();
                }
            }

            if (object != null) {
                panelSelection(object, true);
                updateHud(false);
            }

            revalidate();
            repaint();
        }
    };
    
    protected void enterKeyPressed() {}
    
    protected void deleteKeyPressed() {}
    
    // Listener for clicking on result panels
    protected MouseListener resultMouseListener = new MouseHandler() {

        @Override
        protected void popupTriggered(MouseEvent e) {
            if (e.isConsumed()) {
                return;
            }
            SelectablePanel resultPanel = getSelectablePanelAncestor(e.getComponent());
            // Select the button first
            panelSelection(resultPanel, true);
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
            e.consume();
        }

        @Override
        protected void doubleLeftClicked(MouseEvent e) {
            if (e.isConsumed()) {
                return;
            }
            SelectablePanel resultPanel = getSelectablePanelAncestor(e.getComponent());
            // Select the button first
            panelSelection(resultPanel, true);
            if (resultPanel instanceof PipelineResultPanel) {
                SampleResultContextMenu popupMenu = new SampleResultContextMenu(((PipelineResultPanel)resultPanel).getResult());
                popupMenu.runDefaultAction();
            }
            else if (resultPanel instanceof PipelineErrorPanel) {
                SampleErrorContextMenu popupMenu = new SampleErrorContextMenu(((PipelineErrorPanel)resultPanel).getRun());
                popupMenu.runDefaultAction();
            }
            e.consume();
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            super.mouseReleased(e);
            if (e.isConsumed()) {
                return;
            }
            SelectablePanel resultPanel = getSelectablePanelAncestor(e.getComponent());
            if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() < 0) {
                return;
            }
            panelSelection(resultPanel, true);
        }
    };
    
    public SampleEditorPanel() {

        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());
        setFocusable(true);
        
        viewButton = new DropDownButton("View: ");
        populateViewButton();
        objectiveButton = new DropDownButton("Objective: "+currObjective);
        areaButton = new DropDownButton("Area: "+currArea);
        
        configPanel = new ConfigPanel(true);
        configPanel.addTitleComponent(viewButton, true, true);
        
        lsmPanel = new PaginatedResultsPanel(selectionModel, this) {
            @Override
            protected ResultPage getPage(SearchResults searchResults, int page) throws Exception {
                return searchResults.getPage(page);
            }
        };

        dataPanel = new JPanel();
        dataPanel.setLayout(new BoxLayout(dataPanel, BoxLayout.PAGE_AXIS));

        mainPanel = new ScrollablePanel();
        mainPanel.add(dataPanel, BorderLayout.CENTER);

        scrollPane = new JScrollPane(); 
        scrollPane.setViewportView(mainPanel);

        addKeyListener(keyListener);
        
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
    
    private void panelSelection(SelectablePanel resultPanel, boolean isUserDriven) {
        if (resultPanel==null) return;
        for(SelectablePanel otherResultPanel : resultPanels) {
            if (resultPanel != otherResultPanel) {
                otherResultPanel.setSelected(false);
            }
        }
        currResultIndex = resultPanels.indexOf(resultPanel);
        resultPanel.setSelected(true);

        if (resultPanel instanceof PipelineResultPanel) {
            PipelineResultPanel resultPanel2 = (PipelineResultPanel)resultPanel;
            Events.getInstance().postOnEventBus(new PipelineResultSelectionEvent(this, resultPanel2.getResult(), isUserDriven));
        }
        
        if (isUserDriven) {
            // Only make this the focused component if the user actually clicked on it. The main thing this does is change the 
            // active key listener, which we don't want to do if the selection is the result of some selection cascade. 
            resultPanel.requestFocus();
            // Update the lightbox if necessary
            updateHud(false);
        }
    }
    
    private SelectablePanel getSelectablePanelAncestor(Component component) {
        Component c = component;
        while (c!=null) {
            if (c instanceof SelectablePanel) {
                return (SelectablePanel)c;
            }
            c = c.getParent();
        }
        return null;
    }

    public SelectablePanel getPreviousObject() {
        if (resultPanels == null) {
            return null;
        }
        int i = currResultIndex;
        if (i < 1) {
            // Already at the beginning
            return null;
        }
        return resultPanels.get(i - 1);
    }

    public SelectablePanel getNextObject() {
        if (resultPanels == null) {
            return null;
        }
        int i = currResultIndex;
        if (i > resultPanels.size() - 2) {
            // Already at the end
            return null;
        }
        return resultPanels.get(i + 1);
    }
    
    @Override
    public void setSortField(final String sortCriteria) {

        lsmPanel.showLoadingIndicator();

        SimpleWorker worker = new SimpleWorker() {
        
            @Override
            protected void doStuff() throws Exception {
                final String sortField = (sortCriteria.startsWith("-") || sortCriteria.startsWith("+")) ? sortCriteria.substring(1) : sortCriteria;
                final boolean ascending = !sortCriteria.startsWith("-");
                Collections.sort(lsms, new Comparator<DomainObject>() {
                    @Override
                    @SuppressWarnings({"rawtypes", "unchecked"})
                    public int compare(DomainObject o1, DomainObject o2) {
                        try {
                            // TODO: speed could be improved by moving the reflection calls outside of the sort
                            Comparable v1 = (Comparable) ReflectionUtils.get(o1, sortField);
                            Comparable v2 = (Comparable) ReflectionUtils.get(o2, sortField);
                            Ordering ordering = Ordering.natural().nullsLast();
                            if (!ascending) {
                                ordering = ordering.reverse();
                            }
                            return ComparisonChain.start().compare(v1, v2, ordering).result();
                        }
                        catch (Exception e) {
                            log.error("Problem encountered when sorting DomainObjects", e);
                            return 0;
                        }
                    }
                });
            }

            @Override
            protected void hadSuccess() {
                showResults();
            }

            @Override
            protected void hadError(Throwable error) {
                showNothing();
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        
        worker.execute();
    }
    
    @Override
    public void search() {
        // Nothing needs to be done here, because results were updated by setSortField()
    }

    @Override
    public void export() {
        DomainObjectTableViewer viewer = null;
        if (lsmPanel.getViewer() instanceof DomainObjectTableViewer) {
            viewer = (DomainObjectTableViewer)lsmPanel.getViewer();
        }
        ExportResultsAction<DomainObject> action = new ExportResultsAction<>(lsmSearchResults, viewer);
        action.doAction();
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
    
    protected void updateHud(boolean toggle) {

        Hud hud = Hud.getSingletonInstance();
        hud.setKeyListener(keyListener);
                
        SelectablePanel pipelineResultPanel = resultPanels.get(currResultIndex);
        
        if (pipelineResultPanel instanceof PipelineResultPanel) {
            ResultDescriptor resultDescriptor = ((PipelineResultPanel)pipelineResultPanel).getResultDescriptor();
            
            if (toggle) {
                hud.setObjectAndToggleDialog(sample, resultDescriptor, null);
            }
            else {
                hud.setObject(sample, resultDescriptor, null, true);
            }
        }
    }
    
    @Subscribe
    public void domainObjectInvalidated(DomainObjectInvalidationEvent event) {
        if (event.isTotalInvalidation()) {
            log.info("total invalidation, reloading...");
            Sample updatedSample = DomainMgr.getDomainMgr().getModel().getDomainObject(sample);
            if (updatedSample!=null) {
                loadDomainObject(updatedSample, false, null);
            }
        }
        else {
            for (DomainObject domainObject : event.getDomainObjects()) {
                if (domainObject.getId().equals(sample.getId())) {
                    log.info("objects set invalidated, reloading...");
                    Sample updatedSample = DomainMgr.getDomainMgr().getModel().getDomainObject(sample);
                    if (updatedSample!=null) {
                        loadDomainObject(updatedSample, false, null);
                    }
                    break;
                }
                else if (lsms!=null) {
                    for(LSMImage lsm : lsms) {
                        if (domainObject.getId().equals(lsm.getId())) {
                            log.info("lsm invalidated, reloading...");
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
    }

    @Override
    public void loadDomainObject(final Sample sample, final boolean isUserDriven, final Callable<Void> success) {

        if (sample==null) return;
        
        if (!debouncer.queue(success)) {
            log.info("Skipping load, since there is one already in progress");
            return;
        }
        
        log.info("loadDomainObject({},isUserDriven={})",sample.getName(),isUserDriven);
        
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
                    lsms = model.getLsmsForSample(sample.getId());
                    lsmAnnotations = model.getAnnotations(DomainUtils.getReferences(lsms));
                }
                else if (MODE_RESULTS.equals(currMode))  {
                    // Everything is already in memory
                }
            }
            
            @Override
            protected void hadSuccess() {
                showResults();
                
                if (MODE_RESULTS.equals(currMode))  {
                    if (!resultPanels.isEmpty()) {
                        panelSelection(resultPanels.get(0), isUserDriven);
                    }
                }
                
                ConcurrentUtils.invokeAndHandleExceptions(success);
                debouncer.success();
            }
            
            @Override
            protected void hadError(Throwable error) {
                showNothing();
                debouncer.failure();
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        worker.execute();
    }
    
    public void showNothing() {
        removeAll();
        updateUI();
    }
    
    public void showResults() {
        if (MODE_LSMS.equals(currMode))  {
            showLsmView();
        }
        else if (MODE_RESULTS.equals(currMode)) {
            showResultView();
        }
        updateUI();
    }
    
    private void showLsmView() {

    	configPanel.removeAllConfigComponents();
        configPanel.addConfigComponent(objectiveButton);
        configPanel.addConfigComponent(areaButton);
    	
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
        
        List<LSMImage> filteredLsms = new ArrayList<>();
        for(LSMImage lsm : lsms) {

            boolean display = true;

            if (!currObjective.equals(ALL_VALUE) && !areEqualOrEmpty(currObjective, lsm.getObjective())) {
                display = false;
            }
            
            if (!currArea.equals(ALL_VALUE) && !areEqualOrEmpty(currArea, lsm.getAnatomicalArea())) {
                display = false;
            }
            
            if (display) {
                filteredLsms.add(lsm);
            }
        }
        
        lsmSearchResults = SearchResults.paginate(filteredLsms, lsmAnnotations);
        lsmPanel.showSearchResults(lsmSearchResults, true);
        
        removeAll();
        add(configPanel, BorderLayout.NORTH);
        add(lsmPanel, BorderLayout.CENTER);
    }

    private void showResultView() {

        lips.clear();
        resultPanels.clear();
        dataPanel.removeAll();
        configPanel.removeAllConfigComponents();
        configPanel.addConfigComponent(objectiveButton);
        configPanel.addConfigComponent(areaButton);
        
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
            	run = objectiveSample.getLatestSuccessfulRun();
            	if (run==null) {
            		run = objectiveSample.getLatestRun();
            	}
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
            populateHistoryButton(historyButton.getPopupMenu(), objectiveSample);
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
            boolean diplayObjective = true;
            
            if (!currObjective.equals(ALL_VALUE) && !currObjective.equals(objective)) {
                diplayObjective = false;
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
                
                boolean display = diplayObjective;
                if (!currArea.equals(ALL_VALUE) && !areEqualOrEmpty(currArea, area)) {
                    display = false;
                }
                
                if (display) {
                    PipelineResultPanel resultPanel = new PipelineResultPanel(result);
                    resultPanels.add(resultPanel);
                    dataPanel.add(resultPanel);
                }
            }
            
            if (run.hasError()) {
                PipelineErrorPanel resultPanel = new PipelineErrorPanel(run);
                resultPanels.add(resultPanel);
                dataPanel.add(resultPanel);
            }
            
        }

        removeAll();
        add(configPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void populateHistoryButton(JPopupMenu popupMenu, final ObjectiveSample objectiveSample) {
    	final String objective = objectiveSample.getObjective();
    	popupMenu.removeAll();
        ButtonGroup group = new ButtonGroup();
        for(final SamplePipelineRun run : objectiveSample.getPipelineRuns()) {
            JMenuItem menuItem = new JRadioButtonMenuItem(getLabel(run), currRunMap.get(objective)==run);
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                	currRunMap.put(objective, run);
                	showResults();
                }
            });
            group.add(menuItem);
            popupMenu.add(menuItem);
        }
    }

	private String getLabel(SamplePipelineRun run) {
	    if (run==null) return "";
	    return DomainModelViewUtils.getDateString(run.getCreationDate());
	}	
	
    private void populateViewButton() {
        viewButton.setText(currMode);
        JPopupMenu popupMenu = viewButton.getPopupMenu();
        popupMenu.removeAll();
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
            popupMenu.add(menuItem);
        }
    }

    private void setViewMode(String currMode) {
        this.currMode = currMode;
        loadDomainObject(sample, true, null);
    }
    
    private void populateObjectiveButton(List<String> objectives) {
        objectiveButton.setText("Objective: "+currObjective);
        JPopupMenu popupMenu = objectiveButton.getPopupMenu();
        popupMenu.removeAll();
        ButtonGroup group = new ButtonGroup();
        for (final String objective : objectives) {
            JMenuItem menuItem = new JRadioButtonMenuItem(objective, objective.equals(currObjective));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setObjective(objective);
                }
            });
            group.add(menuItem);
            popupMenu.add(menuItem);
        }
    }
    
    private void setObjective(String objective) {
        this.currObjective = objective;
        loadDomainObject(sample, true, null);
    }
    
    private void populateAreaButton(List<String> areas) {
        areaButton.setText("Area: "+currArea);
        JPopupMenu popupMenu = areaButton.getPopupMenu();
        popupMenu.removeAll();
        ButtonGroup group = new ButtonGroup();
        for (final String area : areas) {
            JMenuItem menuItem = new JRadioButtonMenuItem(area, area.equals(currArea));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setArea(area);
                }
            });
            group.add(menuItem);
            popupMenu.add(menuItem);
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
    
    private class ScrollablePanel extends JPanel implements Scrollable {

        public ScrollablePanel() {
            setLayout(new BorderLayout());
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 30;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 300;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private class PipelineResultPanel extends SelectablePanel {
        
        private final ResultDescriptor resultDescriptor;
        private final PipelineResult result;
        private JLabel label = new JLabel();
        private JLabel subLabel = new JLabel();
        
        private PipelineResultPanel(PipelineResult result) {
            
            this.result = result;
            
            int b = SelectablePanel.BORDER_WIDTH;
            setBorder(BorderFactory.createEmptyBorder(b, b, b, b));
            setLayout(new BorderLayout());
            
            JPanel imagePanel = new JPanel();
            imagePanel.setLayout(new GridLayout(1, 2, 5, 0));

            if (result!=null) {
                this.resultDescriptor = new ResultDescriptor(result);
                if (result instanceof SampleAlignmentResult) {
                    SampleAlignmentResult sar = (SampleAlignmentResult)result;
                    label.setText(resultDescriptor+" ("+sar.getAlignmentSpace()+")");
                }
                else {
                    label.setText(resultDescriptor.toString());
                }
                subLabel.setText(DomainModelViewUtils.getDateString(result.getCreationDate()));
                
                String signalMip = DomainUtils.getFilepath(result, FileType.SignalMip);
                if (signalMip==null) {
                    signalMip = DomainUtils.getFilepath(result, FileType.AllMip);
                }
                if (signalMip==null) {
                    signalMip = DomainUtils.getFilepath(result, FileType.Signal1Mip);
                }
                
                String refMip = DomainUtils.getFilepath(result, FileType.ReferenceMip);
                
                imagePanel.add(getImagePanel(signalMip));
                imagePanel.add(getImagePanel(refMip));
    
                JPanel titlePanel = new JPanel(new BorderLayout());
                titlePanel.add(label, BorderLayout.PAGE_START);
                titlePanel.add(subLabel, BorderLayout.PAGE_END);
                
                add(titlePanel, BorderLayout.NORTH);
                add(imagePanel, BorderLayout.CENTER);

                setFocusTraversalKeysEnabled(false);
                addKeyListener(keyListener);
                addMouseListener(resultMouseListener);
            }
            else {
                this.resultDescriptor = null;
            }
        }

        public PipelineResult getResult() {
            return result;
        }
      
        public ResultDescriptor getResultDescriptor() {
            return resultDescriptor;
        }
    
        private JPanel getImagePanel(String filepath) {
            LoadedImagePanel lip = new LoadedImagePanel(filepath) {
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
        private JLabel subLabel = new JLabel();
        
        private PipelineErrorPanel(SamplePipelineRun run) {
                        
            this.run = run;
            int b = SelectablePanel.BORDER_WIDTH;
            setBorder(BorderFactory.createEmptyBorder(b, b, b, b));
            setLayout(new BorderLayout());
            
            PipelineError error = run.getError();
            if (error==null) throw new IllegalStateException("Cannot create a PipelineErrorPanel for non-error run");

            String errorClass = error.getClassification()==null?"Unclassified Error":error.getClassification();
            String title = run.getParent().getObjective()+" "+errorClass;
            label.setText(title);
            subLabel.setText(error.getDescription());

            JPanel titlePanel = new JPanel(new BorderLayout());
            titlePanel.add(label, BorderLayout.PAGE_START);
            titlePanel.add(subLabel, BorderLayout.PAGE_END);

            JPanel imagePanel = new JPanel();
            imagePanel.setLayout(new BorderLayout());
            imagePanel.add(new JLabel(ERROR_ICON), BorderLayout.CENTER);
            
            add(titlePanel, BorderLayout.NORTH);
            add(imagePanel, BorderLayout.CENTER);

            setFocusTraversalKeysEnabled(false);
            addKeyListener(keyListener);
            addMouseListener(resultMouseListener);
        }

        public SamplePipelineRun getRun() {
            return run;
        }
    }
}
