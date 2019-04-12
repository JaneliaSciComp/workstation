package org.janelia.workstation.browser.gui.colordepth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.workstation.core.model.AnnotatedObjectList;
import org.janelia.workstation.core.model.search.ResultPage;
import org.janelia.model.domain.gui.colordepth.ColorDepthMatch;
import org.janelia.model.domain.ontology.Annotation;

/**
 * One page of color depth results.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthResultPage implements AnnotatedObjectList<ColorDepthMatch,String>, ResultPage<ColorDepthMatch,String> {

    private final List<ColorDepthMatch> matches = new ArrayList<>();
    private final long numTotalResults;
    
    private Map<String, ColorDepthMatch> matchMap;
    
    public ColorDepthResultPage(List<ColorDepthMatch> matches, long totalNumResults) {
        
        for(ColorDepthMatch match : matches) {
            if (matches!=null) {
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
    public List<Annotation> getAnnotations(String filepath) {
        return Collections.emptyList();
    }
    
    private synchronized Map<String, ColorDepthMatch> getMatchByFilepath() {
        if (matchMap==null) {
            this.matchMap = new HashMap<>();
            for(ColorDepthMatch match : matches) {
                matchMap.put(match.getFilepath(), match);
            }
        }
        return matchMap;
    }
    
    @Override
    public synchronized ColorDepthMatch getObjectById(String filepath) {
        return getMatchByFilepath().get(filepath);
    }

    @Override
    public synchronized boolean updateObject(ColorDepthMatch updatedObject) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean updateAnnotations(String filepath, List<Annotation> annotations) {
        return false;
    }
}
