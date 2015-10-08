package org.janelia.it.workstation.api.facade.abstract_facade;

import java.util.Date;
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
 * Time: 9:33 AM
 */
public interface AnnotationFacade {

    public List<Entity> getAnnotationsForEntity(Long entityId) throws Exception;

    public List<Entity> getAnnotationsForEntities(List<Long> entityIds) throws Exception;

    public long getNumDescendantsAnnotated(Long entityId) throws Exception;

    public List<Entity> getAnnotationsForChildren(Long entityId) throws Exception;

    public List<Entity> getEntitiesForAnnotationSession(Long annotationSessionId) throws Exception;

    public List<Entity> getAnnotationsForSession(Long annotationSessionId) throws Exception;

    public List<Entity> getCategoriesForAnnotationSession(Long annotationSessionId) throws Exception;

    public List<Long> getEntityIdsInAlignmentSpace(String opticalRes, String pixelRes, List<Long> guids) throws Exception;

    public Set<Long> getCompletedEntityIds(Long annotationSessionId) throws Exception;

    public void removeAnnotation(Long annotationId) throws Exception;

    public void removeAllOntologyAnnotationsForSession(Long annotationSessionId) throws Exception;

    public Object[] getPatternAnnotationQuantifierMapsFromSummary() throws Exception;

    public Object[] getMaskQuantifierMapsFromSummary(String maskFolderName) throws Exception;

    public List<DataDescriptor> patternSearchGetDataDescriptors(String type) throws Exception;

    public int patternSearchGetState() throws Exception;

    public List<String> patternSearchGetCompartmentList(String type) throws Exception;

    public FilterResult patternSearchGetFilteredResults(String type, Map<String, Set<DataFilter>> filterMap) throws Exception;

    public Entity createDataSet(String dataSetName) throws Exception;
    
    public Entity createFlyLineRelease(String releaseName, Date releaseDate, Integer lagTimeMonths, List<String> dataSetList) throws Exception;

    public List<Entity> getDataSets() throws Exception;
    
    public List<Entity> getFlyLineReleases() throws Exception;

    public Entity createAlignmentBoard(String alignmentBoardName, String alignmentSpace, String opticalRes, String pixelRes) throws Exception;

    public EntityData addAlignedItem(Entity parentEntity, Entity child, String alignedItemName, boolean visible) throws Exception;
}
