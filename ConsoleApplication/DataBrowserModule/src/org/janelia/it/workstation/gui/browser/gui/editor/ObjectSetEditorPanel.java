package org.janelia.it.workstation.gui.browser.gui.editor;

import java.awt.BorderLayout;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.shared.utils.ReflectionUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectChangeEvent;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectRemoveEvent;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.listview.PaginatedResultsPanel;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.browser.model.search.ResultPage;
import org.janelia.it.workstation.gui.browser.model.search.SearchResults;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.common.eventbus.Subscribe;

/**
 * Simple editor panel for viewing object sets. In the future it may support drag and drop editing of object sets. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ObjectSetEditorPanel extends JPanel implements DomainObjectSelectionEditor<ObjectSet>, SearchProvider {

    private final static Logger log = LoggerFactory.getLogger(ObjectSetEditorPanel.class);
    
    private final PaginatedResultsPanel resultsPanel;
    
    private final DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();

    private ObjectSet objectSet;
    private List<DomainObject> domainObjects;
    private List<Annotation> annotations;
    private Set<Long> loadedObjectIds = new HashSet<>();
    
    public ObjectSetEditorPanel() {
        
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
    public void loadDomainObject(final ObjectSet objectSet) {
        log.info("loadDomainObject(ObjectSet:{})",objectSet.getName());
        resultsPanel.showLoadingIndicator();
        load(objectSet);
    }

    public void load(final ObjectSet objectSet) {

        log.info("load(ObjectSet:{})",objectSet.getName());
        this.objectSet = objectSet;
        selectionModel.setParentObject(objectSet);
        
        SimpleWorker childLoadingWorker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                domainObjects = model.getDomainObjects(objectSet.getClassName(), objectSet.getMembers());
                annotations = model.getAnnotations(DomainUtils.getReferences(domainObjects));
                loadedObjectIds.add(objectSet.getId());
                loadedObjectIds.addAll(DomainUtils.getIds(domainObjects));
                log.info("Showing "+domainObjects.size()+" items");
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
	
	public void showResults() {
        SearchResults searchResults = SearchResults.paginate(domainObjects, annotations);
        resultsPanel.showSearchResults(searchResults, true);
	}

	public void showNothing() {
        resultsPanel.showNothing();
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
        return "Object Set Editor";
    }
    
    @Override
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
            ObjectSet updatedSet = DomainMgr.getDomainMgr().getModel().getDomainObject(ObjectSet.class, objectSet.getId());
            load(updatedSet);
        }
        else {
            for (DomainObject domainObject : event.getDomainObjects()) {
                if (domainObject.getId().equals(objectSet.getId())) {
                    log.info("objects set invalidated, reloading...");
                    ObjectSet updatedSet = DomainMgr.getDomainMgr().getModel().getDomainObject(ObjectSet.class, objectSet.getId());
                    load(updatedSet);
                    break;
                }
            }
        }
    }
}
