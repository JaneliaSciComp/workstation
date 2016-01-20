package org.janelia.it.workstation.gui.browser.gui.listview.table;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * UI configuration for a TableViewerPanel. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TableViewerConfiguration {

    private Set<String> hiddenColumns = new HashSet<>();
    
    public TableViewerConfiguration() {
    }
    
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
 
    public static String serialize(TableViewerConfiguration config) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(config);
    }
    
    public static TableViewerConfiguration deserialize(String json) throws Exception  {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, TableViewerConfiguration.class);
    }

    public Set<String> getHiddenColumns() {
        return hiddenColumns;
    }

    public void setHiddenColumns(Set<String> hiddenColumns) {
        this.hiddenColumns = hiddenColumns;
    }
}
