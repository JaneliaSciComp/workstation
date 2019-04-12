package org.janelia.workstation.browser.gui.colordepth;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.Subscribe;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.workstation.browser.gui.hud.Hud;
import org.janelia.workstation.browser.gui.progress.ProgressMeterMgr;
import org.janelia.workstation.browser.gui.support.SelectablePanel;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.api.web.AsyncServiceClient;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.model.DomainObjectChangeEvent;
import org.janelia.workstation.core.events.model.DomainObjectInvalidationEvent;
import org.janelia.workstation.core.events.model.DomainObjectRemoveEvent;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.janelia.workstation.core.events.workers.WorkerEndedEvent;
import org.janelia.workstation.common.gui.editor.DomainObjectEditor;
import org.janelia.workstation.common.gui.editor.DomainObjectEditorState;
import org.janelia.workstation.common.gui.editor.ParentNodeSelectionEditor;
import org.janelia.workstation.common.gui.support.Debouncer;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.support.MouseForwarder;
import org.janelia.workstation.common.nodes.DomainObjectNode;
import org.janelia.workstation.core.util.HelpTextUtils;
import org.janelia.workstation.core.workers.AsyncServiceMonitoringWorker;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.events.selection.DomainObjectSelectionEvent;
import org.janelia.workstation.browser.gui.support.LoadedImagePanel;
import org.janelia.workstation.browser.gui.support.SelectablePanelListPanel;
import org.janelia.workstation.core.workers.BackgroundWorker;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.colordepth.ColorDepthMask;
import org.janelia.model.domain.gui.colordepth.ColorDepthMatch;
import org.janelia.model.domain.gui.colordepth.ColorDepthResult;
import org.janelia.model.domain.gui.colordepth.ColorDepthSearch;
import org.janelia.model.domain.sample.DataSet;
import org.janelia.model.domain.sample.Sample;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specialized component for executing color depth searches on the cluster and viewing their results.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthSearchEditorPanel extends JPanel implements DomainObjectEditor<ColorDepthSearch>, ParentNodeSelectionEditor<ColorDepthSearch, ColorDepthMatch, String> {

    private final static Logger log = LoggerFactory.getLogger(ColorDepthSearchEditorPanel.class);

    // Utilities
    private final Debouncer debouncer = new Debouncer();
    private final AsyncServiceClient asyncServiceClient = new AsyncServiceClient();
    
    // UI Components
//    private final JButton saveButton;
//    private final JButton saveAsButton;
    private final SearchOptionsPanel searchOptionsPanel;
    private final JSplitPane splitPane;
    private final JPanel helpPanel;
    private final JButton searchButton;
    private final JPanel executingPanel;
    private final JLabel executionErrorLabel;
    private final SelectablePanelListPanel maskListPanel;
    private final JScrollPane maskScrollPane;
    private final Set<LoadedImagePanel> lips = new HashSet<>();
    private final ColorDepthResultPanel colorDepthResultPanel;
    
    // State
    private ColorDepthSearchNode searchNode;
    private ColorDepthSearch search;
    private List<ColorDepthMask> masks; // cached masks
    private List<ColorDepthResult> results; // cached results
    private Reference selectedMaskRef = null;
    private Map<ColorDepthMask,MaskPanel> maskPanelMap = new HashMap<>();
    
    public ColorDepthSearchEditorPanel() {

        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());
        setFocusable(true);
                
        searchOptionsPanel = new SearchOptionsPanel();
        
        searchButton = new JButton("Execute Search");
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                if (search.getMasks().isEmpty()) {
                    JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(), "You need to select some masks to search on.");
                    return;
                }
                
                if (search.getDataSets().isEmpty()) {
                    JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(), "You need to select some data sets to search against.");
                    return;
                }
                
                if (executingPanel.isVisible()) {
                Object[] options = { "Yes", "Cancel" };
                int result = JOptionPane.showOptionDialog(ColorDepthSearchEditorPanel.this, 
                        "This search is already running. Are you sure you want to run it again?", 
                        "Execute search", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, options, options[0]);
                
                if (result != 0) return;
                }
                
                SimpleWorker worker = new SimpleWorker() {
                        
                    @Override
                    protected void doStuff() throws Exception {
                        search = searchOptionsPanel.saveChanges();
                    }

                    @Override
                    protected void hadSuccess() {
                        executeSearch();
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        FrameworkImplProvider.handleException(error);
                    }
                };

                worker.execute();
            }
        });
        
        executingPanel = new JPanel(new BorderLayout());
        executingPanel.setVisible(false);
        executingPanel.add(new JLabel("Executing...", Icons.getLoadingIcon(), SwingConstants.RIGHT));
        
        executionErrorLabel = new JLabel("Error encountered while executing search");
        executionErrorLabel.setVisible(false);
        executionErrorLabel.setForeground(Color.red);
        
        colorDepthResultPanel = new ColorDepthResultPanel();
        
        maskListPanel = new SelectablePanelListPanel() {

            @Override
            protected void panelSelected(SelectablePanel resultPanel, boolean isUserDriven) {
                if (resultPanel instanceof MaskPanel) {
                    ColorDepthMask mask = ((MaskPanel)resultPanel).getMask();
                    colorDepthResultPanel.loadSearchResults(search, results, mask, isUserDriven);
                    selectedMaskRef = Reference.createFor(mask);
                    Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(this, Arrays.asList(mask), isUserDriven, true, true));
                }
            }
            
            @Override
            protected void updateHud(SelectablePanel resultPanel, boolean toggle) {
                if (resultPanel instanceof MaskPanel) {
                    ColorDepthMask mask = ((MaskPanel)resultPanel).getMask();
                    Hud.getSingletonInstance().setFilepathAndToggleDialog(mask.getFilepath(), toggle, false);
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
        maskScrollPane.setViewportView(maskListPanel);
        maskScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        maskScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS); 
        
        helpPanel = new JPanel();
        helpPanel.setLayout(new GridBagLayout());
        JPanel panel = new JPanel();
        panel.add(new JLabel("<html>You need to add some masks to this search.<br>"
                + "To do so, right-click any Color Depth Projection and click "+ HelpTextUtils.getBoldedLabel("Create Mask for Color Depth Search")+",<br>"
                + "or upload a custom mask using the "+HelpTextUtils.getMenuItemLabel("File","Upload","Color Depth Mask")+"  menu option.</html>"));
        helpPanel.add(panel, new GridBagConstraints());
        
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, maskScrollPane, colorDepthResultPanel);
        splitPane.setDividerLocation(0.30);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        
        Dimension minimumSize = new Dimension(100, 0);
        maskScrollPane.setMinimumSize(minimumSize);
        colorDepthResultPanel.setMinimumSize(minimumSize);
        
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
    
    private void reload() throws Exception {
        
        if (search==null) {
            // Nothing to reload
            return;
        }
        
        try {
            ColorDepthSearch updatedSearch = DomainMgr.getDomainMgr().getModel().getDomainObject(search.getClass(), search.getId());
            if (updatedSearch!=null) {
                if (searchNode!=null && !searchNode.getColorDepthSearch().equals(updatedSearch)) {
                    searchNode.update(updatedSearch);
                }
                this.search = updatedSearch;
                restoreState(saveState());
                loadDomainObject(updatedSearch, false, null);
            }
            else {
                // The search no longer exists, or we no longer have access to it (perhaps running as a different user?) 
                // Either way, there's nothing to show. 
                showNothing();
            }
        }
        catch (Exception e) {
            FrameworkImplProvider.handleException(e);
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
        return colorDepthResultPanel;
    }

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }

    @Override
    public void loadDomainObjectNode(DomainObjectNode<ColorDepthSearch> domainObjectNode, boolean isUserDriven, Callable<Void> success) {
        this.searchNode = (ColorDepthSearchNode) domainObjectNode;
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

        searchOptionsPanel.setSearch(colorDepthSearch);
        
        setProcessing(false);
        setError(false);
        maskPanelMap.clear();
        
        this.search = colorDepthSearch;
        log.info("Loading {} masks", colorDepthSearch.getMasks().size());
        log.info("Loading {} results", colorDepthSearch.getResults().size());
        
        SimpleWorker worker = new SimpleWorker() {

            private List<DataSet> alignmentSpaceDataSets;
            
            @Override
            protected void doStuff() throws Exception {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                masks = model.getDomainObjectsAs(ColorDepthMask.class, colorDepthSearch.getMasks());
                results = model.getDomainObjectsAs(ColorDepthResult.class, colorDepthSearch.getResults());
                alignmentSpaceDataSets = model.getColorDepthDataSets(colorDepthSearch.getAlignmentSpace());
            }
            
            @Override
            protected void hadSuccess() {

                log.info("Loaded ColorDepthSearch#{}", colorDepthSearch.getId());
                log.info("Loaded {} masks", masks.size());
                log.info("Loaded {} results", results.size());
                log.info("Loaded {} data sets", alignmentSpaceDataSets.size());
                
                searchOptionsPanel.setDataSets(alignmentSpaceDataSets);
                
                showSearchView(isUserDriven);
                
                debouncer.success();
                
                // Update processing status
                for(BackgroundWorker worker : ProgressMeterMgr.getProgressMeterMgr().getActiveWorkers()) {
                    if (worker instanceof SearchMonitoringWorker) {
                        SearchMonitoringWorker searchWorker = (SearchMonitoringWorker)worker;
                        if (searchWorker.getSearch().getId().equals(search.getId())) {
                            setProcessing(true);
                        }
                    }
                }
                
                ActivityLogHelper.logElapsed("ColorDepthSearchEditorPanel.loadDomainObject", search, w);
            }
            
            @Override
            protected void hadError(Throwable error) {
                showNothing();
                debouncer.failure();
                FrameworkImplProvider.handleException(error);
            }
        };
        worker.execute();
    }

    public void showNothing() {
        removeAll();
        updateUI();
    }
    
    private void showSearchView(boolean isUserDriven) {
        
        lips.clear();
        maskListPanel.clearPanels();
        searchOptionsPanel.refresh();
        searchOptionsPanel.addConfigComponent(searchButton);
        searchOptionsPanel.addConfigComponent(executingPanel);
        searchOptionsPanel.addConfigComponent(executionErrorLabel);
        
        JLabel titleLabel = new JLabel("Search Masks:");
        titleLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
     
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        maskListPanel.add(titlePanel);
        
        for(ColorDepthMask mask : masks) {
            MaskPanel maskPanel = new MaskPanel(mask);
            maskListPanel.addPanel(maskPanel);
            maskPanelMap.put(mask, maskPanel);
        }

        removeAll();
        add(searchOptionsPanel, BorderLayout.NORTH);
        
        if (masks.isEmpty()) {
            add(helpPanel, BorderLayout.CENTER);
            colorDepthResultPanel.showNothing();
        }
        else {

            ColorDepthMask selectedMask = null;
            if (selectedMaskRef != null) {
                log.debug("Checking groups for previously selected group: "+selectedMaskRef);
                for(ColorDepthMask mask : masks) {
                    if (mask.getId().equals(selectedMaskRef.getTargetId())) {
                        selectedMask = mask;
                        break;
                    }
                }
            }

            add(splitPane, BorderLayout.CENTER);
            if (selectedMask == null) {
                // Automatically select the first group
                log.debug("Selecting first group");
                maskListPanel.selectFirst(isUserDriven);
            }
            else {
                // Reselect the last selected group
                MaskPanel maskPanel = maskPanelMap.get(selectedMask);
                log.debug("Selecting previously selected group: "+maskPanel.getMask());
                maskListPanel.selectPanel(maskPanel, isUserDriven);
            }
            
            // Update selected mask
            selectedMaskRef = Reference.createFor(((MaskPanel)maskListPanel.getSelectedPanel()).getMask());
        }
        
        updateUI();
    }

    private void rescaleImage(LoadedImagePanel image) {
        double width = image.getParent()==null?0:image.getParent().getSize().getWidth()-18;
        if (width<=0) {
            log.debug("Could not get width from parent, using viewport");
            width = maskScrollPane.getViewport().getSize().getWidth()-18;
        }
        if (width<=0) {
            log.debug("Could not get width from parent or viewport");
            return;
        }
        int w = (int)Math.ceil(width);
        log.trace("Using width={}", w);
        image.scaleImage(w);
    }
    
    @Override
    public ChildSelectionModel<ColorDepthMatch,String> getSelectionModel() {
        return colorDepthResultPanel.getSelectionModel();
    }

    @Override
    public ChildSelectionModel<ColorDepthMatch,String> getEditSelectionModel() {
        return colorDepthResultPanel.getEditSelectionModel();
    }
    
    private void executeSearch() {

        ActivityLogHelper.logUserAction("ColorDepthSearchEditorPanel.executeSearch", search);
        
        Long serviceId = asyncServiceClient.invokeService("colorDepthObjectSearch",
                ImmutableList.of("-searchId", search.getId().toString()),
                null,
                ImmutableMap.of());
        
        AsyncServiceMonitoringWorker executeWorker = new SearchMonitoringWorker(search, serviceId) {
            @Override
            public Callable<Void> getSuccessCallback() {
                return new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        // Refresh and load the search which is completed
                        forceInvalidate();
                        return null;
                    }
                };
            }
        };
        setProcessing(true);
        setError(false);
        executeWorker.executeWithEvents();
    }
    
    @Subscribe
    public void processEvent(WorkerEndedEvent e) {
        if (e.getWorker() instanceof SearchMonitoringWorker) {
            SearchMonitoringWorker worker = (SearchMonitoringWorker)e.getWorker();
            log.info("Got worker ended event: "+worker.getSearch().getId());
            if (worker.getSearch().getId().equals(search.getId())) {
                setProcessing(false);
                if (worker.getError() != null) {
                    setError(true);
                }
            }
        }
    }

    @Subscribe
    public void colorDepthMatchSelected(ColorDepthMatchSelectionEvent event) {

        // We only care about single selections
        ColorDepthMatch match = event.getObjectIfSingle();
        if (match==null) {
            return;
        }

        if (!event.isSelect()) {
            log.debug("Event is not selection: {}",event);
            return;
        }
        
        if (event.isUserDriven()) {
            log.info("colorDepthMatchSelected({})", match.getFilepath());
            FrameworkImplProvider.getInspectionHandler().inspect(getProperties(match));
        }
    }
    
    private Map<String,Object> getProperties(ColorDepthMatch match) {

        Map<String,Object> values = new HashMap<>();
        
        String dataSet = match.getDataSet();
        String owner = dataSet.split("_")[0];

        values.put("Channel Number", match.getChannelNumber());
        values.put("Score (Pixels)", match.getScore());
        values.put("Score (Percent)", MaskUtils.getFormattedScorePct(match));
        values.put("Data Set", dataSet);
        values.put("Owner", owner);
        
        try {
            if (match.getSample()==null) {
                // Non-Workstation Data set
                values.put("Name", match.getFile().getName());
                values.put("Filepath", match.getFilepath());
            }
            else {
                Sample sample = DomainMgr.getDomainMgr().getModel().getDomainObject(match.getSample());
                if (sample!=null) {
                    values.put("Name", sample.getName());
                    values.put("Line", sample.getLine());
                    values.put("VT Line", sample.getVtLine());
                    // Only display the filepath if user has access to the sample
                    values.put("Filepath", match.getFilepath());
                }
            }
        }
        catch (Exception e) {
            FrameworkImplProvider.handleExceptionQuietly(e);
        }
        
        return values;
    }
    
    private void forceInvalidate() {
        SimpleWorker.runInBackground(() -> {
            try {
                log.info("Invaliding search object");
                DomainMgr.getDomainMgr().getModel().invalidate(search);
            }
            catch (Exception ex) {
                FrameworkImplProvider.handleExceptionQuietly(ex);
            }
        });
    }
    
    private void setProcessing(boolean isRunning) {
        executingPanel.setVisible(isRunning);
    }
    
    private void setError(boolean isError) {
        executionErrorLabel.setVisible(isError);
    }
    
    @Override
    public void resetState() {
        colorDepthResultPanel.reset();
    }

    @Override
    public DomainObjectEditorState<ColorDepthSearch,ColorDepthMatch,String> saveState() {
        if (searchNode==null) {
            if (search==null) {
                log.warn("No object is loaded, so state cannot be saved");
                return null;
            }
            return new ColorDepthSearchEditorState(
                    search,
                    selectedMaskRef,
                    colorDepthResultPanel.getCurrResultIndex(),
                    colorDepthResultPanel.getResultPanel().getCurrPage(),
                    colorDepthResultPanel.getResultPanel().getViewer().saveState(),
                    getSelectionModel().getSelectedIds());
        }
        else {
            if (searchNode.getDomainObject()==null) {
                log.warn("No object is loaded, so state cannot be saved");
                return null;
            }
            return new ColorDepthSearchEditorState(
                    searchNode,
                    selectedMaskRef,
                    colorDepthResultPanel.getCurrResultIndex(),
                    colorDepthResultPanel.getResultPanel().getCurrPage(),
                    colorDepthResultPanel.getResultPanel().getViewer().saveState(),
                    getSelectionModel().getSelectedIds());
        }
    }

    @Override
    public void restoreState(DomainObjectEditorState<ColorDepthSearch,ColorDepthMatch,String> state) {
        if (state==null) {
            log.warn("Cannot restore null state");
            return;
        }
        
        ColorDepthSearchEditorState myState = (ColorDepthSearchEditorState)state;
        log.info("Restoring state: {}", myState);
        if (state.getListViewerState()!=null) {
            colorDepthResultPanel.getResultPanel().setViewerType(state.getListViewerState().getType());
        }

        // Prepare to restore the selection
        List<String> selected = colorDepthResultPanel.getResultPanel().getViewer().getSelectionModel().getSelectedIds();
        selected.clear();
        selected.addAll(state.getSelectedIds());

        // Prepare to restore group selection
        this.selectedMaskRef = myState.getSelectedMask();
        
        // Prepare to restore the page
        colorDepthResultPanel.getResultPanel().setCurrPage(state.getPage());
        colorDepthResultPanel.getResultPanel().getViewer().restoreState(state.getListViewerState());
        
        if (state.getDomainObjectNode()==null) {
            loadDomainObject(state.getDomainObject(), true, null);
        }
        else {
            loadDomainObjectNode(state.getDomainObjectNode(), true, null);
        }
    }

    @Subscribe
    public void domainObjectInvalidated(DomainObjectInvalidationEvent event) {
        try {
		    if (search==null) return;
            if (event.isTotalInvalidation()) {
                log.info("total invalidation, reloading...");
                reload();
            }
            else {
                for (DomainObject domainObject : event.getDomainObjects()) {
                    if (StringUtils.areEqual(domainObject.getId(), search.getId())) {
                        log.info("Search invalidated, reloading...");
                        reload();
                        break;
                    }
                }
            }
        }  
        catch (Exception e) {
            FrameworkImplProvider.handleException(e);
        }
    }

    @Subscribe
    public void domainObjectRemoved(DomainObjectRemoveEvent event) {
        if (search==null) return;
        if (StringUtils.areEqual(event.getDomainObject().getId(), search.getId())) {
            this.search = null;
            showNothing();
        }
    }

    @Subscribe
    public void domainObjectChanged(DomainObjectChangeEvent event) {
        if (search==null) return;
        try {
            DomainObject domainObject = event.getDomainObject();
            if (domainObject==null) return;
            if (search != null && domainObject.getId().equals(search.getId())) {
                log.info("Search has changed, reloading...");
                reload();
            }
            else if (selectedMaskRef!=null && selectedMaskRef.getTargetId().equals(domainObject.getId())) {
                log.info("Selected mask has changed, reloading...");
                reload();
            }
        }  
        catch (Exception e) {
            FrameworkImplProvider.handleException(e);
        }
    }
    
    private class MaskPanel extends SelectablePanel {
        
        private final ColorDepthMask mask;
        
        private MaskPanel(ColorDepthMask mask) {
            
            this.mask = mask;
            
            int b = SelectablePanel.BORDER_WIDTH;
            setBorder(BorderFactory.createEmptyBorder(b, b, b, b));
            setLayout(new BorderLayout());
            
            // TODO: this is a crude temporary workaround to the fact that long names resize the 
            // mask panel to be too wide. It should be solved with better layout.
            String name = StringUtils.abbreviate(mask.getName(), 50);
            
            JLabel label = new JLabel();
            label.setText(name);
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
