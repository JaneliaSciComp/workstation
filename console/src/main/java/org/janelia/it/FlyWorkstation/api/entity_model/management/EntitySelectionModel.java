package org.janelia.it.FlyWorkstation.api.entity_model.management;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks the state of entity selections in a set of different viewers. All entities are referred to by a String, 
 * since the identifier could be either an entityId, or a uniqueId (tree path), or maybe something else in the future.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntitySelectionModel {

	public static final String CATEGORY_OUTLINE = "outline";
	public static final String CATEGORY_MAIN_VIEW = "mainViewer";
	public static final String CATEGORY_SEC_VIEW = "secViewer";
	public static final String CATEGORY_CROSS_VIEW = "crossViewer";
	
	private final Map<String, List<String>> selectionModels = new HashMap<String, List<String>>();
	
	public EntitySelectionModel() {
		selectionModels.put(CATEGORY_OUTLINE, new ArrayList<String>());
		selectionModels.put(CATEGORY_MAIN_VIEW, new ArrayList<String>());
		selectionModels.put(CATEGORY_SEC_VIEW, new ArrayList<String>());
		selectionModels.put(CATEGORY_CROSS_VIEW, new ArrayList<String>());
	}
	
	private List<String> getCategory(String category) {
		List<String> selected = selectionModels.get(category);
		if (selected == null) {
			throw new IllegalArgumentException("Unknown selection category "+category);
		}
		return selected;
	}
	
	public void selectEntity(String category, String identifier, boolean clearAll) {
		List<String> selected = getCategory(category);
		if (clearAll) {
			selected.clear();
		}
		if (selected.contains(identifier)) return;
		selected.add(identifier);
		ModelMgr.getModelMgr().notifyEntitySelected(category, identifier, clearAll);
	}

	public void deselectEntity(String category, String identifier) {
		List<String> selected = getCategory(category);
		if (!selected.contains(identifier)) return;
		selected.remove(identifier);
		ModelMgr.getModelMgr().notifyEntityDeselected(category, identifier);
	}

    public List<String> getSelectedEntitiesIds(String category) {
		List<String> selected = getCategory(category);
		return selected;
	}
    
    public String getLastSelectedEntityId(String category) {
    	List<String> selected = getCategory(category);
    	if (selected.isEmpty()) return null;
    	return selected.get(selected.size()-1);
    }

}
