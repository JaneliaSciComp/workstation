package org.janelia.it.workstation.browser.model.search;

import org.janelia.it.workstation.browser.model.AnnotatedObjectList;

public interface ResultPage<T,S> extends AnnotatedObjectList<T,S> {

    public long getNumTotalResults();
    
    public long getNumPageResults();
    
}
