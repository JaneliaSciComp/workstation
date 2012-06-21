package org.janelia.it.FlyWorkstation.gui.dialogs;

import loci.plugins.config.SpringUtilities;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.FileDiscoveryTask;
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
public class RunImportDialog extends ModalDialog{

    private static final String INPUT_DIR = "";
    private static final String TOP_LEVEL_FOLDER_NAME = "%USER%'s Imported Data";

    private static final String TOOLTIP_INPUT_DIR       = "Root directory of the tree that should be loaded into the database";
    private static final String TOOLTIP_TOP_LEVEL_FOLDER= "Name of the folder which should be loaded with the data";


    private final JTextField inputDirectoryField;
    private final JTextField topLevelFolderField;

    public RunImportDialog() {
        final JPanel attrPanel;

        setTitle("Import");

        attrPanel = new JPanel(new SpringLayout());
        attrPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10),
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Import Information")));

        JLabel topLevelFolderLabel = new JLabel("Top Level Folder Name");
        topLevelFolderLabel.setToolTipText(TOOLTIP_TOP_LEVEL_FOLDER);
        topLevelFolderField = new JTextField(40);
        topLevelFolderField.setText(filter(TOP_LEVEL_FOLDER_NAME));
        topLevelFolderField.setToolTipText(TOOLTIP_TOP_LEVEL_FOLDER);
        topLevelFolderLabel.setLabelFor(topLevelFolderField);
        attrPanel.add(topLevelFolderLabel);
        attrPanel.add(topLevelFolderField);

        JLabel inputDirectoryLabel = new JLabel("Input Directory (Linux mounted)");
        inputDirectoryLabel.setToolTipText(TOOLTIP_INPUT_DIR);
        inputDirectoryField = new JTextField(40);
        inputDirectoryField.setText(filter(INPUT_DIR));
        inputDirectoryField.setToolTipText(TOOLTIP_INPUT_DIR);
        inputDirectoryLabel.setLabelFor(inputDirectoryField);
        attrPanel.add(inputDirectoryLabel);
        attrPanel.add(inputDirectoryField);

        add(attrPanel, BorderLayout.CENTER);
        SpringUtilities.makeCompactGrid(attrPanel, attrPanel.getComponentCount()/2, 2, 6, 6, 6, 6);

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
            String process;
            Task task;
            String owner = SessionMgr.getUsername();

            process = "Import";
            task = new FileDiscoveryTask(new HashSet<Node>(),
                    owner, new ArrayList<Event>(), new HashSet<TaskParameter>(), path, topLevelFolderName, false);
            task.setJobName("Import Task");
            task = ModelMgr.getModelMgr().saveOrUpdateTask(task);

                ModelMgr.getModelMgr().submitJob(process, task.getObjectId());
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    private String filter(String s) {
        return s.replaceAll("%USER%", SessionMgr.getUsername());
    }
}
