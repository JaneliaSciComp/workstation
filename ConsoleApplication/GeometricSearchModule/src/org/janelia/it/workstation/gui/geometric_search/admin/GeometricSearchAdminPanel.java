package org.janelia.it.workstation.gui.geometric_search.admin;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.table.TableModel;

import org.janelia.it.jacs.compute.api.GeometricSearchBeanRemote;
import org.janelia.it.jacs.shared.geometric_search.GeometricIndexManagerModel;

import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFactory;
import org.janelia.it.workstation.gui.framework.outline.Refreshable;
import org.janelia.it.workstation.gui.framework.table.DynamicColumn;
import org.janelia.it.workstation.gui.framework.table.DynamicRow;
import org.janelia.it.workstation.gui.framework.table.DynamicTable;
import org.janelia.it.jacs.model.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeometricSearchAdminPanel extends JPanel implements Refreshable {

    private static final Logger logger = LoggerFactory.getLogger(GeometricSearchAdminPanel.class);

    private final ScheduledThreadPoolExecutor adminThreadPool=new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> adminThreadFuture=null;

    /** How many results to load at a time */
    protected static final int PAGE_SIZE = 100;

    protected static final String[] scanResultColumnLabels = {"Signature", "Start", "End", "Total", "Successful", "Error", "Active"};


    // UI Settings
    protected Font boldFont = new Font("Sans Serif", Font.BOLD, 11);
    protected Font plainFont = new Font("Sans Serif", Font.PLAIN, 11);

    // UI Elements
    protected final ScanResultTable scanResultTable;
    protected final Map<String, DynamicColumn> scanResultColumnByName = new HashMap<String, DynamicColumn>();


    // Search state
    protected final Map<String, DynamicColumn> columnByName = new HashMap<String, DynamicColumn>();

    // Results
    protected List<GeometricIndexManagerModel> scanResultList;

    GeometricIndexManagerModel b=null;

    private static class ScanResultTable extends DynamicTable {
        private Long modelLastChangedTimestamp=new Long(0L);

        public Long getModelLastChangedTimestamp() {
            return modelLastChangedTimestamp;
        }

        public void setModelLastChangedTimestamp(Long timestamp) {
            this.modelLastChangedTimestamp=timestamp;
        }

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

        public void updateUserData(List<GeometricIndexManagerModel> newModelList) {

            List<DynamicColumn> columns=getColumns();
            List<DynamicRow> rows=getRows();
            TableModel tableModel=getTableModel();

            int i=0;
            for (DynamicRow row : rows) {
                Object newUserObject=newModelList.get(i);
                int j=0;
                for(DynamicColumn column : columns) {
                    Object value = getValue(row.getUserObject(), column);
                    Object newValue = getValue(newUserObject, column);
                    if (!newValue.equals(value)) {
                        tableModel.setValueAt(newValue, i, j);
                    }
                    j++;
                }
                i++;
            }

        }

    }

    public GeometricSearchAdminPanel() {
        setLayout(new BorderLayout());

        // --------------------------------
        // Results on right
        // --------------------------------
        scanResultTable = new ScanResultTable();

        configureScanResultTableColumns();

        scanResultTable.setMaxColWidth(80);
        scanResultTable.setMaxColWidth(600);
        scanResultTable.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 5));

        JLabel activeDataLabel = new JLabel("Active Data");
        activeDataLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(activeDataLabel, BorderLayout.NORTH);
        add(scanResultTable, BorderLayout.CENTER);

        scanResultTable.updateTableModel();

        refresh();
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
        if (adminThreadFuture!=null) {
            adminThreadFuture.cancel(true);
        }
        adminThreadFuture = adminThreadPool.scheduleWithFixedDelay(new GeometricSearchAdminThread(scanResultTable), 0, 1000, TimeUnit.MILLISECONDS);
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

    private static class GeometricSearchAdminThread implements Runnable {
        ScanResultTable scanResultTable;

        public GeometricSearchAdminThread(ScanResultTable scanResultTable) {
            this.scanResultTable=scanResultTable;
        }

        @Override
        public void run() {
            GeometricSearchBeanRemote gsEJB=EJBFactory.getRemoteGeometricSearchBean();
            List<GeometricIndexManagerModel> modelList=null;
            try {
                modelList=gsEJB.getManagerModel(100);
//                Long modelTimestamp=gsEJB.getModifiedTimestamp();
//                if (modelTimestamp>scanResultTable.getModelLastChangedTimestamp()) {
//                    scanResultTable.setModelLastChangedTimestamp(modelTimestamp);
//                    modelList = gsEJB.getManagerModel(100);
//                }
            } catch (Exception ex) {
                logger.error("Exception using RemoteGeometricSearchBean: " + ex.getMessage(), ex);
            }
            if (modelList!=null) {
                if (modelList.size()==scanResultTable.getRows().size()) {
                    scanResultTable.updateUserData(modelList);
                } else {
                    scanResultTable.removeAllRows();
                    for (GeometricIndexManagerModel model : modelList) {
                        scanResultTable.addRow(model);
                    }
                    scanResultTable.updateTableModel();
                }
            }
        }
    }

}