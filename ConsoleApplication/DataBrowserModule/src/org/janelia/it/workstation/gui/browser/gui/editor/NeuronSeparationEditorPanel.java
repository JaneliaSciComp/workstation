package org.janelia.it.workstation.gui.browser.gui.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;

import org.apache.commons.httpclient.HttpClient;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.FileTreeLoaderPipelineTask;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronWeightsTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.img_3d_loader.V3dMaskFileLoader;
import org.janelia.it.jacs.shared.img_3d_loader.V3dSignalFileLoader;
import org.janelia.it.jacs.shared.utils.ReflectionUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.actions.ExportResultsAction;
import org.janelia.it.workstation.gui.browser.actions.NamedAction;
import org.janelia.it.workstation.gui.browser.actions.OpenInNeuronAnnotatorAction;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.listview.PaginatedResultsPanel;
import org.janelia.it.workstation.gui.browser.gui.listview.table.DomainObjectTableViewer;
import org.janelia.it.workstation.gui.browser.gui.support.Debouncer;
import org.janelia.it.workstation.gui.browser.gui.support.DropDownButton;
import org.janelia.it.workstation.gui.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.browser.model.DomainModelViewUtils;
import org.janelia.it.workstation.gui.browser.model.search.ResultPage;
import org.janelia.it.workstation.gui.browser.model.search.SearchResults;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.util.filecache.WebDavClient;
import org.janelia.it.workstation.shared.util.filecache.WebDavUploader;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.common.eventbus.Subscribe;


/**
 * An editor which can display the neuron separations for a given sample result. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NeuronSeparationEditorPanel extends JPanel implements SampleResultEditor, SearchProvider {

    private final static Logger log = LoggerFactory.getLogger(NeuronSeparationEditorPanel.class);
    
    // Utilities
    private final Debouncer debouncer = new Debouncer();
    
    // UI Elements
    private final ConfigPanel configPanel;
    private final DropDownButton resultButton;
    private final JButton editModeButton;
    private final JButton openInNAButton;
    private final JButton fragmentSortButton;
    private final JButton editOkButton;
    private final JButton editCancelButton;
    private final PaginatedResultsPanel resultsPanel;
    
    // State
    private final DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();
    private final DomainObjectSelectionModel editSelectionModel = new DomainObjectSelectionModel();
    private boolean hideFragments = false;
    private boolean editModeEnabled = false;
    private NeuronSeparation separation;
    
    // Results
    private List<DomainObject> domainObjects;
    private List<Annotation> annotations;
    private SearchResults searchResults;
   
    public NeuronSeparationEditorPanel() {
        
    	setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());
        setFocusable(true);
        
        resultButton = new DropDownButton();
        
        editModeButton = new JButton();
        editModeButton.setIcon(Icons.getIcon("page_white_edit.png"));
        editModeButton.setFocusable(false);
        editModeButton.setToolTipText("Edit the visibility of the fragments in the current separation");
        editModeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enterEditMode();
            }
        });
        editOkButton = new JButton();
        editOkButton.setIcon(Icons.getIcon("button_ok_16x16.png"));
        editOkButton.setFocusable(false);
        editOkButton.setVisible(false);
        editOkButton.setToolTipText("Save your visibility preferences");
        editOkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveVisibilities();
            }
        });
        editCancelButton = new JButton();
        editCancelButton.setIcon(Icons.getIcon("cancel.png"));
        editCancelButton.setVisible(false);
        editCancelButton.setToolTipText("Save your visibility preferences");
        editCancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelEditMode();
            }
        });
        
        fragmentSortButton = new JButton();
        fragmentSortButton.setIcon(Icons.getIcon("sort_descending.png"));
        fragmentSortButton.setFocusable(false);
        fragmentSortButton.setToolTipText("Sort the neuron fragments by voxel weight (largest->first)");
        fragmentSortButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sortByFragmentWeight();
            }
        });

        openInNAButton = new JButton();
        openInNAButton.setIcon(Icons.getIcon("v3d_16x16x32.png"));
        openInNAButton.setFocusable(false);
        openInNAButton.setToolTipText("Open the current separation in Neuron Annotator");
        openInNAButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openInNA();
            }
        });
        
        configPanel = new ConfigPanel(true);
        configPanel.addTitleComponent(fragmentSortButton, true, true);
        configPanel.addTitleComponent(openInNAButton, true, true);
        configPanel.addConfigComponent(resultButton);
        configPanel.addConfigComponent(editModeButton);
        configPanel.addConfigComponent(editOkButton);
        configPanel.addConfigComponent(editCancelButton);

        JCheckBox enableVisibilityCheckBox = new JCheckBox(new AbstractAction("Hide/Unhide") {
            public void actionPerformed(ActionEvent e) {
                JCheckBox cb = (JCheckBox) e.getSource();
                hideFragments = cb.isSelected();
                // find the list of visibilities for this separation Id
                if (hideFragments) {
                    Preference neuronSepVisibility = DomainMgr.getDomainMgr().getPreference(DomainConstants.PREFERENCE_CATEGORY_NEURON_SEPARATION_VISIBILITY,
                            Long.toString(separation.getId()));
                    if (neuronSepVisibility!=null) {
                        Set<Long> fragmentVis = new HashSet((List)neuronSepVisibility.getValue());
                        for (int i=domainObjects.size()-1; i>=0; i--) {
                            NeuronFragment neuronFragment = (NeuronFragment) domainObjects.get(i);

                            // remove items that are hidden
                            if (fragmentVis.contains(neuronFragment.getId())) {
                                domainObjects.remove(i);
                            }

                        }
                    }
                } else {
                    domainObjects = DomainMgr.getDomainMgr().getModel().getDomainObjects(separation.getFragmentsReference());
                }
                showResults(true);

            }
        });
        configPanel.addConfigComponent(enableVisibilityCheckBox);
        openInNAButton.setIcon(Icons.getIcon("v3d_16x16x32.png"));
        openInNAButton.setFocusable(false);
        openInNAButton.setToolTipText("Open the current separation in Neuron Annotator");
        openInNAButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openInNA();
            }
        });

        resultsPanel = new PaginatedResultsPanel(selectionModel, this) {
            @Override
            protected ResultPage getPage(SearchResults searchResults, int page) throws Exception {
                return searchResults.getPage(page);
            }
        };
        resultsPanel.addMouseListener(new MouseForwarder(this, "PaginatedResultsPanel->NeuronSeparationEditorPanel"));
        resultsPanel.getViewer().setEditSelectionModel(editSelectionModel);
    }
    
    private void enterEditMode() {
        List<DomainObject> neuronFrags = DomainMgr.getDomainMgr().getModel().getDomainObjects(separation.getFragmentsReference());
        // get the visibility preference from the domainmgr
        Preference neuronSepVisibility = DomainMgr.getDomainMgr().getPreference(DomainConstants.PREFERENCE_CATEGORY_NEURON_SEPARATION_VISIBILITY,
                Long.toString(separation.getId()));
        List<DomainObject> visibleNeuronFrags = new ArrayList<DomainObject>();
        if (neuronSepVisibility!=null) {
            Set<Long> visibleFragSet = new HashSet((List)neuronSepVisibility.getValue());
            for (int i=0; i<neuronFrags.size(); i++) {
                if (visibleFragSet.contains(neuronFrags.get(i).getId())) {
                    visibleNeuronFrags.add(neuronFrags.get(i));
                }
            }
            resultsPanel.getViewer().selectEditObjects(visibleNeuronFrags, true);
        }

        // show checkboxes for all items
        editModeButton.setVisible(false);
        editOkButton.setVisible(true);
        editCancelButton.setVisible(true);
        editModeEnabled = true;
        resultsPanel.getViewer().toggleEditMode(true);

    }


    private void cancelEditMode() {
        // show checkboxes for all items
        editModeButton.setVisible(true);
        editOkButton.setVisible(false);
        editCancelButton.setVisible(false);
        editModeEnabled = false;
        resultsPanel.getViewer().toggleEditMode(false);
    }

    private void saveVisibilities() {
        try {
            Preference neuronSepVisibility = DomainMgr.getDomainMgr().getPreference(DomainConstants.PREFERENCE_CATEGORY_NEURON_SEPARATION_VISIBILITY,
                    Long.toString(separation.getId()));
            if (neuronSepVisibility == null) {
                neuronSepVisibility = new Preference();
                neuronSepVisibility.setCategory(DomainConstants.PREFERENCE_CATEGORY_NEURON_SEPARATION_VISIBILITY);
                neuronSepVisibility.setKey(Long.toString(separation.getId()));
            }
            List<Long> visibilities = new ArrayList<>();
            List<Reference> visibleFragments = editSelectionModel.getSelectedIds();
            for (int i = 0; i < visibleFragments.size(); i++) {
                visibilities.add(visibleFragments.get(i).getTargetId());
            }
            neuronSepVisibility.setValue(visibilities);
            DomainMgr.getDomainMgr().savePreference(neuronSepVisibility);
        } catch (Exception e) {
            log.error("Problem encountered saving preferences", e);
        }
        cancelEditMode();
    }


    private void openInNA() {
        NamedAction action = new OpenInNeuronAnnotatorAction(separation);
        action.doAction();
    }

    private void sortByFragmentWeight() {
        setSortField("-voxelWeight");
    }
    
    private JPopupMenu populateResultPopupMenu(JPopupMenu popupMenu, PipelineResult pipelineResult) {
        popupMenu.removeAll();
        if (pipelineResult.hasResults()) {
            for(final PipelineResult result : pipelineResult.getResults()) {
                if (result instanceof NeuronSeparation) {
                    final NeuronSeparation separation = (NeuronSeparation)result;
                    JMenuItem viewItem = new JMenuItem(getLabel(separation));
                    viewItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent actionEvent) {
                            setResult(separation, true, null);
                        }
                    });
                    popupMenu.add(viewItem);
                }
            }
        }

        return popupMenu;
    }
    
    private String getLabel(NeuronSeparation separation) {
        if (separation==null) return "";
        return DomainModelViewUtils.getDateString(separation.getCreationDate());
    }
    
    @Override
    public void loadSampleResult(final PipelineResult result, final boolean isUserDriven, final Callable<Void> success) {

        if (result==null) return;
        
        if (!debouncer.queue(null)) {
            log.debug("Skipping load, since there is one already in progress");
            return;
        }
        
        log.info("loadSampleResult(PipelineResult:{})",result.getName());
                
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
        
        populateResultPopupMenu(resultButton.getPopupMenu(), result);
        
        if (separation==null) {
            showNothing();
            debouncer.success();
        }
        else {
            String title = sample.getName();// + " - " + DomainModelViewUtils.getLabel(result);
            configPanel.setTitle(title);
            setResult(separation, isUserDriven, success);
        }
        
        updateUI();
    }
    
    private void setResult(final NeuronSeparation separation, final boolean isUserDriven, final Callable<Void> success) {
        this.resultButton.setText(getLabel(separation));
        resultsPanel.showLoadingIndicator();
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                if (separation==null) {
                    domainObjects = new ArrayList<>();
                    annotations = new ArrayList<>();
                }
                else {
                    domainObjects = model.getDomainObjects(separation.getFragmentsReference());
                    // TODO: set up global preference for visibility, allow users to select other user's preferences

                    annotations = model.getAnnotations(DomainUtils.getReferences(domainObjects));                    
                }
                log.info("Showing "+domainObjects.size()+" neurons");
            }

            @Override
            protected void hadSuccess() {
                sort("number", new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        showResults(isUserDriven);
                        ConcurrentUtils.invokeAndHandleExceptions(success);
                        debouncer.success();
                        return null;
                    }
                });
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

    private void sort(final String sortCriteria, final Callable<Void> success) {

        try {
            final String sortField = (sortCriteria.startsWith("-") || sortCriteria.startsWith("+")) ? sortCriteria.substring(1) : sortCriteria;
            final boolean ascending = !sortCriteria.startsWith("-");
            Collections.sort(domainObjects, new Comparator<DomainObject>() {
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
            
            ConcurrentUtils.invokeAndHandleExceptions(success);
        }
        catch (Exception e) {
            resultsPanel.showNothing();
            SessionMgr.getSessionMgr().handleException(e);
        }
    }
    
    @Override
    public void setSortField(final String sortCriteria) {
        sort(sortCriteria, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                showResults(true);
                return null;
            }
        });
    }
    
    public void showNothing() {
        removeAll();
        updateUI();
    }
    
    public void showResults(boolean isUserDriven) {
        add(configPanel, BorderLayout.NORTH);
        add(resultsPanel, BorderLayout.CENTER);
        updateUI();
        this.searchResults = SearchResults.paginate(domainObjects, annotations);
        resultsPanel.showSearchResults(searchResults, isUserDriven);
    }

    @Override
    public void search() {
        // Nothing needs to be done here, because results were updated by setSortField()
    }

    @Override
    public void export() {
        DomainObjectTableViewer viewer = null;
        if (resultsPanel.getViewer() instanceof DomainObjectTableViewer) {
            viewer = (DomainObjectTableViewer)resultsPanel.getViewer();
        }
        ExportResultsAction<DomainObject> action = new ExportResultsAction<>(searchResults, viewer);
        action.doAction();
    }
    
    @Override
    public String getName() {
        return "Neuron Separation Editor";
    }
    
    public DomainObjectSelectionModel getSelectionModel() {
        return selectionModel;
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
    
    @Subscribe
    public void domainObjectInvalidated(DomainObjectInvalidationEvent event) {
        if (separation==null) {
            return; // Nothing to refresh
        } 
        if (event.isTotalInvalidation()) {
            log.info("Total invalidation, reloading...");
            search();
        }
        else {
            Sample sample = separation.getParentRun().getParent().getParent();
            for (DomainObject domainObject : event.getDomainObjects()) {
                if (domainObject.getId().equals(sample.getId())) {
                    log.info("Sample invalidated, reloading...");
                    Sample updatedSample = DomainMgr.getDomainMgr().getModel().getDomainObject(Sample.class, sample.getId());
                    List<NeuronSeparation> separations = updatedSample.getResultsById(NeuronSeparation.class, separation.getId());
                    if (separations.isEmpty()) {
                        log.info("Sample no longer has result with id: "+separation.getId());
                        showNothing();
                        return;
                    }
                    NeuronSeparation separation = separations.get(separations.size()-1);
                    loadSampleResult(separation.getParentResult(), false, new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            // TODO: reselect the selected neurons
                            return null;
                        }
                    });
                    break;
                }
                else if (domainObject.getClass().equals(NeuronFragment.class)) {
                    log.info("Some objects of class NeuronFragment were invalidated, reloading...");
                    loadSampleResult(separation, false, new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            // TODO: reselect the selected neurons
                            return null;
                        }
                    });
                }
            }
        }
    }
}
