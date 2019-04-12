package org.janelia.workstation.core.model.search;

/**
 * Searches a ResultIterator to find objects matching some string
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class ResultIteratorFind<T,S> {

    private final ResultIterator<T,S> resultIterator;

    public ResultIteratorFind(ResultIterator<T,S> resultIterator) {
        this.resultIterator = resultIterator;
    }

    /**
     * Execute the search and return the first matching object found.
     * This method may request additional results from the server and thus should be
     * run in a background thread.
     * @return first match, or null if no match is found
     */
    public T find() {
        while (resultIterator.hasNext()) {
            T object = resultIterator.next();
            if (object!=null && matches(resultIterator.getCurrResultPage(), object)) {
                return object;
            }
        }
        return null;
    }

    /**
     * Implement this method to describe how an object should be matched.
     * @param resultPage the result page containing the object
     * @param object object to match against
     * @return true if the object matches the search string
     */
    protected abstract boolean matches(ResultPage<T,S> resultPage, T object);
}
