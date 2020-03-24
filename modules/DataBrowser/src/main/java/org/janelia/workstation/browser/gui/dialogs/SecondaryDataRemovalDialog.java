package org.janelia.workstation.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.workstation.core.api.StateMgr;
import org.janelia.workstation.browser.gui.components.DomainExplorerTopComponent;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.common.gui.util.UIUtils;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.core.workers.TaskMonitoringWorker;
import org.janelia.model.domain.sample.ObjectiveSample;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SamplePipelineRun;
import org.janelia.model.domain.sample.SampleProcessingResult;
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
    public static final String OBJECTIVE_LABEL = "Objective";
    public static final String SAMPLE_NAME_LABEL = "Sample Name";
    public static final String SAMPLE_ID_LABEL = "Sample ID";

    public static final String WHOLE_AA_IMPLICATIONS_PROP = "SecondaryDataRemoval.aa_rm_implications";
    public static final String STITCHED_IMPLICATIONS_PROP = "SecondaryDataRemoval.stitched_file_rm_implications";
    public static final String NEURON_SEP_IMPLICATIONS_PROP = "SecondaryDataRemoval.neuron_sep_rm_implications";

    private static final String SAMPLE_ID_DISPLAYABLE_PARM = "sample entity id";
    private static final String SAMPLE_AREAS_DISPLAYABLE_PARM = "sample areas";
    private static final String TRIM_DEPTH_DISPLAYABLE_PARAM = "trim depth";

    private static final String TRIM_DEPTH_AREA_IMAGE_VALUE = "TRIM_AREA_IMAGE";
    private static final String TRIM_DEPTH_WHOLE_AREA_VALUE = "TRIM_WHOLE_AREA";
    private static final String TRIM_DEPTH_NEURON_SEPARATION_VALUE = "TRIM_NEURON_SEPARATION";

    // For now, keeping it simple.
    private static final String[] COLUMN_HEADERS = new String[] {
        PART_LABEL, OBJECTIVE_LABEL, SAMPLE_NAME_LABEL, SAMPLE_ID_LABEL
    };

    private static final Font separatorFont = new Font("Sans Serif", Font.BOLD, 12);

    private JButton executeButton;
    private Sample sample;
    private JTable sampleSubPartTable;
    private String trimDepth;
    private boolean debug = false;
    private final Logger log = LoggerFactory.getLogger(SecondaryDataRemovalDialog.class);

    public SecondaryDataRemovalDialog(JFrame parent, Sample sample, String title, String trimDepth, boolean debug) {
        this(parent, sample, title, trimDepth);
        this.debug = debug;
    }
    public SecondaryDataRemovalDialog(JFrame parent, Sample sample, String title, String trimDepth) {

        super(parent);
        this.sample = sample;
        this.trimDepth = trimDepth;

        setTitle(title);
        int width = 800;
        int height = 500;
        setSize(new Dimension(width, height));
        int x = parent.getLocation().x + ( parent.getWidth() / 2 ) - ( width / 2 );
        int y = parent.getLocation().y + ( parent.getHeight() / 2 ) - ( height / 2 );
        setLocation(x, y);
        
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
                completeOperation();
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(executeButton);
        
        constructTable();
        // Scrolling, as needed.  Likely the vertical is never needed.
        JScrollPane scrollPane = new JScrollPane(sampleSubPartTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sampleSubPartTable.setFillsViewportHeight(true);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPane, BorderLayout.SOUTH);
    }

    public void addSeparator(JPanel panel, String text, boolean first) {
        JLabel label = new JLabel(text);
        label.setFont(separatorFont);
        panel.add(label, "split 2, span" + (first ? "" : ", gaptop 10lp"));
        panel.add(new JSeparator(SwingConstants.HORIZONTAL), "growx, wrap, gaptop 10lp");
    }

    private void completeOperation() {

        UIUtils.setWaitingCursor(SecondaryDataRemovalDialog.this);

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                if (sampleSubPartTable.getSelectedRowCount() > 0  &&  !debug) {
                    launchDeletionTask();
                }

            }

            @Override
            protected void hadSuccess() {
                //SecondaryDataRemovalDialog.this.refresh();
                UIUtils.setDefaultCursor(SecondaryDataRemovalDialog.this);
                setVisible(false);
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
                UIUtils.setDefaultCursor(SecondaryDataRemovalDialog.this);
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
        final DefaultTableModel tableModel = new DefaultTableModel();
        Map<String,Set<String>> osToAA = new HashMap<>();
        int rowCount = 0;
        for (ObjectiveSample os: sample.getObjectiveSamples()) {
            Set<String> aaList = new TreeSet<>();
            osToAA.put(os.getObjective(), aaList);

            SamplePipelineRun run = os.getLatestSuccessfulRun();
            if (run!=null) {
                for (SampleProcessingResult spr: run.getSampleProcessingResults()) {
                    aaList.add(spr.getAnatomicalArea());
                    rowCount ++;
                }
            }
        }
        final String[][] rowData = new String[rowCount][COLUMN_HEADERS.length];
        int i = 0;
        for (String key: osToAA.keySet()) {
            for (String aa: osToAA.get(key)) {
                rowData[i][0] = aa;
                rowData[i][1] = key;
                rowData[i][2] = sample.getName();
                rowData[i][3] = sample.getId().toString();
                i++;
            }
        }
        tableModel.setDataVector(rowData, COLUMN_HEADERS);
        TableColumnModel tcModel = new DefaultTableColumnModel();        
        i = 0;
        for (String hdr: COLUMN_HEADERS) {
            TableColumn tcol = new TableColumn(i);
            tcol.setHeaderValue(hdr);
            tcModel.addColumn(tcol);
            i++;
        }
        sampleSubPartTable.setColumnModel(tcModel);
        sampleSubPartTable.setModel(tableModel);
        sampleSubPartTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                final int inx = e.getFirstIndex();
                Object value = sampleSubPartTable.getValueAt(inx, 0);
                log.info("Selection at {}: {}", inx, value);
            }
            
        });
    }

    private void launchDeletionTask() {
        Task task;
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            int[] selectedRows = sampleSubPartTable.getSelectedRows();
            StringBuilder subpartNames = new StringBuilder();

            int aaColnum = 0;
            int objectiveColnum = 0;
            for (int i = 0; i < sampleSubPartTable.getColumnCount(); i++) {
                if (PART_LABEL.equals(sampleSubPartTable.getModel().getColumnName(i))) {
                    aaColnum = i;
                }
                else if (OBJECTIVE_LABEL.equals(sampleSubPartTable.getModel().getColumnName(i))) {
                    objectiveColnum = i;
                }
            }
            StringBuilder bldr = new StringBuilder();
            for (int selectedRow: selectedRows) {
                if (subpartNames.length() > 0) {
                    subpartNames.append(",");
                }
                String anatomicalAreaName = (String)sampleSubPartTable.getModel().getValueAt(selectedRow, aaColnum);
                String objectiveName = (String)sampleSubPartTable.getModel().getValueAt(selectedRow, objectiveColnum);
                subpartNames.append(anatomicalAreaName);
                saveBasicAnatomicalAreasToMemento(anatomicalAreaName, objectiveName, sample.getId(), bldr);
            }

            // Give the user a last opt-out.
            final String message = getDeletionWarning(subpartNames);
            if (JOptionPane.showConfirmDialog(this, message, "Confirm Removal", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }

            String stringifiedAreas = bldr.toString();
            taskParameters.add(new TaskParameter(SAMPLE_ID_DISPLAYABLE_PARM, sample.getId().toString(), null));
            taskParameters.add(new TaskParameter(SAMPLE_AREAS_DISPLAYABLE_PARM, stringifiedAreas, null));
            taskParameters.add(new TaskParameter(TRIM_DEPTH_DISPLAYABLE_PARAM, trimDepth, null));
            task = StateMgr.getStateMgr().submitJob("ConsoleTrimSample", "Remove Partial Secondary Data", taskParameters);

            log.info("Submitting task {}, {} \n( {}\n{} ).",  task.getJobName(), task.getObjectId(), trimDepth, stringifiedAreas);
        } catch (Exception e) {
            FrameworkAccess.handleException(e);
            return;
        }

        TaskMonitoringWorker taskWorker = new TaskMonitoringWorker(task.getObjectId()) {

            @Override
            public String getName() {
                return "Trimming images from "+PART_LABEL+"(s) of the sample";
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
                        DomainExplorerTopComponent.getInstance().refresh(true, true, null);
                        return null;
                    }
                };
            }
        };

        taskWorker.executeWithEvents();
    }

    /**
     * We need just enough data to get name/objective/sample to the server.
     */
    public static void saveBasicAnatomicalAreasToMemento(String name, String objective, Long sampleId, StringBuilder bldr) {
        if (bldr.length() > 0) {
            bldr.append("\n");
        }
        bldr.append(name)
                .append(",")
                .append(objective)
                .append(",")
                .append(sampleId);
    }

    private String getDeletionWarning(StringBuilder subpartNames) {
        if (trimDepth.equals(TRIM_DEPTH_AREA_IMAGE_VALUE)) {
            return getStitchedFileDeletionWarning(subpartNames);
        }
        else if (trimDepth.equals(TRIM_DEPTH_WHOLE_AREA_VALUE)) {
            return getWholeAADeletionWarning(subpartNames);
        }
        else if (trimDepth.equals(TRIM_DEPTH_NEURON_SEPARATION_VALUE)) {
            return getNeuronSeparationDeletionWarning(subpartNames);
        }
        else {
            return null;
        }
    }

    private String getWholeAADeletionWarning(StringBuilder subpartNames) {
        String appendedTextFile = ConsoleProperties.getInstance().getProperty(WHOLE_AA_IMPLICATIONS_PROP);
        log.debug("Whole AA Appended text file name=" + appendedTextFile);
        String format = readFromResource(appendedTextFile);
        return String.format(format, subpartNames.toString(), sample.getName());
    }

    private String getStitchedFileDeletionWarning(StringBuilder subpartNames) {
        String appendedTextFile = ConsoleProperties.getInstance().getProperty(STITCHED_IMPLICATIONS_PROP);
        log.debug("StitchedImage Appended text file name=" + appendedTextFile);
        String format = readFromResource(appendedTextFile);
        return String.format(format, subpartNames.toString(), sample.getName());
    }

    private String getNeuronSeparationDeletionWarning(StringBuilder subpartNames) {
        String appendedTextFile = ConsoleProperties.getInstance().getProperty(NEURON_SEP_IMPLICATIONS_PROP);
        log.debug("NeuSep Appended text file name=" + appendedTextFile);
        String format = readFromResource(appendedTextFile);
        return String.format(format, subpartNames.toString(), sample.getName());
    }

    private String readFromResource(String resource) {
        StringBuilder builder = new StringBuilder();
        ClassLoader cl = SecondaryDataRemovalDialog.class.getClassLoader();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(cl.getResourceAsStream(resource)))) {
            String inputLine = null;
            while (null != (inputLine = br.readLine())) {
                builder.append(inputLine);
            }

        } catch (Exception ex) {
            builder.append("<span>Other downstream dependencies will be affected.</span>");
            ex.printStackTrace();
        }
        return builder.toString();
    }
}
