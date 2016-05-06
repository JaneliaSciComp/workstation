package org.janelia.it.workstation.gui.browser.gui.listview.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.text.Position.Bias;

import org.janelia.it.workstation.gui.browser.gui.table.DynamicColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Searches a table viewer forward or backward to find objects matching some string.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TableViewerFind<T,S> {

    private static final Logger log = LoggerFactory.getLogger(TableViewerFind.class);
    
    private final TableViewerPanel<T,S> tableViewer;
    private final String searchString;
    private final T startingObject;
    private final Bias bias;

    // Internal search state
    private boolean skipStartingNode = false;
    private T firstMatch;
    private boolean looking = false;
    private boolean hasRun = false;

    public TableViewerFind(TableViewerPanel<T,S> tableViewer, String searchString, T startingObject, 
            Bias bias, boolean skipStartingNode) {
        this.tableViewer = tableViewer;
        this.searchString = searchString==null?null:searchString.toUpperCase();
        this.startingObject = startingObject==null?tableViewer.getObjectList().get(0):startingObject;
        this.bias = bias == null ? Bias.Forward : bias;
        this.skipStartingNode = skipStartingNode;
    }

    /**
     * Execute the search and return the first matching object found. 
     * @return
     */
    public T find() {
        if (hasRun) throw new IllegalStateException("Cannot reuse search once it has been run.");
        hasRun = true;
        
        List<T> objectList = new ArrayList<>(tableViewer.getObjectList());
        if (bias == Bias.Backward) {
            log.debug("Search backward (skipStartingNode={})",skipStartingNode);
            Collections.reverse(objectList);
        }
        else {
            log.debug("Search forward (skipStartingNode={})",skipStartingNode);
        }
        
        for(T object : objectList) {
            T found = checkCurrent(object);
            if (found != null) return found;
        }

        log.debug("Returning first match");
        return firstMatch;
    }
    
    private T checkCurrent(T currObject) {

        String name = tableViewer.getImageModel().getImageLabel(currObject);

        // Begin actually looking only once we get to the starting node
        if (currObject.equals(startingObject)) {
            log.debug("Beginning search at {}", name, looking);
            looking = true;
        }
       
        if (searchString==null || matches(currObject, searchString)) {
            // Found a match
            
            if (looking && (!skipStartingNode || (skipStartingNode && !currObject.equals(startingObject)))) {
                // This is a good match
                log.debug("Found a match at {}", name);
                return currObject;
            }
            else if (firstMatch == null) {
                // We need to use this match if we wrap around without finding anything past the starting node
                log.debug("Setting first match at {}",name);
                firstMatch = currObject;
            }
        }
        return null;
    }
    
    private boolean matches(T currObject, String searchString) {
        // TODO: search the entire row in the table for this object
        String name = tableViewer.getImageModel().getImageLabel(currObject);
        //return name.toUpperCase().contains(searchString);
        for(DynamicColumn column : tableViewer.getColumns()) {
            Object value = tableViewer.getValue(currObject, column.getName());
            if (value!=null) {
                if (value.toString().toUpperCase().contains(searchString)) {
                    log.trace("Found match in attribute {} of {}",column.getLabel(),name    );
                    return true;
                }
            }
        }

        return false;

    }
}
