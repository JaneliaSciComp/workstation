/*
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/4/11
 * Time: 1:08 PM
 */
package org.janelia.it.FlyWorkstation.ws;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.OntologyKeyBindings;
import org.janelia.it.FlyWorkstation.gui.framework.outline.AnnotationSession;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.ExternalClient;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.gui.util.PathTranslator;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The implementation of the Console server interface. 
 * 
 * @author saffordt
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@WebService(endpointInterface="org.janelia.it.FlyWorkstation.ws.ConsoleDataService",
			serviceName="ConsoleDataService",	
			portName="CdsPort", name="Cds")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class ConsoleDataServiceImpl {

	private static final Logger log = LoggerFactory.getLogger(ConsoleDataServiceImpl.class);
	
	public int reservePort(String clientName) {
        int port = SessionMgr.getSessionMgr().addExternalClient(clientName);
		log.info("Reserving port "+port+" for client "+clientName);
		return port;
	}
	
	public void registerClient(int port, String endpointUrl) throws Exception {
    	ExternalClient client = SessionMgr.getSessionMgr().getExternalClientByPort(port);
    	client.init(endpointUrl);
    	log.info("Initialized client on port "+port+" with endpoint "+endpointUrl);

		Map<String,Object> parameters = new HashMap<String,Object>();
		
		if (ModelMgr.getModelMgr().getCurrentOntology() != null) {
			parameters.clear();
			parameters.put("rootId",ModelMgr.getModelMgr().getCurrentOntology().getId());
			client.sendMessage("ontologySelected", parameters);
		}
		
		if (ModelMgr.getModelMgr().getCurrentAnnotationSession() != null) {
			parameters.clear();
			parameters.put("sessionId",ModelMgr.getModelMgr().getCurrentAnnotationSession().getId());
			client.sendMessage("sessionSelected", parameters);
		}
    }
    
    public void selectEntity(long entityId, boolean clearAll) throws Exception {
//        ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_MAIN_VIEW, entityId+"", clearAll);
//        ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_SEC_VIEW, entityId+"", clearAll);
    }

    public void deselectEntity(long entityId) throws Exception {
//        ModelMgr.getModelMgr().getEntitySelectionModel().deselectEntity(EntitySelectionModel.CATEGORY_MAIN_VIEW, entityId+"");
//        ModelMgr.getModelMgr().getEntitySelectionModel().deselectEntity(EntitySelectionModel.CATEGORY_SEC_VIEW, entityId+"");
    }
    
    public Entity createAnnotation(OntologyAnnotation annotation) throws Exception {
        return ModelMgr.getModelMgr().createOntologyAnnotation(annotation);
    }
    
    public void removeAnnotation(long annotationId) throws Exception {
        ModelMgr.getModelMgr().removeAnnotation(annotationId);
    }
    
    public Entity[] getAnnotationsForEntity(long entityId) throws Exception {
    	return ModelMgr.getModelMgr().getAnnotationsForEntity(entityId).toArray(new Entity[0]);
    }

    public Entity[] getAnnotationsForEntities(Long[] entityIds) throws Exception {
    	return ModelMgr.getModelMgr().getAnnotationsForEntities(Arrays.asList(entityIds)).toArray(new Entity[0]);
    }
	
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
	
	public Entity getOntology(long rootId) throws Exception {
		return ModelMgr.getModelMgr().getOntologyTree(rootId);
	}

	public AnnotationSession getAnnotationSession(long sessionId) throws Exception {
		return ModelMgr.getModelMgr().getAnnotationSession(sessionId);
	}
	
	public OntologyKeyBindings getKeybindings(long ontologyId) {
		return ModelMgr.getModelMgr().getKeyBindings(ontologyId);
	}
	
    public Entity getEntityById(long entityId) throws Exception {
        return translatePaths(FacadeManager.getFacadeManager().getEntityFacade().getEntityById(entityId));
    }

    public Entity getEntityAndChildren(long entityId) throws Exception {
        return translatePaths(FacadeManager.getFacadeManager().getEntityFacade().getEntityAndChildren(entityId));
    }

	public Entity getEntityTree(long entityId) throws Exception {
        return translatePaths(FacadeManager.getFacadeManager().getEntityFacade().getEntityTree(entityId));
    }

//    public List<EntityType> getEntityTypes() {
//        return ModelMgr.getModelMgr().getEntityTypes();
//    }

    public Entity[] getParentEntityArray(long childEntityId) throws Exception {
    	List<Entity> list = ModelMgr.getModelMgr().getParentEntities(childEntityId);
    	return list.toArray(new Entity[0]);
    }
    
    public EntityData[] getParentEntityDataArray(long childEntityId) throws Exception {
    	List<EntityData> list = ModelMgr.getModelMgr().getParentEntityDatas(childEntityId);
    	return list.toArray(new EntityData[0]);
    }
    
    public Entity getAncestorWithType(long entityId, String type) throws Exception {
    	return ModelMgr.getModelMgr().getAncestorWithType(ModelMgr.getModelMgr().getEntityById(entityId), type);
    }
    
//    public EntityData saveEntityDataForEntity(EntityData newData) throws Exception {
//        return ModelMgr.getModelMgr().saveOrUpdateEntityData(newData);
//    }
    
    public String getUserAnnotationColor(String username) throws Exception {
        Color color = ModelMgr.getModelMgr().getUserAnnotationColor(username);
        String rgb = Integer.toHexString((color.getRGB() & 0xffffff) | 0x1000000).substring(1);
        return rgb;
    }
    
    private Entity translatePaths(Entity entity) {
        if (ConsoleProperties.getBoolean("console.WebServer.proxyFiles")) {
            return PathTranslator.translatePathsToProxy(entity);
        }
        else {
            return PathTranslator.translatePathsToCurrentPlatform(entity);
        }
    }
}