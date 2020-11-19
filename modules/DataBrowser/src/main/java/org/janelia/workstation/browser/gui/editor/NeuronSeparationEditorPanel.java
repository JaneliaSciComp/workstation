package org.janelia.workstation.browser.gui.editor;

import com.google.common.eventbus.Subscribe;

import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.cdmip.ColorDepthSearch;
import org.janelia.model.domain.interfaces.HasIdentifier;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.sample.NeuronFragment;
import org.janelia.model.domain.sample.NeuronSeparation;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.actions.ExportResultsAction;
import org.janelia.workstation.browser.actions.OpenInNeuronAnnotatorActionListener;
import org.janelia.workstation.browser.actions.context.OpenInVvdNAPluginActionListener;
import org.janelia.workstation.browser.gui.listview.PaginatedDomainResultsPanel;
import org.janelia.workstation.browser.gui.listview.table.DomainObjectTableViewer;
import org.janelia.workstation.browser.selection.PipelineResultSelectionEvent;
import org.janelia.workstation.common.gui.editor.SampleResultEditor;
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
import org.janelia.workstation.core.events.model.DomainObjectInvalidationEvent;
import org.janelia.workstation.core.events.selection.ChildPickingSupport;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.janelia.workstation.core.events.selection.DomainObjectEditSelectionModel;
import org.janelia.workstation.core.events.selection.DomainObjectSelectionModel;
import org.janelia.workstation.core.events.selection.ViewerContextChangeEvent;
import org.janelia.workstation.core.model.DomainModelViewUtils;
import org.janelia.workstation.core.model.ImageModel;
import org.janelia.workstation.core.model.search.DomainObjectSearchResults;
import org.janelia.workstation.core.model.search.ResultPage;
import org.janelia.workstation.core.model.search.SearchResults;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;


/**
 * An editor which can display the neuron separations for a given sample result. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NeuronSeparationEditorPanel 
        extends JPanel 
        implements SampleResultEditor, SearchProvider, PreferenceSupport, ChildPickingSupport<DomainObject, Reference> {

    // Constants
    private final static Logger log = LoggerFactory.getLogger(NeuronSeparationEditorPanel.class);
    private final static String PREFERENCE_KEY = "NeuronSeparationEditor";
    private final static String DEFAULT_SORT_CRITERIA = "number";
    
    // Utilities
    private final Debouncer debouncer = new Debouncer();
    private final Debouncer reloadDebouncer = new Debouncer();
    
    // UI Elements
    private final ConfigPanel configPanel;
    private final DropDownButton resultButton;
    private final JButton openInNAButton;
    private final JButton openInVVDNAButton;
    private final JButton fragmentSortButton;
    private final JButton editModeButton;
    private final JButton editOkButton;
    private final JButton editCancelButton;
    private final PaginatedDomainResultsPanel resultsPanel;
    
    // State
    private final DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();
    private final DomainObjectEditSelectionModel editSelectionModel = new DomainObjectEditSelectionModel();
    private NeuronSeparation separation;
    private boolean editMode;
    
    // Results
    private List<NeuronFragment> neuronFragments;
    private List<Annotation> annotations;
    private DomainObjectSearchResults searchResults;
    private String sortCriteria = DEFAULT_SORT_CRITERIA;

    private Set<Long> hiddenFragments;

    public NeuronSeparationEditorPanel() {
        
    	setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());
        setFocusable(true);
        
        resultButton = new DropDownButton();
        
        editModeButton = new JButton("Show/Hide Neurons");
        editModeButton.setIcon(Icons.getIcon("page_white_edit.png"));
        editModeButton.setFocusable(false);
        editModeButton.setToolTipText("Edit the visibility of the neuron fragments in the current separation");
        editModeButton.addActionListener(e -> enterEditMode());
        editOkButton = new JButton();
        editOkButton.setIcon(Icons.getIcon("button_ok_16x16.png"));
        editOkButton.setFocusable(false);
        editOkButton.setVisible(false);
        editOkButton.setToolTipText("Save your visibility preferences");
        editOkButton.addActionListener(e -> saveVisibilities());
        editCancelButton = new JButton();
        editCancelButton.setIcon(Icons.getIcon("cancel.png"));
        editCancelButton.setVisible(false);
        editCancelButton.setToolTipText("Save your visibility preferences");
        editCancelButton.addActionListener(e -> cancelEditMode());
        
        fragmentSortButton = new JButton();
        fragmentSortButton.setIcon(Icons.getIcon("sort_descending.png"));
        fragmentSortButton.setFocusable(false);
        fragmentSortButton.setToolTipText("Sort the neuron fragments by voxel weight (largest->first)");
        fragmentSortButton.addActionListener(e -> sortByFragmentWeight());

        openInNAButton = new JButton();
        openInNAButton.setIcon(Icons.getIcon("v3d_16x16x32.png"));
        openInNAButton.setFocusable(false);
        openInNAButton.setToolTipText("Open the current separation in Neuron Annotator");
        openInNAButton.addActionListener(e -> new OpenInNeuronAnnotatorActionListener(separation).actionPerformed(e));

        openInVVDNAButton = new JButton();
        openInVVDNAButton.setIcon(Icons.getIcon("vvd16.svg.png"));
        openInVVDNAButton.setFocusable(false);
        openInVVDNAButton.setToolTipText("Open the current separation in VVD");
        openInVVDNAButton.addActionListener(e -> new OpenInVvdNAPluginActionListener(separation).actionPerformed(e));

        configPanel = new ConfigPanel(true) {
            @Override
            protected void titleClicked(MouseEvent e) {
                Events.getInstance().postOnEventBus(new PipelineResultSelectionEvent(this, separation, true));
            }
        };
        configPanel.addTitleComponent(fragmentSortButton, true, true);
        configPanel.addTitleComponent(openInNAButton, true, true);
        configPanel.addTitleComponent(openInVVDNAButton, true, true);
        configPanel.addConfigComponent(resultButton);
        configPanel.addConfigComponent(editModeButton);
        configPanel.addConfigComponent(editOkButton);
        configPanel.addConfigComponent(editCancelButton);

        resultsPanel = new PaginatedDomainResultsPanel(selectionModel, editSelectionModel, this, this) {
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
        resultsPanel.addMouseListener(new MouseForwarder(this, "PaginatedResultsPanel->NeuronSeparationEditorPanel"));
    }

    private void enterEditMode() {
        this.editMode = true;
        editModeButton.setVisible(false);
        editOkButton.setVisible(true);
        editCancelButton.setVisible(true);
        resultsPanel.getViewer().toggleEditMode(true);
        search();
    }

    private void cancelEditMode() {
        this.editMode = false;
        editModeButton.setVisible(true);
        editOkButton.setVisible(false);
        editCancelButton.setVisible(false);
        resultsPanel.getViewer().toggleEditMode(false);
        search();
    }

    private void saveVisibilities() {
        saveHiddenFragments();
        cancelEditMode();
    }

    private void sortByFragmentWeight() {
        this.sortCriteria = "-voxelWeight";
    }
    
    private void populateResultPopupMenu(DropDownButton button, PipelineResult pipelineResult) {
        button.removeAll();
        if (pipelineResult.hasResults()) {
            for(final PipelineResult result : pipelineResult.getResults()) {
                if (result instanceof NeuronSeparation) {
                    final NeuronSeparation separation = (NeuronSeparation)result;
                    JMenuItem viewItem = new JMenuItem(getLabel(separation));
                    viewItem.addActionListener(actionEvent -> setResult(separation, true, null));
                    button.addMenuItem(viewItem);
                }
            }
        }
    }
    
    private String getLabel(NeuronSeparation separation) {
        if (separation==null) return "";
        return DomainModelViewUtils.getDateString(separation.getCreationDate());
    }
    
    @Override
    public void loadSampleResult(final PipelineResult result, final boolean isUserDriven, final Callable<Void> success) {

        if (result==null) return;

        if (!debouncer.queue(success)) {
            log.info("Skipping load, since there is one already in progress");
            return;
        }
        
        log.info("loadSampleResult(PipelineResult:{})",result.getName());
        final StopWatch w = new StopWatch();
                
        PipelineResult parentResult;
        if (result instanceof NeuronSeparation) {
            separation = (NeuronSeparation)result;
            parentResult = separation.getParentResult();
        }
        else {
            parentResult = result;
            separation = result.getLatestSeparationResult();
        }
        
        Sample sample = parentResult.getParentRun().getParent().getParent();
        selectionModel.setParentObject(sample);
        
        populateResultPopupMenu(resultButton, result);
        
        if (separation==null) {
            showNothing();
            debouncer.success();
            ActivityLogHelper.logElapsed("NeuronSeparationEditorPanel.loadSampleResult", w);
        }
        else {
            configPanel.setTitle(sample.getName());
            setResult(separation, isUserDriven, w);
        }
    }
    
    private void setResult(final NeuronSeparation separation, final boolean isUserDriven, final StopWatch w) {
        this.separation = separation;
        this.resultButton.setText(getLabel(separation));
        resultsPanel.showLoadingIndicator();
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                
                if (separation==null) {
                    neuronFragments = new ArrayList<>();
                    annotations = new ArrayList<>();
                }
                else {
                    DomainModel model = DomainMgr.getDomainMgr().getModel();
                    neuronFragments = model.getDomainObjectsAs(NeuronFragment.class, separation.getFragmentsReference());
                    annotations = model.getAnnotations(DomainUtils.getReferences(neuronFragments));
                }

                sortCriteria = loadSortCriteria();
                hiddenFragments = loadHiddenFragments();
                prepareResults();
            }

            @Override
            protected void hadSuccess() {
                showResults(isUserDriven, new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        debouncer.success();
                        if (w!=null) ActivityLogHelper.logElapsed("NeuronSeparationEditorPanel.loadSampleResult", separation, w);
                        return null;
                    }
                    
                });
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

    private void prepareResults() throws Exception {
        
        List<NeuronFragment> filteredFragments = new ArrayList<>();
        
        if (editMode) {
            filteredFragments.addAll(neuronFragments);
            log.info("Showing {} neurons in edit mode", neuronFragments.size());
        }
        else {
            log.info("Removing {} hidden neurons from view", hiddenFragments.size());
            for (NeuronFragment neuronFragment : neuronFragments) {
                if (!hiddenFragments.contains(neuronFragment.getId())) {
                    filteredFragments.add(neuronFragment);
                }
            }
            log.info("Showing "+filteredFragments.size()+" neurons");
        }
        
        DomainUtils.sortDomainObjects(filteredFragments, sortCriteria);
        this.searchResults = new DomainObjectSearchResults(filteredFragments, annotations);
    }
    
    public void showResults(boolean isUserDriven, Callable<Void> success) {

        // Set visibility of the sort button
        fragmentSortButton.setVisible(false);
        for (NeuronFragment neuronFragment : neuronFragments) {
            if (neuronFragment.getVoxelWeight()!=null) {
                fragmentSortButton.setVisible(true);
                break;
            }
        }

        // Check hidden items
        if (editMode) {
            List<DomainObject> visibleFragments = neuronFragments.stream()
                    .filter(n -> !hiddenFragments.contains(n.getId()))
                    .collect(Collectors.toList());
            resultsPanel.getViewer().selectEditObjects(visibleFragments, true);
        }
        
        add(configPanel, BorderLayout.NORTH);
        add(resultsPanel, BorderLayout.CENTER);
        updateUI();
        resultsPanel.showSearchResults(searchResults, isUserDriven, success);
    }

    public void showNothing() {
        removeAll();
        updateUI();
    }
    
    @Override
    public String getSortField() {
        return sortCriteria;
    }

    @Override
    public void setSortField(final String sortCriteria) {
        this.sortCriteria = sortCriteria;
        saveSortCriteria();
    }

    @Override
    public void search() {

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                sortCriteria = loadSortCriteria();
                hiddenFragments = loadHiddenFragments();
                prepareResults();
            }

            @Override
            protected void hadSuccess() {
                showResults(true, null);
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
        if (resultsPanel.getViewer() instanceof DomainObjectTableViewer) {
            viewer = (DomainObjectTableViewer)resultsPanel.getViewer();
        }
        ExportResultsAction<DomainObject, Reference> action = new ExportResultsAction<>(searchResults, viewer);
        action.actionPerformed(null);
    }

    private void saveSortCriteria() {
        if (StringUtils.isEmpty(sortCriteria)) return;
        try {
            FrameworkAccess.setRemotePreferenceValue(DomainConstants.PREFERENCE_CATEGORY_SORT_CRITERIA, PREFERENCE_KEY, sortCriteria);
        }
        catch (Exception e) {
            log.error("Could not save sort criteria",e);
        }
    }
    
    private String loadSortCriteria() {
        try {
            return FrameworkAccess.getRemotePreferenceValue(
                    DomainConstants.PREFERENCE_CATEGORY_SORT_CRITERIA, 
                    PREFERENCE_KEY, DEFAULT_SORT_CRITERIA);
        }
        catch (Exception e) {
            log.error("Could not load sort criteria",e);
        }
        return DEFAULT_SORT_CRITERIA;
    }

    private Set<Long> loadHiddenFragments() {

        Set<Long> hiddenFragments = new HashSet<>();
        if (separation.getId()==null) return hiddenFragments;

        try {
            @SuppressWarnings("unchecked")
            List<Object> neuronSepVisibility = (List<Object>) FrameworkAccess.getRemotePreferenceValue(
                    DomainConstants.PREFERENCE_CATEGORY_NEURON_SEPARATION_VISIBILITY,
                    Long.toString(separation.getId()), null);

            // TODO: set up global preference for visibility, allow users to select other user's preferences
            if (neuronSepVisibility!=null) {
                log.info("Retrieved {} hidden fragment ids", neuronSepVisibility.size());
    
                for (Object object : neuronSepVisibility) {
                    // Just some robustness to how the pref values are transmitted on the wire.  
                    if (object instanceof String) {
                        hiddenFragments.add(new Long((String)object));
                    }
                    else if (object instanceof Long) {
                        hiddenFragments.add((Long)object);
                    }
                    else {
                        throw new IllegalStateException("Unsupported visibility value type: "+object.getClass().getName());
                    }
                }
            }
            
            log.info("Got {} hidden fragments", hiddenFragments.size());
        }
        catch (Exception e) {
            FrameworkAccess.handleException("Could not load hidden fragments", e);
        }
        
        return hiddenFragments;
    }
    
    private void saveHiddenFragments() {
        try {
            List<Long> visibleFragments = DomainUtils.getIdsFromReferences(editSelectionModel.getSelectedIds());
            log.info("User selected {} visible fragments", visibleFragments.size());
            
            hiddenFragments.clear();
            for (NeuronFragment neuronFragment : neuronFragments) {
                if (!visibleFragments.contains(neuronFragment.getId())) {
                    hiddenFragments.add(neuronFragment.getId());
                }
            }
            log.info("Hiding {}/{} fragments", hiddenFragments.size(), neuronFragments.size());
            
            FrameworkAccess.setRemotePreferenceValue(
                    DomainConstants.PREFERENCE_CATEGORY_NEURON_SEPARATION_VISIBILITY, 
                    Long.toString(separation.getId()), hiddenFragments);
            log.info("Saved {} hidden fragments", hiddenFragments.size());
        }
        catch (Exception e) {
            FrameworkAccess.handleException("Could not save hidden fragments", e);
        }
    }
    
    @Override
    public String getName() {
        return "Neuron Separation Editor";
    }

    @Override
    public DomainObjectSelectionModel getSelectionModel() {
        return selectionModel;
    }

    @Override
    public DomainObjectEditSelectionModel getEditSelectionModel() {
        return editSelectionModel;
    }
    
    @Override
    public Long getCurrentContextId() {
        Object parentObject = getSelectionModel().getParentObject();
        if (parentObject instanceof HasIdentifier) {
            return ((HasIdentifier)parentObject).getId();
        }
        throw new IllegalStateException("Parent object has no identifier: "+getSelectionModel().getParentObject());
    }
    
    @Override
    public Object getEventBusListener() {
        return resultsPanel;
    }

    @Override
    public void activate() {
        resultsPanel.activate();
    }

    @Override
    public void deactivate() {
        resultsPanel.deactivate();
    }
    
    private void refresh() {
        loadSampleResult(separation, false, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // TODO: reselect the selected neurons
                return null;
            }
        });
    }

    @Override
    public ViewerContext<DomainObject, Reference> getViewerContext() {
        return new ViewerContext<DomainObject, Reference>() {
            @Override
            public ChildSelectionModel<DomainObject, Reference> getSelectionModel() {
                return selectionModel;
            }

            @Override
            public ChildSelectionModel<DomainObject, Reference> getEditSelectionModel() {
                return resultsPanel.isEditMode() ? resultsPanel.getViewer().getEditSelectionModel() : null;
            }

            @Override
            public ImageModel<DomainObject, Reference> getImageModel() {
                return resultsPanel.getImageModel();
            }
        };
    }

    @Subscribe
    public void domainObjectInvalidated(DomainObjectInvalidationEvent event) {
        try {
            if (separation==null) return;

            if (event.isTotalInvalidation()) {
                log.info("Total invalidation, reloading...");
                search();
            }
            else {
                Sample sample = separation.getParentRun().getParent().getParent();
                for (DomainObject domainObject : event.getDomainObjects()) {
                    if (domainObject.getId().equals(sample.getId())) {
                        log.info("Sample invalidated, reloading...");
                        reload(sample);
                        break;
                    }
                    else if (domainObject.getClass().equals(NeuronFragment.class)) {
                        log.info("Some objects of class NeuronFragment were invalidated, reloading...");
                        refresh();
                    }
                }
            }
        }  catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

    public void reload(Sample sample) {

        if (!reloadDebouncer.queue()) {
            log.info("Skipping reload, since there is one already in progress");
            return;
        }

        SimpleWorker worker = new SimpleWorker() {

            List<NeuronSeparation> separations;

            @Override
            protected void doStuff() throws Exception {
                Sample updatedSample = DomainMgr.getDomainMgr().getModel().getDomainObject(Sample.class, sample.getId());
                separations = updatedSample.getResultsById(NeuronSeparation.class, separation.getId());
            }

            @Override
            protected void hadSuccess() {
                if (separations.isEmpty()) {
                    log.info("Sample no longer has result with id: "+separation.getId());
                    showNothing();
                    return;
                }
                NeuronSeparation separation = separations.get(separations.size()-1);
                loadSampleResult(separation.getParentResult(), false, () -> {
                    // TODO: reselect the selected neurons
                    return null;
                });
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
}
