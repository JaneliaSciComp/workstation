package org.janelia.it.workstation.api.facade.concrete_facade.ejb;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.entity.AbstractEntityLoader;

/**
 * Client-side implementation of the entity loader interface, using the remote EJB.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EJBEntityLoader implements AbstractEntityLoader {

    private EntityBeanRemote entityBean;
    
	public EJBEntityLoader(EntityBeanRemote entityBean) {
	    this.entityBean = entityBean;
	}
	
	@Override
	public Set<EntityData> getParents(Entity entity) throws Exception {
		return new HashSet<EntityData>(entityBean.getParentEntityDatas(null, entity.getId()));
	}

	@Override
	public Entity populateChildren(Entity entity) throws Exception {
	    Map<Long,Entity> childMap = new HashMap<Long,Entity>();
	    for(Entity child : entityBean.getChildEntities(null, entity.getId())) {
	        childMap.put(child.getId(), child);
	    }
	    for(EntityData ed : entity.getEntityData()) {
	        if (ed.getChildEntity()!=null && !EntityUtils.isInitialized(ed.getChildEntity())) {
	            ed.setChildEntity(childMap.get(ed.getChildEntity().getId()));
	        }
	    }
		return entity;
	}	
}
