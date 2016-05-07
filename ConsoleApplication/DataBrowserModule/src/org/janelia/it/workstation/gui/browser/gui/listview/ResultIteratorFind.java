package org.janelia.it.workstation.gui.browser.gui.listview;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.gui.browser.model.search.ResultIterator;

/**
 * Searches a ResultIterator to find objects matching some string
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ResultIteratorFind {

    private final ResultIterator resultIterator;
    private final String searchString;

    public ResultIteratorFind(ResultIterator resultIterator, String searchString) {
        this.resultIterator = resultIterator;
        this.searchString = searchString==null?null:searchString.toUpperCase();
    }

    /**
     * Execute the search and return the first matching object found.
     * This method may request additional results from the server and thus should be
     * run in a background thread.
     * @return first match, or null if no match is found
     */
    public DomainObject find() {
        while (resultIterator.hasNext()) {
            DomainObject domainObject = resultIterator.next();
            if (domainObject!=null && matches(domainObject)) {
                return domainObject;
            }
        }
        return null;
    }

    private boolean matches(DomainObject currObject) {
        // TODO: get the right strings to search
        String name = currObject.getName();
        return name.toUpperCase().contains(searchString);
    }
}
