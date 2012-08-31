package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.AnnotationFacade;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.shared.annotation.DataDescriptor;
import org.janelia.it.jacs.shared.annotation.DataFilter;
import org.janelia.it.jacs.shared.annotation.FilterResult;
import org.janelia.it.jacs.shared.annotation.PatternAnnotationDataManager;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/8/11
 * Time: 9:31 AM
 */
public class EJBAnnotationFacade extends EJBEntityFacade implements AnnotationFacade {

    @Override
    public List<Entity> getAnnotationsForEntity(Long entityId) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getAnnotationsForEntity(SessionMgr.getUsername(), entityId);
    }

    @Override
    public List<Entity> getAnnotationsForEntities(List<Long> entityIds) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getAnnotationsForEntities(SessionMgr.getUsername(),
                entityIds);
    }

    @Override
    public List<Entity> getAnnotationsForChildren(Long parentId) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getAnnotationsForChildren(SessionMgr.getUsername(),
        		parentId);
    }
    
    @Override
    public List<Entity> getEntitiesForAnnotationSession(Long annotationSessionId) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getEntitiesForAnnotationSession(SessionMgr.getUsername(),
                annotationSessionId);
    }

    @Override
    public List<Entity> getAnnotationsForSession(Long annotationSessionId) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getAnnotationsForSession(SessionMgr.getUsername(),
                annotationSessionId);
    }

    @Override
    public List<Entity> getCategoriesForAnnotationSession(Long annotationSessionId) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getCategoriesForAnnotationSession(SessionMgr.getUsername(),
                annotationSessionId);
    }

    @Override
    public Set<Long> getCompletedEntityIds(Long annotationSessionId) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getCompletedEntityIds(annotationSessionId);
    }
    
    @Override
    public void removeAnnotation(Long annotationId) throws Exception {
        EJBFactory.getRemoteAnnotationBean().removeOntologyAnnotation(SessionMgr.getUsername(), annotationId);
    }

    @Override
    public void removeAllOntologyAnnotationsForSession(Long annotationSessionId) throws Exception {
        EJBFactory.getRemoteAnnotationBean().removeAllOntologyAnnotationsForSession(SessionMgr.getUsername(),
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
    public FilterResult patternSearchGetFilteredResults(String type, Map<DataDescriptor, Set<DataFilter>> filterMap) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().patternSearchGetFilteredResults(type, filterMap);
    }

}
