package org.janelia.it.workstation.api.facade.concrete_facade.ejb;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.annotation.DataDescriptor;
import org.janelia.it.jacs.shared.annotation.DataFilter;
import org.janelia.it.jacs.shared.annotation.FilterResult;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/8/11
 * Time: 9:31 AM
 */
public class EJBAnnotationFacade extends EJBEntityFacade implements org.janelia.it.workstation.api.facade.abstract_facade.AnnotationFacade {

    @Override
    public List<Entity> getAnnotationsForEntity(Long entityId) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getAnnotationsForEntity(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSubjectKey(), entityId);
    }
    
    @Override
    public long getNumDescendantsAnnotated(Long entityId) throws Exception {
    	return EJBFactory.getRemoteAnnotationBean().getNumDescendantsAnnotated(entityId);
	}

    @Override
    public List<Entity> getAnnotationsForEntities(List<Long> entityIds) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getAnnotationsForEntities(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSubjectKey(),
                entityIds);
    }

    @Override
    public List<Entity> getAnnotationsForChildren(Long parentId) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getAnnotationsForChildren(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSubjectKey(),
        		parentId);
    }
    
    @Override
    public List<Entity> getEntitiesForAnnotationSession(Long annotationSessionId) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getEntitiesForAnnotationSession(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSubjectKey(),
                annotationSessionId);
    }

    @Override
    public List<Entity> getAnnotationsForSession(Long annotationSessionId) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getAnnotationsForSession(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSubjectKey(),
                annotationSessionId);
    }

    @Override
    public List<Entity> getCategoriesForAnnotationSession(Long annotationSessionId) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getCategoriesForAnnotationSession(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSubjectKey(),
                annotationSessionId);
    }

    @Override
    public List<Long> getEntityIdsInAlignmentSpace(String opticalRes, String pixelRes, List<Long> guids) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getEntityIdsInAlignmentSpace( opticalRes, pixelRes, guids );
    }

    @Override
    public Set<Long> getCompletedEntityIds(Long annotationSessionId) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getCompletedEntityIds(annotationSessionId);
    }
    
    @Override
    public void removeAnnotation(Long annotationId) throws Exception {
        EJBFactory.getRemoteAnnotationBean().removeOntologyAnnotation(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSubjectKey(), annotationId);
    }

    @Override
    public void removeAllOntologyAnnotationsForSession(Long annotationSessionId) throws Exception {
        EJBFactory.getRemoteAnnotationBean().removeAllOntologyAnnotationsForSession(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSubjectKey(),
                annotationSessionId);
    }

    @Override
    public Object[] getPatternAnnotationQuantifierMapsFromSummary() throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getPatternAnnotationQuantifierMapsFromSummary();
    }

    @Override
    public Object[] getMaskQuantifierMapsFromSummary(String maskFolderName) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getMaskQuantifierMapsFromSummary(maskFolderName);
    }

    @Override
    public List<DataDescriptor> patternSearchGetDataDescriptors(String type) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().patternSearchGetDataDescriptors(type);
    }

    @Override
    public int patternSearchGetState() throws Exception {
        return EJBFactory.getRemoteAnnotationBean().patternSearchGetState();
    }

    @Override
    public List<String> patternSearchGetCompartmentList(String type) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().patternSearchGetCompartmentList(type);
    }

    @Override
    public FilterResult patternSearchGetFilteredResults(String type, Map<String, Set<DataFilter>> filterMap) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().patternSearchGetFilteredResults(type, filterMap);
    }

    @Override
    public Entity createDataSet(String dataSetName) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().createDataSet(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSubjectKey(), dataSetName);
    }

    @Override
    public List<Entity> getDataSets() throws Exception {
    	return EJBFactory.getRemoteAnnotationBean().getUserDataSets(Arrays.asList(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSubjectKey()));
	}
    
    @Override
    public Entity createAlignmentBoard(String alignmentBoardName, String alignmentSpace, String opticalRes, String pixelRes) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().createAlignmentBoard(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSubjectKey(), alignmentBoardName, alignmentSpace, opticalRes, pixelRes);
    }

    @Override
    public EntityData addAlignedItem(Entity parentEntity, Entity child, String alignedItemName, boolean visible) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().addAlignedItem(parentEntity, child, alignedItemName, visible);
    }
}
