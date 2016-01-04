package org.janelia.it.workstation.gui.browser.gui.editor;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JPanel;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.shared.utils.ReflectionUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.listview.PaginatedResultsPanel;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.browser.model.SampleResult;
import org.janelia.it.workstation.gui.browser.model.search.ResultPage;
import org.janelia.it.workstation.gui.browser.model.search.SearchResults;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

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
        add(resultsPanel, BorderLayout.CENTER);
    }
    
    @Override
    public void loadSampleResult(final SampleResult sampleResult, final boolean isUserDriven) {

        log.debug("loadDomainObject(ObjectSet:{})",sampleResult.getName());
        
        final NeuronSeparation separation = sampleResult.getResult().getLatestSeparationResult();

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
                showResults(isUserDriven);
            }

            @Override
            protected void hadError(Throwable error) {
                resultsPanel.showNothing();
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        childLoadingWorker.execute();
    }

    @Override
    public void setSortField(final String sortCriteria) {

        resultsPanel.showLoadingIndicator();

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
                showResults(true);
            }

            @Override
            protected void hadError(Throwable error) {
                resultsPanel.showNothing();
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        
        worker.execute();
        
    }
    
    public void showResults(boolean isUserDriven) {
        SearchResults searchResults = SearchResults.paginate(domainObjects, annotations);
        resultsPanel.showSearchResults(searchResults, isUserDriven);
    }

    @Override
    public void search() {
        // Nothing needs to be done here, because results were updated by setSortField()
    }

    @Override
    public void userRequestedSelectAll() {
        resultsPanel.setSelectAllVisible(true);
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
}
