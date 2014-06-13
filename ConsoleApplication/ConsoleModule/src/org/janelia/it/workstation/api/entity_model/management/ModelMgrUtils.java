package org.janelia.it.workstation.api.entity_model.management;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.ForbiddenEntity;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.model.entity.RootedEntity;

/**
 * Utilities for dealing with Entities via the ModelMgr. In general, most of these methods access the database and
 * should be called from a worker thread, not from the EDT. The exception are methods which describe the current user's
 * read/write access permissions to entities. Those used cached data and may be called from the EDT.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ModelMgrUtils {

    /**
     * Returns the subject name part of a given subject key. For example, for "group:flylight", this will return "flylight".
     *
     * @param subjectKey
     * @return
     */
    public static String getNameFromSubjectKey(String subjectKey) {
        if (subjectKey == null) {
            return null;
        }
        return subjectKey.substring(subjectKey.indexOf(':') + 1);
    }

    /**
     * Returns true if the current user owns the given entity.
     *
     * @param entity
     * @return
     */
    public static boolean isOwner(Entity entity) {
        return EntityUtils.isOwner(entity, SessionMgr.getSubjectKey());
    }

    /**
     * Returns true if the current user is authorized to read the given entity, either because they are the owner, or
     * because they or one of their groups has read permission.
     *
     * @param entity
     * @return
     */
    public static boolean hasReadAccess(Entity entity) {
        return EntityUtils.hasReadAccess(entity, SessionMgr.getSubjectKeys());
    }

    /**
     * Returns true if the current user is authorized to write the given entity, either because they are the owner, or
     * because they or one of their groups has write permission.
     *
     * @param entity
     * @return
     */
    public static boolean hasWriteAccess(Entity entity) {
        return EntityUtils.hasWriteAccess(entity, SessionMgr.getSubjectKeys());
    }

    public static EntityData addChild(Entity parent, Entity child) throws Exception {
        return ModelMgr.getModelMgr().addEntityToParent(parent, child);
    }

    public static RootedEntity getChildFolder(RootedEntity parent, String name, boolean createIfMissing) throws Exception {
        Entity entity = parent.getEntity();
        if (!ModelMgrUtils.areChildrenLoaded(entity)) {
            entity = ModelMgr.getModelMgr().loadLazyEntity(entity, false);
            parent.setEntity(entity);
        }
        EntityData repFolderEd = EntityUtils.findChildEntityDataWithName(entity, name);
        if (repFolderEd == null) {
            if (createIfMissing) {
                Entity repFolder = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_FOLDER, name);
                repFolderEd = ModelMgr.getModelMgr().addEntityToParent(entity, repFolder);
            }
            else {
                return null;
            }
        }

        RootedEntity child = parent.getChild(repFolderEd);
        if (!ModelMgrUtils.areChildrenLoaded(child.getEntity())) {
            child.setEntity(ModelMgr.getModelMgr().loadLazyEntity(child.getEntity(), false));
        }
        return child;
    }

    /**
     * Similar to Entity.getDescendantsOfType except it autoloads lazy children when necessary.
     *
     * @param entity
     * @param typeName
     * @param ignoreNested
     * @return
     */
    public static List<Entity> getDescendantsOfType(Entity entity, String typeName, boolean ignoreNested) throws Exception {

        boolean found = false;
        List<Entity> items = new ArrayList<Entity>();
        if (typeName == null || typeName.equals(entity.getEntityTypeName())) {
            items.add(entity);
            found = true;
        }

        if (!found || !ignoreNested) {
            if (!ModelMgrUtils.areChildrenLoaded(entity)) {
                entity = ModelMgr.getModelMgr().loadLazyEntity(entity, !ignoreNested);
            }
            for (EntityData entityData : getAccessibleEntityDatasWithChildren(entity)) {
                items.addAll(ModelMgrUtils.getDescendantsOfType(entityData.getChildEntity(), typeName, ignoreNested));
            }
        }

        return items;
    }

    public static void removeAllChildren(Entity entity) throws Exception {
        List<EntityData> toDelete = new ArrayList<EntityData>();
        for (EntityData ed : ModelMgrUtils.getAccessibleEntityDatasWithChildren(entity)) {
            toDelete.add(ed);
        }
        ModelMgr.getModelMgr().deleteBulkEntityData(entity, toDelete);
    }

    /**
     * Update the ordering of the children of the given entity, to reflect the ordering provided by the comparator.
     *
     * @param entity
     * @param comparator
     * @throws Exception
     */
    public static void fixOrderIndicies(Entity entity, Comparator<EntityData> comparator) throws Exception {
        List<EntityData> orderedData = new ArrayList<EntityData>();
        for (EntityData ed : ModelMgrUtils.getAccessibleEntityDatasWithChildren(entity)) {
            orderedData.add(ed);
        }
        Collections.sort(orderedData, comparator);

        int orderIndex = 0;
        for (EntityData ed : orderedData) {
            ed.setOrderIndex(orderIndex++);
        }

        ModelMgr.getModelMgr().saveOrUpdateEntity(entity);
    }
    
    public static List<EntityData> getAccessibleEntityDatas(Entity entity) {
        List<EntityData> entityDatas = new ArrayList<EntityData>();
        for (EntityData ed : entity.getOrderedEntityData()) {
            Entity child = ed.getChildEntity();
            if (child!=null && child instanceof ForbiddenEntity) {
                continue;
            }
            entityDatas.add(ed);
        }
        return entityDatas;
    } 
    
    public static List<EntityData> getAccessibleEntityDatasWithChildren(Entity entity) {
        List<EntityData> entityDatas = new ArrayList<EntityData>();
        for (EntityData ed : entity.getOrderedEntityData()) {
            Entity child = ed.getChildEntity();
            if (child==null) {
                continue;
            }
            if (child instanceof ForbiddenEntity) {
                continue;
            }
            entityDatas.add(ed);
        }
        return entityDatas;
    } 
            
    public static int getNumAccessibleEntityDatas(Entity entity) {
        int c = 0;
        for (EntityData ed : entity.getEntityData()) {
            Entity child = ed.getChildEntity();
            if (child!=null && child instanceof ForbiddenEntity) {
                continue;
            }
            c++;
        }
        return c;
    } 
    
    public static List<Entity> getAccessibleChildren(Entity entity) {
        List<Entity> children = new ArrayList<Entity>();
        for (EntityData ed : entity.getOrderedEntityData()) {
            Entity child = ed.getChildEntity();
            if (child==null) {
                continue;
            }
            if (child instanceof ForbiddenEntity) {
                continue;
            }
            children.add(child);
        }
        return children;
    } 
    
    public static int getNumAccessibleChildren(Entity entity) {
        int c = 0;
        for (EntityData ed : entity.getEntityData()) {
            Entity child = ed.getChildEntity();
            if (child==null) {
                continue;
            }
            if (child instanceof ForbiddenEntity) {
                continue;
            }
            c++;
        }
        return c;
    } 
    
    public static boolean areChildrenLoaded(Entity entity) {
        for (EntityData entityData : entity.getEntityData()) {
            Entity child = entityData.getChildEntity();
            if (child==null) {
                continue;
            }
            if (child instanceof ForbiddenEntity) {
                continue;
            }
            if (!EntityUtils.isInitialized(child)) {
                return false;
            }
        }
        return true;
    }
}
