package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.BorderLayout;
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
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.actions.ExportResultsAction;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
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
import org.janelia.it.workstation.browser.gui.editor.ConfigPanel;
import org.janelia.it.workstation.browser.gui.editor.DomainObjectEditor;
import org.janelia.it.workstation.browser.gui.editor.SampleErrorContextMenu;
import org.janelia.it.workstation.browser.gui.editor.SampleResultContextMenu;
import org.janelia.it.workstation.browser.gui.hud.Hud;
import org.janelia.it.workstation.browser.gui.keybind.KeymapUtil;
import org.janelia.it.workstation.browser.gui.listview.ListViewerState;
import org.janelia.it.workstation.browser.gui.listview.PaginatedResultsPanel;
import org.janelia.it.workstation.browser.gui.listview.table.DomainObjectTableViewer;
import org.janelia.it.workstation.browser.gui.support.Debouncer;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.LoadedImagePanel;
import org.janelia.it.workstation.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.browser.gui.support.MouseHandler;
import org.janelia.it.workstation.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.browser.gui.support.SelectablePanel;
import org.janelia.it.workstation.browser.gui.support.buttons.DropDownButton;
import org.janelia.it.workstation.browser.model.DomainModelViewUtils;
import org.janelia.it.workstation.browser.model.descriptors.ArtifactDescriptor;
import org.janelia.it.workstation.browser.model.descriptors.ResultArtifactDescriptor;
import org.janelia.it.workstation.browser.model.search.ResultPage;
import org.janelia.it.workstation.browser.model.search.SearchResults;
import org.janelia.it.workstation.browser.util.ConcurrentUtils;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.enums.ErrorType;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.gui.colordepth.ColorDepthSearch;
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
 * Specialized component for executing color depth searches on the cluster and viewing their results.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthSearchEditorPanel extends JPanel implements DomainObjectEditor<ColorDepthSearch>, SearchProvider {

    private final static Logger log = LoggerFactory.getLogger(ColorDepthSearchEditorPanel.class);

    // Constants
    private final static String PREFERENCE_KEY = "ColorDepthSearchEditor";

    // Utilities
    private final Debouncer debouncer = new Debouncer();
    
    // UI Components
    private final ConfigPanel configPanel;
    private final JPanel mainPanel;
    private final PaginatedResultsPanel resultPanel;
    private final JScrollPane scrollPane;
    private final JPanel dataPanel;
    
    // Results
    private final DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();
    
    // State
    private ColorDepthSearch search;
    private String sortCriteria;

    public ColorDepthSearchEditorPanel() {

        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());
        setFocusable(true);

        configPanel = new ConfigPanel(true) {
            @Override
            protected void titleClicked(MouseEvent e) {
                Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(this, Arrays.asList(search), true, true, true));
            }
        };
//        configPanel.addTitleComponent(viewButton, true, true);
        
        resultPanel = new PaginatedResultsPanel(selectionModel, this) {
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
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportView(mainPanel);
        
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
//                for(LoadedImagePanel image : lips) {
//                    rescaleImage(image);
//                    image.invalidate();
//                }
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
//                prepareLsmResults();
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
        return "Search: "+StringUtils.abbreviate(search.getName(), 15);
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
    public void loadDomainObject(final ColorDepthSearch colorDepthSearch, final boolean isUserDriven, final Callable<Void> success) {

        if (colorDepthSearch==null) return;
        
        if (!debouncer.queue(success)) {
            log.info("Skipping load, since there is one already in progress");
            return;
        }
        
        log.info("loadDomainObject({},isUserDriven={})",colorDepthSearch.getName(),isUserDriven);
        final StopWatch w = new StopWatch();

        configPanel.setTitle(colorDepthSearch.getName());
        selectionModel.setParentObject(colorDepthSearch);
        
        this.search = colorDepthSearch;
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                loadPreferences();
//                prepareLsmResults();
            }
            
            @Override
            protected void hadSuccess() {
                showResults(isUserDriven);
                
                
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
    
    public void showResults(boolean isUserDriven) {
        showResultView(isUserDriven);
        updateUI();
    }

//    private void prepareLsmResults() {
//
//        if (lsms==null) return;
//        
//        List<LSMImage> filteredLsms = new ArrayList<>();
//        for(LSMImage lsm : lsms) {
//
//            boolean display = true;
//
//            if (!StringUtils.areEqual(currObjective, ALL_VALUE) && !areEqualOrEmpty(currObjective, lsm.getObjective())) {
//                display = false;
//            }
//
//            if (!StringUtils.areEqual(currArea, ALL_VALUE) && !areEqualOrEmpty(currArea, lsm.getAnatomicalArea())) {
//                display = false;
//            }
//
//            if (display) {
//                filteredLsms.add(lsm);
//            }
//        }
//
//        DomainUtils.sortDomainObjects(filteredLsms, sortCriteria);
//        lsmSearchResults = SearchResults.paginate(filteredLsms, lsmAnnotations);
//    }
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
    private void showResultView(boolean isUserDriven) {

        dataPanel.removeAll();
        configPanel.removeAllConfigComponents();
//        configPanel.addConfigComponent(objectiveButton);
        
//        
//        if (sample!=null) {
//            Set<String> objectiveSet = new LinkedHashSet<>(sample.getObjectives());
//            Set<String> areaSet = new LinkedHashSet<>();
//            
//            // Populate currRunMap
//            for(String objective : objectiveSet) {
//                
//                ObjectiveSample objectiveSample = sample.getObjectiveSample(objective);
//                if (objectiveSample==null) continue;
//                
//                // Is there a run already selected?
//                SamplePipelineRun run = currRunMap.get(objective);
//                
//                if (run==null) {
//                	// If not, pick one as the default
//                    run = objectiveSample.getLatestRun();
//                	if (run!=null) {
//                		currRunMap.put(objective, run);
//                	}
//                }
//            }
//            
//            // Populate drop down buttons
//            for(String objective : objectiveSet) {
//                
//                ObjectiveSample objectiveSample = sample.getObjectiveSample(objective);
//                if (objectiveSample==null) continue;
//                SamplePipelineRun run = currRunMap.get(objective);
//                if (run==null || run.getResults()==null) continue;
//    
//                for(PipelineResult result : run.getResults()) {
//                    String area = null;
//                    if (result instanceof HasAnatomicalArea) {
//                        area = ((HasAnatomicalArea)result).getAnatomicalArea();
//                    }
//                    if (area==null) {
//                        area = "Unknown";
//                    }
//                	areaSet.add(area);
//                }
//                
//                DropDownButton historyButton = new DropDownButton(objective+": "+getLabel(run));
//                populateHistoryButton(historyButton, objectiveSample);
//                historyButtonMap.put(objective, historyButton);
//                configPanel.addConfigComponent(historyButton);
//            }
//    
//        	List<String> objectives = new ArrayList<>(objectiveSet);
//            objectives.add(0, ALL_VALUE);
//            populateObjectiveButton(objectives);
//            
//            List<String> areas = new ArrayList<>(areaSet);
//            areas.add(0, ALL_VALUE);
//            populateAreaButton(areas);
//            
//            for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
//                
//                String objective = objectiveSample.getObjective();
//                if (!StringUtils.areEqual(currObjective, ALL_VALUE) && !StringUtils.areEqual(currObjective, objective)) {
//                    continue;
//                }
//    
//                SamplePipelineRun run = currRunMap.get(objective);
//                if (run==null || run.getResults()==null) continue;
//                
//                for(PipelineResult result : run.getResults()) {
//    
//                    String area = null;
//                    if (result instanceof HasAnatomicalArea) {
//                        area = ((HasAnatomicalArea)result).getAnatomicalArea();
//                    }
//                    
//                    if (area==null) {
//                        area = "Unknown";
//                    }
//                    
//                    if (!StringUtils.areEqual(currArea, ALL_VALUE) && !areEqualOrEmpty(currArea, area)) {
//                        continue;
//                    }
//                    
//                    PipelineResultPanel resultPanel = new PipelineResultPanel(result);
//                    resultPanels.add(resultPanel);
//                    dataPanel.add(resultPanel);
//                }
//                
//                if (run.hasError()) {
//                    PipelineErrorPanel resultPanel = new PipelineErrorPanel(run);
//                    resultPanels.add(resultPanel);
//                    dataPanel.add(resultPanel);
//                }
//                
//            }
//        }
//        
        removeAll();
        add(configPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

	private String getLabel(SamplePipelineRun run) {
	    if (run==null) return "";
	    return DomainModelViewUtils.getDateString(run.getCreationDate());
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
}
