package org.janelia.it.workstation.gui.browser.gui.editor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
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
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.shared.utils.ReflectionUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.browser.actions.ExportResultsAction;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
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
import org.janelia.it.workstation.gui.browser.gui.support.LoadedImagePanel;
import org.janelia.it.workstation.gui.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.browser.gui.support.SelectablePanel;
import org.janelia.it.workstation.gui.browser.model.DomainModelViewUtils;
import org.janelia.it.workstation.gui.browser.model.ResultDescriptor;
import org.janelia.it.workstation.gui.browser.model.search.ResultPage;
import org.janelia.it.workstation.gui.browser.model.search.SearchResults;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
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
    private final SampleEditorToolbar toolbar;
    private final JPanel mainPanel;
    private final PaginatedResultsPanel lsmPanel;
    private final JScrollPane scrollPane;
    private final JPanel dataPanel;
    private final List<PipelineResultPanel> resultPanels = new ArrayList<>();
    private final Set<LoadedImagePanel> lips = new HashSet<>();
    
    // Results
    private SearchResults lsmSearchResults;
    private final DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();
    
    // State
    private Sample sample;
    private List<LSMImage> lsms;
    private List<Annotation> lsmAnnotations;
    private String currMode = MODE_RESULTS;
    private String currObjective = ALL_VALUE;
    private String currArea = ALL_VALUE;
    private int currResultIndex;
    
    // Listener for clicking on result panels
    protected MouseListener resultMouseListener = new MouseHandler() {

        @Override
        protected void popupTriggered(MouseEvent e) {
            if (e.isConsumed()) {
                return;
            }
            PipelineResultPanel resultPanel = getResultPanelAncestor(e.getComponent());
            // Select the button first
            resultPanelSelection(resultPanel, true);
            getButtonPopupMenu(resultPanel.getResult()).show(e.getComponent(), e.getX(), e.getY());
            e.consume();
        }

        @Override
        protected void doubleLeftClicked(MouseEvent e) {
            if (e.isConsumed()) {
                return;
            }
            PipelineResultPanel resultPanel = getResultPanelAncestor(e.getComponent());
            // Select the button first
            resultPanelSelection(resultPanel, true);
            buttonDrillDown(resultPanel.getResult());
            e.consume();
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            super.mouseReleased(e);
            if (e.isConsumed()) {
                return;
            }
            PipelineResultPanel resultPanel = getResultPanelAncestor(e.getComponent());
            if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() < 0) {
                return;
            }
            resultPanelSelection(resultPanel, true);
        }
    };
    
    public SampleEditorPanel() {
        
        setLayout(new BorderLayout());
        
        toolbar = new SampleEditorToolbar();
        populateViewButton();
        // TODO: load from user prefs
        toolbar.getObjectiveButton().setText("Objective: "+currObjective);
        toolbar.getAreaButton().setText("Area: "+currArea);

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
    
    private void resultPanelSelection(PipelineResultPanel resultPanel, boolean isUserDriven) {
        if (resultPanel==null) return;
        for(PipelineResultPanel otherResultPanel : resultPanels) {
            if (resultPanel != otherResultPanel) {
                otherResultPanel.setSelected(false);
            }
        }
        resultPanel.setSelected(true);
        Events.getInstance().postOnEventBus(new PipelineResultSelectionEvent(this, resultPanel.getResult(), isUserDriven));
    }
    
    private PipelineResultPanel getResultPanelAncestor(Component component) {
        Component c = component;
        while (c!=null) {
            if (c instanceof PipelineResultPanel) {
                return (PipelineResultPanel)c;
            }
            c = c.getParent();
        }
        return null;
    }
    
    private JPopupMenu getButtonPopupMenu(PipelineResult result) {
        SampleResultContextMenu popupMenu = new SampleResultContextMenu(result);
        popupMenu.addMenuItems();
        return popupMenu;
    }
    
    private void buttonDrillDown(PipelineResult result) {
        SampleResultContextMenu popupMenu = new SampleResultContextMenu(result);
        popupMenu.runDefaultAction();
    }

    public PipelineResultPanel getPreviousObject() {
        if (resultPanels == null) {
            return null;
        }
        int i = resultPanels.indexOf(currResultIndex);
        if (i < 1) {
            // Already at the beginning
            return null;
        }
        return resultPanels.get(i - 1);
    }

    public PipelineResultPanel getNextObject() {
        if (resultPanels == null) {
            return null;
        }
        int i = resultPanels.indexOf(currResultIndex);
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
        else {
            return "Sample: "+StringUtils.abbreviate(sample.getName(), 15);
        }
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
                
        PipelineResultPanel pipelineResultPanel = resultPanels.get(currResultIndex);
        ResultDescriptor resultDescriptor = pipelineResultPanel.getResultDescriptor();
        
        if (toggle) {
            hud.setObjectAndToggleDialog(sample, resultDescriptor, FileType.SignalMip.toString());
        }
        else {
            hud.setObject(sample, resultDescriptor, FileType.SignalMip.toString());
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
        
        log.info("loadDomainObject({})",sample.getName());
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
            }
            
            @Override
            protected void hadSuccess() {
                showResults();
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

        List<String> objectives = new ArrayList<>(sample.getOrderedObjectives());
        Set<String> areaSet = new LinkedHashSet<>();
        
        for(String objective : objectives) {
            
            ObjectiveSample objSample = sample.getObjectiveSample(objective);
            if (objSample==null) continue;
            SamplePipelineRun run = objSample.getLatestRun();
            if (run==null || run.getResults()==null) continue;
            
            for(PipelineResult result : run.getResults()) {

                String area = null;
                if (result instanceof HasAnatomicalArea) {
                    area = ((HasAnatomicalArea)result).getAnatomicalArea();
                }
                
                if (area==null) area = "";
                areaSet.add(area);
            }
        }
        
        objectives.add(0, ALL_VALUE);
        populateObjectiveButton(objectives);
        
        List<String> areas = new ArrayList<>(areaSet);
        areas.add(0, ALL_VALUE);
        populateAreaButton(areas);
        
        if (MODE_LSMS.equals(currMode))  {
            showLsmView();
        }
        else if (MODE_RESULTS.equals(currMode)) {
            showResultView();
        }
        updateUI();
    }

    private void showLsmView() {

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
        add(toolbar, BorderLayout.NORTH);
        add(lsmPanel, BorderLayout.CENTER);
    }

    private void showResultView() {

        lips.clear();
        resultPanels.clear();
        dataPanel.removeAll();
        
        GridBagConstraints c = new GridBagConstraints();
        int y = 0;
                
        for(String objective : sample.getOrderedObjectives()) {
            
            boolean diplayObjective = true;
            
            if (!currObjective.equals(ALL_VALUE) && !currObjective.equals(objective)) {
                diplayObjective = false;
            }
            
            ObjectiveSample objSample = sample.getObjectiveSample(objective);
            if (objSample==null) continue;
            SamplePipelineRun run = objSample.getLatestRun();
            if (run==null || run.getResults()==null) continue;
            
            for(PipelineResult result : run.getResults()) {

                String area = null;
                if (result instanceof HasAnatomicalArea) {
                    area = ((HasAnatomicalArea)result).getAnatomicalArea();
                }
                
                if (area==null) area = "";
                
                boolean display = diplayObjective;
                if (!currArea.equals(ALL_VALUE) && !areEqualOrEmpty(currArea, area)) {
                    display = false;
                }
                
                if (display) {
                    c.gridx = 0;
                    c.gridy = y++;
                    c.fill = GridBagConstraints.BOTH;
                    c.anchor = GridBagConstraints.PAGE_START;
                    c.weightx = 1;
                    c.weighty = 0.9;
                    PipelineResultPanel resultPanel = new PipelineResultPanel(result);
                    resultPanels.add(resultPanel);
                    dataPanel.add(resultPanel);
                }
            }
        }
        
        if (!resultPanels.isEmpty()) {
            resultPanelSelection(resultPanels.get(0), false);
        }

        removeAll();
        add(toolbar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void populateViewButton() {
        toolbar.getViewButton().setText(currMode);
        JPopupMenu popupMenu = toolbar.getViewButton().getPopupMenu();
        popupMenu.removeAll();
        ButtonGroup group = new ButtonGroup();
        for (final String mode : Arrays.asList(MODE_LSMS, MODE_RESULTS)) {
            JMenuItem menuItem = new JMenuItem(mode);
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    toolbar.getViewButton().setText(mode);
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
        toolbar.getObjectiveButton().setText("Objective: "+currObjective);
        JPopupMenu popupMenu = toolbar.getObjectiveButton().getPopupMenu();
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
        toolbar.getAreaButton().setText("Area: "+currArea);
        JPopupMenu popupMenu = toolbar.getAreaButton().getPopupMenu();
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
                this.resultDescriptor = ClientDomainUtils.getResultDescriptor(result);
                if (result instanceof SampleAlignmentResult) {
                    SampleAlignmentResult sar = (SampleAlignmentResult)result;
                    label.setText(resultDescriptor+" ("+sar.getAlignmentSpace()+")");
                }
                else {
                    label.setText(resultDescriptor.toString());
                }
                subLabel.setText(DomainModelViewUtils.getDateString(result.getCreationDate()));
                
                String signalMip = DomainUtils.getFilepath(result, FileType.SignalMip);
                String refMip = DomainUtils.getFilepath(result, FileType.ReferenceMip);
    
                imagePanel.add(getImagePanel(signalMip));
                imagePanel.add(getImagePanel(refMip));
    
                JPanel titlePanel = new JPanel(new BorderLayout());
                titlePanel.add(label, BorderLayout.PAGE_START);
                titlePanel.add(subLabel, BorderLayout.PAGE_END);
                
                add(titlePanel, BorderLayout.NORTH);
                add(imagePanel, BorderLayout.CENTER);
    
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
}
