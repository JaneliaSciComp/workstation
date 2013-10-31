package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.util.Comparator;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

import com.google.common.collect.ComparisonChain;

/**
 * Comparator for ordering common roots and/or ontology roots.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityRootComparator implements Comparator<Entity> {
    
    public int compare(Entity o1, Entity o2) {
        return ComparisonChain.start()
            .compareTrueFirst(ModelMgrUtils.isOwner(o1), ModelMgrUtils.isOwner(o2))
            .compare(o1.getOwnerKey(), o2.getOwnerKey())
            .compareTrueFirst(EntityUtils.isProtected(o1), EntityUtils.isProtected(o2))
            .compareTrueFirst(o1.getName().equals(EntityConstants.NAME_DATA_SETS), o2.getName().equals(EntityConstants.NAME_DATA_SETS))
            .compareTrueFirst(o1.getName().equals(EntityConstants.NAME_SHARED_DATA), o2.getName().equals(EntityConstants.NAME_SHARED_DATA))
            .compare(o1.getId(), o2.getId()).result();
    }
};