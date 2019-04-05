package org.janelia.it.workstation.gui.large_volume_viewer.dialogs;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.gui.dialogs.ModalDialog;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.BackgroundAnnotationListener;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.janelia.messaging.broker.sharedworkspace.MessageType;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminHistoryDialog extends ModalDialog implements BackgroundAnnotationListener {

    private static final Logger log = LoggerFactory.getLogger(NeuronGroupsDialog.class);
  
    private final JButton closeButton;
    private final JTable historyTable;
    private final JPanel buttonPane;

    private final AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
    private AnnotationModel annotationModel;
    private AdminHistoryDialog dialog;
       
    public AdminHistoryDialog() {
    	super(FrameworkImplProvider.getMainFrame());
        dialog = this;
    	
        setTitle("View neuron change history");
        // set to modeless
        setModalityType(Dialog.ModalityType.MODELESS);

        closeButton = new JButton("Close");
        closeButton.setToolTipText("Close this window");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // remove dialog being updated by refreshhandler
                setVisible(false);
                annotationModel.removeBackgroundAnnotationListener(dialog);
            }
        });

        buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(closeButton);

        
        NeuronHistoryTableModel tableModel = new NeuronHistoryTableModel();
        annotationModel = annotationMgr.getAnnotationModel();
        
        historyTable = new JTable(tableModel); 
        historyTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        setLayout(new BorderLayout());
        add(new JScrollPane(historyTable), BorderLayout.CENTER);

        add(buttonPane, BorderLayout.SOUTH);
        
        // add dialog to the listeners being notificed by the refreshHandler
        annotationModel.addBackgroundAnnotationListener(this);
    }
    
    
    public void showDialog() {   
        packAndShow();
    }
    
    private void updateTable(TmNeuronMetadata neuron, MessageType type) {
         try {
            // get workspace and sample
            TmWorkspace workspace = annotationModel.getWorkspace(neuron.getWorkspaceId());
            TmSample sample = annotationModel.getSample(workspace.getSampleId());
            ((NeuronHistoryTableModel)historyTable.getModel()).addRow(neuron, workspace, sample, type);
        } catch (Exception e) {
            log.error("Problem retrieving sample and workspace information");
            e.printStackTrace();
        }
    }

    @Override
    public void neuronModelChanged(TmNeuronMetadata neuron) {
       updateTable (neuron, MessageType.NEURON_SAVE_NEURONDATA);
    }

    @Override
    public void neuronModelCreated(TmNeuronMetadata neuron) {
       updateTable (neuron, MessageType.NEURON_CREATE);
    }

    @Override
    public void neuronModelDeleted(TmNeuronMetadata neuron) {
       updateTable (neuron, MessageType.NEURON_DELETE);
    }

    @Override
    public void neuronOwnerChanged(TmNeuronMetadata neuron) {
       updateTable (neuron, MessageType.NEURON_OWNERSHIP_DECISION);
    }
    
    class NeuronHistoryTableModel extends AbstractTableModel {
        String[] columnNames = {"Sample",
                                "Workspace",
                                "Neuron",
                                "Action",
                                "Download SWC"};
        List<List<Object>> data = new ArrayList<List<Object>>();
        Map<String,Map<String,Object>> metaData;
        
        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.size();
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            log.info("ROW AND COL, {},{}",row,col);
            return data.get(row).get(col);
        }

        public Class getColumnClass(int c) {
            return (getValueAt(0, c)==null?String.class:getValueAt(0,c).getClass());
        }
        
        public void addRow(TmNeuronMetadata neuron, TmWorkspace workspace, TmSample sample, MessageType type) {
             List row = new ArrayList<Object>();
             row.add(sample.getName());
             row.add(workspace.getName());
             row.add(neuron.getName());
             row.add(type.toString());
             row.add("");
             data.add(row);
             this.fireTableDataChanged();
        }

        public void setValueAt(Object value, int row, int col) {
            data.get(row).set(col, value);
            fireTableCellUpdated(row, col);
        }
    }
}

