package org.janelia.it.workstation.gui.browser.model.search;

import org.janelia.it.jacs.model.domain.DomainObject;

/**
 * Searches a ResultIterator to find objects matching some string
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class ResultIteratorFind {

    private final ResultIterator resultIterator;

    public ResultIteratorFind(ResultIterator resultIterator) {
        this.resultIterator = resultIterator;
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
            if (domainObject!=null && matches(resultIterator.getCurrResultPage(), domainObject)) {
                return domainObject;
            }
        }
        return null;
    }

    /**
     * Implement this method to describe how an object should be matched.
     * @param resultPage the result page containing domainObject
     * @param domainObject object to match against
     * @return true if the object matches the search string
     */
    protected abstract boolean matches(ResultPage resultPage, DomainObject domainObject);
}
