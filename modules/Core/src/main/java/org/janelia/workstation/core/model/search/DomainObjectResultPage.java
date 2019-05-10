package org.janelia.workstation.core.model.search;

import com.google.common.collect.ListMultimap;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.workstation.core.model.AnnotatedObjectList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One page of annotated domain objects, treated as a unit for performance reasons.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectResultPage implements AnnotatedObjectList<DomainObject,Reference>, ResultPage<DomainObject,Reference> {

    private final List<DomainObject> domainObjects = new ArrayList<>();
    private final ListMultimap<Reference,Annotation> annotationMap;
    private final long numTotalResults;
    
    private Map<Reference, DomainObject> domainObjectMap;
    
    public DomainObjectResultPage(List<DomainObject> domainObjects, List<Annotation> annotations, long totalNumResults) {
        
        for(DomainObject domainObject : domainObjects) {
            // Filter out null objects, in case some references could not be resolved
            if (domainObject!=null) {
                this.domainObjects.add(domainObject);
            }
        }
        
        this.annotationMap = DomainUtils.getAnnotationsByDomainObjectReference(annotations);
        this.numTotalResults = totalNumResults;
    }

    @Override
    public long getNumTotalResults() {
        return numTotalResults;
    }

    @Override
    public long getNumPageResults() {
        return domainObjects.size();
    }
    
    @Override
    public synchronized List<DomainObject> getObjects() {
        return domainObjects;
    }
    
    @Override
    public List<Annotation> getAnnotations(Reference domainObjectId) {
        return annotationMap.get(domainObjectId);
    }
    
    private synchronized Map<Reference, DomainObject> getDomainObjectByRefMap() {
        if (domainObjectMap==null) {
            this.domainObjectMap = new HashMap<>();
            for(DomainObject domainObject : domainObjects) {
                domainObjectMap.put(Reference.createFor(domainObject), domainObject);
            }
        }
        return domainObjectMap;
    }
    
    @Override
    public synchronized DomainObject getObjectById(Reference ref) {
        return getDomainObjectByRefMap().get(ref);
    }

    @Override
    public synchronized boolean updateObject(DomainObject updatedObject) {
        if (updatedObject==null) return false;
        getDomainObjectByRefMap().put(Reference.createFor(updatedObject), updatedObject);
        return DomainUtils.replaceDomainObjectInList(domainObjects, updatedObject);
    }
    
    @Override
    public boolean updateAnnotations(Reference ref, List<Annotation> annotations) {
        if (!getDomainObjectByRefMap().containsKey(ref)) return false;
        annotationMap.removeAll(ref);
        annotationMap.putAll(ref, annotations);
        return false;
    }
}
