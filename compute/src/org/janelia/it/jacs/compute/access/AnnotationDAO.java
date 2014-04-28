package org.janelia.it.jacs.compute.access;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Expression;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.compute.service.fly.MaskSampleAnnotationService;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.types.Category;
import org.janelia.it.jacs.model.ontology.types.EnumText;
import org.janelia.it.jacs.model.ontology.types.Interval;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.shared.annotation.MaskAnnotationDataManager;
import org.janelia.it.jacs.shared.annotation.PatternAnnotationDataManager;
import org.janelia.it.jacs.shared.annotation.RelativePatternAnnotationDataManager;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.jacs.shared.utils.entity.AbstractEntityLoader;
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

public class AnnotationDAO extends ComputeBaseDAO implements AbstractEntityLoader {
	
	/** Batch fetch size for JDBC result sets */
	protected final static int JDBC_FETCH_SIZE = 200;
	
    private static final Map<String, EntityType> entityByName = Collections.synchronizedMap(new HashMap<String, EntityType>());
    private static final Map<String, EntityAttribute> attrByName = Collections.synchronizedMap(new HashMap<String, EntityAttribute>());

    public AnnotationDAO(Logger logger) {
        super(logger);
    }

    /******************************************************************************************************************/
    /** ENTITY TYPES AND ATTRIBUTES */
    /******************************************************************************************************************/
    
    private void preloadData() {
        try {
            if (entityByName.isEmpty()) {
                log.debug("preloadData(): preloading entity types");   
                for(EntityType entityType : getAllEntityTypes()) {
                    entityByName.put(entityType.getName(), entityType);
                }
            }
            
            if (attrByName.isEmpty()) {
                log.debug("preloadData(): preloading entity attributes"); 
                for(EntityAttribute entityAttr : getAllEntityAttributes()) {
                    attrByName.put(entityAttr.getName(), entityAttr);
                }
            }
        }
        catch (Exception e) {
            log.error("Unexpected error occurred while trying preload metamodel", e);
        }
    }

    public List<EntityType> getAllEntityTypes() throws DaoException {
        
        if (log.isTraceEnabled()) {
            log.trace("getAllEntityTypes()");    
        }
        
        try {
            // We have to use the Criteria API here, because the HQL API ignores join fetching 
            // for many-to-many associations
            return getCurrentSession().createCriteria(EntityType.class).list();
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public List<EntityAttribute> getAllEntityAttributes() throws DaoException {

        if (log.isTraceEnabled()) {
            log.trace("getAllEntityAttributes()");    
        }
        
        try {
            return getCurrentSession().createCriteria(EntityAttribute.class).list();
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public EntityType getEntityTypeByName(String entityTypeName) {
        preloadData();
        return entityByName.get(entityTypeName);    
    }
    
    public EntityAttribute getEntityAttributeByName(String attrName) {
        preloadData();
        return attrByName.get(attrName);    
    }
    
    public EntityType createNewEntityType(String name) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("createNewEntityType(name="+name+")");    
        }
        EntityType entityType = getEntityTypeByName(name);
    	if (entityType != null) {
    		return entityType;
    	}
    	try {
	    	entityType = new EntityType();
	    	entityType.setName(name);
	        saveOrUpdate(entityType);
            log.info("Created new EntityType '" + name + "'");
    		entityByName.put(name, entityType);
	        return entityType;
    	}
    	catch (Exception e) {
    		throw new DaoException(e);
    	}
    }

    public EntityAttribute createNewEntityAttr(String entityTypeName, String attrName) throws ComputeException {

        if (log.isTraceEnabled()) {
            log.trace("createNewEntityAttr(entityTypeName="+entityTypeName+",attrName="+attrName+")");    
        }
        
        EntityType entityType = getEntityTypeByName(entityTypeName);
        if (entityType == null) {
            throw new ComputeException("Entity type '"+entityTypeName+"' does not exist");
        }
        EntityAttribute entityAttr = getEntityAttributeByName(attrName);
        if (entityAttr != null) {
            return addAttributeToEntityType(entityType, entityAttr);
        }
        else {
            return addAttributeToEntityType(entityType, attrName);  
        }
    }
    
    private EntityAttribute addAttributeToEntityType(EntityType entityType, String attrName) throws DaoException  {

        if (log.isTraceEnabled()) {
            log.trace("addAttributeToEntityType(entityType="+entityType+",attrName="+attrName+")");    
        }
        Session session = getCurrentSession();
        EntityAttribute entityAttr = null;
        
    	try {
	    	entityAttr = new EntityAttribute();
	    	entityAttr.setName(attrName);
	        session.saveOrUpdate(entityAttr);
    		attrByName.put(attrName, entityAttr);
            log.info("Created new EntityAttribute '" + attrName + "'");
    	}
    	catch (Exception e) {
    		throw new DaoException(e);
    	}
    	
        return addAttributeToEntityType(entityType, entityAttr);
    }
    
    private EntityAttribute addAttributeToEntityType(EntityType entityType, EntityAttribute entityAttr) throws DaoException  {

        if (log.isTraceEnabled()) {
            log.trace("addAttributeToEntityType(entityType="+entityType+",entityAttr="+entityAttr+")");    
        }
        
    	try {
    	    Session session = getCurrentSession();
    		Set<EntityAttribute> attrs = entityType.getAttributes();
    		if (attrs == null) {
    			attrs = new HashSet<EntityAttribute>();
    			entityType.setAttributes(attrs);
    		}
    		
    		for(EntityAttribute currAttr : attrs) {
    		    if (currAttr.getName().equals(entityAttr.getName())) {
    		        log.info("EntityAttribute '" + entityAttr.getName() + "' already exists on EntityType '"+entityType.getName()+"'");
    		        return currAttr;
    		    }
    		}
    		
    		attrs.add(entityAttr);
	        session.saveOrUpdate(entityType);
            log.info("Added EntityAttribute '" + entityAttr.getName() + "' to EntityType '"+entityType.getName()+"'");
	        return entityAttr;
    	}
    	catch (Exception e) {
    		throw new DaoException(e);
    	}
    }

    public void deleteAttribute(String ownerKey, String attributeName) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("deleteAttribute(attributeName="+attributeName+")");  
        }
        
        try {
            StringBuilder hql = new StringBuilder();
            hql.append("delete EntityData ed ");
            hql.append("where ed.entityAttrName = :attrName ");
            if (ownerKey!=null) {
                hql.append(" and ed.ownerKey = :ownerKey ");
            }
            
            final Session currentSession = getCurrentSession();
            Query query = currentSession.createQuery(hql.toString());
            query.setParameter("attrName", attributeName);
            if (ownerKey!=null) {
                query.setParameter("ownerKey", ownerKey);
            }
            
            int rows = query.executeUpdate();
            log.debug("Bulk deleted "+rows+" EntityData rows");
            // TODO: if any of these attributes have child entities then we've just messed up numChildren on all the parents...
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    /******************************************************************************************************************/
    /** ENTITY CREATION */
    /******************************************************************************************************************/
    
    public Entity createEntity(String subjectKey, String entityTypeName, String entityName) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("createEntity(subjectKey="+subjectKey+", entityTypeName="+entityTypeName+", entityName="+entityName+")");
        }
        
        if (entityTypeName==null) throw new DaoException("Error creating entity with null type");
        Entity entity = newEntity(entityTypeName, entityName, subjectKey);
        saveOrUpdate(entity);
        return entity;
    }
    
    public Entity saveBulkEntityTree(Entity root) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("saveBulkEntityTree(root="+root+")");
        }
        
        if (root.getOwnerKey()==null) {
            throw new IllegalArgumentException("Root entity must specify the owner key");
        }
        return saveBulkEntityTree(root, root.getOwnerKey());
    }
    
    public Entity saveBulkEntityTree(Entity root, String subjectKey) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("saveBulkEntityTree(root="+root+", subjectKey="+subjectKey+")");
        }

        final int batchSize = 800;  
        
        log.info("Saving bulk entity tree rooted at "+root.getName());
        
        log.debug("Using owner key: "+subjectKey);
        
        java.sql.Date defaultDate = new java.sql.Date(new Date().getTime());
        
        Connection conn = null;
        PreparedStatement stmtEntity = null;
        PreparedStatement stmtEd = null;
        try {
            List<Entity> entities = new ArrayList<Entity>();
            int count = getEntitiesInTree(root, entities);
            log.info("Found "+entities.size()+" entities in tree, and "+count+" objects to be persisted.");
            
            List<Long> ids = TimebasedIdentifierGenerator.generateIdList(count);
            
            conn = getJdbcConnection();
            conn.setAutoCommit(false);
            
            String entitySql = "insert into entity (id,name,owner_dkey,entity_type,creation_date,updated_date,num_children) values (?,?,?,?,?,?,?)";
            stmtEntity = conn.prepareStatement(entitySql);
            
            String edSql = "insert into entityData (id,parent_entity_id,entity_att,value,owner_dkey,creation_date,updated_date,orderIndex,child_entity_id) values (?,?,?,?,?,?,?,?,?)";
            stmtEd = conn.prepareStatement(edSql);
            
            int idIndex = ids.size()-1;
            int entityCount = 0;
            int edCount = 0;
            Long newEntityId = null;
            
            for(Entity entity : entities) {
            
                newEntityId = ids.get(idIndex--);
                entity.setId(newEntityId);
                
                stmtEntity.setLong(1, newEntityId);
                stmtEntity.setString(2, entity.getName());
                stmtEntity.setString(3, subjectKey);
                stmtEntity.setString(4, entity.getEntityTypeName());
                
                if (entity.getCreationDate()!=null) {
                    stmtEntity.setDate(5, new java.sql.Date(entity.getCreationDate().getTime()));   
                }
                else {
                    stmtEntity.setDate(5, defaultDate);
                }

                if (entity.getUpdatedDate()!=null) {
                    stmtEntity.setDate(6, new java.sql.Date(entity.getUpdatedDate().getTime()));
                }
                else {
                    stmtEntity.setDate(6, defaultDate);
                }
                
                stmtEntity.setInt(7, entity.getChildren().size());
                
                stmtEntity.addBatch();
                
                for(EntityData ed : entity.getEntityData()) {

                    Long newEdId = ids.get(idIndex--);
                    ed.setId(newEdId);
                    
                    stmtEd.setLong(1, newEdId);
                    stmtEd.setLong(2, newEntityId);
                    stmtEd.setString(3, ed.getEntityAttrName()); 
                    stmtEd.setString(4, ed.getValue());
                    stmtEd.setString(5, subjectKey);

                    if (ed.getCreationDate()!=null) {
                        stmtEd.setDate(6, new java.sql.Date(ed.getCreationDate().getTime()));   
                    }
                    else {
                        stmtEd.setDate(6, defaultDate);
                    }
                    
                    if (ed.getUpdatedDate()!=null) {
                        stmtEd.setDate(7, new java.sql.Date(ed.getUpdatedDate().getTime()));
                    }
                    else {
                        stmtEd.setDate(7, defaultDate);
                    }
                    
                    if (ed.getOrderIndex()==null) {
                        stmtEd.setNull(8, java.sql.Types.INTEGER);  
                    }
                    else {
                        stmtEd.setObject(8, ed.getOrderIndex());    
                    }

                    if (ed.getChildEntity()==null) {
                        stmtEd.setNull(9, java.sql.Types.BIGINT);   
                    }
                    else {
                        stmtEd.setObject(9, ed.getChildEntity().getId());   
                    }
                    
                    stmtEd.addBatch();
                }
                
                if (++entityCount % batchSize == 0) {
                    stmtEntity.executeBatch();
                }
                if (++edCount % batchSize == 0) {
                    stmtEd.executeBatch();
                }
            }
            
            stmtEntity.executeBatch();
            stmtEd.executeBatch();
            
            conn.commit();
            
            log.info("Saved bulk entity tree with root id="+newEntityId);
            Entity saved = getEntityById(newEntityId);
            if (saved==null) {
                throw new DaoException("Unknown error saving bulk entity tree");
            }
            return saved;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
        finally {
            try {
                if (stmtEntity!=null) stmtEntity.close();
                if (stmtEd!=null) stmtEd.close();
                if (conn!=null) conn.close();   
            }
            catch (SQLException e) {
                log.warn("Ignoring error encountered while closing JDBC connection",e);
            }
        }
    }

    private Entity newEntity(String entityTypeName, String name, String subjectKey) {
        if (log.isTraceEnabled()) {
            log.trace("newEntity(entityTypeName="+entityTypeName+", name="+name+", subjectKey="+subjectKey+")");
        }
        Date date = new Date();
        return new Entity(null, name, subjectKey, entityTypeName, date, date, new HashSet<EntityData>());   
    }
        
    private EntityData newData(Entity parent, String attributeName, String subjectKey) {
        if (log.isTraceEnabled()) {
            log.trace("newData(parent="+parent+", attributeName="+attributeName+", subjectKey="+subjectKey+")");
        }
        Date date = new Date();
        return new EntityData(null, attributeName, parent, null, subjectKey, null, date, date, null);
    }
    
    /******************************************************************************************************************/
    /** ENTITY RETRIEVAL */
    /******************************************************************************************************************/
    
    public Entity getEntityById(String subjectKey, Long entityId) {
        if (log.isTraceEnabled()) {
            log.trace("getEntityById(subjectKey="+subjectKey+", entityId="+entityId+")");
        }

        StringBuilder hql = new StringBuilder();
        hql.append("select distinct e from Entity e ");
        hql.append("left outer join e.entityActorPermissions p ");
        hql.append("where e.id = :entityId ");
        if (null != subjectKey) {
            hql.append("and (e.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
        }
        
        final Session currentSession = getCurrentSession();
        Query query = currentSession.createQuery(hql.toString());
        query.setParameter("entityId", entityId);
        if (null != subjectKey) {
            List<String> subjectKeyList = getSubjectKeys(subjectKey);
            query.setParameterList("subjectKeyList", subjectKeyList);
        }

        return filter((Entity)query.uniqueResult());
    }

    public Entity getEntityByEntityDataId(String subjectKey, Long entityDataId) {
        if (log.isTraceEnabled()) {
            log.trace("getEntityByEntityDataId(subjectKey="+subjectKey+", entityDataId="+entityDataId+")");
        }

        StringBuilder hql = new StringBuilder();
        hql.append("select ed.parentEntity from EntityData ed ");
        hql.append("left outer join fetch ed.parentEntity.entityActorPermissions p ");
        hql.append("where ed.id = :entityDataId ");
        if (null != subjectKey) {
            hql.append("and (ed.parentEntity.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
        }
        
        final Session currentSession = getCurrentSession();
        Query query = currentSession.createQuery(hql.toString());
        query.setParameter("entityDataId", entityDataId);
        if (null != subjectKey) {
            List<String> subjectKeyList = getSubjectKeys(subjectKey);
            query.setParameterList("subjectKeyList", subjectKeyList);
        }

        return filter((Entity)query.uniqueResult());
    }

    public Entity getEntityById(Long targetId) {
        if (log.isTraceEnabled()) {
            log.trace("getEntityById(targetId="+targetId+")");    
        }
        return getEntityById(null, targetId);
    }
    
    public Collection<Entity> getEntitiesByName(String subjectKey, String entityName) throws DaoException {

        if (log.isTraceEnabled()) {
            log.trace("getEntitiesByName(subjectKey="+subjectKey+",entityName="+entityName+")");    
        }
        
        try {
            StringBuilder hql = new StringBuilder();
            hql.append("select distinct e from Entity e ");
            hql.append("left outer join fetch e.entityActorPermissions p ");
            hql.append("where e.name = :entityName ");
            if (subjectKey!=null) {
                hql.append("and (e.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            
            final Session currentSession = getCurrentSession();
            Query query = currentSession.createQuery(hql.toString());
            query.setParameter("entityName", entityName);
            if (subjectKey!=null) {
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
                query.setParameterList("subjectKeyList", subjectKeyList);
            }
            
            return filter(query.list());
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }
    

    public List<Entity> getEntitiesByTypeName(String subjectKey, String entityTypeName) throws DaoException {
        
        if (log.isTraceEnabled()) {
            log.trace("getEntitiesByTypeName(subjectKey="+subjectKey+",entityTypeName="+entityTypeName+")");
        }
        
        try {
            StringBuilder hql = new StringBuilder(256);

            hql.append("select e from Entity e ");
            hql.append("left outer join fetch e.entityActorPermissions p ");
            hql.append("where e.entityTypeName = :entityTypeName ");
            if (null != subjectKey) {
            	hql.append("and (e.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }

            final Session currentSession = getCurrentSession();
            Query query = currentSession.createQuery(hql.toString());
            query.setParameter("entityTypeName", entityTypeName);
            if (null != subjectKey) {
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
                query.setParameterList("subjectKeyList", subjectKeyList);
            }

            return filter(query.list());
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }


    public List<Entity> getEntitiesByNameAndTypeName(String subjectKey, String entityName, String entityTypeName) throws DaoException {
        
        if (log.isTraceEnabled()) {
            log.trace("getEntitiesByNameAndTypeName(subjectKey="+subjectKey+", entityName="+entityName+", entityTypeName=entityTypeName)");
        }

        try {
            StringBuilder hql = new StringBuilder();
            hql.append("select e from Entity e ");
            hql.append("left outer join fetch e.entityActorPermissions p ");
            hql.append("where e.name = :entityName ");
            hql.append("and e.entityTypeName=:entityTypeName ");
            if (null != subjectKey) {
                hql.append("and (e.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            
            final Session currentSession = getCurrentSession();
            Query query = currentSession.createQuery(hql.toString());
            query.setParameter("entityName", entityName);
            query.setParameter("entityTypeName", entityTypeName);
            if (null != subjectKey) {
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
                query.setParameterList("subjectKeyList", subjectKeyList);
            }
            
            return filter(query.list());
        }
        catch (Exception e) {
            throw handleException(e, "getUserEntitiesByNameAndTypeName");
        }
    }

    public List<Entity> getEntitiesWithAttributeValue(String subjectKey, String typeName, String attrName, String attrValue) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getEntitiesWithAttributeValue(subjectKey="+subjectKey+", typeName="+typeName+", attrName="+attrName+", attrValue="+attrValue+")");
        }
        
        try {
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select ed.parentEntity from EntityData ed ");
            hql.append("join ed.parentEntity ");
            hql.append("left outer join fetch ed.parentEntity.entityActorPermissions p ");
            hql.append("where ed.entityAttrName=:attrName and ed.value like :value ");
            if (typeName != null) {
                hql.append("and ed.parentEntity.entityTypeName=:typeName ");
            }
            if (null != subjectKey) {
                hql.append("and (ed.parentEntity.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            Query query = session.createQuery(hql.toString());
            query.setString("attrName", attrName);
            query.setString("value", attrValue);
            if (typeName != null) {
                query.setString("typeName", typeName);
            }
            if (null != subjectKey) {
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
                query.setParameterList("subjectKeyList", subjectKeyList);
            }
            return filter(query.list());
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public List<Entity> getEntitiesWithAttributeValue(String subjectKey, String attrName, String attrValue) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getEntitiesWithAttributeValue(subjectKey="+subjectKey+", attrName="+attrName+", attrValue="+attrValue+")");
        }
        return getUserEntitiesWithAttributeValue(subjectKey, null, attrName, attrValue);
    }
    
    public Collection<Entity> getUserEntitiesByName(String subjectKey, String entityName) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getUserEntitiesByName(subjectKey="+subjectKey+", entityName="+entityName+")");  
        }
        
        try {
            StringBuilder hql = new StringBuilder();
            hql.append("select e from Entity e ");
            hql.append("left outer join fetch e.entityActorPermissions p ");
            hql.append("where e.name = :entityName ");
            if (subjectKey!=null) {
                hql.append("and e.ownerKey = :subjectKey ");
            }
            
            final Session currentSession = getCurrentSession();
            Query query = currentSession.createQuery(hql.toString());
            query.setParameter("entityName", entityName);
            if (subjectKey!=null) {
                query.setParameter("subjectKey", subjectKey);
            }
            
            return filter(query.list());
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public List<Entity> getUserEntitiesByTypeName(String subjectKey, String entityTypeName) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getUserEntitiesByTypeName(subjectKey="+subjectKey+", entityTypeName="+entityTypeName+")");
        }
        
        try {
            StringBuilder hql = new StringBuilder(256);

            hql.append("select e from Entity e ");
            hql.append("left outer join fetch e.entityActorPermissions p ");
            hql.append("where e.entityTypeName = :entityTypeName ");
            if (subjectKey!=null) {
                hql.append("and e.ownerKey = :subjectKey ");
            }

            final Session currentSession = getCurrentSession();
            Query query = currentSession.createQuery(hql.toString());
            query.setParameter("entityTypeName", entityTypeName);
            if (subjectKey!=null) {
                query.setParameter("subjectKey", subjectKey);
            }

            return filter(query.list());
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public List<Entity> getUserEntitiesByNameAndTypeName(String subjectKey, String entityName, String entityTypeName) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getUserEntitiesByNameAndTypeName(subjectKey="+subjectKey+", entityName="+entityName+", entityTypeName=entityTypeName)");
        }
        
        try {
            StringBuilder hql = new StringBuilder();
            hql.append("select e from Entity e ");
            hql.append("left outer join fetch e.entityActorPermissions p ");
            hql.append("where e.name = :entityName ");
            hql.append("and e.entityTypeName=:entityTypeName ");
            if (null != subjectKey) {
                hql.append("and e.ownerKey = :subjectKey ");
            }
            
            final Session currentSession = getCurrentSession();
            Query query = currentSession.createQuery(hql.toString());
            query.setParameter("entityName", entityName);
            query.setParameter("entityTypeName", entityTypeName);
            if (null != subjectKey) {
                query.setParameter("subjectKey", subjectKey);
            }
            
            return filter(query.list());
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public List<Entity> getUserEntitiesWithAttributeValue(String subjectKey, String typeName, String attrName, String attrValue) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getUserEntitiesWithAttributeValue(subjectKey="+subjectKey+", typeName="+typeName+", attrName="+attrName+", attrValue="+attrValue+")");
        }
        
        try {
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select ed.parentEntity from EntityData ed ");
            hql.append("join ed.parentEntity ");
            hql.append("where ed.entityAttrName=:attrName and ed.value like :value ");
            if (typeName != null) {
                hql.append("and ed.parentEntity.entityTypeName=:typeName ");
            }
            if (null != subjectKey) {
                hql.append("and ed.parentEntity.ownerKey=:subjectKey ");
            }
            Query query = session.createQuery(hql.toString());
            query.setString("attrName", attrName);
            query.setString("value", attrValue);
            if (typeName != null) {
                query.setString("typeName", typeName);
            }
            if (null != subjectKey) {
                query.setString("subjectKey", subjectKey);
            }
            return filter(query.list());
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public List<Entity> getUserEntitiesWithAttributeValue(String subjectKey, String attrName, String attrValue) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getUserEntitiesWithAttributeValue(subjectKey="+subjectKey+", attrName="+attrName+", attrValue="+attrValue+")");
        }
        return getUserEntitiesWithAttributeValue(subjectKey, null, attrName, attrValue);
    }

    public long getCountUserEntitiesWithAttributeValue(String subjectKey, String typeName, String attrName, String attrValue) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getCountUserEntitiesWithAttributeValue(subjectKey="+subjectKey+", typeName="+typeName+", attrName="+attrName+", attrValue="+attrValue+")");
        }
        
        try {
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select count(ed.parentEntity) from EntityData ed ");
            hql.append("join ed.parentEntity ");
            hql.append("where ed.entityAttrName=:attrName and ed.value like :value ");
            if (typeName != null) {
                hql.append("and ed.parentEntity.entityTypeName=:typeName ");
            }
            if (null != subjectKey) {
                hql.append("and ed.parentEntity.ownerKey=:subjectKey ");
            }
            Query query = session.createQuery(hql.toString());
            query.setString("attrName", attrName);
            query.setString("value", attrValue);
            if (typeName != null) {
                query.setString("typeName", typeName);
            }
            if (null != subjectKey) {
                query.setString("subjectKey", subjectKey);
            }
            return (Long)query.list().get(0);
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public long getCountUserEntitiesWithAttributeValue(String subjectKey, String attrName, String attrValue) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getCountUserEntitiesWithAttributeValue(subjectKey="+subjectKey+", attrName="+attrName+", attrValue="+attrValue+")");
        }
        return getCountUserEntitiesWithAttributeValue(subjectKey, null, attrName, attrValue);
    }

    public List<Entity> getEntitiesWithTag(String subjectKey, String attrTag) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getEntitiesWithTag(subjectKey="+subjectKey+", attrTag="+attrTag+")");
        }
        
        try {
            List<String> subjectKeyList = null;
            
            Session session = getCurrentSession();
            StringBuilder hql = new StringBuilder();
            hql.append("select e from Entity e ");
            hql.append("left outer join fetch e.entityActorPermissions p ");
            hql.append("join e.entityData as ed ");
            hql.append("where ed.entityAttrName = :attrName ");
            if (null != subjectKey) {
                hql.append("and (e.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            hql.append("order by e.id ");
            Query query = session.createQuery(hql.toString());
            query.setString("attrName", attrTag);
            if (null != subjectKey) {
                subjectKeyList = getSubjectKeys(subjectKey);
                query.setParameterList("subjectKeyList", subjectKeyList);
            }
            
            return filter(query.list());
            
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Entity getCommonRootFolderByName(String subjectKey, String folderName, boolean createIfNecessary) throws DaoException {

        if (log.isTraceEnabled()) {
            log.trace("getCommonRootFolderByName(subjectKey="+subjectKey+", folderName="+folderName+", createIfNecessary="+createIfNecessary+")");
        }
        
        Entity folder = null;
        for(Entity entity : getUserEntitiesByNameAndTypeName(subjectKey, folderName, EntityConstants.TYPE_FOLDER)) {
            if (entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT)!=null) {
                if (folder!=null) {
                    throw new IllegalStateException("Multiple common roots owned by "+subjectKey+" with name: "+folderName);
                }
                folder = entity;
            }
        }
        
        if (folder!=null) {
            return filter(folder);
        }
        
        if (createIfNecessary) {
            log.info("Creating new topLevelFolder with name=" + folderName);
            folder = createEntity(subjectKey, EntityConstants.TYPE_FOLDER, folderName);
            EntityUtils.addAttributeAsTag(folder, EntityConstants.ATTRIBUTE_COMMON_ROOT);
            saveOrUpdate(folder);
            log.info("Saved top level folder as " + folder.getId());
        }
        
        return filter(folder);
    }

    public List<Entity> getEntitiesInList(String subjectKey, String entityIds) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getEntitiesInList(subjectKey="+subjectKey+", entityIds="+entityIds+")");
        }
        
        String[] idStrs = entityIds.split("\\s*,\\s*");
        List<Long> ids = new ArrayList<Long>();
        for(String idStr : idStrs) {
            ids.add(new Long(idStr));
        }
        return getEntitiesInList(subjectKey, ids);
    }

    public List<Entity> getEntitiesInList(String subjectKey, List<Long> entityIds) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getEntitiesInList(subjectKey="+subjectKey+", entityIds.size="+entityIds.size()+")");
        }
        
        try {           
            if (entityIds == null || entityIds.isEmpty()) {
                return new ArrayList<Entity>();
            }
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select e from Entity e ");
            hql.append("left outer join fetch e.entityActorPermissions p ");
            hql.append("where e.id in (:ids) ");
            if (null != subjectKey) {
                hql.append("and (e.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            
            Query query = session.createQuery(hql.toString());
            query.setParameterList("ids", entityIds);
            if (null != subjectKey) {
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
                query.setParameterList("subjectKeyList", subjectKeyList);
            }
            
            List<Entity> results = query.list();
            
            // Resort the results in the order that the ids were given
            
            Map<Long,Entity> map = new HashMap<Long,Entity>();
            for(Entity entity : results) {
                map.put(entity.getId(), entity);
            }
            
            List<Entity> sortedList = new ArrayList<Entity>();
            for(Long entityId : entityIds) {
                Entity entity = map.get(entityId);
                if (entity != null) {
                    sortedList.add(entity);
                }
            }
            
            return filter(sortedList);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    private int getEntitiesInTree(Entity entity, List<Entity> allEntities) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("getEntitiesInTree(entity="+entity+", allEntities.size="+allEntities+")");
        }

        int count = 1;
        
        for(EntityData ed : entity.getEntityData()) {
            count++;   
            Entity child = ed.getChildEntity();
            if (child!=null) {
                count += getEntitiesInTree(child, allEntities);
            }
        }
        
        allEntities.add(entity);
        return count;
    }
    
    public List<Entity> getEntitiesWithFilePath(String filePath) {
        if (log.isTraceEnabled()) {
            log.trace("getEntitiesWithFilePath(filePath="+filePath+")");    
        }
        Session session = getCurrentSession();
        StringBuffer hql = new StringBuffer("select clazz.parentEntity from EntityData clazz where value=?");
        Query query = session.createQuery(hql.toString()).setString(0, filePath);
        return filter(query.list());
    }
    
    /**
     * Iterate recursively through all children in the Entity graph in order to preload them.
     * @param entity
     * @return
     */
    public Set<Long> getDescendantIds(String subjectKey, Entity entity) {
        if (log.isTraceEnabled()) {
            log.trace("getDescendantIds(subjectKey="+subjectKey+", entity="+entity+")");
        }
        
        Set<String> subjectKeys = getSubjectKeySet(subjectKey);
        Set<Long> visited = new HashSet<Long>();
        getDescendantIds(subjectKeys, entity, visited);
        return visited;
    }
    
    private void getDescendantIds(Set<String> subjectKeys, Entity entity, Set<Long> visited) {
        if (log.isTraceEnabled()) {
            log.trace("getDescendantIds(subjectKeys="+subjectKeys+", entity="+entity+", visited.size="+visited.size()+")");
        }
        
        if (entity == null) return;
        if (subjectKeys!=null && !subjectKeys.contains(entity.getOwnerKey())) return;
        if (visited.contains(entity.getId())) return;
        visited.add(entity.getId());
        
        for(EntityData ed : entity.getEntityData()) {
            Entity child = ed.getChildEntity();
            if (child != null) {
                getDescendantIds(subjectKeys, child, visited);
            }
        }
    }
    
    public Entity getEntityAndChildren(String subjectKey, Long entityId) {
        if (log.isTraceEnabled()) {
            log.trace("getEntityAndChildren(subjectKey="+subjectKey+", entityId="+entityId+")");
        }
        Entity parent = getEntityById(subjectKey, entityId);
        if (parent == null)
            return null;
        for (EntityData ed : parent.getEntityData()) {
            Entity child=ed.getChildEntity();
            if (child!=null) {
                String childName = child.getName(); // forces load of attributes but not subchild entities
            }
        }
        return parent;
    }

    /**
     * Searches the tree for the given entity and returns its ancestor of a given type, or null if the entity or 
     * ancestor does not exist.
     * @param entity
     * @param type
     * @return
     */
    public Entity getAncestorWithType(String subjectKey, Long entityId, String type) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getAncestorWithType(subjectKey="+subjectKey+", entityId="+entityId+", type="+type+")");
        }
        
        return getAncestorWithType(subjectKey, entityId, type, true, new HashSet<Long>());
    }
    
    private Entity getAncestorWithType(String subjectKey, Long entityId, String type, boolean start, Set<Long> visited) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getAncestorWithType(subjectKey="+subjectKey+", entityId="+entityId+", type="+type+", start="+start+")");
        }

        if (visited.contains(entityId)) {
            // We've already been here, don't need to search it again
        	return null;
        }
        visited.add(entityId);
        
        Entity entity = getEntityById(entityId);
        // Do not return the starting node as the ancestor, even if type matches
        if (!start && entity.getEntityTypeName().equals(type)) return entity;
        
        for(Entity parent : getParentEntities(subjectKey, entityId)) {
            Entity ancestor = getAncestorWithType(subjectKey, parent.getId(), type, false, visited);
            if (ancestor != null) return ancestor;
        }
        
        return null;
    }

    public List<List<Entity>> getEntityPathsToRoots(String subjectKey, Long entityId) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getEntityPathsToRoots(subjectKey="+subjectKey+", entityId="+entityId+")");
        }
        
        Entity entity = getEntityById(entityId);
        List<List<Entity>> paths = new ArrayList<List<Entity>>();
        Set<Entity> parents = getParentEntities(subjectKey, entityId);
        StringBuffer sb = new StringBuffer();
        for(Entity parent : parents) {
            if (sb.length()>0) sb.append(", ");
            sb.append(parent.getName());
        }
        if (parents.isEmpty()) {
            List<Entity> path = new ArrayList<Entity>();
            path.add(entity);
            paths.add(path);
        }
        else {
            for(Entity parent : parents) {
                List<List<Entity>> ancestorPaths = getEntityPathsToRoots(subjectKey, parent.getId());
                for(List<Entity> ancestorPath : ancestorPaths) {
                    ancestorPath.add(entity);
                    paths.add(ancestorPath);
                }
            }
        }
        return paths;
    }

    public List<List<EntityData>> getEntityDataPathsToRoots(String subjectKey, EntityData entityData) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getEntityPathsToRoots(subjectKey="+subjectKey+", entityData="+entityData+")");
        }
        
        Entity entity = entityData.getParentEntity();
        List<List<EntityData>> paths = new ArrayList<List<EntityData>>();
        Set<EntityData> parents = getParentEntityDatas(subjectKey, entity.getId());
        if (parents.isEmpty()) {
            List<EntityData> path = new ArrayList<EntityData>();
            path.add(entityData);
            paths.add(path);
        }
        else {
            Set<String> subjectKeys = getSubjectKeySet(subjectKey);
            for(EntityData parent : parents) {
                if (subjectKeys==null || subjectKeys.contains(parent.getOwnerKey())) {
                    List<List<EntityData>> ancestorPaths = getEntityDataPathsToRoots(subjectKey, parent);
                    for(List<EntityData> ancestorPath : ancestorPaths) {
                        if (entityData.getId()!=null) ancestorPath.add(entityData);
                        paths.add(ancestorPath);
                    }
                }
            }
        }
        return paths;
    }
    
    public List<Long> getPathToRoot(String subjectKey, Long entityId, Long rootId) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getPathToRoot(subjectKey="+subjectKey+", entityId="+entityId+", rootId="+rootId+")");
        }
        
        List<Long> ids = new ArrayList<Long>();
        
        if (entityId.equals(rootId)) {
            ids.add(rootId);
            return ids;
        }
        
        for(Entity parent : getParentEntities(subjectKey, entityId)) {
            List<Long> path = getPathToRoot(subjectKey, parent.getId(), rootId);
            if (path != null) {
                path.add(entityId);
                return path;
            }
        }
        
        // No path to the given root
        return null;
    }

    public List<MappedId> getProjectedResults(String subjectKey, List<Long> entityIds, List<String> upProjection, List<String> downProjection) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getProjectedResults(subjectKey="+subjectKey+", entityIds.size="+entityIds.size()+", upProjection.size="+upProjection.size()+", downProjection.size="+downProjection.size()+")");
        }
        
        // TODO: filter results by subjectKey?
        
        if (entityIds.isEmpty()) {
            throw new DaoException("getProjectedResults: entity ids cannot be empty");
        }
        
        if (upProjection.isEmpty() && downProjection.isEmpty()) {
            throw new DaoException("getProjectedResults: both up and down projections cannot be empty");
        }
        
        List<MappedId> list = new ArrayList<MappedId>();
        
        StringBuffer entityCommaList = new StringBuffer();
        for(Long id : entityIds) {
            if (entityCommaList.length()>0) entityCommaList.append(",");
            entityCommaList.append(id);
        }

        StringBuffer sql = new StringBuffer();
        
        int i = 1;
        String prevTable = "i";
        String prevFk = "id";
        String targetIdAlias = "i.id";
        
        for(String attr : upProjection) {
            sql.append("join entityData ed"+i+" on ed"+i+".child_entity_id = "+prevTable+"."+prevFk+" \n"); 
            sql.append("join entity e"+i+" on ed"+i+".parent_entity_id = e"+i+".id \n");
            prevTable = "ed"+i;
            prevFk = "parent_entity_id";
            targetIdAlias = prevTable+".parent_entity_id";
            i++;
        }
        
        for(String attr : downProjection) {
            sql.append("join entityData ed"+i+" on ed"+i+".parent_entity_id = "+prevTable+"."+prevFk+" \n");    
            sql.append("join entity e"+i+" on ed"+i+".child_entity_id = e"+i+".id \n");
            prevTable = "ed"+i;
            prevFk = "child_entity_id";
            targetIdAlias = prevTable+".child_entity_id";
            i++;
        }
        
        sql.insert(0, "select distinct i.id,"+targetIdAlias+" from entity i \n");
        sql.append("where i.id in ("+entityCommaList+") \n");
        
        i = 1;
        
        for(String type : upProjection) {
            sql.append("and e"+i+".entity_type = '"+type+"' \n"); 
            i++;
        }

        for(String type : downProjection) {
            sql.append("and e"+i+".entity_type = '"+type+"' \n"); 
            i++;
        }
        
        sql.append("and "+targetIdAlias+" is not null");
        
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            
            log.debug(sql);
            
            conn = getJdbcConnection();
            stmt = conn.prepareStatement(sql.toString());
                        
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Long entityId = rs.getBigDecimal(1).longValue();
                Long projId = rs.getBigDecimal(2).longValue();
                list.add(new MappedId(entityId, projId));
            }
        }
        catch (SQLException e) {
            log.error("Error executing SQL: ["+sql+"]");
            throw new DaoException(e);
        }
        finally {
            try {
                if (stmt!=null) stmt.close();
                if (conn!=null) conn.close();   
            }
            catch (SQLException e) {
                log.warn("Ignoring error encountered while closing JDBC connection",e);
            }
        }
        
        return list;
    }

    public List<Long> getEntityIdsInAlignmentSpace( String opticalRes, String pixelRes, List<Long> rawIds ) throws DaoException {
        if ( log.isTraceEnabled()) {
            log.trace("getEntityIdsInAlignmentSpace("+opticalRes+", "+pixelRes+", rawIds["+rawIds.size()+"])");
        }

        List<Long> rtnVal = new ArrayList<Long>();
        if ( rawIds == null  ||  rawIds.size() == 0 ) {
            return rtnVal;
        }

        if ( opticalRes == null  ||  pixelRes == null ) {
            throw new DaoException("Must provide both non-null optical resolution and non-null pixel resolution.");
        }

        String queryStrFormat = "select nf.id from entity nf \n" +
                "join entityData nfcEd on nfcEd.child_entity_id=nf.id \n" +
                "join entityData nsEd on nsEd.child_entity_id=nfcEd.parent_entity_id \n" +
                "join entity ns on nsEd.parent_entity_id=ns.id \n" +
                "join entityData nsOpticalRes on nsOpticalRes.parent_entity_id=ns.id and nsOpticalRes.entity_att='Optical Resolution' \n" +
                "join entityData nsPixelRes on nsPixelRes.parent_entity_id=ns.id and nsPixelRes.entity_att='Pixel Resolution' \n" +
                "where nf.id in (%s) \n" +
                "and nsOpticalRes.value='%s'\n" +
                "and nsPixelRes.value='%s'";

        StringBuilder idsBuilder = new StringBuilder();
        for ( Long rawId: rawIds ) {
            if ( idsBuilder.length() > 0 ) {
                idsBuilder.append( ',' );
            }
            idsBuilder.append(rawId);
        }
        String query = String.format( queryStrFormat, idsBuilder.toString(), opticalRes, pixelRes );
        log.info("Querying with " + query);
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getJdbcConnection();
            stmt = conn.prepareStatement( query );

            ResultSet rs = stmt.executeQuery();
            while ( rs.next() ) {
                rtnVal.add(rs.getLong(1));
            }
         } catch (SQLException sqle) {
            throw new DaoException( sqle );
        } finally {
            try {
                if ( stmt != null ) {
                    stmt.close();
                }
                if ( conn != null ) {
                    conn.close();
                }
            } catch ( SQLException sqlInnerE ) {
                log.warn( "Ignoring error during JDBC stmt/conn closure.", sqlInnerE );
            }
        }

        return rtnVal;
    }
    
    public List<Long> getImageIdsWithName(Connection connection, String subjectKey, String imagePath) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getImageIdsWithName(connection="+connection+", subjectKey="+subjectKey+", imagePath="+imagePath+")");
        }
        
        List<Long> sampleIds = new ArrayList<Long>();
        Connection conn = connection;
        PreparedStatement stmt = null;
        
        try {
            StringBuffer sql = new StringBuffer("select distinct i.id from entity i ");
            sql.append("where i.name = ? ");
            if (subjectKey!=null) {
                sql.append("and i.ownerKey = ? ");
            }
            
            if (conn==null) {
                conn = getJdbcConnection();
            }
            stmt = conn.prepareStatement(sql.toString());
            stmt.setString(1, imagePath);
            if (subjectKey!=null) {
                stmt.setString(2, subjectKey);
            }
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                sampleIds.add(rs.getBigDecimal(1).longValue());
            }
        }
        catch (SQLException e) {
            throw new DaoException(e);
        }
        finally {
            try {
                if (stmt!=null) stmt.close();
                // only close the connection if it didn't come from outside
                if (connection==null && conn!=null) conn.close(); 
            }
            catch (SQLException e) {
                log.warn("Ignoring error encountered while closing JDBC connection",e);
            }
        }
        
        return sampleIds;
    }
    
    public Entity getChildFolderByName(String subjectKey, Long parentId, String folderName, boolean createIfNecessary) throws DaoException {

        if (log.isTraceEnabled()) {
            log.trace("getChildFolderByName(subjectKey="+subjectKey+", parentId="+parentId+", folderName="+folderName+", createIfNecessary="+createIfNecessary+")");
        }
        
        Entity parent = getEntityById(subjectKey, parentId);
        if (parent==null) {
            throw new IllegalArgumentException("Parent folder does not exist: "+parentId);
        }
        
        for(Entity child : parent.getChildren()) {
            if (child.getName().equals(folderName)) {
                return child;
            }
        }
        
        Entity folder = null;
        if (createIfNecessary) {
            log.info("Creating new child folder with name=" + folderName);
            folder = createEntity(subjectKey, EntityConstants.TYPE_FOLDER, folderName);
            addEntityToParent(parent, folder, parent.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
        }
        
        return folder;
    }
    
    @Override
    public Set<EntityData> getParents(Entity entity) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("getParents(entity="+entity+")");
        }
        
        return getParentEntityDatas(null, entity.getId());
    }
    
    public Set<EntityData> getParentEntityDatas(String subjectKey, Long childEntityId) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getParentEntityDatas(subjectKey="+childEntityId+", childEntityId="+childEntityId+")");
        }
        
        try {   
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select ed from EntityData ed ");
            hql.append("join fetch ed.childEntity ");
            hql.append("join fetch ed.parentEntity ");
            hql.append("left outer join fetch ed.parentEntity.entityActorPermissions p ");
            hql.append("where ed.childEntity.id=?");
            if (null != subjectKey) {
                hql.append("and (ed.parentEntity.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            Query query = session.createQuery(hql.toString()).setLong(0, childEntityId);
            if (null != subjectKey) {
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
                query.setParameterList("subjectKeyList", subjectKeyList);
            }
            return new HashSet(query.list());
        } 
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Set<Long> getParentIdsForAttribute(String subjectKey, Long childEntityId, String attributeName) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getParentIdsForAttribute(childEntityId="+childEntityId+", attributeName="+attributeName+")");
        }
        
        try {   
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select ed.parentEntity.id from EntityData ed ");
            hql.append("left outer join ed.parentEntity.entityActorPermissions p ");
            hql.append("where ed.childEntity.id=:childEntityId ");
            hql.append("and ed.entityAttrName=:attrName ");
            if (null != subjectKey) {
                hql.append("and (ed.parentEntity.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            Query query = session.createQuery(hql.toString());
            query.setLong("childEntityId", childEntityId);
            query.setString("attrName", attributeName);
            if (null != subjectKey) {
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
                query.setParameterList("subjectKeyList", subjectKeyList);
            }
            return new HashSet(query.list());
        } 
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Set<Entity> getParentEntities(String subjectKey, Long entityId) throws DaoException {
        try {
            if (log.isTraceEnabled()) {
                log.trace("getParentEntities(subjectKey="+subjectKey+", entityId="+entityId+")");
            }
            
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select ed.parentEntity from EntityData ed ");
            hql.append("join ed.parentEntity ");
            hql.append("left outer join fetch ed.parentEntity.entityActorPermissions p ");
            hql.append("where ed.childEntity.id=? ");
            if (null != subjectKey) {
                hql.append("and (ed.parentEntity.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            Query query = session.createQuery(hql.toString()).setLong(0, entityId);
            if (null != subjectKey) {
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
                query.setParameterList("subjectKeyList", subjectKeyList);
            }
            return new HashSet(filter(query.list()));
        } 
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Set<Entity> getChildEntities(String subjectKey, Long entityId) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getChildEntities(subjectKey="+subjectKey+", entityId="+entityId+")");
        }
        List<String> subjectKeyList = subjectKey==null?null:getSubjectKeys(subjectKey);
        return getChildEntities(subjectKeyList, entityId);
    }
    
    public Set<Entity> getChildEntities(List<String> subjectKeyList, Long entityId) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getChildEntities(subjectKeyList="+subjectKeyList+", entityId="+entityId+")");
        }
        
        try {   
            Session session = getCurrentSession();
            
            StringBuffer hql = new StringBuffer("select ed.childEntity from EntityData ed ");
            hql.append("join ed.childEntity ");
            hql.append("left outer join fetch ed.childEntity.entityActorPermissions p ");
            hql.append("where ed.parentEntity.id=? ");
            if (subjectKeyList != null) {
                hql.append("and (ed.childEntity.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            Query query = session.createQuery(hql.toString()).setLong(0, entityId);
            if (subjectKeyList != null) {
                query.setParameterList("subjectKeyList", subjectKeyList);
            }

            List<Entity> childEntities = filter(query.list(), false);

            if (subjectKeyList==null) {
                // Case 1: This method is being called locally, and we know that because it's bypassing authorization.
                // In this case, we want to load the EntityDatas normally, using filter. This is because the SQL hack 
                // below creates an object model that is detached from Hibernate.
                return new HashSet(filter(childEntities));
            }
            
            // Case2: This method is being called remotely, and we know that because it's using authorization. 
            // In this case we can use a trick to load the EntityDatas in a single query, rather than one by one. 
            
            // Note: Either of the HQL statements below _should_ work, but for whatever broken Hibernate reasoning, 
            // they just return the EntityData id, and then lazy load during iteration, which doesn't gain us anything.

            //hql = new StringBuffer("select ced from EntityData ed ");
            //hql.append("inner join ed.childEntity ce ");
            //hql.append("inner join ce.entityData ced ");
            //hql.append("where ed.parentEntity.id=? ");

            //hql = new StringBuffer("select ced from EntityData ced ");
            //hql.append("inner join ced.parentEntity pe, ");
            //hql.append("EntityData ed ");
            //hql.append("where ed.childEntity=pe ");
            //hql.append("and ed.parentEntity.id=? ");

            // Therefore, we need to use JDBC. Bypassing Hibernate is not ideal, but the huge performance gain justifies it. 
            
            Multimap<Long,EntityData> edMap = HashMultimap.<Long,EntityData>create();
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                conn = getJdbcConnection();

                StringBuffer sql = new StringBuffer("select distinct ced.id, ced.parent_entity_id, ced.child_entity_id, ");
                sql.append("ced.entity_att, ced.owner_key, ced.value, ced.creation_date, ced.updated_date, ced.orderIndex ");
                sql.append("from entityData ced  ");
                sql.append("inner join entity e on ced.parent_entity_id=e.id ");
                sql.append("inner join entityData ed on ed.child_entity_id=e.id  ");
                sql.append("and ed.parent_entity_id=? ");

                stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                stmt.setFetchSize(Integer.MIN_VALUE);
                
                stmt.setLong(1, entityId);
                
                rs = stmt.executeQuery();
                while (rs.next()) {
                    Long id = rs.getBigDecimal(1).longValue();
                    
                    BigDecimal parentEntityBI = rs.getBigDecimal(2);
                    Long parentEntityId = parentEntityBI==null?null:parentEntityBI.longValue();
                    
                    BigDecimal childEntityBI = rs.getBigDecimal(3);
                    Long childEntityId = childEntityBI==null?null:childEntityBI.longValue();
                    
                    String entityAttrName = rs.getString(4);
                    String ownerKey = rs.getString(5);
                    String value = rs.getString(6);
                    java.util.Date creationDate = rs.getTimestamp(7);
                    java.util.Date updatedDate = rs.getTimestamp(8);
                    Integer orderIndex = rs.getInt(9);
                    
                    Entity parentEntity = parentEntityId==null?null:new Entity(parentEntityId);
                    Entity childEntity = childEntityId==null?null:new Entity(childEntityId);
                    
                    EntityData ed = new EntityData(id, entityAttrName, parentEntity, childEntity, ownerKey, value, creationDate, updatedDate, orderIndex);
                    edMap.put(ed.getParentEntity().getId(), ed);
                }
            }
            catch (SQLException e) {
                throw new DaoException(e);
            }
            finally {
                try {
                    if (rs!=null) rs.close();
                    if (stmt!=null) stmt.close();
                    if (conn!=null) conn.close();   
                }
                catch (Exception e) {
                    log.warn("Error closing JDBC connection",e);
                }
            }
            
            for(Entity entity : childEntities) {
                // We have to evict the entity so that Hibernate does not try to manage our lazy EntityData collection
                session.evict(entity);
                entity.setEntityData(new HashSet<EntityData>(edMap.get(entity.getId())));
            }
            
            return new HashSet(childEntities);
        } 
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Map<Long, String> getChildEntityNames(Long entityId) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getChildEntityNames(entityId="+entityId+")");
        }
        
        try {
            Map<Long,String> nameMap = new LinkedHashMap<Long,String>();
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select ed.childEntity.id, ed.childEntity.name from EntityData ed ");
            hql.append("where ed.parentEntity.id=? ");
            Query query = session.createQuery(hql.toString()).setLong(0, entityId);
            List results = query.list();
            for(Object o : results) {
                Object[] row = (Object[])o;
                nameMap.put((Long)row[0],(String)row[1]);
            }
            return nameMap;
        } 
        catch (Exception e) {
            throw new DaoException(e);
        }
    }
    

    /******************************************************************************************************************/
    /** ENTITY LOADING */
    /******************************************************************************************************************/
    
    @Override
    public Entity populateChildren(Entity entity) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("populateChildren(entity="+entity+")");
        }

        EntityUtils.replaceChildNodes(entity, getChildEntities((String)null, entity.getId()));
        return entity;
    }
    
    public Entity loadLazyEntity(String subjectKey, Entity entity, boolean recurse) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("loadLazyEntity(subjectKey="+subjectKey+", entity="+entity+", recurse="+recurse+")");
        }
        List<String> subjectKeyList = subjectKey==null?null:getSubjectKeys(subjectKey);
        return loadLazyEntity(subjectKeyList, entity, recurse, new HashSet<Long>());
    }

    public Entity loadLazyEntity(List<String> subjectKeyList, Entity entity, boolean recurse, Set<Long> visited) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("loadLazyEntity(subjectKeyList="+subjectKeyList+", entity="+entity+", recurse="+recurse+", visited.size="+visited.size()+")");
        }
        
        if (entity==null) return null;
        if (visited.contains(entity.getId())) return entity;
        visited.add(entity.getId());
        
        if (!EntityUtils.areLoaded(entity.getEntityData())) {
            EntityUtils.replaceChildNodes(entity, getChildEntities(subjectKeyList, entity.getId()));
        }

        if (recurse) {
            for (EntityData ed : entity.getEntityData()) {
                if (ed.getChildEntity() != null) {
                    loadLazyEntity(subjectKeyList, ed.getChildEntity(), true, visited);
                }
            }
        }
        
        return entity;
    }

    
    /******************************************************************************************************************/
    /** ENTITY UPDATES */
    /******************************************************************************************************************/
    
    public void saveOrUpdateEntity(Entity entity) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("saveOrUpdateEntity(entity.id="+entity.getId()+")");    
        }
        entity.setUpdatedDate(new Date());
        saveOrUpdate(entity);
        updateChildCount(entity);
    }

    public void saveOrUpdateEntityData(EntityData entityData) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("saveOrUpdateEntityData(entityData.id="+entityData.getId()+")");    
        }
        entityData.setUpdatedDate(new Date());
        saveOrUpdate(entityData);
        updateChildCount(entityData.getParentEntity());
    }


    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("addEntityToParent(parent="+parent+", entity="+entity+", index="+index+", attrName="+attrName+")");
        }
        
        return addEntityToParent(parent, entity, index, attrName, null);
    }

    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName, String value) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("addEntityToParent(parent="+parent+", entity="+entity+", index="+index+", attrName="+attrName+", value="+value+")");
        }
        if (parent.getId().equals(entity.getId())) {
        	throw new IllegalArgumentException("Cannot add entity to itself: "+parent.getName());
        }
        if (attrName==null) throw new DaoException("Error adding entity child with null attribute name");
        EntityData ed = parent.addChildEntity(entity, attrName);
        ed.setOrderIndex(index);
        if (value!=null) {
            ed.setValue(value);
        }
        saveOrUpdate(ed);
        propagatePermissions(parent, entity, true);
        updateChildCount(parent);
        return ed;
    }

    public void addChildren(String subjectKey, Long parentId, List<Long> childrenIds, String attributeName) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("addChildren(subjectKey="+subjectKey+", parentId="+parentId+", childrenIds.size="+childrenIds.size()+", attributeName="+attributeName+")");
        }
        
        Entity parent = getEntityById(parentId);
        
        Set<Long> existingChildrenIds = new HashSet<Long>();
        for(EntityData entityData : parent.getEntityData()) {
            if (entityData.getChildEntity()!=null) { 
                existingChildrenIds.add(entityData.getChildEntity().getId());
            }
        }
                    
        Date createDate = new Date();
        
        for (Long childId : childrenIds) {
            if (existingChildrenIds.contains(childId)) continue;
            if (childId.equals(parentId)) continue;
            
            Entity child = new Entity();
            child.setId(childId);
            
            EntityData ed = new EntityData();
            ed.setParentEntity(parent);
            ed.setChildEntity(child);
            ed.setOwnerKey(subjectKey);
            ed.setCreationDate(createDate);
            ed.setUpdatedDate(createDate);
            ed.setEntityAttrName(attributeName);
            
            saveOrUpdate(ed);
            parent.getEntityData().add(ed);

            propagatePermissions(parent, child, true);
        }
        
        updateChildCount(parent);
    }
    
    private void updateChildCount(Entity entity) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("updateChildCount(entity.id="+entity.getId()+")");    
        }

        try {   
            Session session = getCurrentSession();
            
            // We need to pull the count back so that we can update the in-memory model
            StringBuffer hql = new StringBuffer("select count(*) from EntityData ed ");
            hql.append("where ed.parentEntity.id=? ");
            hql.append("and ed.childEntity is not null ");
            Query query = session.createQuery(hql.toString()).setLong(0, entity.getId());
            Long count = (Long)query.uniqueResult();
            Integer intCount = count.intValue();
            
            // Update the database
            hql = new StringBuffer("update Entity set numChildren = :numChildren where id = :entityId");
            query = session.createQuery(hql.toString());
            query.setParameter("numChildren", intCount);
            query.setParameter("entityId", entity.getId());
            int rows = query.executeUpdate();
            if (rows!=1) {
                log.warn("Updating numChildren to "+intCount+" for entity "+entity.getId()+" failed. "+rows+" rows were updated.");
            }
            
            // Update in-memory model
            entity.setNumChildren(intCount);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public int bulkUpdateEntityDataValue(String oldValue, String newValue) throws DaoException {
        try {
            if (log.isTraceEnabled()) {
                log.trace("bulkUpdateEntityDataValue(oldValue="+oldValue+",newValue="+newValue+")");  
            }
            
            StringBuilder hql = new StringBuilder();
            hql.append("update EntityData set value = :newValue where value = :oldValue ");
            
            final Session currentSession = getCurrentSession();
            Query query = currentSession.createQuery(hql.toString());
            query.setParameter("newValue", newValue);
            query.setParameter("oldValue", oldValue);
            
            int rows = query.executeUpdate();
            log.debug("Bulk updated entity data value for "+rows+" rows");
            return rows;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public int bulkUpdateEntityDataPrefix(String oldPrefix, String newPrefix) throws DaoException {
        try {
            if (log.isTraceEnabled()) {
                log.trace("bulkUpdateEntityDataPrefix(oldPrefix="+oldPrefix+",newPrefix="+newPrefix+")");  
            }
            
            StringBuilder hql = new StringBuilder();
            hql.append("update EntityData ed set ed.value = concat(:newPrefix,substring(ed.value, :prefixOffset)) "); 
            hql.append("where ed.value like :oldPrefix");
            
            final Session currentSession = getCurrentSession();
            Query query = currentSession.createQuery(hql.toString());
            query.setParameter("newPrefix", newPrefix);
            query.setParameter("prefixOffset", oldPrefix.length()+1);
            query.setParameter("oldPrefix", oldPrefix+"%");
                        
            int rows = query.executeUpdate();
            log.debug("Bulk updated entity data prefix for "+rows+" rows");
            return rows;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    
    /******************************************************************************************************************/
    /** ENTITY DELETION */
    /******************************************************************************************************************/
    
    public boolean deleteEntityById(String subjectKey, Long entityId) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("deleteEntityById(subjectKey="+subjectKey+", entityId="+entityId+")");
        }
        
        Session session = null;
        Transaction transaction = null;
        try {
            session = getCurrentSession();
            transaction = session.beginTransaction();
            Set<EntityData> parentEds = getParentEntityDatas(subjectKey, entityId);
            for(EntityData parentEd : parentEds) {
                deleteEntityData(parentEd);
                log.debug("The parent entity data with id=" + parentEd.getId() + " has been deleted.");
            }
            Entity entity = getEntityById(subjectKey, entityId);
            if (entity!=null && (subjectKey==null || entity.getOwnerKey().equals(subjectKey))) {
                genericDelete(entity);
                log.debug("The entity with id=" + entityId + " has been deleted.");
            }
            else {
                throw new DaoException("Could not find entity with id="+entityId+" owned by "+subjectKey);
            }
            transaction.commit();
            return true;
        }
        catch (Exception e) {
            transaction.rollback();
            throw new DaoException(e);
        }
    }
    
    public void deleteEntityData(EntityData entityData) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("deleteEntityData(entityData.id="+entityData.getId()+")");
        }
        boolean hasChild = entityData.getChildEntity()!=null;
        Entity parent = entityData.getParentEntity();
        if (parent!=null) {
            // We have to manually remove the EntityData from its parent, otherwise we get this error: 
            // "deleted object would be re-saved by cascade (remove deleted object from associations)"
            parent.getEntityData().remove(entityData);
        }
        genericDelete(entityData);
        if (parent!=null && hasChild) {
            updateChildCount(parent);
        }
    }

    public void deleteEntityTree(String subjectKey, Entity entity) throws DaoException {
        
        if (log.isTraceEnabled()) {
            log.trace("deleteSmallEntityTree(subjectKey="+subjectKey+", entity="+entity+")");    
        }
        
        deleteEntityTree(subjectKey, entity, false);
    }
    
    public void deleteEntityTree(String subjectKey, Entity entity, boolean unlinkMultipleParents) throws DaoException {
        
        if (log.isTraceEnabled()) {
            log.trace("deleteSmallEntityTree(subjectKey="+subjectKey+", entity="+entity+", unlinkMultipleParents="+unlinkMultipleParents+")");    
        }
        
        deleteEntityTree(subjectKey, entity, unlinkMultipleParents, 0, new HashSet<Long>());
    }
    
    private void deleteEntityTree(String subjectKey, Entity entity, boolean unlinkMultipleParents, int level, Set<Long> deleted) throws DaoException {

        if (log.isTraceEnabled()) {
            log.trace("deleteSmallEntityTree(subjectKey="+subjectKey+", entity="+entity+", unlinkMultipleParents="+unlinkMultipleParents+", level="+level+", deleted.size="+deleted.size()+")");    
        }
        
        StringBuffer indent = new StringBuffer();
        for (int i = 0; i < level; i++) {
            indent.append("    ");
        }

        // Null check
        if (entity == null) {
            log.warn(indent+"Cannot delete null entity");
            return;
        }

        log.info(indent+"Deleting entity "+entity.getName()+" (id="+entity.getId()+")");
        
        if (deleted.contains(entity.getId())) {
            log.warn(indent+"Cannot delete entity which was already deleted");
            return;
        }
        
        List<String> subjectKeys = getSubjectKeys(subjectKey);
        
        // Permission check - can't delete entities that we don't have write access to
        if (!EntityUtils.hasWriteAccess(entity, subjectKeys)) {
            log.info(indent+"Cannot delete entity because owner ("+entity.getOwnerKey()+") does not match invoker ("+subjectKey+")");
            return;
        }

        // Common root check - can't delete entities that are held in place by virtue of being common roots
        if (level>0 && entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT)!=null) {
            log.info(indent+"Cannot delete "+entity.getName()+" because it is a common root");
            return;
        }
        
        // Multiple parent check - can't delete entities that we have references to elsewhere
        Set<EntityData> eds = getParentEntityDatas(null, entity.getId());
        Set<EntityData> ownedEds = new HashSet<EntityData>();
        for(EntityData ed : eds) {
            if (EntityUtils.isOwner(ed.getParentEntity(), subjectKeys)) {
                ownedEds.add(ed);
            }
        }
        
        boolean moreThanOneParent = ownedEds.size() > 1;
        if (level>0 && moreThanOneParent && !unlinkMultipleParents) {
            log.info(indent+"Cannot delete "+entity.getName()+" because more than one parent is pointing to it");
            return;
        }
        
        // Delete all descendants first
        for(EntityData ed : new ArrayList<EntityData>(entity.getEntityData())) {
            Entity child = ed.getChildEntity();
            if (child != null) {
                deleteEntityTree(subjectKey, child, unlinkMultipleParents, level + 1, deleted);
            }
            log.debug(indent+"Deleting child entity data (id="+ed.getId()+")");
            if (deleted.contains(ed.getId())) {
                log.debug(indent+"EntityData (id="+ed.getId()+") was already deleted in this session");
                continue;
            }
            deleteEntityData(ed);
            deleted.add(ed.getId());
        }
        
        // Delete all parent EDs
        for(EntityData ed : eds) {
            // This ED points to the term to be deleted. We must delete the ED first to avoid violating constraints.
            log.debug(indent+"Deleting parent entity data (id="+ed.getId()+")");
            if (deleted.contains(ed.getId())) {
                log.debug(indent+"EntityData (id="+ed.getId()+") was already deleted in this session");
                continue;
            }
            deleteEntityData(ed);
            deleted.add(ed.getId());
        }
        
        // Finally we can delete the entity itself
        genericDelete(entity);
        deleted.add(entity.getId());
    }
    
    /******************************************************************************************************************/
    /** ENTITY PERMISSIONS */
    /******************************************************************************************************************/

    public EntityActorPermission getEntityActorPermission(Long eapId) {
        if (log.isTraceEnabled()) {
            log.trace("getEntityActorPermission(eapId="+eapId+")");
        }

        StringBuilder hql = new StringBuilder();
        hql.append("select eap from EntityActorPermission eap ");
        hql.append("left outer join fetch eap.entity p ");
        hql.append("where eap.id = :eapId ");
        
        final Session currentSession = getCurrentSession();
        Query query = currentSession.createQuery(hql.toString());
        query.setParameter("eapId", eapId);

        return (EntityActorPermission)query.uniqueResult();
    }
    
    public Set<EntityActorPermission> getFullPermissions(Entity entity) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getFullPermissions(entity="+entity+")");    
        }
        return getFullPermissions(entity.getOwnerKey(), entity.getId());
    }
    
    public Set<EntityActorPermission> getFullPermissions(String subjectKey, Long entityId) throws DaoException {

        if (log.isTraceEnabled()) {
            log.trace("getFullPermissions(subjectKey="+subjectKey+", entityId="+entityId+")");    
        }
        Entity entity = getEntityById(entityId);
        if (entity==null) {
            throw new IllegalArgumentException("Unknown entity: "+entityId);
        }
        // TODO: revisit this later
//        if (subjectKey!=null) {
//            List<String> subjectKeyList = getSubjectKeys(subjectKey);
//            if (!EntityUtils.hasWriteAccess(entity, subjectKeyList)) {
//                throw new DaoException("User "+subjectKey+" does not have the right to view all permissions for "+entity.getId());
//            }
//        }
        
        return entity.getEntityActorPermissions();
    }
    
    public EntityActorPermission grantPermissions(String subjectKey, Long entityId, String granteeKey, String permissions, 
            boolean recursive) throws DaoException {

        if (log.isTraceEnabled()) {
            log.trace("grantPermissions(subjectKey="+subjectKey+", entityId="+entityId+", granteeKey="+granteeKey+", permissions="+permissions+", recursive="+recursive+")");    
        }
        Entity entity = getEntityById(entityId);
        if (entity==null) {
            throw new IllegalArgumentException("Unknown entity: "+entityId);
        }

        if (subjectKey!=null) {
            if (!subjectKey.equals(entity.getOwnerKey())) {
                throw new DaoException("User "+subjectKey+" does not have the right to grant access to "+entity.getId());
            }   
        }
        
        long start = System.currentTimeMillis();
        EntityActorPermission perm = grantPermissions(entity, granteeKey, permissions, recursive);
        long stop = System.currentTimeMillis();
        log.info("grantPermissions took "+(stop-start)+" ms");
        
        return perm;
    }
    
    public EntityActorPermission grantPermissions(final Entity rootEntity, final String granteeKey, 
            final String permissions, boolean recursive) throws DaoException {

        if (log.isTraceEnabled()) {
            log.trace("grantPermissions(rootEntity="+rootEntity+", granteeKey="+granteeKey+", permissions="+permissions+", recursive="+recursive+")");    
        }
        
        Subject subject = getSubjectByNameOrKey(granteeKey);
        if (subject==null) {
            throw new IllegalArgumentException("Unknown subject: "+granteeKey);
        }
        
        if (rootEntity.getOwnerKey().equals(granteeKey)) {
            return null;
        }
        
        try {
            EntityVistationBuilder visitationBuilder = new EntityVistationBuilder(this).startAt(rootEntity);
            visitationBuilder = recursive ? visitationBuilder.descendants() : visitationBuilder.root();
            visitationBuilder.run(new EntityVisitor() {
                @Override
                public void visit(Entity entity) throws Exception {
                    EntityActorPermission existingPerm = null;
                    for(EntityActorPermission eap : entity.getEntityActorPermissions()) {
                        if (eap.getSubjectKey().equals(granteeKey)) {
                            existingPerm = eap;
                            break;
                        }
                    }

                    if (existingPerm != null) {
                        if (!existingPerm.getPermissions().equals(permissions)) {
                            existingPerm.setPermissions(permissions);
                            saveOrUpdate(existingPerm);
                            log.info("Updated "+permissions+" permission to "+granteeKey+" for "+entity.getId());
                        }
                    }
                    else {
                        EntityActorPermission eap = new EntityActorPermission(entity, granteeKey, permissions);
                        entity.getEntityActorPermissions().add(eap);
                        saveOrUpdate(eap);
                        log.info("Granted "+permissions+" permission to "+granteeKey+" for "+entity.getId());
                    }
                }
            });
            
            // Check if the grantee already has a link to the entity
            boolean granteeHasLink = false;
            for(Entity parent : getParentEntities(null, rootEntity.getId())) {
                if (EntityUtils.hasReadAccess(parent, getSubjectKeys(granteeKey))) {
                    granteeHasLink = true;
                    break;
                }
            }
            
            if (!granteeHasLink) {
                // Grantee has no link, so expose it in the Shared Data folder
                Entity sharedDataFolder = null;
                List<Entity> entities = getUserEntitiesByNameAndTypeName(granteeKey, EntityConstants.NAME_SHARED_DATA, EntityConstants.TYPE_FOLDER);
                if (entities != null && !entities.isEmpty()) {
                    sharedDataFolder = entities.get(0);
                }
                
                if (sharedDataFolder==null) {
                    sharedDataFolder = createEntity(granteeKey, EntityConstants.TYPE_FOLDER, EntityConstants.NAME_SHARED_DATA);
                    EntityUtils.addAttributeAsTag(sharedDataFolder, EntityConstants.ATTRIBUTE_COMMON_ROOT);
                    EntityUtils.addAttributeAsTag(sharedDataFolder, EntityConstants.ATTRIBUTE_IS_PROTECTED);
                    saveOrUpdate(sharedDataFolder);
                }
                
                addEntityToParent(sharedDataFolder, rootEntity, sharedDataFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
            }
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
        
        for(EntityActorPermission eap : rootEntity.getEntityActorPermissions()) {
            if (eap.getSubjectKey().equals(granteeKey)) {
                return eap;
            }
        }
        return null;
    }

    private void propagatePermissions(Entity parent, Entity child, boolean recursive) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("propagatePermissions(parent="+parent+", child="+child+", recursive="+recursive+")");
        }
        
        for(EntityActorPermission permission : getFullPermissions(parent)) {
            grantPermissions(child, permission.getSubjectKey(), permission.getPermissions(), recursive);    
        }
        
        if (!parent.getOwnerKey().equals(child.getOwnerKey())) {
            grantPermissions(child, parent.getOwnerKey(), "rw", false);
        }
    }
    
    public void revokePermissions(String subjectKey, Long entityId, String revokeeKey, boolean recursive) throws DaoException {

        if (log.isTraceEnabled()) {
            log.trace("revokePermissions(subjectKey="+subjectKey+", entityId="+entityId+", revokeeKey="+revokeeKey+", recursive="+recursive+")");    
        }
        
        Entity entity = getEntityById(entityId);
        if (entity==null) {
            throw new IllegalArgumentException("Unknown entity: "+entityId);
        }
        
        if (subjectKey!=null) {
            if (!subjectKey.equals(entity.getOwnerKey())) {
                throw new DaoException("User "+subjectKey+" does not have the right to grant access to "+entity.getId());
            }   
        }

        long start = System.currentTimeMillis();
        revokePermissions(entity, entity.getOwnerKey(), revokeeKey, recursive);
        long stop = System.currentTimeMillis();
        log.info("revokePermissions took "+(stop-start)+" ms");
    }
    
    public void revokePermissions(final Entity rootEntity, final String rootOwner, final String revokeeKey, boolean recursive) throws DaoException {

        if (log.isTraceEnabled()) {
            log.trace("revokePermissions(rootEntity="+rootEntity+", rootOwner="+rootOwner+", revokeeKey="+revokeeKey+", recursive="+recursive+")");    
        }
        
        Subject subject = getSubjectByNameOrKey(revokeeKey);
        if (subject==null) {
            throw new IllegalArgumentException("Unknown subject: "+revokeeKey);
        }
        
        if (rootEntity.getOwnerKey().equals(revokeeKey)) {
            throw new IllegalArgumentException("Cannot revoke permission from entity owner");
        }

        try {
            final Set<Long> revokedIds = new HashSet<Long>();
            EntityVistationBuilder visitationBuilder = new EntityVistationBuilder(this).startAt(rootEntity);
            visitationBuilder = recursive ? visitationBuilder.descendants() : visitationBuilder.root();
            visitationBuilder.run(new EntityVisitor() {
                @Override
                public void visit(Entity entity) throws Exception {
                    for(Iterator<EntityActorPermission> i = entity.getEntityActorPermissions().iterator(); i.hasNext(); ) {
                        EntityActorPermission eap = i.next();
                        if (eap.getSubjectKey().equals(revokeeKey)) {
                            i.remove();
                            genericDelete(eap);
                            log.info("Revoked permission to "+revokeeKey+" for "+entity.getId());
                            revokedIds.add(eap.getEntity().getId());
                        }
                    }
                }
            });

            Entity sharedDataFolder = null;
            List<Entity> entities = getUserEntitiesByNameAndTypeName(revokeeKey, EntityConstants.NAME_SHARED_DATA, EntityConstants.TYPE_FOLDER);
            if (entities != null && !entities.isEmpty()) {
                sharedDataFolder = entities.get(0);
            }
            
            if (sharedDataFolder!=null) {
                Set<EntityData> toDelete = new HashSet<EntityData>();
                for(EntityData ed : sharedDataFolder.getEntityData()) {
                    Entity child = ed.getChildEntity();
                    if (child!=null && revokedIds.contains(child.getId())) {
                        toDelete.add(ed);
                    }
                }
                for(EntityData ed : toDelete) {
                    log.info("Removed "+rootEntity.getId()+" from "+revokeeKey+"'s Shared Data");
                    deleteEntityData(ed);
                }   
            }
            
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Entity annexEntityTree(String subjectKey, Long entityId) throws ComputeException {
        if (log.isTraceEnabled()) {
            log.trace("annexEntityTree(subjectKey="+subjectKey+", entityId="+entityId+")");
        }
        
        Entity entity = getEntityById(entityId);
        log.info(subjectKey+" is annexing entity tree starting at "+entity.getName()+
                " (id="+entity.getId()+") from "+entity.getOwnerKey());
        annexEntityTree(subjectKey, entity, "  ");  
        disassociateTreeFromNonOwners(subjectKey, getEntityById(entity.getId()), "  ");
        return entity;
    }
    
    private Entity annexEntityTree(String subjectKey, Entity entity, String indent) throws ComputeException {
        if (log.isTraceEnabled()) {
            log.trace("annexEntityTree(subjectKey="+subjectKey+", entity="+entity+", indent="+indent+")");
        }
        
        if (!entity.getOwnerKey().equals(subjectKey)) {
            log.info(indent+"annexing entity "+entity.getName()+" (id="+entity.getId()+")");
            entity.setOwnerKey(subjectKey);
            saveOrUpdate(entity);
        }
        for(EntityData ed : new ArrayList<EntityData>(entity.getEntityData())) {
            if (!ed.getOwnerKey().equals(entity.getOwnerKey())) {
                ed.setOwnerKey(subjectKey);
                saveOrUpdate(ed);
            }
            if (ed.getChildEntity()!=null) {
                annexEntityTree(subjectKey, ed.getChildEntity(), indent+"  ");
            }
        }
        
        // TODO: move files in filestore?
        
        return entity;
    }
    
    private Entity disassociateTreeFromNonOwners(String subjectKey, Entity entity, String indent) throws ComputeException {
        if (log.isTraceEnabled()) {
            log.trace("disassociateTreeFromNonOwners(subjectKey="+subjectKey+", entity="+entity+", indent="+indent+")");
        }
        
        for(EntityData parentEd : getParentEntityDatas(subjectKey, entity.getId())) {
            if (parentEd.getOwnerKey().equals(subjectKey)) continue;
            log.info(indent+"deleting "+parentEd.getOwnerKey()+"'s link ("+parentEd.getEntityAttrName()+") from entity "+parentEd.getParentEntity().getName()+" to entity "+entity.getName());
            deleteEntityData(parentEd);
        }
        
        // This would be the correct thing to do, but it makes this far too slow.
        // In practice it's probably not needed with the current state of the data, but maybe it will be in the future.
//      loadLazyEntity(subjectKey, entity, false);
//      for(EntityData ed : new ArrayList<EntityData>(entity.getEntityData())) {
//          if (ed.getChildEntity()!=null) {
//              disassociateTreeFromNonOwners(subjectKey, ed.getChildEntity(), indent+"  ");
//          }
//      }
        
        return entity;
    }
    
    /******************************************************************************************************************/
    /** ONTOLOGIES */
    /******************************************************************************************************************/

    public Entity createOntologyRoot(String subjectKey, String rootName) throws ComputeException {
        if (log.isTraceEnabled()) {
            log.trace("createOntologyRoot(subjectKey="+subjectKey+", rootName="+rootName+")");
        }
        
        Entity newOntologyRoot = newEntity(EntityConstants.TYPE_ONTOLOGY_ROOT, rootName, subjectKey);
        saveOrUpdate(newOntologyRoot);

        // Add the type
        newOntologyRoot.setValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE, Category.class.getSimpleName());
        saveOrUpdate(newOntologyRoot);
        
        return newOntologyRoot;
    }

    public EntityData createOntologyTerm(String subjectKey, Long ontologyTermParentId, String termName, OntologyElementType type, Integer orderIndex) throws ComputeException {
        if (log.isTraceEnabled()) {
            log.trace("createOntologyTerm(subjectKey="+subjectKey+", ontologyTermParentId="+ontologyTermParentId+", termName="+termName+", type="+type+", orderIndex="+orderIndex+")");
        }
        
        Entity parentOntologyElement = getEntityById(ontologyTermParentId);

        // You need write access to the parent ontology to make a new element in this ontology
        List<String> subjectKeyList = getSubjectKeys(subjectKey);
        if (!EntityUtils.hasWriteAccess(parentOntologyElement, subjectKeyList)) {
            throw new ComputeException(subjectKey+" has no write access to ontology term "+ontologyTermParentId);
        }
        
        // The new term will be owned by the ontology owner, even if someone else is creating it
        String newTermOwner = parentOntologyElement.getOwnerKey();
        
        // Create and save the new entity
        Entity newOntologyElement = newEntity(EntityConstants.TYPE_ONTOLOGY_ELEMENT, termName, newTermOwner);

        // If no order index is given then we add in last place
        if (orderIndex == null) {
            int max = 0;
            for(EntityData data : parentOntologyElement.getEntityData()) {
                if (data.getOrderIndex() != null && data.getOrderIndex() > max) max = data.getOrderIndex(); 
            }
            orderIndex = max + 1;
        }

        Set<EntityData> eds = new HashSet<EntityData>();
        newOntologyElement.setEntityData(eds);
        
        // Add the type
        EntityData termData = newData(newOntologyElement, EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE, newTermOwner);
        termData.setValue(type.getClass().getSimpleName());
        eds.add(termData);

        // Add the type-specific parameters
        if (type instanceof Interval) {

            Interval interval = (Interval)type;

            EntityData lowerData = newData(newOntologyElement, EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_LOWER, newTermOwner);
            lowerData.setValue(interval.getLowerBound().toString());
            eds.add(lowerData);

            EntityData upperData = newData(newOntologyElement, EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_UPPER, newTermOwner);
            upperData.setValue(interval.getUpperBound().toString());
            eds.add(upperData);
        }

        // Add the type-specific parameters
        if (type instanceof EnumText) {
            EnumText enumText = (EnumText)type;
            Entity valueEnumEntity = getEntityById(enumText.getValueEnumId());
            EntityData lowerData = newData(newOntologyElement, EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_ENUMTEXT_ENUMID, newTermOwner);
            lowerData.setValue(enumText.getValueEnumId().toString());
            lowerData.setChildEntity(valueEnumEntity);
            eds.add(lowerData);
        }
        
        // Save the new element
        saveOrUpdate(newOntologyElement);
        
        // Associate the entity to the parent
        EntityData childData = addEntityToParent(parentOntologyElement, newOntologyElement, orderIndex, EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT);
                
        return childData;
    }

    public boolean deleteOntologyTerm(String subjectKey, String ontologyTermId) throws DaoException {
        
        if (log.isTraceEnabled()) {
            log.trace("deleteOntologyTerm(subjectKey="+subjectKey+", ontologyTermId="+ontologyTermId+")");    
        }
        
        Session session = getCurrentSession();
        try {
            Criteria c = session.createCriteria(Entity.class);
            c.add(Expression.eq("id", Long.valueOf(ontologyTermId)));
            Entity entity = (Entity) c.uniqueResult();
            if (null == entity) {
                // This should never happen
                throw new DaoException("Cannot complete deletion when there are no entities with that identifier.");
            }
            if (!entity.getOwnerKey().equals(subjectKey)) {
                throw new DaoException("Cannot delete the entity as the requestor doesn't own the item.");
            }

            log.info("Will delete tree rooted at Entity "+entity.getName());
            deleteEntityTree(subjectKey, entity);
            return true;
        }
        catch (Exception e) {
            log.error("Error deleting ontology term "+ontologyTermId,e);
            throw new DaoException(e);
        }
    }
    
    public void fixInternalOntologyConsistency(Long sourceRootId) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("fixInternalOntologyConsistency(sourceRootId="+sourceRootId+")");
        }
        
        Entity ontologyRoot = getEntityById(sourceRootId);
        ontologyRoot = loadLazyEntity(ontologyRoot.getOwnerKey(), ontologyRoot, true);
        if (ontologyRoot==null) return;
        log.warn("Fixing internal consistency for ontology "+ontologyRoot.getName()+" (id="+ontologyRoot.getId()+")");
        Map<String,Long> enumMap = new HashMap<String,Long>();
        buildEnumMap(ontologyRoot, enumMap);
        updateEnumTexts(ontologyRoot, enumMap);
    }
    
    public void buildEnumMap(Entity entity, Map<String,Long> enumMap) {
        if (log.isTraceEnabled()) {
            log.trace("buildEnumMap(entity="+entity+", enumMap.size="+enumMap.size()+")");
        }
        
        if ("Enum".equals(entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE))) {
            enumMap.put(entity.getName(), entity.getId());
        }
        for(Entity child : entity.getChildren()) {
            buildEnumMap(child, enumMap);
        }
    }

    public void updateEnumTexts(Entity entity, Map<String,Long> enumMap) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("updateEnumTexts(entity="+entity+", enumMap.size="+enumMap.size()+")");
        }
        
        if ("EnumText".equals(entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE))) {
            EntityData oldEnumIdEd = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_ENUMTEXT_ENUMID);
            if (oldEnumIdEd != null && oldEnumIdEd.getValue()!=null) {
                Entity oldEnum = getEntityById(new Long(oldEnumIdEd.getValue()));
                if (oldEnum!=null) {
                    Long newEnumId = enumMap.get(oldEnum.getName());
                    if (newEnumId!=null) {
                        oldEnumIdEd.setValue(newEnumId.toString());
                        oldEnumIdEd.setChildEntity(getEntityById(newEnumId));
                        log.warn("Updating EnumText "+entity.getName()+" to reference the correct Enum id="+newEnumId);
                        saveOrUpdate(oldEnumIdEd);
                    }
                    else {
                        log.warn("Cannot find enum with name "+oldEnum.getName()+" in ontology");
                    }
                }
                else {
                    log.warn("Cannot find old EnumText entity with id="+oldEnumIdEd.getValue());
                }
            }
            else {
                log.warn("EnumText (id="+entity.getId()+") does not reference an Enum");
            }
        }
        for(Entity child : entity.getChildren()) {
            updateEnumTexts(child, enumMap);
        }
    }
    
    public Entity getErrorOntology() throws ComputeException {

        if (log.isTraceEnabled()) {
            log.trace("getErrorOntology()");
        }
        
        String owner = "group:flylight";
        
        // TODO: this ontology should be owned by the system user
        List<Entity> list = getEntitiesByNameAndTypeName(owner, "Error Ontology", EntityConstants.TYPE_ONTOLOGY_ROOT);
        if (list.isEmpty()) {
            throw new ComputeException("Cannot find Error Ontology");
        }
        else if (list.size()>1) {
            log.warn("Found more than one Error Ontology, using the first one, "+list.get(0).getId());
        }
        Entity root = list.get(0);
        loadLazyEntity(owner, root, true);
        return root;
    }
    
    /******************************************************************************************************************/
    /** ANNOTATIONS */
    /******************************************************************************************************************/

    public Entity createOntologyAnnotation(String subjectKey, OntologyAnnotation annotation) throws ComputeException {
        if (log.isTraceEnabled()) {
            log.trace("createOntologyAnnotation(subjectKey="+subjectKey+", annotation="+annotation+")");
        }

        try {           
            Entity keyEntity = null;
            boolean isCustom = false;
            
            if (annotation.getKeyEntityId() != null) {
                keyEntity = getEntityById(annotation.getKeyEntityId());
                String termType = keyEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE);
                
                isCustom = keyEntity!=null && "Custom".equals(termType);
                if (isCustom && StringUtils.isEmpty(annotation.getValueString())) {
                    throw new ComputeException("Value cannot be empty for custom annotation");
                }

                if (!"Text".equals(termType) && !"Custom".equals(termType) && !"Tag".equals(termType)) {
                    // Non-text annotations are exclusive, so delete existing annotations first
                    List<Entity> existingAnnotations = getAnnotationsByEntityId(subjectKey, annotation.getTargetEntityId());
                    for(Entity existingAnnotation : existingAnnotations) {
                        EntityData eaKeyEd = existingAnnotation.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID);
                        if (eaKeyEd==null) continue;
                        if (eaKeyEd.getChildEntity().getId().equals(annotation.getKeyEntityId())) {
                            deleteEntityById(subjectKey, existingAnnotation.getId());
                        }
                    }
                }
            }
            
            String tag = isCustom ? annotation.getValueString() : 
                        (annotation.getValueString()==null ? annotation.getKeyString() : 
                         annotation.getKeyString() + " = " + annotation.getValueString());
            
            Entity newAnnotation = newEntity(EntityConstants.TYPE_ANNOTATION, tag, subjectKey);
            
            Set<EntityData> eds = new HashSet<EntityData>();
            newAnnotation.setEntityData(eds);
            
            // Add the target id
            EntityData targetIdData = newData(newAnnotation, 
                    EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID, subjectKey);
            targetIdData.setValue(""+annotation.getTargetEntityId());
            eds.add(targetIdData);
                
            // Add the key string
            EntityData keyData = newData(newAnnotation,
                    EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_TERM, subjectKey);
            keyData.setValue(annotation.getKeyString());
            eds.add(keyData);

            // Add the value string
            EntityData valueData = newData(newAnnotation,
                    EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_TERM, subjectKey);
            valueData.setValue(annotation.getValueString());
            eds.add(valueData);
            
            // Add the key entity
            if (annotation.getKeyEntityId() != null) {
                EntityData keyEntityData = newData(newAnnotation,
                        EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID, subjectKey);
                keyEntityData.setChildEntity(keyEntity);
                keyEntityData.setValue(""+keyEntity.getId());
                eds.add(keyEntityData);
            }

            // Add the value entity
            if (annotation.getValueEntityId() != null) {
                Entity valueEntity = getEntityById(annotation.getValueEntityId());
                EntityData valueEntityData = newData(newAnnotation,
                        EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_ENTITY_ID, subjectKey);
                valueEntityData.setChildEntity(valueEntity);
                valueEntityData.setValue(""+valueEntity.getId());
                eds.add(valueEntityData);
            }
            
            // Add the session id
            if (annotation.getSessionId() != null) {
                EntityData sessionIdData = newData(newAnnotation,
                        EntityConstants.ATTRIBUTE_ANNOTATION_SESSION_ID, subjectKey);
                sessionIdData.setValue(""+annotation.getSessionId());
                eds.add(sessionIdData);
            }

            // Add the is computational flag
            if (annotation.getIsComputational() != null && annotation.getIsComputational()) {
                EntityData keyEntityData = newData(newAnnotation,
                        EntityConstants.ATTRIBUTE_ANNOTATION_IS_COMPUTATIONAL, subjectKey);
                keyEntityData.setValue(EntityConstants.ATTRIBUTE_ANNOTATION_IS_COMPUTATIONAL);
                eds.add(keyEntityData);
            }
            
            saveOrUpdate(newAnnotation);
            
            // Notify the session 
            if (annotation.getSessionId() != null) {
                updateAnnotationSession(annotation.getSessionId());
            }
            
            return newAnnotation;
        }
        catch (Exception e) {
            log.error("Error creating ontology annotation for subject "+subjectKey,e);
            throw new ComputeException("Error creating ontology annotation for subject "+subjectKey,e);
        }
    }

    public Long removeOntologyAnnotation(String subjectKey, long annotationId) throws ComputeException {
        if (log.isTraceEnabled()) {
            log.trace("removeOntologyAnnotation(subjectKey="+subjectKey+", annotationId="+annotationId+")");
        }
        
        Entity entity = getEntityById(subjectKey, annotationId);
        genericDelete(entity);  
        
        // Notify the session 
        String sessionId = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_SESSION_ID);
        if (sessionId != null) updateAnnotationSession(Long.parseLong(sessionId));
        
        return new Long(entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID));
    }
    
    public List<Entity> getAnnotationsForChildren(String subjectKey, Long entityId) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getAnnotationsForChildren(subjectKey="+subjectKey+", entityId="+entityId+")");
        }
        
        try {    
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select distinct targetEd.parentEntity from EntityData targetEd, EntityData childEd ");
            hql.append("join targetEd.parentEntity ");
            hql.append("left outer join fetch targetEd.parentEntity.entityActorPermissions p ");
            hql.append("where targetEd.entityAttrName = :attrName ");	
            hql.append("and childEd.childEntity.id is not null ");
            hql.append("and cast(childEd.childEntity.id as string) = targetEd.value ");
            hql.append("and childEd.parentEntity.id = :entityId ");
            if (null != subjectKey) {
            	hql.append("and (targetEd.parentEntity.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            
            Query query = session.createQuery(hql.toString());
            query.setString("attrName", EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
        	query.setLong("entityId", entityId);
            if (null != subjectKey) {
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
	            query.setParameterList("subjectKeyList", subjectKeyList);
            }
            
            return filter(query.list());
        } 
        catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public List<Entity> getAnnotationsByEntityId(String subjectKey, Long entityId) throws DaoException {
    	if (log.isTraceEnabled()) {
    		log.trace("getAnnotationsByEntityId(subjectKey="+subjectKey+", entityId="+entityId+")");
    	}
    	
    	List<Long> entityIds = new ArrayList<Long>();
    	entityIds.add(entityId);
    	return filter(getAnnotationsByEntityId(subjectKey, entityIds));
    }
    
    public List<Entity> getAnnotationsByEntityId(String subjectKey, List<Long> entityIds) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getAnnotationsByEntityId(subjectKey="+subjectKey+",entityIds.size="+entityIds.size()+")");
        }
        
        try {
        	
        	if (entityIds.isEmpty()) {
        		return new ArrayList<Entity>();
        	}

            List<String> entityIdStrs = new ArrayList<String>();
        	for(Long id : entityIds) {
        		entityIdStrs.add(""+id);
        	}
            
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select ed.parentEntity from EntityData ed ");
            hql.append("join ed.parentEntity ");
            hql.append("left outer join fetch ed.parentEntity.entityActorPermissions p ");
            hql.append("where ed.entityAttrName = :attrName ");
            hql.append("and ed.value in (:entityIds) ");
            if (subjectKey!=null) {
            	hql.append("and (ed.parentEntity.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            hql.append("order by ed.parentEntity.id ");
            
            Query query = session.createQuery(hql.toString());
            query.setString("attrName", EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
        	query.setParameterList("entityIds", entityIdStrs);
            if (subjectKey!=null) {
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
	            query.setParameterList("subjectKeyList", subjectKeyList);
            }
            
            return filter(query.list());
        } 
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public List<Long> getOrphanAnnotationIdsMissingTargets(String subjectKey) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getOrphanAnnotationIdsMissingTargets(subjectKey="+subjectKey+")");
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        List<Long> annotationIds = new ArrayList<Long>();
        
        try {
            StringBuffer sql = new StringBuffer("select a.id from entity a ");
            sql.append("join entityData aed on aed.parent_entity_id=a.id and aed.entity_att = ? ");
            sql.append("left outer join entity target on aed.value=target.id ");
            sql.append("where a.entity_type = ? ");
            sql.append("and target.id is null ");
            if (subjectKey!=null) {
                sql.append("and a.ownerKey = ? ");
            }
            
            conn = getJdbcConnection();
            stmt = conn.prepareStatement(sql.toString());
            stmt.setString(1, EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
            stmt.setString(2, EntityConstants.TYPE_ANNOTATION);
            if (subjectKey!=null) {
                stmt.setString(3, subjectKey);
            }
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                annotationIds.add(rs.getBigDecimal(1).longValue());
            }
        }
        catch (SQLException e) {
            throw new DaoException(e);
        }
        finally {
            try {
                if (stmt!=null) stmt.close();
                if (conn!=null) conn.close(); 
            }
            catch (SQLException e) {
                log.warn("Ignoring error encountered while closing JDBC connection",e);
            }
        }
        
        return annotationIds;
    }

    /******************************************************************************************************************/
    /** ANNOTATION SESSIONS */
    /******************************************************************************************************************/
    
    public List<Task> getAnnotationSessions(String subjectKey) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getAnnotationSessions(subjectKey="+subjectKey+")");
        }
        
        try {
            String hql = "select clazz from Task clazz where subclass='" + AnnotationSessionTask.TASK_NAME + "' and clazz.owner='" + subjectKey + "' order by clazz.objectId";
            Query query = sessionFactory.getCurrentSession().createQuery(hql);
            return query.list();
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }

	public List<Entity> getAnnotationsForSession(String subjectKey, long sessionId) throws DaoException {

        if (log.isTraceEnabled()) {
            log.trace("getAnnotationsForSession(subjectKey="+subjectKey+",sessionId="+sessionId+")");
        }
        
        Task task = getTaskById(sessionId);
        if (task == null) 
            throw new DaoException("Session not found");
        
		if (!(task instanceof AnnotationSessionTask)) 
			throw new DaoException("Task is not an annotation session");
		
        try {
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select ed.parentEntity from EntityData ed ");
            hql.append("join ed.parentEntity ");
            hql.append("left outer join fetch ed.parentEntity.entityActorPermissions p ");
            hql.append("where ed.entityAttrName = :attrName ");
    		hql.append("and ed.value = :sessionId ");
            if (subjectKey!=null) {
            	hql.append("and (ed.parentEntity.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
    		hql.append("order by ed.parentEntity.id ");
            Query query = session.createQuery(hql.toString());
            query.setString("attrName", EntityConstants.ATTRIBUTE_ANNOTATION_SESSION_ID);
            query.setString("sessionId", ""+sessionId);
            if (subjectKey!=null) {
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
	            query.setParameterList("subjectKeyList", subjectKeyList);
            }
            // TODO: check userLogin if the session is private
            return filter(query.list());
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
	}
    
    public List<Entity> getEntitiesForAnnotationSession(String subjectKey, long sessionId) throws ComputeException {

        if (log.isTraceEnabled()) {
            log.trace("getEntitiesForAnnotationSession(subjectKey="+subjectKey+",sessionId="+sessionId+")");
        }
        
        Task task = getTaskById(sessionId);
        if (task == null) 
            throw new DaoException("Session not found");
        
		if (!(task instanceof AnnotationSessionTask)) 
			throw new DaoException("Task is not an annotation session");
    	
        String entityIds = task.getParameter(AnnotationSessionTask.PARAM_annotationTargets);
        if (entityIds == null || "".equals(entityIds)) {
        	return new ArrayList<Entity>();
        }
        else {
        	List<Entity> entities = getEntitiesInList(subjectKey, entityIds);	
        	for(Entity entity : entities) {
        		loadLazyEntity(entity.getOwnerKey(), entity, true);
        	}
        	return filter(entities);
        }
    }
	
    public List<Entity> getCategoriesForAnnotationSession(String subjectKey, long sessionId) throws ComputeException {

        if (log.isTraceEnabled()) {
            log.trace("getCategoriesForAnnotationSession(subjectKey="+subjectKey+",sessionId="+sessionId+")");
        }
        
        Task task = getTaskById(sessionId);
        if (task == null) 
            throw new DaoException("Session not found");
        
		if (!(task instanceof AnnotationSessionTask)) 
			throw new DaoException("Task is not an annotation session");
    	
        String entityIds = task.getParameter(AnnotationSessionTask.PARAM_annotationCategories);
        if (entityIds == null || "".equals(entityIds)) {
        	return new ArrayList<Entity>();
        }
        else {
        	return filter(getEntitiesInList(subjectKey, entityIds));	
        }
    }

    public Set<Long> getCompletedEntityIds(long sessionId) throws ComputeException {

        if (log.isTraceEnabled()) {
            log.trace("getCompletedEntityIds(sessionId="+sessionId+")");
        }
        
        Task task = getTaskById(sessionId);
        if (task == null) 
            throw new DaoException("Session not found");
        
		if (!(task instanceof AnnotationSessionTask)) 
			throw new DaoException("Task is not an annotation session");
        
        Set<Long> completedEntityIds = new HashSet<Long>();
        String entityIds = task.getParameter(AnnotationSessionTask.PARAM_completedTargets);
        if (entityIds == null || "".equals(entityIds)) return completedEntityIds;
        
        for(String id : entityIds.split("\\s*,\\s*")) {
			try {
				completedEntityIds.add(Long.parseLong(id));
			} 
			catch (NumberFormatException e) {
				log.warn("Error parsing id in AnnotationSessionTask.PARAM_completedTargets: "+id,e);
			}
        }
        
        return completedEntityIds;
    }
    
    /**
     * Updates the given session and returns all the annotations within it. 
     * @param sessionId
     * @throws ComputeException
     */
	private List<Entity> updateAnnotationSession(long sessionId) throws ComputeException {

        if (log.isTraceEnabled()) {
            log.trace("updateAnnotationSession(sessionId="+sessionId+")");
        }
        
		Task task = getTaskById(sessionId);
        if (task == null) 
            throw new DaoException("Session not found");
        
		if (!(task instanceof AnnotationSessionTask)) 
			throw new DaoException("Task is not an annotation session");
		
		List<Entity> categories = getCategoriesForAnnotationSession(task.getOwner(), sessionId);
		List<Entity> annotations = getAnnotationsForSession(task.getOwner(), sessionId);
	
        Map<String, List<Entity>> map = new HashMap<String, List<Entity>>();
        for (Entity annotation : annotations) {
            EntityData ed = annotation.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
            if (ed == null) continue;
            String entityId = ed.getValue();
            List<Entity> entityAnnots = map.get(entityId);
            if (entityAnnots == null) {
                entityAnnots = new ArrayList<Entity>();
                map.put(entityId, entityAnnots);
            }
            entityAnnots.add(annotation);
        }
	    
        Set<String> completed = new HashSet<String>();
        
        for(String entityId : map.keySet()) {
        	List<Entity> entityAnnotations = map.get(entityId);
        	
        	Set<Long> termIds = new HashSet<Long>();

			for(Entity annotation : entityAnnotations) {
				EntityData keyTermED = annotation.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID);
				if (keyTermED==null) continue;
				Entity keyTerm = keyTermED.getChildEntity();
				if (keyTerm==null) continue;
				Long termId = keyTerm.getId();
    			String termType = keyTerm.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE);
				
				if ("Tag".equals(termType)) {
					Entity parent = null;
					for(Entity p : getParentEntities(null, termId)) {
						String parentTypeName = p.getEntityTypeName();
						if (parentTypeName.equals(EntityConstants.TYPE_ONTOLOGY_ELEMENT) 
								|| parentTypeName.equals(EntityConstants.TYPE_ONTOLOGY_ROOT)) {
							parent = p;
							break;
						}
					}
					if (parent != null) termId = parent.getId();
				}
				
				termIds.add(termId);
			}
        	
        	
        	// Ensure this entity is annotated with a term in each category
        	int c = 0;
    		for(Entity category : categories) {
				if (termIds.contains(category.getId())) {
					c++;
				}
    		}	
    		
    		if (c == categories.size()) {
    			completed.add(entityId);
    		}
        }
        
        StringBuffer buf = new StringBuffer();
        for(String entityId : completed) {
        	if (buf.length()>0) buf.append(",");
        	buf.append(entityId);
        }
		
        task.setParameter(AnnotationSessionTask.PARAM_completedTargets, buf.toString());
        saveOrUpdate(task);
        
        return annotations;
	}

	/**
	 * Removes all annotations in the given session and then returns them.
	 * @param userLogin
	 * @param sessionId
	 * @return
	 * @throws DaoException
	 */
    public List<Entity> removeAllOntologyAnnotationsForSession(String subjectKey, long sessionId) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("removeAllOntologyAnnotationsForSession(subjectKey="+subjectKey+", sessionId="+sessionId+")");
        }

        try {
        	List<Entity> annotations = getAnnotationsForSession(subjectKey, sessionId);
            for(Object o : annotations) {
                Entity entity = (Entity)o;
                if (!entity.getOwnerKey().equals(subjectKey)) {
                	log.info("Cannot remove annotation "+entity.getId()+" not owned by "+subjectKey);
                }
                else {
                	log.info("Removing annotation "+entity.getId());
                	genericDelete(entity);	
                }
            }
            // Notify the session 
            updateAnnotationSession(sessionId);
            return annotations;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    /******************************************************************************************************************/
    /** PATTERN ANNOTATION */
    /******************************************************************************************************************/
    
    public Map<Entity, Map<String, Double>> getPatternAnnotationQuantifiers() throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getPatternAnnotationQuantifiers()");
        }
        
        log.info("getPatternQuantifiersForScreenSample: starting search for entities of type="+EntityConstants.TYPE_SCREEN_SAMPLE);
        List<Entity> flyScreenSampleEntityList = getUserEntitiesByTypeName(null, EntityConstants.TYPE_SCREEN_SAMPLE);
        log.info("getPatternQuantifiersForScreenSample: found "+flyScreenSampleEntityList.size()+" entities of type="+EntityConstants.TYPE_SCREEN_SAMPLE);
        Map<Entity, Map<String, Double>> entityQuantMap=new HashMap<Entity, Map<String, Double>>();
        long count=0;
        for (Entity screenSample : flyScreenSampleEntityList) {
            // Get the file-path for the quantifier file
            log.info("Exploring screenSample name="+screenSample.getName() + " index="+count+" of "+flyScreenSampleEntityList.size());
            Set<Entity> children=screenSample.getChildren();
            for (Entity child : children) {
               // _logger.info("Child id="+child.getId()+" type="+child.getEntityTypeName()+" name="+child.getName());
                if (child.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER) &&
                        child.getName().toLowerCase().equals("pattern annotation")) {
                    Set<Entity> children2=child.getChildren();
                    for (Entity child2 : children2) {
                       // _logger.info("Child2 id="+child2.getId()+" type="+child2.getEntityTypeName()+" name="+child2.getName());
                        if (child2.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER) &&
                                child2.getName().toLowerCase().equals("supportingfiles")) {
                            Set<Entity> children3=child2.getChildren();
                            for (Entity child3 : children3) {
                                //_logger.info("Child3 id="+child3.getId()+" type="+child3.getEntityTypeName()+" name="+child3.getName());
                                if (child3.getEntityTypeName().equals(EntityConstants.TYPE_TEXT_FILE) && child3.getName().endsWith("quantifiers.txt")) {
                                    String quantifierFilePath=child3.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                                    log.info("Quantifier file path="+quantifierFilePath);
                                    File quantFile=new File(quantifierFilePath);
                                    Map<String, Double> quantMap=new HashMap<String, Double>();
                                    try {
                                        BufferedReader br=new BufferedReader(new FileReader(quantFile));
                                        String currentLine="";
                                        while((currentLine=br.readLine())!=null) {
                                            String[] kv=currentLine.split("=");
                                            if (kv.length==2) {
                                                Double d=new Double(kv[1].trim());
                                                quantMap.put(kv[0], d);
                                            }
                                        }
                                        br.close();
                                        log.info("Added "+quantMap.size()+" entries");
                                        entityQuantMap.put(screenSample, quantMap);
                                    } catch (Exception ex) {
                                        throw new DaoException(ex);
                                    }

                                }
                            }
                        }
                    }
                }
            }
            count++;
        }
        return entityQuantMap;
    }


    public Map<Entity, Map<String, Double>> getMaskQuantifiers(String maskFolderName) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getMaskQuantifiers(maskFolderName="+maskFolderName+")");
        }
        
        log.info("getMaskQuantifiers() folder name=" + maskFolderName + " : starting search for entities of type=" + EntityConstants.TYPE_SCREEN_SAMPLE);
        List<Entity> flyScreenSampleEntityList = getUserEntitiesByTypeName(null, EntityConstants.TYPE_SCREEN_SAMPLE);
        log.info("getPatternQuantifiersForScreenSample: found " + flyScreenSampleEntityList.size() + " entities of type=" + EntityConstants.TYPE_SCREEN_SAMPLE);
        Map<Entity, Map<String, Double>> entityQuantMap = new HashMap<Entity, Map<String, Double>>();
        long count = 0;
        for (Entity screenSample : flyScreenSampleEntityList) {
            // Get the file-path for the quantifier file
            log.info("Exploring screenSample name=" + screenSample.getName() + " index=" + count + " of " + flyScreenSampleEntityList.size());
            Set<Entity> topChildren = screenSample.getChildren();
            for (Entity topChild : topChildren) {
                if (topChild.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER) &&
                        topChild.getName().equals(MaskSampleAnnotationService.MASK_ANNOTATION_FOLDER_NAME)) {
                    Set<Entity> children = topChild.getChildren();
                    for (Entity child : children) {
                        if (child.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER) &&
                                child.getName().equals(maskFolderName)) {
                            Set<Entity> children2 = child.getChildren();
                            for (Entity child2 : children2) {
                                // _logger.info("Child2 id="+child2.getId()+" type="+child2.getEntityTypeName()+" name="+child2.getName());
                                if (child2.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER) &&
                                        child2.getName().toLowerCase().equals("supportingfiles")) {
                                    Set<Entity> children3 = child2.getChildren();
                                    for (Entity child3 : children3) {
                                        //_logger.info("Child3 id="+child3.getId()+" type="+child3.getEntityTypeName()+" name="+child3.getName());
                                        if (child3.getEntityTypeName().equals(EntityConstants.TYPE_TEXT_FILE) && child3.getName().endsWith("quantifiers.txt")) {
                                            String quantifierFilePath = child3.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                                            log.info("Quantifier file path=" + quantifierFilePath);
                                            File quantFile = new File(quantifierFilePath);
                                            Map<String, Double> quantMap = new HashMap<String, Double>();
                                            try {
                                                BufferedReader br = new BufferedReader(new FileReader(quantFile));
                                                String currentLine = "";
                                                while ((currentLine = br.readLine()) != null) {
                                                    String[] kv = currentLine.split("=");
                                                    if (kv.length == 2) {
                                                        Double d = new Double(kv[1].trim());
                                                        quantMap.put(kv[0], d);
                                                    }
                                                }
                                                br.close();
                                                log.info("Added " + quantMap.size() + " entries");
                                                entityQuantMap.put(screenSample, quantMap);
                                            }
                                            catch (Exception ex) {
                                                throw new DaoException(ex);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            count++;
        }
        return entityQuantMap;
    }


    // This method returns two objects,  Map<Long, Map<String, String>> sampleInfoMap, Map<Long, List<Double>> quantifierInfoMap
    public Object[] getPatternAnnotationQuantifierMapsFromSummary() throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getPatternAnnotationQuantifierMapsFromSummary()");
        }
        
        try {
            //Object[] mapObjects = PatternAnnotationDataManager.loadPatternAnnotationQuantifierSummaryFile();
            //return mapObjects;
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new DaoException(ex);
        }
    }

    public Object[] getMaskQuantifierMapsFromSummary(String maskFolderName) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getMaskQuantifierMapsFromSummary(maskFolderName="+maskFolderName+")");
        }
        
        try {
            MaskAnnotationDataManager maskManager=new MaskAnnotationDataManager();
            String resourceDirString= SystemConfigurationProperties.getString("FileStore.CentralDir")+
                    SystemConfigurationProperties.getString("MaskSampleAnnotation.ResourceDir");
            String quantifierSummaryFilename= SystemConfigurationProperties.getString("FlyScreen.PatternAnnotationQuantifierSummaryFile");
            File summaryFile=new File(resourceDirString + File.separator+maskFolderName, quantifierSummaryFilename);
            File nameIndexFile=new File(resourceDirString + File.separator+maskFolderName, "maskNameIndex.txt");
            maskManager.loadMaskCompartmentList(nameIndexFile.toURI().toURL());
            Object[] mapObjects=maskManager.loadMaskSummaryFile(summaryFile.toURI().toURL());
            return mapObjects;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new DaoException(ex);
        }
    }

    public PatternAnnotationDataManager getPatternAnnotationDataManagerByType(String type) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getPatternAnnotationDataManagerByType(type="+type+")");
        }
        
        try {
            PatternAnnotationDataManager dataManager=null;
            if (type.equals(RelativePatternAnnotationDataManager.RELATIVE_TYPE)) {
                dataManager=new RelativePatternAnnotationDataManager();
                dataManager.setup();
                return dataManager;
            } else {
                throw new Exception("Do not recognize PatternAnnotationDataManager type="+type);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new DaoException(ex);
        }
    }

    /******************************************************************************************************************/
    /** DATA SETS */
    /******************************************************************************************************************/
    
    public Entity createDataSet(String subjectKey, String dataSetName) throws ComputeException {

        if (log.isTraceEnabled()) {
            log.trace("createDataSet(subjectKey="+subjectKey+", dataSetName="+dataSetName+")");
        }
        
        String dataSetIdentifier = EntityUtils.createDenormIdentifierFromName(subjectKey, dataSetName);
        
        if (!getUserEntitiesWithAttributeValue(null, EntityConstants.TYPE_DATA_SET, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier).isEmpty()) {
            throw new ComputeException("Data Set with identifier '"+dataSetIdentifier+"' already exists.");
        }
        
        Entity newDataSet = newEntity(EntityConstants.TYPE_DATA_SET, dataSetName, subjectKey);
        saveOrUpdate(newDataSet);

        EntityData dataSetIdEd = newData(newDataSet, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, subjectKey);
        newDataSet.getEntityData().add(dataSetIdEd);
        dataSetIdEd.setValue(dataSetIdentifier);
        saveOrUpdate(dataSetIdEd);

        return newDataSet;
    }
    
    /******************************************************************************************************************/
    /** ALIGNMENT BOARD */
    /******************************************************************************************************************/
    
    public Entity createAlignmentBoard(String subjectKey, String alignmentBoardName, String alignmentSpace, String opticalRes, String pixelRes) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("createAlignmentBoard(subjectKey="+subjectKey+", alignmentBoardName="+alignmentBoardName+", alignmentSpace="+alignmentSpace+", opticalRes="+opticalRes+", pixelRes="+pixelRes+")");  
        }
        
        Entity board = newEntity(EntityConstants.TYPE_ALIGNMENT_BOARD, alignmentBoardName, subjectKey);
        board.setValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE, alignmentSpace);
        board.setValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, opticalRes);
        board.setValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION, pixelRes);
        saveOrUpdate(board);

        Entity alignmentBoardFolder = getCommonRootFolderByName(subjectKey, EntityConstants.NAME_ALIGNMENT_BOARDS, true);
        if (alignmentBoardFolder.getValueByAttributeName(EntityConstants.ATTRIBUTE_IS_PROTECTED)==null) {
            EntityUtils.addAttributeAsTag(alignmentBoardFolder, EntityConstants.ATTRIBUTE_IS_PROTECTED);
            saveOrUpdate(alignmentBoardFolder);
        }
        
        addEntityToParent(alignmentBoardFolder, board, alignmentBoardFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
        
        return board;
    }

    public EntityData addAlignedItem(Entity parentEntity, Entity child, String alignedItemName, boolean visible) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("addAlignedItem(parentEntity="+parentEntity+", child="+child+", alignedItemName="+alignedItemName+", visible="+visible+")");
        }
        
        Entity alignedItemEntity = newEntity(EntityConstants.TYPE_ALIGNED_ITEM, alignedItemName, parentEntity.getOwnerKey());
        alignedItemEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_VISIBILITY, new Boolean(visible).toString());
        saveOrUpdate(alignedItemEntity);
        
        addEntityToParent(alignedItemEntity, child, alignedItemEntity.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
        
        return addEntityToParent(parentEntity, alignedItemEntity, parentEntity.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ITEM);
    }

    private Entity filter(Object obj) {
        return filter(obj, true);
    }

    private Entity filter(Object obj, boolean loadEntityData) {
        if (obj==null) return null;
        Entity entity = (Entity)obj;
        if (loadEntityData && entity.getEntityData()!=null) {
            entity.getEntityData().size(); // load entity data
        }
        entity.getEntityActorPermissions().size(); // ensure permissions are loaded
        return entity;
    }
    
    private List filter(List list) {
        return filter(list, true);
    }
    
    private List filter(List list, boolean loadEntityData) {
        List filtered = ImmutableSet.copyOf(list).asList();
        if (loadEntityData) {
            for(Object obj : filtered) {
                filter(obj, true);
            }
        }
        return filtered;
    }
}
