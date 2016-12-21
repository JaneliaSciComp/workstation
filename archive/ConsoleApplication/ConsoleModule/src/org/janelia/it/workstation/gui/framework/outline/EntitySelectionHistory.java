package org.janelia.it.workstation.gui.framework.outline;

import java.util.ArrayList;
import java.util.List;
import org.janelia.it.workstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the history of what entities are selected by the user and allows back/forward navigation.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntitySelectionHistory {

    private static final Logger log = LoggerFactory.getLogger(EntitySelectionHistory.class);
    
    private List<EntityViewerState> history = new ArrayList<EntityViewerState>();
    private int historyPosition = -1;

    public EntitySelectionHistory() {
    }

    public boolean isBackEnabled() {
        return historyPosition > 0;
    }

    public boolean isNextEnabled() {
        return historyPosition < history.size() - 1;
    }

    public synchronized void goBack(EntityViewerState state) {
        pushHistory(state, false);
        if (historyPosition > 0) {
            historyPosition--;
            EntityViewerState currState = history.get(historyPosition);
            try {
                SessionMgr.getBrowser().getViewerManager().restoreViewerState(currState);
            }
            catch (Exception e) {
                SessionMgr.getSessionMgr().handleException(e);
            }
            ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, currState.getContextRootedEntity().getId(), true);
        }
        logCurrHistory();
    }

    public synchronized void goForward() {
        if (historyPosition < history.size() - 1) {
            historyPosition++;
            EntityViewerState currState = history.get(historyPosition);
            try {
                SessionMgr.getBrowser().getViewerManager().restoreViewerState(currState);
            }
            catch (Exception e) {
                SessionMgr.getSessionMgr().handleException(e);
            }
            ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, currState.getContextRootedEntity().getId(), true);
        }
        logCurrHistory();
    }

    public synchronized void pushHistory(EntityViewerState state) {
        pushHistory(state, true);
    }
    
    private void pushHistory(EntityViewerState state, boolean clearForward) {
        if (state.getContextRootedEntity()==null) {
            return;
        }
        
        log.trace("Pushing {} ({} selected)",state.getContextRootedEntity().getName(),state.getSelectedIds().size());
                    
        if (clearForward) {
            // Clear out history in the future direction
            history = history.subList(0, historyPosition + 1);
        }
        
        if (!history.isEmpty()) {
            EntityViewerState currState = history.get(historyPosition);
            if (state.getContextRootedEntity().getId().equals(currState.getContextRootedEntity().getId())) {
                log.trace("Already got it, update the selections");
                // Already got this, let's update the selections
                currState.getSelectedIds().clear();
                currState.getSelectedIds().addAll(state.getSelectedIds());
                return;
            }
        }
        // Add the new entity to the end of the list
        history.add(state);
        historyPosition = history.size() - 1;
        logCurrHistory();
    }

    private void logCurrHistory() {
        if (!log.isTraceEnabled()) return;
        log.trace("History: ");
        int i = 0 ;
        for(EntityViewerState state2 : history) {
            log.trace("  "+i+" "+state2.getContextRootedEntity().getName()+" "+(historyPosition==i?"<-CURR":""));
            i++;
        }
        log.trace("------------------------");
    }
}
