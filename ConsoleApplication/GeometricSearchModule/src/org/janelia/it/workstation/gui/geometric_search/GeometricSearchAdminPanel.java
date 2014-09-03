package org.janelia.it.workstation.gui.geometric_search;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.*;
import java.util.concurrent.Callable;
import javax.swing.*;

import org.janelia.it.jacs.shared.geometric_search.GeometricIndexManagerModel;

import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFactory;
import org.janelia.it.workstation.gui.framework.outline.Refreshable;
import org.janelia.it.workstation.gui.framework.table.DynamicColumn;
import org.janelia.it.workstation.gui.framework.table.DynamicRow;
import org.janelia.it.workstation.gui.framework.table.DynamicTable;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeometricSearchAdminPanel extends JPanel implements Refreshable {

    private static final Logger logger = LoggerFactory.getLogger(GeometricSearchAdminPanel.class);

    /** How many results to load at a time */
    protected static final int PAGE_SIZE = 100;

    protected static final String[] scanResultColumnLabels = {"Signature", "Start", "End", "Total", "Successful", "Error", "Active"};


    // UI Settings
    protected Font boldFont = new Font("Sans Serif", Font.BOLD, 11);
    protected Font plainFont = new Font("Sans Serif", Font.PLAIN, 11);

    // UI Elements
    protected final DynamicTable scanResultTable;
    protected final Map<String, DynamicColumn> scanResultColumnByName = new HashMap<String, DynamicColumn>();


    // Search state
    protected final Map<String, DynamicColumn> columnByName = new HashMap<String, DynamicColumn>();

    // Results
    protected List<GeometricIndexManagerModel> scanResultList;

    GeometricIndexManagerModel b=null;

    public GeometricSearchAdminPanel() {
        setLayout(new BorderLayout());

        // --------------------------------
        // Results on right
        // --------------------------------
        scanResultTable = new DynamicTable() {
            @Override
            public Object getValue(Object userObject, DynamicColumn column) {
                GeometricIndexManagerModel model = (GeometricIndexManagerModel)userObject;
                if (column.getLabel().equals("Signature")) {
                    return model.getAbbreviatedSignature();
                } else if (column.getLabel().equals("Start")) {
                    Long startTime=model.getStartTime();
                    if (startTime!=null) {
                        return new Date(model.getStartTime()).toString();
                    } else {
                        return "-";
                    }
                } else if (column.getLabel().equals("End")) {
                    Long endTime=model.getEndTime();
                    if (endTime!=null) {
                        return new Date(model.getEndTime()).toString();
                    } else {
                        return "-";
                    }
                } else if (column.getLabel().equals("Total")) {
                    return new Long(model.getTotalIdCount()).toString();
                } else if (column.getLabel().equals("Successful")) {
                    return new Long(model.getSuccessfulCount()).toString();
                } else if (column.getLabel().equals("Error")) {
                    return new Long(model.getErrorCount()).toString();
                } else if (column.getLabel().equals("Active")) {
                    return new Long(model.getActiveScannerCount()).toString();
                } else {
                    return "-";
                }
            }
            @Override
            protected void loadMoreResults(Callable<Void> success) {
                //performSearch(searchResults.getNumLoadedPages(), false, success);
            }

            @Override
            protected void rowClicked(int row) {
                if (row<0) return;
                DynamicRow drow = getRows().get(row);
            }
        };

        configureScanResultTableColumns();

        List<GeometricIndexManagerModel> modelList=null;
        try {
            modelList=EJBFactory.getRemoteGeometricSearchBean().getManagerModel(100);
        } catch (Exception ex) {
            logger.error("Exception using RemoteGeometricSearchBean: " + ex.getMessage(), ex);
        }
        if (modelList!=null) {
            for (GeometricIndexManagerModel model : modelList) {
                scanResultTable.addRow(model);
            }
        }

        scanResultTable.setMaxColWidth(80);
        scanResultTable.setMaxColWidth(600);
        scanResultTable.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 5));

        JLabel activeDataLabel = new JLabel("Active Data");
        activeDataLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(activeDataLabel, BorderLayout.NORTH);
        add(scanResultTable, BorderLayout.CENTER);

        scanResultTable.updateTableModel();
    }

    public void configureScanResultTableColumns() {
        for (String columnLabel : scanResultColumnLabels) {
            DynamicColumn column = scanResultTable.addColumn(columnLabel, columnLabel, true, false, false, true);
            scanResultColumnByName.put(columnLabel, column);
        }
    }

    public void entitySelected(Entity entity) {
    }

    @Override
    public void refresh() {
        //performSearch(false, false, true);
    }

    @Override
    public void totalRefresh() {
        refresh();
    }


    public void clear() {
//        searchResults.clear();
//        statusLabel.setText("");
//        statusLabel.setToolTipText("");
//        facetsPanel.removeAll();
//        resultsTable.removeAllRows();
//        projectionTable.removeAllRows();
//        projectionPane.setVisible(false);
    }

    public DynamicTable getScanResultTable() {
        return scanResultTable;
    }

    public void setColumnVisibility(String attrName, boolean visible) {
        final DynamicColumn column = scanResultTable.getColumn(attrName);
        column.setVisible(visible);
    }

    public List<GeometricIndexManagerModel> getDummyManagerModel() {
        List<GeometricIndexManagerModel> modelList=new ArrayList<>();
        for (int i=0;i<100;i++) {
            GeometricIndexManagerModel model=new GeometricIndexManagerModel();
            model.setScannerSignature("Signature "+i);
            model.setStartTime(new Date().getTime() - 100000L*i);
            model.setEndTime(new Date().getTime() - 100000L*i + 3000);
            model.setTotalIdCount(10000 + i);
            model.setSuccessfulCount( (int)((10000 + i) * 0.99) );
            model.setErrorCount( (int)((10000 + i) * 0.01) );
            model.setActiveScannerCount(30 + i%10);
            modelList.add(model);
        }
        return modelList;
    }

}