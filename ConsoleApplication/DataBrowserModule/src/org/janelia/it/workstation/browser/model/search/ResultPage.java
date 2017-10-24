package org.janelia.it.workstation.browser.model.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.workstation.browser.model.AnnotatedDomainObjectList;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.support.DomainUtils;

import com.google.common.collect.ListMultimap;

/**
 * One page of annotated results, treated as a unit for performance reasons.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ResultPage implements AnnotatedDomainObjectList {

    private final List<DomainObject> domainObjects = new ArrayList<>();
    private final ListMultimap<Long,Annotation> annotationsByDomainObjectId;
    private final long numTotalResults;
    
    private Map<Long, DomainObject> domainObjectById;
    
    public ResultPage(List<DomainObject> domainObjects, List<Annotation> annotations, long totalNumResults) {
        
        for(DomainObject domainObject : domainObjects) {
            // Filter out null objects, in case some references could not be resolved
            if (domainObject!=null) {
                this.domainObjects.add(domainObject);
            }
        }
        
        this.annotationsByDomainObjectId = DomainUtils.getAnnotationsByDomainObjectId(annotations);
        this.numTotalResults = totalNumResults;
    }

    public long getNumTotalResults() {
        return numTotalResults;
    }
    
    public long getNumPageResults() {
        return domainObjects.size();
    }
    
    @Override
    public synchronized List<DomainObject> getDomainObjects() {
        return domainObjects;
    }
    
    @Override
    public List<Annotation> getAnnotations(Long domainObjectId) {
        return annotationsByDomainObjectId.get(domainObjectId);
    }
    
    private synchronized Map<Long, DomainObject> getDomainObjectByIdMap() {
        if (domainObjectById==null) {
            this.domainObjectById = new HashMap<>();
            for(DomainObject domainObject : domainObjects) {
                domainObjectById.put(domainObject.getId(), domainObject);
            }
        }
        return domainObjectById;
    }
    
    @Override
    public synchronized DomainObject getDomainObject(Long domainObjectId) {
        return getDomainObjectByIdMap().get(domainObjectId);
    }

    @Override
    public synchronized boolean updateObject(DomainObject updatedObject) {
        if (updatedObject==null) return false;
        getDomainObjectByIdMap().put(updatedObject.getId(), updatedObject);
        return DomainUtils.replaceDomainObjectInList(domainObjects, updatedObject);
    }
    
    @Override
    public boolean updateAnnotations(Long domainObjectId, List<Annotation> annotations) {
        if (!getDomainObjectByIdMap().containsKey(domainObjectId)) return false;
        annotationsByDomainObjectId.removeAll(domainObjectId);
        annotationsByDomainObjectId.putAll(domainObjectId, annotations);
        return false;
    }
}
