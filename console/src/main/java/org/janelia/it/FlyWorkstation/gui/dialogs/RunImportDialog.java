package org.janelia.it.FlyWorkstation.gui.dialogs;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.FileTreeLoaderPipelineTask;
import org.janelia.it.jacs.model.user_data.Node;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 * User: kimmelr
 * Date: 5/23/12
 * Time: 2:05 PM
 */
public class RunImportDialog extends ModalDialog {

    private static final String INPUT_DIR = "";
    private static final String TOP_LEVEL_FOLDER_NAME = "%USER%'s Imported Data";

    private static final String TOOLTIP_INPUT_DIR       = "Root directory of the tree that should be loaded into the database";
    private static final String TOOLTIP_TOP_LEVEL_FOLDER= "Name of the folder which should be loaded with the data";

    private static final int REFRESH_DELAY = 3000;

    private final JTextField inputDirectoryField;
    private final JTextField topLevelFolderField;
    private JComboBox referenceChannelComboBox;
    private JComboBox backgroundChannelComboBox;

    private long taskID;

    private Timer refreshTimer;

    public RunImportDialog() {
        final JPanel attrPanel;

        setTitle("Import");

        attrPanel = new JPanel();
        attrPanel.setLayout(new BoxLayout(attrPanel, BoxLayout.PAGE_AXIS));
        attrPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel locationPanel = new JPanel();
        locationPanel.setLayout(new GridLayout(4,2));
        locationPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10),
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Location Settings")));

        JLabel topLevelFolderLabel = new JLabel("Top Level Folder Name");
        topLevelFolderLabel.setToolTipText(TOOLTIP_TOP_LEVEL_FOLDER);
        topLevelFolderField = new JTextField(40);
        topLevelFolderField.setText(filter(TOP_LEVEL_FOLDER_NAME));
        topLevelFolderField.setToolTipText(TOOLTIP_TOP_LEVEL_FOLDER);
        topLevelFolderLabel.setLabelFor(topLevelFolderField);
        locationPanel.add(topLevelFolderLabel);
        locationPanel.add(topLevelFolderField);

        JLabel inputDirectoryLabel = new JLabel("Input Directory (Linux mounted)");
        inputDirectoryLabel.setToolTipText(TOOLTIP_INPUT_DIR);
        inputDirectoryField = new JTextField(40);
        inputDirectoryField.setText(filter(INPUT_DIR));
        inputDirectoryField.setToolTipText(TOOLTIP_INPUT_DIR);
        inputDirectoryLabel.setLabelFor(inputDirectoryField);
        locationPanel.add(inputDirectoryLabel);
        locationPanel.add(inputDirectoryField);
        locationPanel.setPreferredSize(new Dimension(500,150));

        JPanel imageAttPanel = new JPanel();
        imageAttPanel.setLayout(new GridLayout(2, 2));
        imageAttPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10),
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Optional Image Attributes")));
        String[] channelRange = new String[]{"Not Applicable", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
        JLabel refLabel = new JLabel("Reference Channel");
        JLabel backgroundLabel = new JLabel("Background Channel");
        referenceChannelComboBox = new JComboBox(channelRange);
        backgroundChannelComboBox= new JComboBox(channelRange);

        imageAttPanel.add(refLabel);
        imageAttPanel.add(referenceChannelComboBox);
        imageAttPanel.add(backgroundLabel);
        imageAttPanel.add(backgroundChannelComboBox);
        imageAttPanel.setPreferredSize(new Dimension(500,100));

        attrPanel.add(locationPanel);
        attrPanel.add(imageAttPanel);

        JButton okButton = new JButton("Import");
        okButton.setToolTipText("Import a directory or file");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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

        add(attrPanel, BorderLayout.CENTER);
        add(buttonPane, BorderLayout.SOUTH);

    }

    public void runImport() {

        Utils.setWaitingCursor(this);

        final String inputDirPath = inputDirectoryField.getText();
        final String topLevelFolderName = topLevelFolderField.getText();

        SimpleWorker executeWorker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                startImport(inputDirPath, topLevelFolderName);
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(RunImportDialog.this);
                Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
                browser.getTaskOutline().loadTasks();
                browser.getOutlookBar().setVisibleBarByName(Browser.BAR_DATA);
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

    private void startImport(String path, String topLevelFolderName) {

        try {
            String owner = SessionMgr.getSubjectKey();
            String process = "FileTreeLoader";
            Task task = new FileTreeLoaderPipelineTask(new HashSet<Node>(),
                    owner, new ArrayList<Event>(), new HashSet<TaskParameter>(), path, topLevelFolderName,
                    referenceChannelComboBox.getSelectedItem().toString(), backgroundChannelComboBox.getSelectedItem().toString());
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
