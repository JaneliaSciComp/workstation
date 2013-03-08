package org.janelia.it.FlyWorkstation.gui.dialogs;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.console.Perspective;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.FileTreeLoaderPipelineTask;
import org.janelia.it.jacs.model.user_data.Node;

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
 * User: kimmelr
 * Date: 5/23/12
 * Time: 2:05 PM
 */
public class RunImportDialog extends ModalDialog{

    private static final String TOP_LEVEL_FOLDER_NAME = "%USER%'s Imported Data";

    private static final String TOOLTIP_INPUT_DIR       = "Root directory of the tree that should be loaded into the database";
    private static final String TOOLTIP_TOP_LEVEL_FOLDER= "Name of the folder which should be loaded with the data";

    private static final int REFRESH_DELAY = 3000;

    private final JTextField topLevelFolderField;
    private JTextField pathTextField;
    private JFileChooser fileChooser;

    private long taskID;

    private Timer refreshTimer;

    public RunImportDialog() {
        setTitle("Import");

        JPanel attrPanel = new JPanel();
        attrPanel.setLayout(new GridBagLayout());
        attrPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10),
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Import Information")));

        JLabel topLevelFolderLabel = new JLabel("Top Level Folder Name");
        topLevelFolderLabel.setToolTipText(TOOLTIP_TOP_LEVEL_FOLDER);
        topLevelFolderField = new JTextField(40);
        topLevelFolderField.setText(filter(TOP_LEVEL_FOLDER_NAME));
        topLevelFolderField.setToolTipText(TOOLTIP_TOP_LEVEL_FOLDER);
        topLevelFolderLabel.setLabelFor(topLevelFolderField);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0;
        attrPanel.add(topLevelFolderLabel, c);
        c.gridx = 1;
        attrPanel.add(topLevelFolderField, 1);

        String pathText="";
        pathTextField = new JTextField(40);
        pathTextField.setText(pathText);
        setSize(400, 400);
        File pathTest = new File(ConsoleProperties.getString("remote.defaultLinuxPath"));
        c.gridx = 0; c.gridy = 1;
        JLabel pathLabel = new JLabel("Path:");
        pathLabel.setToolTipText(TOOLTIP_INPUT_DIR);
        attrPanel.add(new JLabel("Path:"), c);
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
                        int returnVal = fileChooser.showOpenDialog(RunImportDialog.this);
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            pathTextField.setText(fileChooser.getSelectedFile().getAbsolutePath());
                        }
                    }
                });
            }
            c.gridx = 2;
            attrPanel.add(_filePathButton, c);
        }

        add(attrPanel, BorderLayout.CENTER);
//        SpringUtilities.makeCompactGrid(attrPanel, attrPanel.getComponentCount()/3, 2, 6, 6, 6, 6);

        JButton okButton = new JButton("Import");
        okButton.setToolTipText("Import a directory or file");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String path = pathTextField.getText();
                if (null==path || "".equals(path) || !path.startsWith("/groups/")) {
                    JOptionPane.showMessageDialog(RunImportDialog.this,
                            "Please define a valid network path (/groups/...)", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                runImport();
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

    public void runImport() {

        Utils.setWaitingCursor(this);

        SimpleWorker executeWorker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                startImport();
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(RunImportDialog.this);
                Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
                browser.setPerspective(Perspective.ImageBrowser);
                setVisible(false);
            }

            @Override
            protected void hadError(Throwable error) {
                error.printStackTrace();
                Utils.setDefaultCursor(RunImportDialog.this);
                JOptionPane.showMessageDialog(RunImportDialog.this,
                        "Error submitting job: "+error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        executeWorker.execute();

    }

    public void showDialog() {
        packAndShow();
    }

    private void startImport() {
        try {
            String process;
            Task task;
            String owner = SessionMgr.getSubjectKey();
            process = "FileTreeLoader";

            String path = pathTextField.getText();
            String topLevelFolderName = topLevelFolderField.getText().trim();
            task = new FileTreeLoaderPipelineTask(new HashSet<Node>(),
                    owner, new ArrayList<Event>(), new HashSet<TaskParameter>(), path, topLevelFolderName);
            task.setJobName("Import Files Task");
            task = ModelMgr.getModelMgr().saveOrUpdateTask(task);
            taskID = task.getObjectId();

            ModelMgr.getModelMgr().submitJob(process, task.getObjectId());
            refreshTimer = new Timer(REFRESH_DELAY, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        Task importCompleteYet = ModelMgr.getModelMgr().getTaskById(taskID);
                        if (importCompleteYet.isDone()) {
                            refreshTimer.stop();
                            SessionMgr.getBrowser().getEntityOutline().refresh();
                        }
                    }
                    catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            });
            refreshTimer.setInitialDelay(0);
            refreshTimer.start();

        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    private String filter(String s) {
        return s.replaceAll("%USER%", SessionMgr.getUsername());
    }
}
