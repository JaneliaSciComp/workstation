package org.janelia.it.FlyWorkstation.gui.dialogs.search;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityTree;
import org.janelia.it.jacs.compute.api.support.EntityMapStep;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * A projection from a given entity context to a related entity. The projection may traverse upwards and then downwards
 * in the given entity tree.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ResultTreeMapping {
	
	private List<EntityMapStep> upMappingSteps = new ArrayList<EntityMapStep>();
	private List<EntityMapStep> downMappingSteps = new ArrayList<EntityMapStep>();
	
	public ResultTreeMapping(EntityTree entityTree, String uniqueId1, String uniqueId2) {
		
		String[] path1 = uniqueId1.split("/");
		String[] path2 = uniqueId2.split("/");

		// Calculate the index where the paths diverge
		int ci = 0;
		for(; ci<path1.length && ci<path2.length; ci++) {
			if (!path1[ci].equals(path2[ci])) {
				break;
			}
		}
		
		// Subtract one because we went too far, and one for the empty root path
		ci-=2;
		if (ci<0) ci = 0; 
		
		// Calculate the full path at each step
		List<String> fullpath1 = getFullPaths(path1);
		List<String> fullpath2 = getFullPaths(path2);
		
		// Calculate the "up" projection
		for(int i=fullpath1.size()-1; i>ci; i--) {
			String uniqueId = fullpath1.get(i);
			if (uniqueId.matches(".*?e_\\d+$")) {
				EntityData entityData = entityTree.getEntityDataByUniqueId(uniqueId);
				if (entityData!=null) {
					upMappingSteps.add(new EntityMapStep(entityData, true));
				}
			}	
		}

		// Calculate the "down" projection
		for(int i=ci+1; i<fullpath2.size(); i++) {
			String uniqueId = fullpath2.get(i);
			if (uniqueId.matches(".*?e_\\d+$")) {
				EntityData entityData = entityTree.getEntityDataByUniqueId(uniqueId);
				if (entityData!=null) {
					downMappingSteps.add(new EntityMapStep(entityData, false));
				}
			}	
		}
	}

	public List<MappedId> getProjectedIds(List<Long> entityIds) throws Exception {
		if (entityIds==null||entityIds.isEmpty()) return new ArrayList<MappedId>();
		List<String> upMapping = new ArrayList<String>();
		List<String> downMapping = new ArrayList<String>();
		for(EntityMapStep step : upMappingSteps) {
			upMapping.add(step.getEntityType());
		}
		for(EntityMapStep step : downMappingSteps) {
			downMapping.add(step.getEntityType());
		}
		return ModelMgr.getModelMgr().getProjectedResults(entityIds, upMapping, downMapping);
	}
	
	private List<String> getFullPaths(String[] path) {
		List<String> fullPaths = new ArrayList<String>();
		StringBuffer sb = new StringBuffer();
		for(String stepId : path) {
			if (StringUtils.isEmpty(stepId)) continue;
			sb.append("/");
			sb.append(stepId);
			fullPaths.add(sb.toString());
		}
		return fullPaths;
	}

	public String getDescription() {
		StringBuffer desc = new StringBuffer();
		for(EntityMapStep step : upMappingSteps) {
			desc.append("<'"+step.getEntityType()+"'");
		}
		for(EntityMapStep step : downMappingSteps) {
			desc.append(">'"+step.getEntityType()+"'");
		}
		return desc.toString();
	}
}
