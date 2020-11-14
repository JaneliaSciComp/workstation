package org.janelia.workstation.browser.gui.colordepth;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.cdmip.*;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.gui.hud.Hud;
import org.janelia.workstation.browser.gui.progress.ProgressMeterMgr;
import org.janelia.workstation.browser.gui.support.LoadedImagePanel;
import org.janelia.workstation.browser.gui.support.SelectablePanel;
import org.janelia.workstation.browser.gui.support.SelectablePanelListPanel;
import org.janelia.workstation.common.gui.editor.DomainObjectEditor;
import org.janelia.workstation.common.gui.editor.DomainObjectEditorState;
import org.janelia.workstation.common.gui.editor.ParentNodeSelectionEditor;
import org.janelia.workstation.common.gui.support.Debouncer;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.support.MouseForwarder;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.api.web.AsyncServiceClient;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.model.DomainObjectChangeEvent;
import org.janelia.workstation.core.events.model.DomainObjectInvalidationEvent;
import org.janelia.workstation.core.events.model.DomainObjectRemoveEvent;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.janelia.workstation.core.events.selection.DomainObjectSelectionEvent;
import org.janelia.workstation.core.events.selection.ViewerContextChangeEvent;
import org.janelia.workstation.core.events.workers.WorkerEndedEvent;
import org.janelia.workstation.core.model.ImageModel;
import org.janelia.workstation.core.nodes.DomainObjectNode;
import org.janelia.workstation.core.util.HelpTextUtils;
import org.janelia.workstation.core.util.StringUtilsExtra;
import org.janelia.workstation.core.workers.BackgroundWorker;
import org.janelia.workstation.core.workers.SearchMonitoringWorker;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Specialized component for executing color depth searches on the cluster and viewing their results.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthSearchEditorPanel
        extends JPanel
        implements DomainObjectEditor<ColorDepthSearch>,
                   ParentNodeSelectionEditor<ColorDepthSearch,ColorDepthMatch,Reference> {

    private final static Logger log = LoggerFactory.getLogger(ColorDepthSearchEditorPanel.class);

    // Utilities
    private final Debouncer debouncer = new Debouncer();
    private final Debouncer reloadDebouncer = new Debouncer();

    // UI Components
    private final ColorDepthSearchOptionsPanel searchOptionsPanel;
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
    private List<ColorDepthLibrary> alignmentSpaceLibraries; // cached results
    private Reference selectedMaskRef = null;
    private Map<ColorDepthMask,MaskPanel> maskPanelMap = new HashMap<>();
    
    public ColorDepthSearchEditorPanel() {

        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());
        setFocusable(true);
                
        searchOptionsPanel = new ColorDepthSearchOptionsPanel();
        
        searchButton = new JButton("Execute Search");
        searchButton.addActionListener(e -> executeSearch());
        
        executingPanel = new JPanel(new BorderLayout());
        executingPanel.setVisible(false);
        executingPanel.add(new JLabel("Executing...", Icons.getLoadingIcon(), SwingConstants.RIGHT));
        
        executionErrorLabel = new JLabel("Error encountered while executing search");
        executionErrorLabel.setVisible(false);
        executionErrorLabel.setForeground(Color.red);
        
        colorDepthResultPanel = new ColorDepthResultPanel() {
            @Override
            protected void viewerContextChanged() {
                Events.getInstance().postOnEventBus(new ViewerContextChangeEvent(this, getViewerContext()));
            }
        };
        
        maskListPanel = new SelectablePanelListPanel() {

            @Override
            protected void panelSelected(SelectablePanel resultPanel, boolean isUserDriven) {
                if (resultPanel instanceof MaskPanel) {
                    ColorDepthMask mask = ((MaskPanel)resultPanel).getMask();
                    if (results!=null) {
                        colorDepthResultPanel.loadSearchResults(search, results, mask, isUserDriven);
                    }
                    else {
                        colorDepthResultPanel.showNoMatches();
                    }
                    selectedMaskRef = Reference.createFor(mask);
                    Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(this, Arrays.asList(mask), isUserDriven, true, true));
                }
            }
            
            @Override
            protected void updateHud(SelectablePanel resultPanel, boolean toggle) {
                if (resultPanel instanceof MaskPanel) {
                    ColorDepthMask mask = ((MaskPanel)resultPanel).getMask();
                    Hud.getSingletonInstance().setFilepathAndToggleDialog(mask.getFilepath(), null, toggle, false);
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

    private void executeSearch() {

        if (!ClientDomainUtils.isOwner(search)) {
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), "Only the search owner ("+search.getOwnerName()+") can execute this search.");
            return;
        }

        if (search.getMasks().isEmpty()) {
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), "You need to select some masks to search on.");
            return;
        }

        if (search.getCDSTargets().isEmpty()) {
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), "You need to select some color depth libraries to search against.");
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

        log.info("Executing color depth search {}", search);

        setProcessing(true);
        setError(false);

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                search = searchOptionsPanel.saveChanges();
                boolean allMasks = searchOptionsPanel.isAllMasks();
                ActivityLogHelper.logUserAction("ColorDepthSearchEditorPanel.executeSearch", search);
                DomainMgr.getDomainMgr().getAsyncFacade().executeColorDepthService(search, allMasks?null:selectedMaskRef);
            }

            @Override
            protected void hadSuccess() {
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };

        worker.execute();
    }

    private void reload() {
        
        if (search==null) {
            // Nothing to reload
            return;
        }

        if (!reloadDebouncer.queue()) {
            log.info("Skipping reload, since there is one already in progress");
            return;
        }

        SimpleWorker worker = new SimpleWorker() {

            ColorDepthSearch updatedSearch;

            @Override
            protected void doStuff() throws Exception {
                updatedSearch = DomainMgr.getDomainMgr().getModel().getDomainObject(search.getClass(), search.getId());
            }

            @Override
            protected void hadSuccess() {
                if (updatedSearch!=null) {
                    if (searchNode!=null && !searchNode.getColorDepthSearch().equals(updatedSearch)) {
                        searchNode.update(updatedSearch);
                    }
                    search = updatedSearch;
                    restoreState(saveState());
                }
                else {
                    // The search no longer exists, or we no longer have access to it (perhaps running as a different user?)
                    // Either way, there's nothing to show.
                    showNothing();
                }
                reloadDebouncer.success();
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
                reloadDebouncer.failure();
            }
        };

        worker.execute();
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

        showLoadingIndicator();

        searchOptionsPanel.setSearch(colorDepthSearch);

        // Reset state
        if (isUserDriven) {
            log.info("Reset progress UI");
            setProcessing(false);
            setError(false);
        }
        maskPanelMap.clear();
        this.masks = null;
        this.results = null;
        this.alignmentSpaceLibraries = null;
        this.search = colorDepthSearch;

        log.info("Loading {} masks", colorDepthSearch.getMasks().size());
        log.info("Loading {} results", colorDepthSearch.getResults().size());
        
        SimpleWorker worker = new SimpleWorker() {
            
            @Override
            protected void doStuff() throws Exception {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                masks = model.getDomainObjectsAs(ColorDepthMask.class, colorDepthSearch.getMasks());
                results = model.getDomainObjectsAs(ColorDepthResult.class, colorDepthSearch.getResults());
                alignmentSpaceLibraries = model.getColorDepthLibraries(colorDepthSearch.getAlignmentSpace());
            }
            
            @Override
            protected void hadSuccess() {

                log.info("Loaded ColorDepthSearch#{}", colorDepthSearch.getId());
                log.info("Loaded {} masks", masks.size());
                log.info("Loaded {} results", results.size());
                log.info("Loaded {} libraries for alignment space", alignmentSpaceLibraries.size(), colorDepthSearch.getAlignmentSpace());
                
                searchOptionsPanel.setLibraries(alignmentSpaceLibraries);
                
                showSearchView(isUserDriven);
                
                debouncer.success();

                setProcessing(checkIfProcessing());
                
                ActivityLogHelper.logElapsed("ColorDepthSearchEditorPanel.loadDomainObject", search, w);
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

    private boolean checkIfProcessing() {
        for(BackgroundWorker worker : ProgressMeterMgr.getProgressMeterMgr().getActiveWorkers()) {
            if (worker instanceof SearchMonitoringWorker) {
                SearchMonitoringWorker searchWorker = (SearchMonitoringWorker)worker;
                log.info("Checking active worker {}?={}", searchWorker.getSearch().getId(), search.getId());
                if (searchWorker.getSearch().getId().equals(search.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void showNothing() {
        removeAll();
        updateUI();
    }

    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()));
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

        removeAll();
        add(searchOptionsPanel, BorderLayout.NORTH);
        
        if (masks==null || masks.isEmpty()) {
            add(helpPanel, BorderLayout.CENTER);
            colorDepthResultPanel.showNothing();
        }
        else {

            for(ColorDepthMask mask : masks) {
                MaskPanel maskPanel = new MaskPanel(mask);
                maskListPanel.addPanel(maskPanel);
                maskPanelMap.put(mask, maskPanel);
            }

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
    public ChildSelectionModel<ColorDepthMatch,Reference> getSelectionModel() {
        return colorDepthResultPanel.getSelectionModel();
    }

    @Override
    public ChildSelectionModel<ColorDepthMatch,Reference> getEditSelectionModel() {
        return colorDepthResultPanel.getEditSelectionModel();
    }

    @Override
    public ViewerContext<ColorDepthMatch,Reference> getViewerContext() {
        return new ViewerContext<ColorDepthMatch,Reference>() {
            @Override
            public ChildSelectionModel<ColorDepthMatch,Reference> getSelectionModel() {
                return colorDepthResultPanel.getSelectionModel();
            }

            @Override
            public ChildSelectionModel<ColorDepthMatch,Reference> getEditSelectionModel() {
                return colorDepthResultPanel.isEditMode() ? colorDepthResultPanel.getEditSelectionModel() : null;
            }

            @Override
            public ImageModel<ColorDepthMatch,Reference> getImageModel() {
                return colorDepthResultPanel.getImageModel();
            }
        };
    }
    
    @Subscribe
    public void processEvent(WorkerEndedEvent e) {
        ColorDepthSearch search = this.search;
        if (search!=null) {
            if (e.getWorker() instanceof SearchMonitoringWorker) {
                SearchMonitoringWorker worker = (SearchMonitoringWorker) e.getWorker();
                log.info("Got worker ended event: " + worker.getSearch().getId());
                if (worker.getSearch().getId().equals(this.search.getId())) {
                    setProcessing(false);
                    setError(worker.getError() != null);
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
            log.info("colorDepthMatchSelected({})", match.getImageRef());
            FrameworkAccess.getInspectionController().inspect(getProperties(match));
        }
    }
    
    private Map<String, Object> getProperties(ColorDepthMatch match) {

        Map<String, Object> values = new HashMap<>();

        try {
            values.put("Score (Pixels)", match.getScore());
            values.put("Score (Percent)", MaskUtils.getFormattedScorePct(match));
            if (match.getNormalizedScore() != null) values.put("Normalized score", match.getNormalizedScore());
            if (match.getGradientAreaGap() != null) values.put("Area gap", match.getGradientAreaGap());
            if (match.getHighExpressionArea() != null) values.put("High Expression", match.getHighExpressionArea());

            ColorDepthResultImageModel imageModel = colorDepthResultPanel.getImageModel();
            if (imageModel==null) {
                log.warn("Image model is not ready, no image properties could be loaded for {}", match.getImageRef());
                return values;
            }

            ColorDepthImage image = imageModel.getImage(match);
            if (image!=null) {
                    values.put("Channel Number", image.getChannelNumber());
                    values.put("Owner", image.getOwnerName());
                    values.put("File Name", image.getName());
                    values.put("File Path", image.getFilepath());

                    String libraries = StringUtilsExtra.getCommaDelimited(image.getLibraries());
                    values.put("Color Depth Libraries", libraries);

                    if (image.getSampleRef() != null) {
                        Sample sample = imageModel.getSample(match);
                        if (sample != null) {
                            values.put("Sample Name", sample.getName());
                            values.put("Line", sample.getLine());
                            values.put("VT Line", sample.getVtLine());
                        }
                        else {
                            // Hide the filepath if user can't access the sample
                            values.remove("Filepath");
                        }
                    }
            }
        } catch (Exception e) {
            FrameworkAccess.handleExceptionQuietly(e);
        }

        return values;
    }

    private void setProcessing(boolean isProcessing) {
        log.info("Updating progress UI to: {}", isProcessing);
        executingPanel.setVisible(isProcessing);
        searchButton.setEnabled(!isProcessing);
        searchOptionsPanel.updateUI();
    }
    
    private void setError(boolean isError) {
        log.info("Updating error UI to: {}", isError);
        executionErrorLabel.setVisible(isError);
        searchOptionsPanel.updateUI();
    }
    
    @Override
    public void resetState() {
        colorDepthResultPanel.reset();
    }

    @Override
    public DomainObjectEditorState<ColorDepthSearch,ColorDepthMatch,Reference> saveState() {
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
    public void restoreState(DomainObjectEditorState<ColorDepthSearch,ColorDepthMatch,Reference> state) {
        if (state==null) {
            log.warn("Cannot restore null state");
            return;
        }
        
        ColorDepthSearchEditorState myState = (ColorDepthSearchEditorState)state;
        log.info("Restoring state: {}", myState);
        if (state.getListViewerState()!=null && state.getListViewerState().getType()!=null) {
            colorDepthResultPanel.getResultPanel().setViewerType(state.getListViewerState().getType());
        }

        // Prepare to restore the selection
        List<Reference> selected = colorDepthResultPanel.getResultPanel().getViewer().getSelectionModel().getSelectedIds();
        selected.clear();
        selected.addAll(state.getSelectedIds());

        // Prepare to restore group selection
        this.selectedMaskRef = myState.getSelectedMask();
        
        // Prepare to restore the page
        colorDepthResultPanel.getResultPanel().setCurrPage(state.getPage());
        colorDepthResultPanel.getResultPanel().getViewer().restoreState(state.getListViewerState());
        
        if (state.getDomainObjectNode()==null) {
            loadDomainObject(state.getDomainObject(), false, null);
        }
        else {
            loadDomainObjectNode(state.getDomainObjectNode(), false, null);
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
                    if (StringUtilsExtra.areEqual(domainObject.getId(), search.getId())) {
                        log.info("Search invalidated, reloading...");
                        reload();
                        break;
                    }
                }
            }
        }  
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

    @Subscribe
    public void domainObjectRemoved(DomainObjectRemoveEvent event) {
        if (search==null) return;
        if (StringUtilsExtra.areEqual(event.getDomainObject().getId(), search.getId())) {
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
            FrameworkAccess.handleException(e);
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
