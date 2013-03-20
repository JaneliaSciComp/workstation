package org.janelia.it.FlyWorkstation.gui.dialogs;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.console.Perspective;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.cellCounting.CellCountingTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.jdesktop.swingx.VerticalLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 3/19/13
 * Time: 3:17 PM
 */
public class CellCounterDialog extends ModalDialog {
    public static final String PREF_CELL_COUNTING_SERVICE_INPUT_DIR     = "CellCountingService.InputDir";
    public static final String PREF_CELL_COUNTING_SERVICE_FOLDER_NAME   = "CellCountingService.TopLevelFolderName";
    public static final String PREF_CELL_COUNTING_SERVICE_PLAN_INFO     = "CellCountingService.PlanInfo";

    private static final String TOP_LEVEL_FOLDER_NAME = "'s Cell Counting Data";

    private static final String TOOLTIP_INPUT_FILE      = "File or directory of files to count against";
    private static final String TOOLTIP_TOP_LEVEL_FOLDER= "Name of the folder where the results should go";

    private final JTextField pathTextField;
    private final JTextField folderField;
    private final JTextArea  planTextArea;

    private JFileChooser fileChooser;

    public CellCounterDialog() {

        setTitle("Launch Cell Counter");

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new VerticalLayout(5));

        JPanel attrPanel = new JPanel();
        attrPanel.setLayout(new GridBagLayout());
        attrPanel.setBorder(BorderFactory.createTitledBorder("Basic Options"));

        JLabel outputFolder = new JLabel("Output Folder:");
        outputFolder.setToolTipText(TOOLTIP_TOP_LEVEL_FOLDER);
        folderField = new JTextField(40);
        // Use the previous destination; otherwise, suggest the default user location
        if (null!=SessionMgr.getSessionMgr().getModelProperty(PREF_CELL_COUNTING_SERVICE_FOLDER_NAME)&&
                !"".equals(SessionMgr.getSessionMgr().getModelProperty(PREF_CELL_COUNTING_SERVICE_FOLDER_NAME))) {
            folderField.setText((String)SessionMgr.getSessionMgr().getModelProperty(PREF_CELL_COUNTING_SERVICE_FOLDER_NAME));
        }
        else {
            folderField.setText(SessionMgr.getUsername()+TOP_LEVEL_FOLDER_NAME);
        }
        folderField.setToolTipText(TOOLTIP_TOP_LEVEL_FOLDER);
        outputFolder.setLabelFor(folderField);
        GridBagConstraints c = new GridBagConstraints();
        c.ipadx = 5;
        c.gridx = 0; c.gridy = 0;
        attrPanel.add(outputFolder, c);
        c.gridx = 1;
        attrPanel.add(folderField, 1);

        String pathText;
        // Figure out the user path preference
        if (null!=SessionMgr.getSessionMgr().getModelProperty(PREF_CELL_COUNTING_SERVICE_INPUT_DIR)&&
                !"".equals(SessionMgr.getSessionMgr().getModelProperty(PREF_CELL_COUNTING_SERVICE_INPUT_DIR))) {
            pathText=((String)SessionMgr.getSessionMgr().getModelProperty(PREF_CELL_COUNTING_SERVICE_INPUT_DIR));
        }
        else {
            pathText = ConsoleProperties.getString("remote.defaultLinuxPath");
        }
        pathTextField = new JTextField(40);
        pathTextField.setText(pathText);
        setSize(400, 400);
        File pathTest = new File(pathText);
        c.gridx = 0; c.gridy = 1;
        JLabel pathLabel = new JLabel("Input File Path:");
        pathLabel.setToolTipText(TOOLTIP_INPUT_FILE);
        attrPanel.add(pathLabel, c);
        c.gridx = 1;
        attrPanel.add(pathTextField, c);
        if (pathTest.exists() && pathTest.canRead()) {
            fileChooser  = new JFileChooser(pathTest);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

            JButton _filePathButton = null;
            try {
                _filePathButton = new JButton(Utils.getClasspathImage("magnifier.png"));
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if (_filePathButton != null) {
                _filePathButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int returnVal = fileChooser.showOpenDialog(CellCounterDialog.this);
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            pathTextField.setText(fileChooser.getSelectedFile().getAbsolutePath());
                        }
                    }
                });
            }
            c.gridx = 2;
            attrPanel.add(_filePathButton, c);
        }

        String plan;
        if (null!=SessionMgr.getSessionMgr().getModelProperty(PREF_CELL_COUNTING_SERVICE_PLAN_INFO)&&
                !"".equals(SessionMgr.getSessionMgr().getModelProperty(PREF_CELL_COUNTING_SERVICE_PLAN_INFO))) {
            plan = ((String)SessionMgr.getSessionMgr().getModelProperty(PREF_CELL_COUNTING_SERVICE_PLAN_INFO));
        }
        else {
            plan = CellCountingTask.DEFAULT_PLAN;
        }
        c.gridx = 0; c.gridy = 2;
        JLabel planLabel = new JLabel("Counting Plan:");
        planLabel.setToolTipText("Settings to help drive the Cell Counting");
        attrPanel.add(planLabel, c);
        c.gridx = 1;
        planTextArea = new JTextArea(plan);
        JScrollPane scrollPane = new JScrollPane(planTextArea);
        scrollPane.setPreferredSize(new Dimension(400,200));
        attrPanel.add(scrollPane,c);

        mainPanel.add(attrPanel);
        add(mainPanel, BorderLayout.CENTER);

        JButton okButton = new JButton("Run");
        okButton.setToolTipText("Run the cell counter");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runCellCountingService();
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Cancel and close this dialog");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);
        buttonPane.add(cancelButton);

        add(buttonPane, BorderLayout.SOUTH);
    }

    public void runCellCountingService() {

        Utils.setWaitingCursor(this);

        final String inputPath = pathTextField.getText().trim();
        final String topLevelFolderName = folderField.getText().trim();
        final String planInfo = planTextArea.getText();

        if (null==inputPath || "".equals(inputPath) || !(inputPath.startsWith("/groups/") || inputPath.startsWith("/archive/"))) {
            JOptionPane.showMessageDialog(CellCounterDialog.this,
                    "Please define a valid network path (/groups/...)", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Update user Preferences
        if (null!=topLevelFolderName) {
            SessionMgr.getSessionMgr().setModelProperty(PREF_CELL_COUNTING_SERVICE_FOLDER_NAME,folderField.getText());
        }
        SessionMgr.getSessionMgr().setModelProperty(PREF_CELL_COUNTING_SERVICE_INPUT_DIR, pathTextField.getText());
        SessionMgr.getSessionMgr().setModelProperty(PREF_CELL_COUNTING_SERVICE_PLAN_INFO, planTextArea.getText());
        // Prompt a save of the user settings because we can't trust the Mac exit yet
        SessionMgr.getSessionMgr().saveUserSettings();

        SimpleWorker executeWorker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                startCellCounting(inputPath, topLevelFolderName, planInfo);
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(CellCounterDialog.this);
                Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
                browser.setPerspective(Perspective.TaskMonitoring);
                setVisible(false);
            }

            @Override
            protected void hadError(Throwable error) {
                error.printStackTrace();
                Utils.setDefaultCursor(CellCounterDialog.this);
                JOptionPane.showMessageDialog(CellCounterDialog.this,
                        "Error submitting job: "+error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        executeWorker.execute();
    }

    public void showDialog() {
        packAndShow();
    }

    /**
     * Begin the separation task, wrapped in a continuous execution task.
     * @param path root directory to use for file discovery
     */
    private void startCellCounting(String path, String topLevelFolderName, String planInformation) {

        try {
            String owner = SessionMgr.getSubjectKey();
            String process = "CellCounting";
            Task task = new CellCountingTask(new HashSet<Node>(), owner, new ArrayList<org.janelia.it.jacs.model.tasks.Event>(),
                    new HashSet<TaskParameter>(), path, topLevelFolderName, planInformation);
            task.setJobName("Cell Counting Task");
            task = ModelMgr.getModelMgr().saveOrUpdateTask(task);

            ModelMgr.getModelMgr().submitJob(process, task.getObjectId());
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

}
