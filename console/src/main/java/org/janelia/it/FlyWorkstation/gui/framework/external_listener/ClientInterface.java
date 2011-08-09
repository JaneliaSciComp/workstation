package org.janelia.it.FlyWorkstation.gui.framework.external_listener;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.AnnotationFacade;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/4/11
 * Time: 1:08 PM
 */
public class ClientInterface implements AnnotationFacade {

    private static ClientInterface soapClientInterface = new ClientInterface();

    private ClientInterface(){}

    public static ClientInterface getSOAPClientInterface() {
        return soapClientInterface;
    }

    /**
     * Registers the client to the Console and gets a port returned that the client should listen on.
     * @param clientName - name of the tool connecting
     * @return returns a port for the client to listen on
     */
    public String registerClientAndGetPort(String clientName) {
        int clientPort = SessionMgr.getSessionMgr().addExternalClient(clientName);
        return Integer.toString(clientPort);
    }

    public AnnotationSessionTask getCurrentAnnotationSessionTask() {
        return (AnnotationSessionTask)ModelMgr.getModelMgr().getCurrentAnnotationSessionTask();
    }

    @Override
    public void deleteAnnotation(Long annotatedEntityId, String tag) {
        ModelMgr.getModelMgr().deleteAnnotation(annotatedEntityId, tag);
    }

    @Override
    public List<Entity> getAnnotationsForEntities(List<Long> entityIds) throws Exception {
        return ModelMgr.getModelMgr().getAnnotationsForEntities(entityIds);  
    }

    @Override
    public List<Entity> getAnnotationsForEntity(Long entityId) throws Exception {
        return ModelMgr.getModelMgr().getAnnotationsForEntity(entityId);  
    }

    @Override
    public List<Entity> getAnnotationsForSession(Long annotationSessionId) throws Exception {
        return ModelMgr.getModelMgr().getAnnotationsForSession(annotationSessionId);
    }

    @Override
    public List<Entity> getCategoriesForAnnotationSession(Long annotationSessionId) throws Exception {
        return ModelMgr.getModelMgr().getCategoriesForAnnotationSession(annotationSessionId);
    }

    @Override
    public List<Entity> getEntitiesForAnnotationSession(Long annotationSessionId) throws Exception {
        return ModelMgr.getModelMgr().getEntitiesForAnnotationSession(annotationSessionId);
    }

    @Override
    public void removeAllOntologyAnnotationsForSession(Long annotationSessionId) throws Exception {
        ModelMgr.getModelMgr().removeAllOntologyAnnotationsForSession(annotationSessionId);
    }

    @Override
    public Entity cloneEntityTree(Long entityId, String rootName) throws Exception {
        return ModelMgr.getModelMgr().cloneEntityTree(entityId, rootName);
    }

    @Override
    public boolean deleteEntityById(Long entityId) {
        return ModelMgr.getModelMgr().deleteEntityById(entityId);
    }

    @Override
    public void deleteEntityTree(Long entityId) throws Exception {
        ModelMgr.getModelMgr().deleteEntityTree(entityId);
    }

    @Override
    public Entity getCachedEntityTree(Long entityId) throws Exception {
        return ModelMgr.getModelMgr().getCachedEntityTree(entityId);
    }

    @Override
    public Set<Entity> getChildEntities(Long parentEntityId) {
        return ModelMgr.getModelMgr().getChildEntities(parentEntityId);
    }

    @Override
    public List<Entity> getCommonRootEntitiesByType(Long entityTypeId) {
        return ModelMgr.getModelMgr().getCommonRootEntitiesByType(entityTypeId);
    }

    @Override
    public List<Entity> getEntitiesByName(String entityName) {
        return ModelMgr.getModelMgr().getEntitiesByName(entityName);
    }

    @Override
    public List<Entity> getEntitiesByType(Long entityTypeId) {
        return ModelMgr.getModelMgr().getEntitiesByType(entityTypeId);
    }

    @Override
    public Entity getEntityById(String entityId) throws Exception {
        return ModelMgr.getModelMgr().getEntityById(entityId);
    }

    @Override
    public Entity getEntityTree(Long entityId) throws Exception {
        return ModelMgr.getModelMgr().getEntityTree(entityId);
    }

    @Override
    public List<EntityType> getEntityTypes() {
        return ModelMgr.getModelMgr().getEntityTypes();
    }

    @Override
    public List<EntityData> getParentEntityDatas(Long childEntityId) {
        return ModelMgr.getModelMgr().getParentEntityDatas(childEntityId);
    }

    @Override
    public EntityData saveEntityDataForEntity(EntityData newData) throws Exception {
        return ModelMgr.getModelMgr().saveOrUpdateEntityData(newData);
    }
}