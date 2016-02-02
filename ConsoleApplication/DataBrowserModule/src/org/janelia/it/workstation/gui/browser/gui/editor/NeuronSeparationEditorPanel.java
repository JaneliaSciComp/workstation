package org.janelia.it.workstation.gui.browser.gui.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.shared.utils.ReflectionUtils;
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
import org.janelia.it.workstation.gui.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.browser.model.DomainModelViewUtils;
import org.janelia.it.workstation.gui.browser.model.search.ResultPage;
import org.janelia.it.workstation.gui.browser.model.search.SearchResults;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.common.eventbus.Subscribe;

import de.javasoft.swing.SimpleDropDownButton;
import net.miginfocom.swing.MigLayout;

/**
 * An editor which can display the neuron separations for a given sample result. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NeuronSeparationEditorPanel extends JPanel implements SampleResultEditor, SearchProvider {

    private final static Logger log = LoggerFactory.getLogger(NeuronSeparationEditorPanel.class);
    
    private final JPanel separationPanel;
    private final JLabel titleLabel;
    private final JLabel historyLabel;
    private final SimpleDropDownButton resultButton;
    private final JToggleButton editModeButton;
    private final JButton openInNAButton;
    private final PaginatedResultsPanel resultsPanel;
    
    private final DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();

    private SearchResults searchResults;
    
    private NeuronSeparation separation;
    private List<DomainObject> domainObjects;
    private List<Annotation> annotations;
    
    private final Debouncer debouncer = new Debouncer();
    
    public NeuronSeparationEditorPanel() {
        
        setLayout(new BorderLayout());
        
        separationPanel = new JPanel();
        separationPanel.setLayout(new MigLayout(
                "ins 10 5 5 5, fillx", 
                "[grow 0, growprio 0][grow 0, growprio 0][grow 0, growprio 0][grow 0, growprio 0][grow 100, growprio 100]"
        ));
        add(separationPanel);
        
        titleLabel = new JLabel("");
        historyLabel = new JLabel("History:");

        resultButton = new SimpleDropDownButton("Choose version...");
        resultButton.setFocusable(false);
        
        editModeButton = new JToggleButton();
        editModeButton.setIcon(Icons.getIcon("page_white_edit.png"));
        editModeButton.setFocusable(false);
        editModeButton.setToolTipText("Edit the visibility of the fragments in the current separation");
        editModeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enterEditMode();
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
        
        separationPanel.add(titleLabel, "span, wrap");
        separationPanel.add(historyLabel);
        separationPanel.add(resultButton, "gapx 0 5");
        separationPanel.add(editModeButton, "width 40:40:40");
        separationPanel.add(openInNAButton, "width 40:40:40");
        separationPanel.add(Box.createHorizontalGlue());
        
        resultsPanel = new PaginatedResultsPanel(selectionModel, this) {
            @Override
            protected ResultPage getPage(SearchResults searchResults, int page) throws Exception {
                return searchResults.getPage(page);
            }
        };
        resultsPanel.addMouseListener(new MouseForwarder(this, "PaginatedResultsPanel->NeuronSeparationEditorPanel"));
    }
    
    private void enterEditMode() {
        // TODO: implement editing mode
    }

    private void openInNA() {
        NamedAction action = new OpenInNeuronAnnotatorAction(separation);
        action.doAction();
    }
    
    private JPopupMenu getResultPopupMenu(PipelineResult pipelineResult) {

        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);
        
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
        
        JPopupMenu popupMenu = getResultPopupMenu(result);
        historyLabel.setText("History ("+popupMenu.getComponentCount()+"):");
        resultButton.setPopupMenu(popupMenu);
        
        if (separation==null) {
            showNothing();
            debouncer.success();
        }
        else {
            String title = sample.getName() + " - " + DomainModelViewUtils.getLabel(result);
            titleLabel.setText(title);
            setResult(separation, isUserDriven, success);
        }
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
        add(separationPanel, BorderLayout.NORTH);
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
                    PipelineResult result = updatedSample.findResultById(separation.getId());
                    if (result==null) {
                        log.info("Sample no longer has result with id: "+separation.getId());
                        showNothing();
                        return;
                    }
                    loadSampleResult(result, false, new Callable<Void>() {
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
