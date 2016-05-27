package org.janelia.it.workstation.gui.browser.gui.listview.table;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;

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

        TableViewerConfiguration config;
        Preference columnsPreference = DomainMgr.getDomainMgr().getPreference(
                DomainConstants.PREFERENCE_CATEGORY_TABLE_COLUMNS,
                DomainConstants.PREFERENCE_CATEGORY_TABLE_COLUMNS);

        if (columnsPreference==null) {
            config = new TableViewerConfiguration();
        }
        else {
            try {
                config = TableViewerConfiguration.deserialize((String)columnsPreference.getValue());
            }
            catch (Exception e) {
                throw new IllegalStateException("Cannot deserialize column preference: "+columnsPreference.getValue());
            }
        }

        return config;
    }

    public void save() throws Exception {
        Preference columnsPreference = new Preference(AccessManager.getSubjectKey(),
                DomainConstants.PREFERENCE_CATEGORY_TABLE_COLUMNS,
                DomainConstants.PREFERENCE_CATEGORY_TABLE_COLUMNS, "");
        String value = TableViewerConfiguration.serialize(this);
        columnsPreference.setValue(value);
        DomainMgr.getDomainMgr().savePreference(columnsPreference);
    }
}
