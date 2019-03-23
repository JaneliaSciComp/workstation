package org.janelia.it.workstation.browser.model.search;

import org.janelia.it.workstation.browser.model.AnnotatedObjectList;

/**
 * An annotated page of results. 
 * 
 * T - type of the result objects
 * S - type of the unique identifier for the results
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface ResultPage<T,S> extends AnnotatedObjectList<T,S> {

    public long getNumTotalResults();
    
    public long getNumPageResults();
    
}
