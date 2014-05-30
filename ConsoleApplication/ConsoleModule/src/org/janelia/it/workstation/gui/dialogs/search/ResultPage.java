package org.janelia.it.workstation.gui.dialogs.search;

import java.util.*;

import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.shared.solr.SolrResults;
import org.janelia.it.jacs.model.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One page of results, treated as a unit for performance reasons.  
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ResultPage {
	
	private static final Logger log = LoggerFactory.getLogger(ResultPage.class);
	
	private SolrResults solrResults;
	private Map<Long,Entity> resultEntityById = new HashMap<Long,Entity>();
	
	private List<MappedId> mapping = new ArrayList<MappedId>();
	private List<Entity> mappedEntityList = new ArrayList<Entity>();
	private Map<Long,Entity> mappedEntityById = new HashMap<Long,Entity>();
	private Map<Long,List<Entity>> mappedEntitiesByResultId = new HashMap<Long,List<Entity>>();
	private Map<Long,List<Entity>> resultEntitiesByMappedId = new HashMap<Long,List<Entity>>();
	
	
	public ResultPage(SolrResults solrResults) {
		this.solrResults = solrResults;
		resultEntityById.clear();
		for(Entity entity : solrResults.getResultList()) {
			resultEntityById.put(entity.getId(), entity);
		}
	}
	
	public void clearMapping() {
		mapping.clear();
		mappedEntityById.clear();
		resultEntitiesByMappedId.clear();
		mappedEntitiesByResultId.clear();
		mappedEntityList.clear();
	}
	
	public void projectResults(final ResultTreeMapping projection, final org.janelia.it.workstation.gui.dialogs.search.SearchResults searchResults) throws Exception {

		clearMapping();
		
		final List<Long> entityIds = new ArrayList<Long>();
		for(Entity entity : solrResults.getResultList()) {
			entityIds.add(entity.getId());
		}
		
		mapping.addAll(projection.getProjectedIds(entityIds));
		
		Set<Long> locallyVisitedMappedIds = new HashSet<Long>();
		Set<Long> mappedEntityIds = new HashSet<Long>();
		for(MappedId mappedId : mapping) {
			Long mappedEntityId = mappedId.getMappedId();
			if (locallyVisitedMappedIds.contains(mappedEntityId)) {
				continue;
			}
			locallyVisitedMappedIds.add(mappedEntityId);
			mappedEntityIds.add(mappedEntityId);
		}
		
		List<Entity> allMappedEntities = org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().getEntityByIds(new ArrayList<Long>(mappedEntityIds));
		
		for(Entity entity : allMappedEntities) {
		    if (entity!=null) {
		        mappedEntityById.put(entity.getId(), entity);
		    }
		}

		for(MappedId mappedId : mapping) {
			List<Entity> resultEntities = resultEntitiesByMappedId.get(mappedId.getMappedId());
			if (resultEntities==null) {
				resultEntities = new ArrayList<Entity>();
				resultEntitiesByMappedId.put(mappedId.getMappedId(), resultEntities);
			}
			Entity resultEntity = resultEntityById.get(mappedId.getOriginalId());
			if (resultEntity!=null) {
				resultEntities.add(resultEntity);
			}
			else {
				log.warn("Cannot find result entity "+mappedId.getOriginalId());
			}
		}

		for(MappedId mappedId : mapping) {
			List<Entity> mappedEntities = mappedEntitiesByResultId.get(mappedId.getOriginalId());
			if (mappedEntities==null) {
				mappedEntities = new ArrayList<Entity>();
				mappedEntitiesByResultId.put(mappedId.getOriginalId(), mappedEntities);
			}
			Entity mappedEntity = mappedEntityById.get(mappedId.getMappedId());
			if (mappedEntity!=null) {
				mappedEntities.add(mappedEntity);
			}
			else {
				log.warn("Cannot find mapped entity "+mappedId.getMappedId());
			}
		}
		
		// Order the mapped entities by result entity
		locallyVisitedMappedIds.clear();
		for(Entity entity : solrResults.getResultList()) {
			List<Entity> mappedEntities = mappedEntitiesByResultId.get(entity.getId());
			if (mappedEntities==null) {
				continue;
			}
			for(Entity mappedEntity : mappedEntities) {
				Long mappedEntityId = mappedEntity.getId();
				if (locallyVisitedMappedIds.contains(mappedEntityId)) {
					continue;
				}
				if (searchResults.hasMappedEntity(mappedEntityId)) {
					continue;
				}
				locallyVisitedMappedIds.add(mappedEntityId);
				
				mappedEntityList.add(mappedEntity);
			}
		}
	}
	
	public SolrResults getSolrResults() {
		return solrResults;
	}

	public List<MappedId> getMappedIds() {
		return mapping;
	}

	public List<Entity> getResults() {
		return solrResults.getResultList();
	}
	
	public List<Entity> getMappedResults() {
		return mappedEntityList;
	}

	public Entity getResultEntity(Long entityId) {
		return resultEntityById.get(entityId);
	}
	
	public Entity getMappedEntity(Long mappedEntityId) {
		return mappedEntityById.get(mappedEntityId);
	}

	public List<Entity> getMappedEntities(Long entityId) {
		List<Entity> list = mappedEntitiesByResultId.get(entityId);
		if (list==null) {
			return new ArrayList<Entity>();
		}
		return list;
	}

	public List<Entity> getResultEntities(Long mappedEntityId) {
		List<Entity> list =  resultEntitiesByMappedId.get(mappedEntityId);
		if (list==null) return new ArrayList<Entity>();
		return list;
	}
}