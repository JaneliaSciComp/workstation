package org.janelia.it.workstation.gui.browser.model.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.gui.browser.model.AnnotatedDomainObjectList;

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
    public Class<? extends DomainObject> getDomainClass() {
        if (domainObjects.isEmpty()) {
            return null;
        }
        else {
            return domainObjects.get(0).getClass();
        }
    }
    
    @Override
    public List<DomainObject> getDomainObjects() {
        return domainObjects;
    }
    
    @Override
    public List<Annotation> getAnnotations(Long domainObjectId) {
        return annotationsByDomainObjectId.get(domainObjectId);
    }
    
    @Override
    public DomainObject getDomainObject(Long domainObjectId) {
        if (domainObjectById==null) {
            this.domainObjectById = new HashMap<>();
            for(DomainObject domainObject : domainObjects) {
                domainObjectById.put(domainObject.getId(), domainObject);
            }
        }
        return domainObjectById.get(domainObjectId);
    }

    @Override
    public void updateAnnotations(Long domainObjectId, List<Annotation> annotations) {
        annotationsByDomainObjectId.removeAll(domainObjectId);
        annotationsByDomainObjectId.putAll(domainObjectId, annotations);
    }
}
