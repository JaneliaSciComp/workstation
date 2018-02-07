package org.janelia.it.workstation.browser.model.search;

import static org.janelia.it.workstation.browser.model.search.DomainObjectSearchResults.PAGE_SIZE;

import java.util.Iterator;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.text.Position;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Efficiently iterate over a result set, with an arbitrary starting point and direction.
 *
 * This iterator may call the server to load additional results, so it should only be called
 * in a background worker thread!
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ResultIterator<T,S> implements Iterator<T> {

    private static final Logger log = LoggerFactory.getLogger(ResultIterator.class);

    SearchResults<T,S> searchResults;
    private final int startIndex;
    private final int startPage;
    private final Position.Bias bias;
    private boolean skipStartingIndex = false;

    private int currPage;
    private int currIndex;
    private boolean hasNext;
    private int lastPage;
    
    public ResultIterator(SearchResults<T,S> searchResults, int globalStartIndex, Position.Bias bias, boolean skipStartingIndex) {
        this.searchResults = searchResults;
        this.bias = bias;
        this.skipStartingIndex = skipStartingIndex;
        this.startPage = this.currPage = this.lastPage = globalStartIndex / PAGE_SIZE;
        this.startIndex = this.currIndex = globalStartIndex % PAGE_SIZE;
        this.hasNext = searchResults.getNumTotalResults() > (skipStartingIndex ? 2 : 1);
        
        if (log.isDebugEnabled()) {
            log.debug("Init result iterator:");
            log.debug("  globalStartIndex={}",globalStartIndex);
            log.debug("  numTotalPages={}",searchResults.getNumTotalPages());
            log.debug("  numTotalResults={}",searchResults.getNumTotalResults());
            log.debug("  bias={}",bias);
            log.debug("  startPage={}",startPage);
            log.debug("  startIndex={}",startIndex);
            log.debug("  hasNext={}",hasNext);
        }
        
        if (skipStartingIndex) {
            log.debug("Skipping starting index.");
            try {
                ResultPage<T,S> page = searchResults.getPage(currPage);
                List<T> objects = page.getObjects();
                moveNext(objects.size());
            }
            catch (Exception e) {
                // This shouldn't happen because the page should already be cached
                throw new IllegalStateException("Error fetching current page",e);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Starting at:");
            log.debug("  currPage={}",currPage);
            log.debug("  currIndex={}",currIndex);
        }
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public T next() {

        if (SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("ResultIterator.next called in the EDT");
        }

        log.trace("Getting next object at currPage={}, currIndex={}",currPage,currIndex);

        try {
            // Find the current object that we need to return
            lastPage = currPage;
            ResultPage<T,S> page = searchResults.getPage(currPage);
            List<T> objects = page.getObjects();
            
            if (currIndex>=objects.size()) {
                // This clamp is needed for the edge case where we start on the first page 
                // and go backwards to the last page, which has less than a full page of results.
                currIndex = objects.size()-1;
            }
                        
            T object = objects.isEmpty() ? null : objects.get(currIndex);

            if (currPage==startPage && currIndex==startIndex && skipStartingIndex) {
                // We have looped back to the starting index, which was initially skipped,
                // so this is the last object we will serve.
                hasNext = false;
                log.trace("Returning starting index and ending iteration");
                return object;
            }

            // Prepare for the next call
            moveNext(objects.size());

            if (currPage==startPage && currIndex==startIndex && !skipStartingIndex) {
                // We have looped back to the starting index and we didn't skip it before,
                // so we'll not serve it again.
                log.trace("Reached starting index, ending iteration");
                hasNext = false;
            }

            return object;
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
            return null;
        }
    }

    private void moveNext(int pageSize) {
        if (bias == Position.Bias.Backward) {
            currIndex--;
            if (currIndex<0) {
                currIndex = pageSize;
                currPage--;
                if (currPage<0) {
                    currPage = searchResults.getNumTotalPages()-1;
                }
            }
        }
        else {
            currIndex++;
            if (currIndex>=pageSize) {
                currIndex = 0;
                currPage++;
                if (currPage>=searchResults.getNumTotalPages()) {
                    currPage = 0;
                }
            }
        }
    }

    /**
     * Returns the index of the page containing the last result returned by getNext().
     * @return 0-indexed page number
     */
    public int getCurrPage() {
        return lastPage;
    }

    /**
     * Returns the result page containing the last result returned by getNext().
     * @return loaded result page
     */
    public ResultPage<T,S> getCurrResultPage() {
        try {
            return searchResults.getPage(lastPage);
        }
        catch (Exception e) {
            // This shouldn't happen because the page should already be cached
            throw new IllegalStateException("Error fetching current page",e);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Removing results is not supported by this API");
    }
}
