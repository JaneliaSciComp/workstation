package org.janelia.it.workstation.browser.gui.editor;

import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JPanel;

import org.janelia.it.workstation.browser.gui.listview.PaginatedResultsPanel;
import org.janelia.workstation.common.gui.editor.DomainObjectEditorState;
import org.janelia.workstation.common.gui.editor.ParentNodeSelectionEditor;
import org.janelia.workstation.common.nodes.DomainObjectNode;
import org.janelia.model.domain.DomainObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A base class for common code that all domain object editors can benefit from.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class DomainObjectEditorPanel<P extends DomainObject, T, S> extends JPanel implements ParentNodeSelectionEditor<P, T, S> {

    private static final Logger log = LoggerFactory.getLogger(DomainObjectEditorPanel.class);

    protected abstract PaginatedResultsPanel<T,S> getResultsPanel();
    
    protected abstract P getDomainObject();
    
    protected abstract DomainObjectNode<P> getDomainObjectNode();
    
    @Override
    public abstract void loadDomainObject(P domainObject, boolean isUserDriven, Callable<Void> success);

    @Override
    public abstract void loadDomainObjectNode(DomainObjectNode<P> domainObjectNode, boolean isUserDriven, Callable<Void> success);

    @Override
    public DomainObjectEditorState<P,T,S> saveState() {
        if (getDomainObjectNode()==null) {
            if (getDomainObject()==null) {
                log.warn("No object is loaded, so state cannot be saved");
                return null;
            }
            return new DomainObjectEditorStateImpl<>(
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
            return new DomainObjectEditorStateImpl<>(
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
    public void restoreState(final DomainObjectEditorState<P,T,S> state) {
        
        if (state==null) {
            log.warn("Cannot restore null state");
            return;
        }
        
        log.info("Restoring state: {}", state);
        if (state.getListViewerState()!=null) {
            getResultsPanel().setViewerType(state.getListViewerState().getType());
        }

        // Prepare to restore the selection
        List<S> selected = getSelectionModel().getSelectedIds();
        selected.clear();
        selected.addAll(state.getSelectedIds());
        
        // Prepare to restore the page
        getResultsPanel().setCurrPage(state.getPage());

        getResultsPanel().getViewer().restoreState(state.getListViewerState());
        
        // Prepare to restore viewer state, after the reload
        Callable<Void> success = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (state.getListViewerState()!=null) {
                    log.info("State load completed, restoring viewer state {}", state.getListViewerState());
//                    getResultsPanel().getViewer().restoreState(state.getListViewerState());
                }
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
}
