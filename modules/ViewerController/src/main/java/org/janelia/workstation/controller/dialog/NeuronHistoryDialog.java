package org.janelia.workstation.controller.dialog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;
import org.apache.axis2.databinding.types.xsd.String;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.action.SwcExport;
import org.janelia.workstation.controller.eventbus.NeuronHistoryEvent;
import org.janelia.workstation.controller.model.TmHistoricalEvent;
import org.janelia.workstation.controller.model.TmHistory;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class uses a buffer to store the last ten neuron updates that have occurred.
 * It displays these items as a history and neurons can be jumped and restored to that history.
 * However, you can also walk back and forth through the history using the Undo-Redo
 * commands.  An undo does a regular save, but starts doing a painful byte compare on updates to determine
 * whether new changes are coming in.  This enables redo to be executed as expected.  As soon
 * as a new change comes in, history starts being appended to again.
 **/
public class NeuronHistoryDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(NeuronGroupsDialog.class);

    private final JButton closeButton;
    private final JTable historyTable;
    private final JPanel buttonPane;
    private final JPanel groupsPanel;

    private NeuronManager neuronManager;
    private NeuronHistoryDialog dialog;
       
    public NeuronHistoryDialog() {
        super(FrameworkAccess.getMainFrame());

        setTitle("View change history");

        groupsPanel = new JPanel();
        add(groupsPanel, BorderLayout.CENTER);

        closeButton = new JButton("Close");
        closeButton.setToolTipText("Close this window");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                ViewerEventBus.unregisterForEvents(this);
            }
        });

        ViewerEventBus.registerForEvents(this);
        NeuronHistoryTableModel tableModel = new NeuronHistoryTableModel();

        historyTable = new JTable(tableModel);
        historyTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        TableCellRenderer buttonRenderer = new JTableButtonRenderer();
        TableCellEditor buttonEditor = new JTableButtonEditor();

        historyTable.getColumn("Restore Neuron").setCellRenderer(buttonRenderer);
        historyTable.getColumn("Download SWC").setCellRenderer(buttonRenderer);
        historyTable.getColumn("Restore Neuron").setCellEditor(buttonEditor);
        historyTable.getColumn("Download SWC").setCellEditor(buttonEditor);

        // populate the history table
        TmHistory neuronHistory = TmModelManager.getInstance().getNeuronHistory();
        for (TmHistoricalEvent event: neuronHistory.getHistoryOperations()) {
            tableModel.addRow(event.getNeurons(), event.getType(), event.getTimestamp());
        }

        buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(closeButton);


        add(new JScrollPane(historyTable), BorderLayout.CENTER);
        add(buttonPane, BorderLayout.SOUTH);
    }

    @Subscribe
    public void updateHistory (NeuronHistoryEvent event) {
        try {
            ((NeuronHistoryTableModel)historyTable.getModel()).addRow(event.getHistoricalEvent().getNeurons(),
                    event.getHistoricalEvent().getType(), event.getHistoricalEvent().getTimestamp());
        } catch (Exception e) {
            log.error("Issue parsing historical neuron data", e);
        }
    }
    
    public void showDialog() {   
        packAndShow();
    }
    
    class NeuronHistoryTableModel extends AbstractTableModel {
        java.lang.String[] columnNames = {"Timestamp", "Neuron ID", "Neuron Name",
                "Event",
                "Restore Neuron",
                "Download SWC"};
        List<List<Object>> data = new ArrayList<List<Object>>();
        
        public int getColumnCount() {
            return columnNames.length;
        }


        @Override
        public boolean isCellEditable(int row, int col) {
            if (col>3)
                return true;
            return false;
        }

        public int getRowCount() {
            return data.size();
        }

        public java.lang.String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            return data.get(row).get(col);
        }

        public Class getColumnClass(int c) {
            return (getValueAt(0, c)==null?String.class:getValueAt(0,c).getClass());
        }

        public void addRow(Map<Long,byte[]> neuronMap, TmHistoricalEvent.EVENT_TYPE type, Date timestamp) {
            List<TmNeuronMetadata> backupNeurons = new ArrayList<>();
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                for (Long neuronId: neuronMap.keySet()) {

                    backupNeurons.add(objectMapper.readValue(
                            neuronMap.get(neuronId),TmNeuronMetadata.class));
                }
            } catch (IOException e) {
                FrameworkAccess.handleException(e);
            }

            int dataRow = getRowCount();
            List row = new ArrayList<Object>();
            DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
            row.add(df.format(timestamp));

            switch (type) {
                case NEURON_MERGE:
                    StringJoiner ids = new StringJoiner(",");
                    StringJoiner names = new StringJoiner(",");
                    for (TmNeuronMetadata neuron: backupNeurons) {
                        ids.add(neuron.getId().toString());
                        names.add(neuron.getName());
                    }
                    row.add(ids.toString());
                    row.add(names.toString());
                    break;
                default:
                    TmNeuronMetadata neuron = backupNeurons.iterator().next();
                    row.add(neuron.getId());
                    row.add(neuron.getName());
                    break;
            }
            row.add(type);

            JButton restoreButton = new JButton("Restore");
            restoreButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SimpleWorker restorer = new SimpleWorker() {
                        @Override
                        protected void doStuff() throws Exception {
                            TmHistoricalEvent event = TmModelManager.getInstance().getNeuronHistory().restoreAction(dataRow);
                            if (event==null)
                                return;
                            Map<Long,byte[]> neuronMap = event.getNeurons();
                            ObjectMapper objectMapper = new ObjectMapper();

                            for (Long neuronId: neuronMap.keySet()) {
                                TmNeuronMetadata restoredNeuron = objectMapper.readValue(
                                        neuronMap.get(neuronId), TmNeuronMetadata.class);
                                restoredNeuron.initNeuronData();
                                NeuronManager.getInstance().restoreNeuron(restoredNeuron);
                            }
                        }

                        @Override
                        protected void hadSuccess() {
                            // nothing here; annotationModel emits signals
                        }

                        @Override
                        protected void hadError(Throwable error) {
                            FrameworkAccess.handleException(error);
                        }
                    };
                    restorer.execute();
                }
            });

             row.add(restoreButton);

            JButton downloadButton = new JButton("Download");
            downloadButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    List<TmHistoricalEvent> history = TmModelManager.getInstance().
                            getNeuronHistory().getHistoryOperations();
                    TmHistoricalEvent event = history.get(dataRow);

                    SimpleWorker exporter = new SimpleWorker() {
                        @Override
                        protected void doStuff() throws Exception {
                            // convert this serialized version out to SWC file
                            if (backupNeurons.size()>0) {
                                SwcExport export = new SwcExport();
                                SwcExport.ExportParameters params = export.getExportParameters(
                                        backupNeurons.get(0).getName());
                                NeuronManager.getInstance().exportSWCData(params.getSelectedFile(), params.getDownsampleModulo(),
                                        backupNeurons, params.getExportNotes(),this);
                            }
                        }

                        @Override
                        protected void hadSuccess() {
                            // nothing here; annotationModel emits signals
                        }

                        @Override
                        protected void hadError(Throwable error) {
                            FrameworkAccess.handleException(error);
                        }
                    };
                    exporter.execute();
                }
            });

            row.add(downloadButton);

             data.add(row);
             this.fireTableDataChanged();
        }

        public void setValueAt(Object value, int row, int col) {
            data.get(row).set(col, value);
            fireTableCellUpdated(row, col);
        }
    }

    private static class JTableButtonRenderer implements TableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JButton button = (JButton)value;
            return button;
        }
    }

    private static class JTableButtonEditor extends AbstractCellEditor implements TableCellEditor {
        JButton field;

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            field = (JButton)value;
            return field;
        }

        @Override
        public Object getCellEditorValue() {
            return field;
        }

    }
}

