package org.janelia.it.workstation.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.model.DomainObjectChangeEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectCreateEvent;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.table.DynamicColumn;
import org.janelia.workstation.common.gui.table.DynamicRow;
import org.janelia.workstation.common.gui.table.DynamicTable;
import org.janelia.workstation.common.gui.util.UIUtils;
import org.janelia.it.workstation.browser.model.DomainModelViewConstants;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.domain.sample.DataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * Dialog for viewing and editing Data Sets. 
 *
 * @author <a href="mailto:schauderd@janelia.hhmi.org">David Schauder</a>
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataSetListDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(DataSetListDialog.class);
    
    private final JLabel loadingLabel;
    private final JPanel mainPanel;
    private final DynamicTable dynamicTable;
    private final DataSetDialog dataSetDialog;
    private final DomainDetailsDialog detailsDialog;
    private final CompressionDialog compressionDialog;
    
    public DataSetListDialog() {
        setTitle("Data Sets");

        dataSetDialog = new DataSetDialog(this);
        detailsDialog = new DomainDetailsDialog(this);
        compressionDialog = new CompressionDialog(this);
        
        loadingLabel = new JLabel();
        loadingLabel.setOpaque(false);
        loadingLabel.setIcon(Icons.getLoadingIcon());
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setVerticalAlignment(SwingConstants.CENTER);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(loadingLabel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        dynamicTable = new DynamicTable(true, false) {
            @Override
            public Object getValue(Object userObject, DynamicColumn column) {
                DataSet dataSet = (DataSet) userObject;
                if (dataSet != null) {
                    if ((DomainModelViewConstants.DATASET_OWNER).equals(column.getName())) {
                        return dataSet.getOwnerName();
                    }
                    if (DomainModelViewConstants.DATASET_NAME.equals(column.getName())) {
                        return dataSet.getName();
                    }
                    else if ((DomainModelViewConstants.DATASET_PIPELINE_PROCESS).equals(column.getName())) {
                        List<String> processes = dataSet.getPipelineProcesses();
                        return processes == null || processes.isEmpty() ? null : dataSet.getPipelineProcesses().get(0);
                    }
                    else if ((DomainModelViewConstants.DATASET_SAMPLE_NAME).equals(column.getName())) {
                        return dataSet.getSampleNamePattern();
                    }
                    else if ((DomainModelViewConstants.DATASET_SAGE_SYNC).equals(column.getName())) {
                        return dataSet.isSageSync();
                    }
                    else if ((DomainModelViewConstants.DATASET_NEURON_SEPARATION).equals(column.getName())) {
                        return dataSet.isNeuronSeparationSupported();
                    }
                }
                return null;
            }

            @Override
            protected JPopupMenu createPopupMenu(MouseEvent e) {
                JPopupMenu menu = super.createPopupMenu(e);

                if (menu != null) {
                    JTable table = getTable();
                    ListSelectionModel lsm = table.getSelectionModel();
                    if (lsm.getMinSelectionIndex() != lsm.getMaxSelectionIndex()) {
                        return menu;
                    }

                    final DataSet dataSet = (DataSet) getRows().get(table.getSelectedRow()).getUserObject();

                    JMenuItem editItem = new JMenuItem("  View Details");
                    editItem.addActionListener((e2) -> {
                        dataSetDialog.showForDataSet(dataSet);
                    });
                    menu.add(editItem);

                    JMenuItem permItem = new JMenuItem("  Change Permissions");
                    permItem.addActionListener((e2) -> {
                        detailsDialog.showForDomainObject(dataSet, DomainInspectorPanel.TAB_NAME_PERMISSIONS);
                    });
                    
                    menu.add(permItem);

                    JMenuItem compItem = new JMenuItem("  Change Default Compression Strategy");
                    compItem.addActionListener((e2) -> {
                        compressionDialog.showForDataSet(dataSet);
                    });
                    menu.add(compItem);
                    
                    JMenuItem deleteItem = new JMenuItem("  Delete");
                    deleteItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {

                            int result = JOptionPane.showConfirmDialog(DataSetListDialog.this, "Are you sure you want to delete data set '" + dataSet.getName()
                                            + "'? This will not delete the images associated with the data set.",
                                    "Delete Data Set", JOptionPane.OK_CANCEL_OPTION);
                            if (result != 0) {
                                return;
                            }

                            UIUtils.setWaitingCursor(DataSetListDialog.this);

                            SimpleWorker worker = new SimpleWorker() {

                                @Override
                                protected void doStuff() throws Exception {
                                    final DomainModel model = DomainMgr.getDomainMgr().getModel();
                                    model.remove(dataSet);
                                }

                                @Override
                                protected void hadSuccess() {
                                    UIUtils.setDefaultCursor(DataSetListDialog.this);
                                    loadDataSets();
                                }

                                @Override
                                protected void hadError(Throwable error) {
                                    FrameworkImplProvider.handleException(error);
                                    UIUtils.setDefaultCursor(DataSetListDialog.this);
                                    loadDataSets();
                                }
                            };
                            worker.execute();
                        }
                    });
                    menu.add(deleteItem);
                    
                    if (!ClientDomainUtils.hasWriteAccess(dataSet)) {
                        permItem.setEnabled(false);
                        deleteItem.setEnabled(false);
                        compItem.setEnabled(false);
                    }
                }

                return menu;
            }

            @Override
            protected void rowDoubleClicked(int row) {
                final DataSet dataSet = (DataSet) getRows().get(row).getUserObject();
                dataSetDialog.showForDataSet(dataSet);
            }

            @Override
            public Class<?> getColumnClass(int column) {
                DynamicColumn dc = getColumns().get(column);
                if (dc.getName().equals(DomainModelViewConstants.DATASET_SAGE_SYNC)  ||
                    dc.getName().equals(DomainModelViewConstants.DATASET_NEURON_SEPARATION)) {
                    return Boolean.class;
                }
                return super.getColumnClass(column);
            }

            @Override
            protected void valueChanged(final DynamicColumn dc, int row, Object data) {
                if (dc.getName().equals(DomainModelViewConstants.DATASET_SAGE_SYNC) ||
                    dc.getName().equals(DomainModelViewConstants.DATASET_NEURON_SEPARATION)) {
                    
                    final Boolean selected = data == null ? Boolean.FALSE : (Boolean) data;
                    DynamicRow dr = getRows().get(row);
                    final DataSet dataSet = (DataSet) dr.getUserObject();
                    
                    if (!ClientDomainUtils.hasWriteAccess(dataSet)) {
                        JOptionPane.showMessageDialog(DataSetListDialog.this,
                                "You do not have permission to change this data set",
                                "Permission denied", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    SimpleWorker.runInBackground(() -> {
                        DomainModel model = DomainMgr.getDomainMgr().getModel();
                        try {
                            if (dc.getName().equals(DomainModelViewConstants.DATASET_SAGE_SYNC)) {
                                dataSet.setSageSync(selected);
                                model.save(dataSet);
                            }
                            if (dc.getName().equals(DomainModelViewConstants.DATASET_NEURON_SEPARATION)) {
                                dataSet.setNeuronSeparationSupported(selected);
                                model.save(dataSet);
                            }
                        }
                        catch (Exception e) {
                            FrameworkImplProvider.handleException(e);
                        }
                    });
                }
            }
        };

        dynamicTable.addColumn(DomainModelViewConstants.DATASET_OWNER);
        dynamicTable.addColumn(DomainModelViewConstants.DATASET_NAME);
        dynamicTable.addColumn(DomainModelViewConstants.DATASET_PIPELINE_PROCESS);
        dynamicTable.addColumn(DomainModelViewConstants.DATASET_SAMPLE_NAME);
        // Disable editable columns because it's too easy to make a mistake, and there is no undo or cancel
        dynamicTable.addColumn(DomainModelViewConstants.DATASET_SAGE_SYNC);//.setEditable(true);
        dynamicTable.addColumn(DomainModelViewConstants.DATASET_NEURON_SEPARATION);//.setEditable(SUPPORT_NEURON_SEPARATION_PARTIAL_DELETION_IN_GUI);

        JButton addButton = new JButton("Add new");
        addButton.setToolTipText("Add a new data set definition");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dataSetDialog.showForNewDataSet();
            }
        });

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close this dialog");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(addButton);
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);
    }

    public void showDialog() {
        loadDataSets();

        Component mainFrame = FrameworkImplProvider.getMainFrame();
        setPreferredSize(new Dimension((int) (mainFrame.getWidth() * 0.5), (int) (mainFrame.getHeight() * 0.4)));

        ActivityLogHelper.logUserAction("DataSetListDialog.showDialog");

        // Show dialog and wait
        Events.getInstance().registerOnEventBus(this);
        packAndShow();
        Events.getInstance().unregisterOnEventBus(this);
    }

    private void loadDataSets() {

        log.info("loadDataSets");
        mainPanel.removeAll();
        mainPanel.add(loadingLabel, BorderLayout.CENTER);

        SimpleWorker worker = new SimpleWorker() {

            private List<DataSet> dataSetList = new ArrayList<>();

            @Override
            protected void doStuff() throws Exception {
                for (DataSet dataSet : DomainMgr.getDomainMgr().getModel().getDataSets()) {
                    dataSetList.add(dataSet);
                }
            }

            @Override
            protected void hadSuccess() {

                // Update the attribute table
                dynamicTable.removeAllRows();
                for (DataSet dataSet : dataSetList) {
                    dynamicTable.addRow(dataSet);
                }

                dynamicTable.updateTableModel();
                mainPanel.removeAll();
                mainPanel.add(dynamicTable, BorderLayout.CENTER);
                mainPanel.revalidate();
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkImplProvider.handleException(error);
                mainPanel.removeAll();
                mainPanel.add(dynamicTable, BorderLayout.CENTER);
                mainPanel.revalidate();
            }
        };
        worker.execute();
    }
    
    public void refresh() {
        loadDataSets();
    }

    public void totalRefresh() {
        throw new UnsupportedOperationException();
    }

    @Subscribe
    public void objectChanged(DomainObjectCreateEvent event) {
        // This happens if a new data set is created
        loadDataSets();
    }
    
    @Subscribe
    public void objectChanged(DomainObjectChangeEvent event) {
        // This happens if the child dialog changes anything about the selected data set
        loadDataSets();
    }
}
