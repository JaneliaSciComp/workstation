package org.janelia.it.workstation.browser.gui.editor;

import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JPanel;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.workstation.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.browser.gui.listview.PaginatedResultsPanel;
import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A base class for common code that all domain object editors can benefit from.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class DomainObjectEditorPanel<T extends DomainObject> extends JPanel implements DomainObjectNodeSelectionEditor<T> {

    private static final Logger log = LoggerFactory.getLogger(DomainObjectEditorPanel.class);

    private final DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();

    protected abstract PaginatedResultsPanel getResultsPanel();
    
    protected abstract T getDomainObject();
    
    protected abstract AbstractDomainObjectNode<T> getDomainObjectNode();
    
    @Override
    public abstract void loadDomainObject(T domainObject, boolean isUserDriven, Callable<Void> success);

    @Override
    public abstract void loadDomainObjectNode(AbstractDomainObjectNode<T> domainObjectNode, boolean isUserDriven, Callable<Void> success);

    @Override
    public DomainObjectEditorState<T> saveState() {
        if (getDomainObjectNode()==null) {
            if (getDomainObject()==null) {
                log.warn("No object is loaded, so state cannot be saved");
                return null;
            }
            return new DomainObjectEditorState<T>(
                    getDomainObject(),
                    getResultsPanel().getCurrPage(),
                    getResultsPanel().getViewer().saveState(),
                    getSelectionModel().getSelectedIds());
        }
        else {
            if (getDomainObjectNode()==null || getDomainObjectNode().getDomainObject()==null) {
                log.warn("No object is loaded, so state cannot be saved");
                return null;
            }
            return new DomainObjectEditorState<>(
                    getDomainObjectNode(),
                    getResultsPanel().getCurrPage(),
                    getResultsPanel().getViewer().saveState(),
                    getSelectionModel().getSelectedIds());
        }
    }
    
    @Override
    public void resetState() {
        getResultsPanel().reset();
    }
    
    @Override
    public void restoreState(final DomainObjectEditorState<T> state) {
        
        log.debug("Restoring state: {}", state);
        getResultsPanel().setViewerType(state.getListViewerState().getType());

        // Prepare to restore the selection
        List<Reference> selected = getResultsPanel().getViewer().getSelectionModel().getSelectedIds();
        selected.clear();
        selected.addAll(state.getSelectedIds());
        
        // Prepare to restore the page
        getResultsPanel().setCurrPage(state.getPage());

        // Prepare to restore viewer state, after the reload
        Callable<Void> success = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                log.info("State load completed, restoring viewer state {}", state.getListViewerState());
                getResultsPanel().getViewer().restoreState(state.getListViewerState());
                return null;
            }
        };
                
        if (state.getDomainObjectNode()==null) {
            loadDomainObject(state.getDomainObject(), true, success);
        }
        else {
            loadDomainObjectNode(state.getDomainObjectNode(), true, success);
        }
    }

    @Override
    public Object getEventBusListener() {
        return getResultsPanel();
    }

    @Override
    public void activate() {
        getResultsPanel().activate();
    }

    @Override
    public void deactivate() {
        getResultsPanel().deactivate();
    }

    @Override
    public DomainObjectSelectionModel getSelectionModel() {
        return selectionModel;
    }
}
