package org.janelia.it.workstation.gui.browser.gui.listview.table;

import java.util.HashSet;
import java.util.Set;

/**
 * UI configuration for a TableViewerPanel. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TableViewerConfiguration {

    private Set<String> hiddenColumns = new HashSet<>();
    
    public void setAttributeVisibility(String attrName, boolean visible) {
        if (visible) {
            hiddenColumns.remove(attrName);
        }
        else {
            hiddenColumns.add(attrName);
        }
    }
    
    public boolean isVisible(String attrName) {
        return !hiddenColumns.contains(attrName);
    }

    public void clear() {
        hiddenColumns.clear();
    }
    
}
