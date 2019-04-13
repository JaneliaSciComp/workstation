package org.janelia.workstation.browser.gui.listview.table;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.janelia.workstation.integration.FrameworkImplProvider;
import org.janelia.model.domain.DomainConstants;

/**
 * UI configuration for a TableViewerPanel. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TableViewerConfiguration {

    private Set<String> hiddenColumns = new HashSet<>();

    public TableViewerConfiguration() {
    }
    
    public void setColumnVisibility(String columnName, boolean visible) {
        if (visible) {
            hiddenColumns.remove(columnName);
        }
        else {
            hiddenColumns.add(columnName);
        }
    }
    
    public boolean isColumnVisible(String columnName) {
        return !hiddenColumns.contains(columnName);
    }

    public void clearHiddenColumns() {
        hiddenColumns.clear();
    }

    public Set<String> getHiddenColumns() {
        return hiddenColumns;
    }
 
    private static String serialize(TableViewerConfiguration config) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(config);
    }

    private static TableViewerConfiguration deserialize(String json) throws Exception  {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, TableViewerConfiguration.class);
    }

    public static TableViewerConfiguration loadConfig() {
        try {
            TableViewerConfiguration config;
            String columnsPreference = FrameworkImplProvider.getRemotePreferenceValue(DomainConstants.PREFERENCE_CATEGORY_TABLE_COLUMNS, DomainConstants.PREFERENCE_CATEGORY_TABLE_COLUMNS, null);
            
            if (columnsPreference==null) {
                config = new TableViewerConfiguration();
            }
            else {
                try {
                    config = TableViewerConfiguration.deserialize(columnsPreference);
                }
                catch (Exception e) {
                    throw new IllegalStateException("Cannot deserialize column preference: "+columnsPreference);
                }
            }

            return config;
        }  
        catch (Exception e) {
            FrameworkImplProvider.handleException(e);
            return null;
        }
    }

    public void save() throws Exception {
        String value = TableViewerConfiguration.serialize(this);
        FrameworkImplProvider.setRemotePreferenceValue(DomainConstants.PREFERENCE_CATEGORY_TABLE_COLUMNS, DomainConstants.PREFERENCE_CATEGORY_TABLE_COLUMNS, value);
    }
}
