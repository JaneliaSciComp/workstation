package org.janelia.it.workstation.model.utils;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import static org.janelia.it.jacs.shared.utils.EntityUtils.areLoaded;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for dealing with model objects.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ModelUtils {

    private static final Logger log = LoggerFactory.getLogger(ModelUtils.class);
    
    public static Collection<Entity> getInternalEntities(Collection<org.janelia.it.workstation.model.domain.EntityWrapper> wrappers) {
        List<Entity> entities = new ArrayList<Entity>();
        for (org.janelia.it.workstation.model.domain.EntityWrapper wrapper : wrappers) {
            entities.add(wrapper.getInternalEntity());
        }
        return entities;
    }

    public static List<EntityData> getSortedEntityDatas(Entity entity) {

        String sortCriteria = null;
        try {
            sortCriteria = ModelMgr.getModelMgr().loadSortCriteria(entity.getId());
        }
        catch (Exception e) {
            log.error("Error loading sort criteria for {}"+entity.getId());
        }
        
        if (StringUtils.isEmpty(sortCriteria)) {
            return entity.getOrderedEntityData();
        }

        List<EntityData> eds = new ArrayList<EntityData>(entity.getEntityData());
        if (!areLoaded(eds)) {
            log.warn("Cannot sort unloaded children for {}", entity.getName());
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

                ComparisonChain chain = ComparisonChain.start();
                if (EntityConstants.VALUE_SC_GUID.equals(sortField)) {
                    chain = chain.compare(e1.getId(), e2.getId(), Ordering.natural().nullsLast());
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

                chain = chain.compare(e1.getId(), e2.getId());
                return chain.result();
            }
        });

        return eds;
    }
}
