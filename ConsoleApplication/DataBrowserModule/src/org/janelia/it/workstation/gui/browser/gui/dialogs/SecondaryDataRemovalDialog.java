package org.janelia.it.workstation.gui.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.workstation.shared.workers.TaskMonitoringWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog for viewing and selecting subparts of a sample, to prune their
 * secondary data, setting flags to prevent its re-creation.
 *
 * @author <a href="mailto:fosterl@janelia.hhmi.org">Les Foster</a>
 */
public class SecondaryDataRemovalDialog extends ModalDialog {
    public static final String PART_LABEL = "Anatomical Area";
    // For now, keeping it simple.
    private static final String[] COLUMN_HEADERS = new String[] {
        PART_LABEL, "Objective"
    };

    private static final Font separatorFont = new Font("Sans Serif", Font.BOLD, 12);

    private JButton executeButton;
    private Sample sample;
    private JTable sampleSubPartTable;
    private boolean debug = false;
    private final Logger log = LoggerFactory.getLogger(SecondaryDataRemovalDialog.class);

    public SecondaryDataRemovalDialog(JFrame parent, Sample sample, boolean debug) {
        this(parent, sample);
        this.debug = debug;
    }
    public SecondaryDataRemovalDialog(JFrame parent, Sample sample) {

        super(parent);
        this.sample = sample;

        setTitle("Sample Selective Secondary Content Removal");

        setSize(new Dimension(500, 500));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without executing deletion");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        this.executeButton = new JButton("Delete Marked Items");
        executeButton.setToolTipText("Delete the secondary data of the marked items, and prevent that data re-creation if sample is rerun.");
        executeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Need to take the selection in the table, as the list
                // of things to be deleted.
                completeOperation(Collections.EMPTY_LIST);
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(executeButton);
        
        constructTable();
        add(sampleSubPartTable, BorderLayout.CENTER);
        add(buttonPane, BorderLayout.SOUTH);
    }

    public void addSeparator(JPanel panel, String text, boolean first) {
        JLabel label = new JLabel(text);
        label.setFont(separatorFont);
        panel.add(label, "split 2, span" + (first ? "" : ", gaptop 10lp"));
        panel.add(new JSeparator(SwingConstants.HORIZONTAL), "growx, wrap, gaptop 10lp");
    }

    //public static String getAnatomicalAreaRemovalTarget(Sample sample) {
    //    // Find all anatomical areas.
    //    sample.getObjectives()
    //}

    /**
     * Supporting a remove/preclude for the stitched image.  Multiple, possible objectives may have a stitched
     * image.
     *
     * @param sample which sample to examine.
     * @return sufficient information for caller to decide what to do.
     */
    public static String getStitchedImageRemovalTarget(Sample sample) {
        // Find all objectives containing a stitched image.
        List<String> objectivesWithStitchedImages = new ArrayList<>();
        for (ObjectiveSample os: sample.getObjectiveSamples()) {
            SamplePipelineRun run = os.getLatestSuccessfulRun();
            SampleProcessingResult spr = run.getLatestProcessingResult();
            Map<FileType, String> fileTypeToFile = spr.getFiles();
            String losslessStack = fileTypeToFile.get(FileType.VisuallyLosslessStack);
            if (losslessStack.contains("/stitched")) {
                objectivesWithStitchedImages.add(os.getObjective());
            }
            //run.getResultsById()
        }
        String foundValue = null;
        if (! objectivesWithStitchedImages.isEmpty()) {
            if (objectivesWithStitchedImages.size() == 1) {
                int response = JOptionPane.showConfirmDialog(
                        FrameworkImplProvider.getMainFrame(),
                        "Confirm Removal", "Are you sure you would like to remove " + sample.getName() + "'s Stitched Image?",
                        JOptionPane.YES_NO_OPTION);
                if (response == JOptionPane.YES_OPTION) {
                    foundValue = objectivesWithStitchedImages.get(0);
                }
            }
            else {
                // Should show a dropdown selection of possibilities.
                foundValue = (String)JOptionPane.showInputDialog(
                        FrameworkImplProvider.getMainFrame(),
                        "Please select an objective for stitched image removal",
                        "Stitched Image Objective Selection",
                        JOptionPane.OK_CANCEL_OPTION,
                        null,
                        objectivesWithStitchedImages.toArray(new String[objectivesWithStitchedImages.size()]),
                        objectivesWithStitchedImages.get(0)

                );
            }
        }

        return foundValue; // may be "63x", etc.
    }
    
    private void completeOperation(final List<Object> deletedItems) {

        Utils.setWaitingCursor(SecondaryDataRemovalDialog.this);

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                if (! deletedItems.isEmpty()  &&  !debug) {
                    launchDeletionTask();
                }

            }

            @Override
            protected void hadSuccess() {
                //SecondaryDataRemovalDialog.this.refresh();
                Utils.setDefaultCursor(SecondaryDataRemovalDialog.this);
                setVisible(false);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                Utils.setDefaultCursor(SecondaryDataRemovalDialog.this);
                setVisible(false);
            }
        };

        worker.execute();
    }
    
    private void constructTable() {
        sampleSubPartTable = new JTable();
        sampleSubPartTable.setCellSelectionEnabled(false);
        sampleSubPartTable.setColumnSelectionAllowed(false);
        sampleSubPartTable.setRowSelectionAllowed(true);
        sampleSubPartTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        // Scrolling, as needed.  Likely the vertical is never needed.
        JScrollPane scrollPane = new JScrollPane(sampleSubPartTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sampleSubPartTable.setFillsViewportHeight(true);
        final DefaultTableModel tableModel = new DefaultTableModel();
        Map<String,Set<String>> osToAA = new HashMap<>();
        int rowCount = 0;
        for (ObjectiveSample os: sample.getObjectiveSamples()) {
            Set<String> aaList = new TreeSet<>();
            osToAA.put(os.getObjective(), aaList);

            SamplePipelineRun run = os.getLatestSuccessfulRun();
            for (SampleProcessingResult spr: run.getSampleProcessingResults()) {
                aaList.add(spr.getAnatomicalArea());
                rowCount ++;
            }
        }
        final String[][] rowData = new String[rowCount][COLUMN_HEADERS.length];
        int i = 0;
        for (String key: osToAA.keySet()) {
            for (String aa: osToAA.get(key)) {
                rowData[i][0] = aa;
                rowData[i][1] = key;
            }
        }
//        final String[][] tempData = new String[][] {
//            new String[] { "VNC", },       // a row
//            new String[] { "Brain", },     // a row
//        };
//        TreeMap<String,String[]> sortedMap = new TreeMap<String,String[]>();
//        for (String[] rowArr: tempData) {
//            sortedMap.put(rowArr[0], rowArr);
//        }
//        int i = 0;
//        for (String nextKey: sortedMap.keySet()) {
//            tempData[i++] = sortedMap.get(nextKey);
//        }
        tableModel.setDataVector(rowData, COLUMN_HEADERS);
        sampleSubPartTable.setModel(tableModel);
    }

    private void launchDeletionTask() {
        Task task;
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            task = ModelMgr.getModelMgr().submitJob("SamplePartialSecondaryDataRemoval", "Remove Partial Secondary Data", taskParameters);
            taskParameters.add(new TaskParameter("sample entity id", sample.getId().toString(), null));
            int[] selectedRows = sampleSubPartTable.getSelectedRows();
            StringBuilder subpartNames = new StringBuilder();
            
            int colnum = 0;
            for (int i = 0; i < sampleSubPartTable.getColumnCount(); i++) {
                if (PART_LABEL.equals(sampleSubPartTable.getModel().getColumnName(i))) {
                    colnum = i;
                }
            }
            for (int selectedRow: selectedRows) {
                if (subpartNames.length() > 0) {
                    subpartNames.append(",");
                }
                subpartNames.append(sampleSubPartTable.getModel().getValueAt(selectedRow, colnum));
            }

            // Give the user a last opt-out.
            final String message = "Are you sure you want to remove " + subpartNames.toString() + " from sample " + sample.getName() + "?";
            if (JOptionPane.showConfirmDialog(this, message, "Confirm Removal", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
            
            taskParameters.add(new TaskParameter("subpart names", subpartNames.toString(), task));
            log.info("Submitted task ().",  task.getObjectId());
        } catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
            return;
        }

        TaskMonitoringWorker taskWorker = new TaskMonitoringWorker(task.getObjectId()) {

            @Override
            public String getName() {
                return "Trimming unwanted "+PART_LABEL+"(s) of the sample";
            }

            @Override
            protected void doStuff() throws Exception {
                setStatus("Executing");
                super.doStuff();
            }

            @Override
            public Callable<Void> getSuccessCallback() {
                return new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        SessionMgr.getBrowser().getEntityOutline().refresh(true, true, null);
                        return null;
                    }
                };
            }
        };

        taskWorker.executeWithEvents();
    }
}
