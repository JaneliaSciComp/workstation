package org.janelia.it.workstation.gui.browser.gui.listview.table;

import java.util.HashSet;
import java.util.Set;

/**
 * UI configuration for a TableViewerPanel. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TableViewerConfiguration {

    private Set<String> displayedAttributes = new HashSet<>();
    
    public void setAttributeVisibility(String attrName, boolean visible) {
        if (visible) {
            displayedAttributes.add(attrName);
        }
        else {
            displayedAttributes.remove(attrName);
        }
    }
    
    public boolean isVisible(String attrName) {
        return displayedAttributes.contains(attrName);
    }

    public void clear() {
        displayedAttributes.clear();
    }
    
}
