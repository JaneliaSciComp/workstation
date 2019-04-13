package org.janelia.workstation.browser.gui.listview;

import java.util.List;

import org.janelia.workstation.integration.FrameworkImplProvider;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.events.model.DomainObjectAnnotationChangeEvent;
import org.janelia.workstation.core.events.model.DomainObjectChangeEvent;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.janelia.workstation.core.events.selection.DomainObjectSelectionEvent;
import org.janelia.workstation.core.model.search.ResultPage;
import org.janelia.workstation.core.model.search.SearchResults;
import org.janelia.workstation.common.gui.support.PreferenceSupport;
import org.janelia.workstation.common.gui.support.SearchProvider;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;

/**
 * A paginated results panel which displays domain objects, and responds to changes to those domain objects.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class PaginatedDomainResultsPanel extends PaginatedResultsPanel<DomainObject,Reference> {

    private static final Logger log = LoggerFactory.getLogger(PaginatedDomainResultsPanel.class);
    
    private static final List<ListViewerType> viewerTypes = ImmutableList.of(ListViewerType.IconViewer, ListViewerType.TableViewer);
    
    public PaginatedDomainResultsPanel(
            ChildSelectionModel<DomainObject, Reference> selectionModel,
            ChildSelectionModel<DomainObject, Reference> editSelectionModel,
            PreferenceSupport preferenceSupport, 
            SearchProvider searchProvider) {
        super(selectionModel, editSelectionModel, preferenceSupport, searchProvider, viewerTypes);
    }

    @Subscribe
    public void domainObjectSelected(DomainObjectSelectionEvent event) {
        if (event.getSource()!=resultsView) return;
        updateStatusBar();
    }

    @Subscribe
    public void domainObjectChanged(DomainObjectChangeEvent event) {
        if (searchResults==null) return;
        if (searchResults.updateIfFound(event.getDomainObject())) {
            log.info("Updated search results with changed domain object: {}", event.getDomainObject());
            resultsView.refresh(event.getDomainObject());
        }
    }
    
    @Subscribe
    public void annotationsChanged(DomainObjectAnnotationChangeEvent event) {
        if (searchResults==null) return;
        for(final ResultPage<DomainObject,Reference> page : searchResults.getPages()) {
            if (page==null) continue; // Page not yet loaded
            final Reference ref = Reference.createFor(event.getDomainObject());
            final DomainObject pageObject = page.getObjectById(ref);
            if (pageObject!=null) {

                SimpleWorker annotationUpdateWorker = new SimpleWorker() {

                    @Override
                    protected void doStuff() throws Exception {
                        DomainModel model = DomainMgr.getDomainMgr().getModel();
                        page.updateAnnotations(ref, model.getAnnotations(Reference.createFor(pageObject)));
                    }

                    @Override
                    protected void hadSuccess() {
                        resultsView.refresh(pageObject);
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        FrameworkImplProvider.handleException(error);
                    }
                };

                annotationUpdateWorker.execute();
                break;
            }
        }
    }

    @Override
    protected abstract ResultPage<DomainObject, Reference> getPage(
            SearchResults<DomainObject, Reference> searchResults,
            int page) throws Exception;
    
}
