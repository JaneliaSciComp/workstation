package org.janelia.it.workstation.gui.browser.gui.editor;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.workstation.gui.browser.api.DomainDAO;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.components.DomainListViewTopComponent;
import org.janelia.it.workstation.gui.browser.gui.listview.PaginatedResultsPanel;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
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
public class ObjectSetEditorPanel extends JPanel implements DomainObjectEditor<ObjectSet> {

    private final static Logger log = LoggerFactory.getLogger(DomainListViewTopComponent.class);
    
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
        
        log.trace("loadDomainObject "+objectSet);
        
        SimpleWorker childLoadingWorker = new SimpleWorker() {

            private List<DomainObject> domainObjects;
            private List<Annotation> annotations;

            @Override
            protected void doStuff() throws Exception {
                log.debug("Getting children...");

                DomainDAO dao = DomainMgr.getDomainMgr().getDao();
                domainObjects = dao.getDomainObjects(SessionMgr.getSubjectKey(), objectSet);
                List<Long> ids = new ArrayList<>();
                for(DomainObject domainObject : domainObjects) {
                    ids.add(domainObject.getId());
                }
                annotations = dao.getAnnotations(SessionMgr.getSubjectKey(), ids);
                log.debug("  Showing "+domainObjects.size()+" items");
            }

            @Override
            protected void hadSuccess() {
                if (domainObjects==null || domainObjects.isEmpty()) {
                    resultsPanel.showNothing();
                    return;
                }
                SearchResults searchResults = SearchResults.paginate(domainObjects, annotations);
                resultsPanel.showSearchResults(searchResults);
            }

            @Override
            protected void hadError(Throwable error) {
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
    public Object getEventBusListener() {
        return resultsPanel;
    }
}
