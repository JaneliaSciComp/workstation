package org.janelia.it.workstation.gui.dialogs.search;

import java.util.*;

import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.shared.solr.SolrResults;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One page of results, treated as a unit for performance reasons.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ResultPage {

    private static final Logger log = LoggerFactory.getLogger(ResultPage.class);

    private final SolrResults solrResults;
    private final Map<Long, Entity> resultEntityById = new HashMap<>();
    private final List<MappedId> mapping = new ArrayList<>();
    private final List<Entity> mappedEntityList = new ArrayList<>();
    private final Map<Long, Entity> mappedEntityById = new HashMap<>();
    private final Map<Long, List<Entity>> mappedEntitiesByResultId = new HashMap<>();
    private final Map<Long, List<Entity>> resultEntitiesByMappedId = new HashMap<>();

    public ResultPage(SolrResults solrResults) {
        this.solrResults = solrResults;
        resultEntityById.clear();
        for (Entity entity : solrResults.getResultList()) {
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

    public void projectResults(final ResultTreeMapping projection, final SearchResults searchResults) throws Exception {

        clearMapping();

        final List<Long> entityIds = new ArrayList<>();
        for (Entity entity : solrResults.getResultList()) {
            entityIds.add(entity.getId());
        }

        List<MappedId> newMapping = projection.getProjectedIds(entityIds);
        mapping.addAll(newMapping);
        log.info("Added "+newMapping.size()+" mappings");

        Set<Long> locallyVisitedMappedIds = new HashSet<>();
        Set<Long> mappedEntityIds = new HashSet<>();
        for (MappedId mappedId : mapping) {
            Long mappedEntityId = mappedId.getMappedId();
            if (locallyVisitedMappedIds.contains(mappedEntityId)) {
                continue;
            }
            locallyVisitedMappedIds.add(mappedEntityId);
            mappedEntityIds.add(mappedEntityId);
        }

        List<Entity> allMappedEntities = ModelMgr.getModelMgr().getEntitiesByIds(new ArrayList<>(mappedEntityIds));
        log.info("Got "+allMappedEntities.size()+" mapped entities");

        for (Entity entity : allMappedEntities) {
            if (entity != null) {
                mappedEntityById.put(entity.getId(), entity);
            }
        }

        for (MappedId mappedId : mapping) {
            List<Entity> resultEntities = resultEntitiesByMappedId.get(mappedId.getMappedId());
            if (resultEntities == null) {
                resultEntities = new ArrayList<>();
                resultEntitiesByMappedId.put(mappedId.getMappedId(), resultEntities);
            }
            Entity resultEntity = resultEntityById.get(mappedId.getOriginalId());
            if (resultEntity != null) {
                resultEntities.add(resultEntity);
            }
            else {
                log.warn("Cannot find result entity " + mappedId.getOriginalId());
            }
        }

        for (MappedId mappedId : mapping) {
            List<Entity> mappedEntities = mappedEntitiesByResultId.get(mappedId.getOriginalId());
            if (mappedEntities == null) {
                mappedEntities = new ArrayList<>();
                mappedEntitiesByResultId.put(mappedId.getOriginalId(), mappedEntities);
            }
            Entity mappedEntity = mappedEntityById.get(mappedId.getMappedId());
            if (mappedEntity != null) {
                mappedEntities.add(mappedEntity);
            }
            else {
                log.warn("Cannot find mapped entity " + mappedId.getMappedId());
            }
        }

        // Order the mapped entities by result entity
        locallyVisitedMappedIds.clear();
        for (Entity entity : solrResults.getResultList()) {
            List<Entity> mappedEntities = mappedEntitiesByResultId.get(entity.getId());
            if (mappedEntities == null) {
                continue;
            }
            for (Entity mappedEntity : mappedEntities) {
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
        if (list == null) {
            return new ArrayList<>();
        }
        return list;
    }

    public List<Entity> getResultEntities(Long mappedEntityId) {
        List<Entity> list = resultEntitiesByMappedId.get(mappedEntityId);
        if (list == null) {
            return new ArrayList<>();
        }
        return list;
    }
}
