package org.janelia.it.workstation.gui.browser.api.navigation;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.components.DomainListViewTopComponent;
import org.janelia.it.workstation.gui.browser.gui.editor.DomainObjectEditorState;
import org.janelia.it.workstation.gui.browser.nb_action.NavigateBack;
import org.janelia.it.workstation.gui.browser.nb_action.NavigateForward;
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

    private List<DomainObjectEditorState> history = new ArrayList<>();
    private int historyPosition = -1;

    public NavigationHistory() {
    }

    public boolean isBackEnabled() {
        return historyPosition > 0;
    }

    public boolean isForwardEnabled() {
        return historyPosition < history.size() - 1;
    }

    public synchronized DomainObjectEditorState goBack() {
        //pushHistory(state, false);
        if (historyPosition > 0) {
            historyPosition--;
            DomainObjectEditorState state = history.get(historyPosition);
            loadState(state);
            updateButtons();
            logCurrHistory();
            return state;
        }
        return null;
    }

    public synchronized DomainObjectEditorState goForward() {
        if (historyPosition < history.size() - 1) {
            historyPosition++;
            DomainObjectEditorState state = history.get(historyPosition);
            loadState(state);
            updateButtons();
            logCurrHistory();
            return state;
        }
        return null;
    }

    public synchronized void pushHistory(DomainObjectEditorState state) {
        pushHistory(state, true);
    }
    
    private void pushHistory(DomainObjectEditorState state, boolean clearForward) {
        if (state.getDomainObjectNode()==null) {
            return;
        }
        
        log.trace("Pushing {} ({} selected)",state.getDomainObjectNode().getName(),state.getSelectedIds().size());
                    
        if (clearForward) {
            // Clear out navigation in the future direction
            history = history.subList(0, historyPosition + 1);
        }
        
        if (!history.isEmpty()) {
            DomainObjectEditorState currState = history.get(historyPosition);
            if (state.getDomainObjectNode().getId().equals(currState.getDomainObjectNode().getId())) {
                log.trace("Already got this state, updating selections");
                // Already got this, let's update the selections
                currState.getSelectedIds().clear();
                currState.getSelectedIds().addAll(state.getSelectedIds());
                return;
            }
        }
        // Add the new state to the end of the list
        history.add(state);
        historyPosition = history.size() - 1;

        updateButtons();
        logCurrHistory();
    }

    private void updateButtons() {
        CallableSystemAction.get(NavigateBack.class).setEnabled(isBackEnabled());
        CallableSystemAction.get(NavigateForward.class).setEnabled(isForwardEnabled());
    }

    private void loadState(DomainObjectEditorState state) {

        DomainListViewTopComponent tc = state.getTopComponent();
        if (!tc.isOpened()) {
            tc.open();
        }
        tc.requestVisible();
        log.info("Restoring state to {}",tc.getName());
        tc.loadState(state);

        // Only select the node after loading the state to editor above, so that it doesn't trigger a normal load
        DomainExplorerTopComponent.getInstance().selectNode(state.getDomainObjectNode());
    }

    private void logCurrHistory() {
        if (!log.isTraceEnabled()) return;
        log.trace("History: ");
        int i = 0 ;
        for(DomainObjectEditorState state2 : history) {
            log.trace("  "+i+" "+state2.getDomainObjectNode().getName()+" "+(historyPosition==i?"<-CURR":""));
            i++;
        }
        log.trace("------------------------");
    }
}
