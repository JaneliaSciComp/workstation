package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.aggregate;

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.AnnotationFacade;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 4:50 PM
 */
public class AggregateAnnotationFacade extends AggregateEntityFacade implements AnnotationFacade {

    static private Object[] parameters = new Object[]{EntityConstants.TYPE_ANNOTATION};

    protected String getMethodNameForAggregates() {
        return ("getFacade");
    }

    protected Class[] getParameterTypesForAggregates() {
        return new Class[]{String.class};
    }

    protected Object[] getParametersForAggregates() {
        return parameters;
    }

    @Override
    public List<Entity> getAnnotationsForEntity(String username, Long entityId) throws Exception {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        List<Entity> tmpAnnotations;
        for (Object aggregate : aggregates) {
            tmpAnnotations = ((AnnotationFacade) aggregate).getAnnotationsForEntity(username, entityId);
            if (null != tmpAnnotations) {
                returnList.addAll(tmpAnnotations);
            }
        }
        return returnList;
    }

    @Override
    public List<Entity> getAnnotationsForEntities(String username, List<Long> entityIds) throws Exception {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        List<Entity> tmpAnnotations;
        for (Object aggregate : aggregates) {
            tmpAnnotations = ((AnnotationFacade) aggregate).getAnnotationsForEntities(username, entityIds);
            if (null != tmpAnnotations) {
                returnList.addAll(tmpAnnotations);
            }
        }
        return returnList;
    }

    @Override
    public List<Entity> getEntitiesForAnnotationSession(String username, Long annotationSessionId) throws Exception {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        List<Entity> tmpEntities;
        for (Object aggregate : aggregates) {
            tmpEntities = ((AnnotationFacade) aggregate).getEntitiesForAnnotationSession(username, annotationSessionId);
            if (null != tmpEntities) {
                returnList.addAll(tmpEntities);
            }
        }
        return returnList;
    }

    @Override
    public List<Entity> getAnnotationsForSession(String username, Long annotationSessionId) throws Exception {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        List<Entity> tmpEntities;
        for (Object aggregate : aggregates) {
            tmpEntities = ((AnnotationFacade) aggregate).getAnnotationsForSession(username, annotationSessionId);
            if (null != tmpEntities) {
                returnList.addAll(tmpEntities);
            }
        }
        return returnList;
    }

    @Override
    public List<Entity> getCategoriesForAnnotationSession(String username, Long annotationSessionId) throws Exception {
        Object[] aggregates = getAggregates();
        List<Entity> returnList = new ArrayList<Entity>();
        List<Entity> tmpCategories;
        for (Object aggregate : aggregates) {
            tmpCategories = ((AnnotationFacade) aggregate).getCategoriesForAnnotationSession(username, annotationSessionId);
            if (null != tmpCategories) {
                returnList.addAll(tmpCategories);
            }
        }
        return returnList;
    }

    @Override
    public void deleteAnnotation(String userlogin, Long annotatedEntityId, String tag) {
        Object[] aggregates = getAggregates();
        for (Object aggregate : aggregates) {
            ((AnnotationFacade) aggregate).deleteAnnotation(userlogin, annotatedEntityId, tag);
        }
    }

    @Override
    public void removeAllOntologyAnnotationsForSession(String username, Long annotationSessionId) throws Exception {
        Object[] aggregates = getAggregates();
        for (Object aggregate : aggregates) {
            ((AnnotationFacade) aggregate).removeAllOntologyAnnotationsForSession(username, annotationSessionId);
        }
    }
}
