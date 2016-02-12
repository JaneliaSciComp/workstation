package org.janelia.it.workstation.gui.browser.gui.dialogs;

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

import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.entity.cv.NamedEnum;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.gui.table.DynamicColumn;
import org.janelia.it.workstation.gui.browser.gui.table.DynamicRow;
import org.janelia.it.workstation.gui.browser.gui.table.DynamicTable;
import org.janelia.it.workstation.gui.browser.model.DomainConstants;
import org.janelia.it.workstation.gui.dialogs.ModalDialog;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

/**
 * A port of data sets management dialog to use domain objects
 *
 * @author <a href="mailto:schauderd@janelia.hhmi.org">David Schauder</a>
 */


public class DataSetListDialog extends ModalDialog {

    private final JLabel loadingLabel;
    private final JPanel mainPanel;
    private final DynamicTable dynamicTable;
    private final DataSetDialog dataSetDialog;

    public DataSetListDialog() {
        setTitle("My Data Sets");

        dataSetDialog = new DataSetDialog(this);

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
                    if (DomainConstants.DATASET_NAME.equals(column.getName())) {
                        return dataSet.getName();
                    } else if ((DomainConstants.DATASET_PIPELINE_PROCESS).equals(column.getName())) {
                        return dataSet.getPipelineProcesses()==null?null:dataSet.getPipelineProcesses().get(0);
                    } else if ((DomainConstants.DATASET_SAMPLE_NAME).equals(column.getName())) {
                        return dataSet.getSampleNamePattern();
                    } else if ((DomainConstants.DATASET_SAGE_SYNC).equals(column.getName())) {
                        if (dataSet.getSageSync()==null) {
                            return Boolean.FALSE;
                        }
                        return dataSet.getSageSync();
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

                    JMenuItem editItem = new JMenuItem("  Edit");
                    editItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            dataSetDialog.showForDataSet(dataSet);
                        }
                    });
                    menu.add(editItem);

                    JMenuItem deleteItem = new JMenuItem("  Delete");
                    deleteItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {

                            int result = JOptionPane.showConfirmDialog(SessionMgr.getMainFrame(), "Are you sure you want to delete data set '" + dataSet.getName()
                                            + "'? This will not delete the images associated with the data set.",
                                    "Delete Data Set", JOptionPane.OK_CANCEL_OPTION);
                            if (result != 0) {
                                return;
                            }

                            Utils.setWaitingCursor(DataSetListDialog.this);

                            SimpleWorker worker = new SimpleWorker() {

                                @Override
                                protected void doStuff() throws Exception {
                                    final DomainModel model = DomainMgr.getDomainMgr().getModel();
                                    model.remove(dataSet);
                                }

                                @Override
                                protected void hadSuccess() {
                                    Utils.setDefaultCursor(DataSetListDialog.this);
                                    loadDataSets();
                                }

                                @Override
                                protected void hadError(Throwable error) {
                                    SessionMgr.getSessionMgr().handleException(error);
                                    Utils.setDefaultCursor(DataSetListDialog.this);
                                    loadDataSets();
                                }
                            };
                            worker.execute();
                        }
                    });
                    menu.add(deleteItem);
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
                if (dc.getName().equals(DomainConstants.DATASET_SAGE_SYNC)) {
                    return Boolean.class;
                }
                return super.getColumnClass(column);
            }

            @Override
            protected void valueChanged(DynamicColumn dc, int row, Object data) {
                if (dc.getName().equals(DomainConstants.DATASET_SAGE_SYNC)) {
                    final Boolean selected = data == null ? Boolean.FALSE : (Boolean) data;
                    DynamicRow dr = getRows().get(row);
                    final DataSet dataSet = (DataSet) dr.getUserObject();
                    SimpleWorker worker = new SimpleWorker() {

                        @Override
                        protected void doStuff() throws Exception {
                            DomainModel model = DomainMgr.getDomainMgr().getModel();
                            dataSet.setSageSync(selected);
                            model.save(dataSet);
                        }

                        @Override
                        protected void hadSuccess() {
                        }

                        @Override
                        protected void hadError(Throwable error) {
                            SessionMgr.getSessionMgr().handleException(error);
                        }
                    };
                    worker.execute();
                }
            }
        };

        dynamicTable.addColumn(DomainConstants.DATASET_NAME);
        dynamicTable.addColumn(DomainConstants.DATASET_PIPELINE_PROCESS);
        dynamicTable.addColumn(DomainConstants.DATASET_SAMPLE_NAME);
        dynamicTable.addColumn(DomainConstants.DATASET_SAGE_SYNC).setEditable(true);

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

        Component mainFrame = SessionMgr.getMainFrame();
        setPreferredSize(new Dimension((int) (mainFrame.getWidth() * 0.4), (int) (mainFrame.getHeight() * 0.4)));

        // Show dialog and wait
        packAndShow();
    }

    private void loadDataSets() {

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
                SessionMgr.getSessionMgr().handleException(error);
                mainPanel.removeAll();
                mainPanel.add(dynamicTable, BorderLayout.CENTER);
                mainPanel.revalidate();
            }
        };
        worker.execute();
    }

    private String decodeEnumList(Class enumType, String list) {

        StringBuffer buf = new StringBuffer();
        for (String key : list.split(",")) {
            if (key.isEmpty()) {
                continue;
            }
            try {
                String value = ((NamedEnum) Enum.valueOf(enumType, key)).getName();
                if (buf.length() > 0) {
                    buf.append(", ");
                }
                buf.append(value);
            } catch (Exception e) {
                SessionMgr.getSessionMgr().handleException(new Exception("Unrecognized enumerated value: " + key));
            }
        }
        return buf.toString();
    }

    public void refresh() {
        loadDataSets();
    }

    public void totalRefresh() {
        throw new UnsupportedOperationException();
    }
}
