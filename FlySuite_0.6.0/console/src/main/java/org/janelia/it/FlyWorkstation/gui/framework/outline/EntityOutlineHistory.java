package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;

/**
 * Manages the history of what entities are selected by the user and allows back/forward navigation.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityOutlineHistory {

    private List<String> history = new ArrayList<String>();
    private int historyPosition = -1;
    
	public EntityOutlineHistory() {
        ModelMgr.getModelMgr().addModelMgrObserver(new ModelMgrAdapter() {
			@Override
			public void entityOutlineSelected(final String uniqueId, final boolean clearAll) {
				pushHistory(uniqueId);
			}
        });
	}
	
	public boolean isBackEnabled() {
		return historyPosition > 0;
	}
	
	public boolean isNextEnabled() {
		return historyPosition < history.size()-1;
	}
	
	public synchronized void goBack() {
    	if (historyPosition > 0) {
    		historyPosition--;
    		String uniqueIdToView = history.get(historyPosition);
    		ModelMgr.getModelMgr().selectOutlineEntity(uniqueIdToView, true);
    	}    	
    }

    public synchronized void goForward() {
    	if (historyPosition < history.size()-1) {
    		historyPosition++;
    		String uniqueIdToView = history.get(historyPosition);
    		ModelMgr.getModelMgr().selectOutlineEntity(uniqueIdToView, true);	
    	}
    }
    
    private synchronized void pushHistory(String uniqueId) {
    	if (!history.isEmpty()) {
    		if (uniqueId.equals(history.get(historyPosition))) {
    			// already got this
    			return;
    		}
    	}
    	
    	// Clear out history in the future direction
    	history = history.subList(0, historyPosition+1);
    	// Add the new entity to the end of the list
    	history.add(uniqueId);
    	historyPosition = history.size()-1;
    }
	
}
