package org.janelia.it.workstation.browser.api.state;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.workstation.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.browser.components.DomainListViewTopComponent;
import org.janelia.it.workstation.browser.gui.editor.DomainObjectEditorState;
import org.janelia.it.workstation.browser.nb_action.NavigateBack;
import org.janelia.it.workstation.browser.nb_action.NavigateForward;
import org.openide.util.actions.CallableSystemAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the navigation through the user's node selection history.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NavigationHistory {

    private static final Logger log = LoggerFactory.getLogger(NavigationHistory.class);

    private List<DomainObjectEditorState<?,?,?>> history = new ArrayList<>();
    private int historyPosition = -1;

    public NavigationHistory() {
    }

    public boolean isBackEnabled() {
        return historyPosition > 0;
    }

    public boolean isForwardEnabled() {
        return historyPosition < history.size() - 1;
    }

    public synchronized DomainObjectEditorState<?,?,?> goBack() {
        if (historyPosition > 0) {
            DomainObjectEditorState<?,?,?> state = history.get(historyPosition-1);
            loadState(state);
            historyPosition--;
            log.info("Now at history position {}", historyPosition);
            // Now the buttons can be updated
            updateButtons();
            logCurrHistory();
            return state;
        }
        return null;
    }

    public synchronized DomainObjectEditorState<?,?,?> goForward() {
        if (historyPosition < history.size() - 1) {
            DomainObjectEditorState<?,?,?> state = history.get(historyPosition+1);
            loadState(state);
            // Same logic as goBack()
            historyPosition++;
            log.info("Now at history position {}", historyPosition);
            updateButtons();
            logCurrHistory();
            return state;
        }
        return null;
    }

    public synchronized void pushHistory(DomainObjectEditorState<?,?,?> state) {
        pushHistory(state, true);
    }
    
    private void pushHistory(DomainObjectEditorState<?,?,?> state, boolean clearForward) {
        
        if (state==null) {
            throw new IllegalStateException("Null state");
        }
        
        if (state.getDomainObject()==null) {
            throw new IllegalStateException("State with null domain object");
        }
        
        log.info("Pushing editor state:\n{}", state);
                    
        if (clearForward) {
            // Clear out navigation in the future direction
            history = history.subList(0, historyPosition + 1);
        }
        
        if (!history.isEmpty()) {
            DomainObjectEditorState<?,?,?> currState = history.get(historyPosition);
            if (currState!=null) {
                if (currState.getDomainObject()!=null && state.getDomainObject().getId().equals(currState.getDomainObject().getId())) {
                    log.warn("We already have this state. This shouldn't happen.");
                }
            }
        }
        // Add the new state to the end of the list
        history.add(state);
        historyPosition = history.size() - 1;
        
        updateButtons();
        logCurrHistory();
    }

    public void updateCurrentState(DomainObjectEditorState<?,?,?> state) {

        if (state==null) {
            throw new IllegalStateException("Null state");
        }
        
        if (state.getDomainObject()==null) {
            throw new IllegalStateException("State with null domain object");
        }
        
        if (historyPosition<0 || historyPosition>=history.size()) return;
        log.debug("Updating current state:\n{}", state);
        history.set(historyPosition, state);
    }
    
    public void updateButtons() {
        CallableSystemAction.get(NavigateBack.class).setEnabled(isBackEnabled());
        CallableSystemAction.get(NavigateForward.class).setEnabled(isForwardEnabled());
    }

    private void loadState(DomainObjectEditorState<?,?,?> state) {

        DomainListViewTopComponent tc = (DomainListViewTopComponent)state.getTopComponent();
        if (!tc.isOpened()) {
            tc.open();
        }
        tc.requestVisible();
        log.info("Loading state in {}",tc.getClass().getSimpleName());
        tc.loadState(state);

        if (state.getDomainObjectNode()!=null) {
            // Only select the node after loading the state to editor above, so that it doesn't trigger a normal load
            DomainExplorerTopComponent.getInstance().selectNode((org.openide.nodes.Node)state.getDomainObjectNode());
        }
    }

    private void logCurrHistory() {
        if (!log.isTraceEnabled()) return;
        log.trace("History: ");
        int i = 0 ;
        for(DomainObjectEditorState<?,?,?> state : history) {
            String domainObjectName = state.getDomainObject()==null ? "null" : state.getDomainObject().getName();
            log.trace("  "+i+" "+domainObjectName+" "+(historyPosition==i?"<-CURR":""));
            i++;
        }
        log.trace("------------------------");
    }

}
