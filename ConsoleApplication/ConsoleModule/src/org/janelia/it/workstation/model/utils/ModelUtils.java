package org.janelia.it.workstation.model.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.model.domain.EntityWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

import org.janelia.it.jacs.model.entity.ForbiddenEntity;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgrUtils;

/**
 * Utilities for dealing with model objects.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ModelUtils {

    private static final Logger log = LoggerFactory.getLogger(ModelUtils.class);
    
    public static Collection<Entity> getInternalEntities(Collection<EntityWrapper> wrappers) {
        List<Entity> entities = new ArrayList<Entity>();
        for(EntityWrapper wrapper : wrappers) {
            entities.add(wrapper.getInternalEntity());
        }
        return entities;
    }

    public static List<EntityData> getSortedEntityDatas(Entity entity) {

        String sortCriteria = null;
        if (entity.getId()!=null) {
            try {
                sortCriteria = ModelMgr.getModelMgr().getSortCriteria(entity.getId());
            }
            catch (Exception e) {
                log.error("Error loading sort criteria for {}", entity.getName());
            }
        }
        
        List<EntityData> eds = ModelMgrUtils.getAccessibleEntityDatas(entity);
        
        if (StringUtils.isEmpty(sortCriteria)) {
            log.trace("Sorted {} by default ordering",entity.getName());
            return eds;
        }

        if (!EntityUtils.areLoaded(eds)) {
            return entity.getOrderedEntityData();
        }

        final String sortField = sortCriteria.substring(1);
        final boolean sortAsc = !sortCriteria.startsWith("-");

        Collections.sort(eds, new Comparator<EntityData>() {

            @Override
            public int compare(EntityData o1, EntityData o2) {

                EntityData ed1 = o1;
                EntityData ed2 = o2;
                if (!sortAsc) {
                    ed1 = o2;
                    ed2 = o1;
                }

                Entity e1 = ed1.getChildEntity();
                Entity e2 = ed2.getChildEntity();

                if (e1 == null && e2 == null) {
                    return ed1.getId().compareTo(ed2.getId());
                }
                else if (e1 == null) {
                    return sortAsc ? -1 : 1;
                }
                else if (e2 == null) {
                    return sortAsc ? 1 : -1;
                }
                
                if (e1 instanceof ForbiddenEntity) {
                    if (e2 instanceof ForbiddenEntity) {
                        return 0;
                    }
                    return 1;
                }
                if (e2 instanceof ForbiddenEntity) {
                    return -1;
                }
                
                ComparisonChain chain = ComparisonChain.start();
                if (EntityConstants.VALUE_SC_GUID.equals(sortField)) {
                }
                else if (EntityConstants.VALUE_SC_NAME.equals(sortField)) {
                    chain = chain.compare(e1.getName(), e2.getName(), Ordering.natural().nullsLast());
                }
                else if (EntityConstants.VALUE_SC_DATE_CREATED.equals(sortField)) {
                    chain = chain.compare(e1.getCreationDate(), e2.getCreationDate(), Ordering.natural().nullsLast());
                }
                else if (EntityConstants.VALUE_SC_DATE_UPDATED.equals(sortField)) {
                    chain = chain.compare(e1.getUpdatedDate(), e2.getUpdatedDate(), Ordering.natural().nullsLast());
                }
                else {
                    chain = chain.compare(e1.getValueByAttributeName(sortField), e2.getValueByAttributeName(sortField), Ordering.natural().nullsLast());
                }

                chain = chain.compare(e1.getId(), e2.getId(), Ordering.natural().nullsLast());
                return chain.result();
            }
        });

        log.trace("Sorted {} by {}",entity.getName(),sortCriteria);
        return eds;
    }

    public static String getChildUniqueId(String parentUniqueId, EntityData entityData) {
        String uniqueId = parentUniqueId;
        uniqueId += "/ed_" + entityData.getId();
        uniqueId += "/e_" + entityData.getChildEntity().getId();
        return uniqueId;
    }
}
