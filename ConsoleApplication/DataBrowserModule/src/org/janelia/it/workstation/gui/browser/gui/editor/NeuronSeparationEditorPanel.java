package org.janelia.it.workstation.gui.browser.gui.editor;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JPanel;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.shared.utils.ReflectionUtils;
import org.janelia.it.workstation.gui.browser.actions.ExportResultsAction;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.listview.PaginatedResultsPanel;
import org.janelia.it.workstation.gui.browser.gui.listview.table.DomainObjectTableViewer;
import org.janelia.it.workstation.gui.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.browser.model.SampleResult;
import org.janelia.it.workstation.gui.browser.model.search.ResultPage;
import org.janelia.it.workstation.gui.browser.model.search.SearchResults;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.common.eventbus.Subscribe;

/**
 * An editor which can display the most recent neuron separation on a given sample result. 
 * 
 * TODO: allow the user to toggle between different separation runs on the same result
 * TODO: allow users to hide certain neurons (persisted)
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NeuronSeparationEditorPanel extends JPanel implements SampleResultEditor, SearchProvider {

    private final static Logger log = LoggerFactory.getLogger(NeuronSeparationEditorPanel.class);
    
    private final PaginatedResultsPanel resultsPanel;
    
    private final DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();

    private SearchResults searchResults;
    
    private SampleResult sampleResult;
    private List<DomainObject> domainObjects;
    private List<Annotation> annotations;
    
    public NeuronSeparationEditorPanel() {
        
        setLayout(new BorderLayout());
        
        resultsPanel = new PaginatedResultsPanel(selectionModel, this) {
            @Override
            protected ResultPage getPage(SearchResults searchResults, int page) throws Exception {
                return searchResults.getPage(page);
            }
        };
        resultsPanel.addMouseListener(new MouseForwarder(this, "PaginatedResultsPanel->NeuronSeparationEditorPanel"));
        add(resultsPanel, BorderLayout.CENTER);
    }
    
    @Override
    public void loadSampleResult(final SampleResult sampleResult, final boolean isUserDriven) {

        log.debug("loadDomainObject(ObjectSet:{})",sampleResult.getName());
        
        this.sampleResult = sampleResult;
        final NeuronSeparation separation = sampleResult.getResult().getLatestSeparationResult();

        // TODO: Should Samples be parents of neurons?
//        selectionModel.setParentObject(sampleResult.getSample());
        
        resultsPanel.showLoadingIndicator();
        
        SimpleWorker childLoadingWorker = new SimpleWorker() {

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
                        return null;
                    }
                });
            }

            @Override
            protected void hadError(Throwable error) {
                showNothing();
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        childLoadingWorker.execute();
    }

    private void sort(final String sortCriteria, final Callable<Void> success) {

        SimpleWorker worker = new SimpleWorker() {
        
            @Override
            protected void doStuff() throws Exception {
                final String sortField = (sortCriteria.startsWith("-")||sortCriteria.startsWith("+")) ? sortCriteria.substring(1) : sortCriteria;
                final boolean ascending = !sortCriteria.startsWith("-");
                Collections.sort(domainObjects, new Comparator<DomainObject>() {
                    @Override
                    @SuppressWarnings({ "rawtypes", "unchecked" })
                    public int compare(DomainObject o1, DomainObject o2) {
                        try {
                            // TODO: speed could be improved by moving the reflection calls outside of the sort
                            Comparable v1 = (Comparable)ReflectionUtils.get(o1, sortField);
                            Comparable v2 = (Comparable)ReflectionUtils.get(o2, sortField);
                            Ordering ordering = Ordering.natural().nullsLast();
                            if (!ascending) {
                                ordering = ordering.reverse();
                            }
                            return ComparisonChain.start().compare(v1, v2, ordering).result();
                        }
                        catch (Exception e) {
                            log.error("Problem encountered when sorting DomainObjects",e);
                            return 0;
                        }
                    }
                }); 
            }

            @Override
            protected void hadSuccess() {
                ConcurrentUtils.invokeAndHandleExceptions(success);
            }

            @Override
            protected void hadError(Throwable error) {
                resultsPanel.showNothing();
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        
        worker.execute();
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
        resultsPanel.showNothing();
    }
    
    public void showResults(boolean isUserDriven) {
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
        if (event.isTotalInvalidation()) {
            log.info("total invalidation, reloading...");
            search();
        }
        else {
            Sample sample = sampleResult.getSample();
            for (DomainObject domainObject : event.getDomainObjects()) {
                if (domainObject.getId().equals(sample.getId())) {
                    log.info("sample invalidated, reloading...");
                    Sample updatedSample = DomainMgr.getDomainMgr().getModel().getDomainObject(Sample.class, sample.getId());
                    PipelineResult result = updatedSample.findResultById(sampleResult.getResult().getId());
                    if (result==null) {
                        log.info("Sample no longer has result with id: "+sampleResult.getResult().getId());
                        showNothing();
                        return;
                    }
                    loadSampleResult(new SampleResult(updatedSample, result), false);
                    break;
                }
                else if (domainObject.getClass().equals(NeuronFragment.class)) {
                    log.info("some objects of class NeuronFragment were invalidated, reloading...");
                    loadSampleResult(sampleResult, false);
                    // TODO: reselect the selected neurons
                }
            }
        }
    }
}
