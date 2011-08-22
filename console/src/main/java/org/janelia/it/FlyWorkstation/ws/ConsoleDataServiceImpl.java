/*
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/4/11
 * Time: 1:08 PM
 */
package org.janelia.it.FlyWorkstation.ws;

import java.util.List;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.ExternalClient;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;


/**
 * The Console server interface for clients to call in order to request data. 
 * 
 * @author saffordt
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@WebService(endpointInterface="org.janelia.it.FlyWorkstation.ws.ConsoleDataService",
			serviceName="ConsoleDataService",	
			portName="CdsPort", name="Cds")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class ConsoleDataServiceImpl {

	public int reservePort(String clientName) {
        int port = SessionMgr.getSessionMgr().addExternalClient(clientName);
		System.out.println("Reserving port "+port+" for client "+clientName);
		return port;
	}
	
	public void registerClient(int port, String endpointUrl) throws Exception {
    	ExternalClient client = SessionMgr.getSessionMgr().getExternalClientByPort(port);
    	client.init(endpointUrl);
		System.out.println("Initialized client on port "+port+" with endpoint "+endpointUrl);
    }

//    public void deleteAnnotation(Long annotatedEntityId, String tag) {
//        ModelMgr.getModelMgr().deleteAnnotation(annotatedEntityId, tag);
//    }
//
//    public List<Entity> getAnnotationsForEntities(List<Long> entityIds) throws Exception {
//        return ModelMgr.getModelMgr().getAnnotationsForEntities(entityIds);  
//    }
//
//    public List<Entity> getAnnotationsForEntity(Long entityId) throws Exception {
//        return ModelMgr.getModelMgr().getAnnotationsForEntity(entityId);  
//    }
//
//    public List<Entity> getAnnotationsForSession(Long annotationSessionId) throws Exception {
//        return ModelMgr.getModelMgr().getAnnotationsForSession(annotationSessionId);
//    }
//
//    public List<Entity> getCategoriesForAnnotationSession(Long annotationSessionId) throws Exception {
//        return ModelMgr.getModelMgr().getCategoriesForAnnotationSession(annotationSessionId);
//    }
//
//    public List<Entity> getEntitiesForAnnotationSession(Long annotationSessionId) throws Exception {
//        return ModelMgr.getModelMgr().getEntitiesForAnnotationSession(annotationSessionId);
//    }
//
//    public void removeAllOntologyAnnotationsForSession(Long annotationSessionId) throws Exception {
//        ModelMgr.getModelMgr().removeAllOntologyAnnotationsForSession(annotationSessionId);
//    }
//
//    public Entity cloneEntityTree(Long entityId, String rootName) throws Exception {
//        return ModelMgr.getModelMgr().cloneEntityTree(entityId, rootName);
//    }
//
//    public boolean deleteEntityById(Long entityId) {
//        return ModelMgr.getModelMgr().deleteEntityById(entityId);
//    }
//
//    public void deleteEntityTree(Long entityId) throws Exception {
//        ModelMgr.getModelMgr().deleteEntityTree(entityId);
//    }
//
//    public Entity getCachedEntityTree(Long entityId) throws Exception {
//        return ModelMgr.getModelMgr().getCachedEntityTree(entityId);
//    }
//
//    public Set<Entity> getChildEntities(Long parentEntityId) {
//        return ModelMgr.getModelMgr().getChildEntities(parentEntityId);
//    }
//
//    public List<Entity> getCommonRootEntitiesByType(Long entityTypeId) {
//        return ModelMgr.getModelMgr().getCommonRootEntitiesByType(entityTypeId);
//    }
//
//    public List<Entity> getEntitiesByName(String entityName) {
//    	List<Entity> entities = ModelMgr.getModelMgr().getEntitiesByName(entityName);
//    	return entities;
//    }
//
//    public List<Entity> getEntitiesByType(Long entityTypeId) {
//        return ModelMgr.getModelMgr().getEntitiesByType(entityTypeId);
//    }
	
	public Entity getCurrentOntology() {
		return ModelMgr.getModelMgr().getSelectedOntology();
	}

    public Entity getEntityById(long entityId) throws Exception {
        return ModelMgr.getModelMgr().getEntityById(""+entityId);
    }

	public Entity getEntityTree(long entityId) throws Exception {
        return ModelMgr.getModelMgr().getEntityTree(entityId);
    }

//    public List<EntityType> getEntityTypes() {
//        return ModelMgr.getModelMgr().getEntityTypes();
//    }

    public EntityData[] getParentEntityDataArray(long childEntityId) {
    	List<EntityData> list = ModelMgr.getModelMgr().getParentEntityDatas(childEntityId);
    	return list.toArray(new EntityData[0]);
    }
    
//    public EntityData saveEntityDataForEntity(EntityData newData) throws Exception {
//        return ModelMgr.getModelMgr().saveOrUpdateEntityData(newData);
//    }
}