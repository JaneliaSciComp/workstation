package org.janelia.workstation.colordepth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.model.domain.Reference;
import org.janelia.workstation.core.model.AnnotatedObjectList;
import org.janelia.workstation.core.model.search.ResultPage;
import org.janelia.model.domain.gui.cdmip.ColorDepthMatch;
import org.janelia.model.domain.ontology.Annotation;

/**
 * One page of color depth results.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthResultPage implements AnnotatedObjectList<ColorDepthMatch,Reference>, ResultPage<ColorDepthMatch,Reference> {

    private final List<ColorDepthMatch> matches = new ArrayList<>();
    private final long numTotalResults;
    
    private Map<Reference,ColorDepthMatch> matchMap;
    
    public ColorDepthResultPage(List<ColorDepthMatch> matches, long totalNumResults) {
        
        for(ColorDepthMatch match : matches) {
            if (match!=null) {
                this.matches.add(match);
            }
        }
        
        this.numTotalResults = totalNumResults;
    }

    @Override
    public long getNumTotalResults() {
        return numTotalResults;
    }

    @Override
    public long getNumPageResults() {
        return matches.size();
    }
    
    @Override
    public synchronized List<ColorDepthMatch> getObjects() {
        return matches;
    }
    
    @Override
    public List<Annotation> getAnnotations(Reference id) {
        return Collections.emptyList();
    }
    
    private synchronized Map<Reference,ColorDepthMatch> getMatchByImageRef() {
        if (matchMap==null) {
            this.matchMap = new HashMap<>();
            for(ColorDepthMatch match : matches) {
                matchMap.put(match.getImageRef(), match);
            }
        }
        return matchMap;
    }
    
    @Override
    public synchronized ColorDepthMatch getObjectById(Reference id) {
        return getMatchByImageRef().get(id);
    }

    @Override
    public synchronized boolean updateObject(ColorDepthMatch updatedObject) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean updateAnnotations(Reference id, List<Annotation> annotations) {
        return false;
    }
}
