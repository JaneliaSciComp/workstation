package org.janelia.it.workstation.gui.browser.gui.editor;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JPanel;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.listview.PaginatedResultsPanel;
import org.janelia.it.workstation.gui.browser.model.search.ResultPage;
import org.janelia.it.workstation.gui.browser.model.search.SearchResults;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ObjectSetEditorPanel extends JPanel implements DomainObjectSelectionEditor<ObjectSet> {

    private final static Logger log = LoggerFactory.getLogger(ObjectSetEditorPanel.class);
    
    private final PaginatedResultsPanel resultsPanel;
    
    private final DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();
    
    public ObjectSetEditorPanel() {
        
        setLayout(new BorderLayout());
        
        resultsPanel = new PaginatedResultsPanel(selectionModel) {
            @Override
            protected ResultPage getPage(SearchResults searchResults, int page) throws Exception {
                return searchResults.getPage(page);
            }
        };
        add(resultsPanel, BorderLayout.CENTER);
    }
    
    @Override
    public void loadDomainObject(final ObjectSet objectSet) {
        
        log.debug("loadDomainObject(ObjectSet:{})",objectSet.getName());
        resultsPanel.showLoadingIndicator();
        
        SimpleWorker childLoadingWorker = new SimpleWorker() {

            private List<DomainObject> domainObjects;
            private List<Annotation> annotations;

            @Override
            protected void doStuff() throws Exception {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                domainObjects = model.getDomainObjects(objectSet.getCollectionName(), objectSet.getMembers());
                annotations = model.getAnnotations(DomainUtils.getIdList(domainObjects));
                log.info("Showing "+domainObjects.size()+" items");
            }

            @Override
            protected void hadSuccess() {
                SearchResults searchResults = SearchResults.paginate(domainObjects, annotations);
                resultsPanel.showSearchResults(searchResults);
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
}
