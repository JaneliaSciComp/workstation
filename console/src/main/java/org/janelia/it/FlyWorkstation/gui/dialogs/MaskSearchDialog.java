package org.janelia.it.FlyWorkstation.gui.dialogs;

import org.janelia.it.FlyWorkstation.api.entity_model.access.TaskRequestStatusObserverAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.TaskRequest;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.TaskRequestState;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.TaskRequestStatus;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.TaskThreadBase;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.maskSearch.MaskSearchTask;
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
 * Time: 3:13 PM
 */
public class MaskSearchDialog extends ModalDialog {

    public static final String PREF_MASK_SEARCH_SERVICE_INPUT_DIR    = "MaskSearchService.InputDir";
    public static final String PREF_MASK_SEARCH_SERVICE_FOLDER_NAME  = "MaskSearchService.TopLevelFolderName";
    public static final String PREF_MASK_SEARCH_SERVICE_MATRIX       = "MaskSearchService.Matrix";
    public static final String PREF_MASK_SEARCH_QUERY_CHANNEL        = "MaskSearchService.QueryChannel";
    public static final String PREF_MASK_SEARCH_MAX_HITS             = "MaskSearchService.MaxHits";
    public static final String PREF_MASK_SEARCH_SKIP_ZEROES          = "MaskSearchService.SkipZeroes";

    private static final String TOP_LEVEL_FOLDER_NAME = "'s Mask Search Data";

    private static final String TOOLTIP_INPUT_FILE      = "File to be used as the query";
    private static final String TOOLTIP_TOP_LEVEL_FOLDER= "Name of the folder where the results should go";
    private static final String TOOLTIP_MATRIX = "Parameters that set the search qualities";
    private static final String TOOLTIP_QUERY = "Channel with the signal to search against";

    private final JTextField pathTextField;
    private final JTextField folderField;
    private final JTextField matrixTextField;
    private final JTextField queryChannelTextField;
    private final JTextField maxHitsTextField;
    private final JCheckBox  skipZeroesCheckBox;
    private JFileChooser fileChooser;
    private Long currentTaskId;
    private TaskRequest searchRequest;

    public MaskSearchDialog() {

        setTitle("Launch Pattern Mask Search");

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new VerticalLayout(5));

        JPanel attrPanel = new JPanel();
        attrPanel.setLayout(new GridBagLayout());
        attrPanel.setBorder(BorderFactory.createTitledBorder("Basic Options"));

        JLabel topLevelFolderLabel = new JLabel("Output Folder:");
        topLevelFolderLabel.setToolTipText(TOOLTIP_TOP_LEVEL_FOLDER);
        folderField = new JTextField(40);
        // Use the previous destination; otherwise, suggest the default user location
        if (null!=SessionMgr.getSessionMgr().getModelProperty(PREF_MASK_SEARCH_SERVICE_FOLDER_NAME)&&
                !"".equals(SessionMgr.getSessionMgr().getModelProperty(PREF_MASK_SEARCH_SERVICE_FOLDER_NAME))) {
            folderField.setText((String)SessionMgr.getSessionMgr().getModelProperty(PREF_MASK_SEARCH_SERVICE_FOLDER_NAME));
        }
        else {
            folderField.setText(SessionMgr.getUsername()+TOP_LEVEL_FOLDER_NAME);
        }
        folderField.setToolTipText(TOOLTIP_TOP_LEVEL_FOLDER);

        topLevelFolderLabel.setLabelFor(folderField);
        GridBagConstraints c = new GridBagConstraints();
        c.ipadx = 5;
        c.gridx = 0; c.gridy = 0;
        attrPanel.add(topLevelFolderLabel, c);
        c.gridx = 1;
        attrPanel.add(folderField, 1);

        String pathText;
        // Figure out the user path preference
        if (null!=SessionMgr.getSessionMgr().getModelProperty(PREF_MASK_SEARCH_SERVICE_INPUT_DIR)&&
                !"".equals(SessionMgr.getSessionMgr().getModelProperty(PREF_MASK_SEARCH_SERVICE_INPUT_DIR))) {
            pathText=((String)SessionMgr.getSessionMgr().getModelProperty(PREF_MASK_SEARCH_SERVICE_INPUT_DIR));
        }
        else {
            pathText = ConsoleProperties.getString("remote.defaultLinuxPath");
        }
        pathTextField = new JTextField(40);
        pathTextField.setText(pathText);
        setSize(400, 400);
        File pathTest = new File(pathText);
        c.gridx = 0; c.gridy = 1;
        JLabel pathLabel = new JLabel("Mask File Path:");
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
                        int returnVal = fileChooser.showOpenDialog(MaskSearchDialog.this);
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            pathTextField.setText(fileChooser.getSelectedFile().getAbsolutePath());
                        }
                    }
                });
            }
            c.gridx = 2;
            attrPanel.add(_filePathButton, c);
        }

        JLabel matrixFieldLabel  = new JLabel("Matrix:");
        matrixFieldLabel.setToolTipText(TOOLTIP_MATRIX);
        matrixTextField = new JTextField(40);
        // Use the previous setting; otherwise, suggest the default matrix setting
        if (null!=SessionMgr.getSessionMgr().getModelProperty(PREF_MASK_SEARCH_SERVICE_MATRIX)&&
                !"".equals(SessionMgr.getSessionMgr().getModelProperty(PREF_MASK_SEARCH_SERVICE_MATRIX))) {
            matrixTextField.setText((String) SessionMgr.getSessionMgr().getModelProperty(PREF_MASK_SEARCH_SERVICE_MATRIX));
        }
        else {
            matrixTextField.setText(MaskSearchTask.DEFAULT_MATRIX);
        }
        matrixTextField.setToolTipText(TOOLTIP_MATRIX);
        c.gridx=0;c.gridy=2;
        attrPanel.add(matrixFieldLabel,c);
        c.gridx=1;
        attrPanel.add(matrixTextField,c);

        JLabel queryChannelFieldLabel  = new JLabel("Query Channel:");
        queryChannelFieldLabel.setToolTipText(TOOLTIP_QUERY);
        queryChannelTextField = new JTextField(40);
        // Use the previous setting; otherwise, suggest the default channel setting
        if (null!=SessionMgr.getSessionMgr().getModelProperty(PREF_MASK_SEARCH_QUERY_CHANNEL)&&
                !"".equals(SessionMgr.getSessionMgr().getModelProperty(PREF_MASK_SEARCH_QUERY_CHANNEL))) {
            queryChannelTextField.setText((String) SessionMgr.getSessionMgr().getModelProperty(PREF_MASK_SEARCH_QUERY_CHANNEL));
        }
        else {
            queryChannelTextField.setText(MaskSearchTask.DEFAULT_QUERY_CHANNEL);
        }
        queryChannelTextField.setToolTipText(TOOLTIP_QUERY);
        c.gridx=0;c.gridy=3;
        attrPanel.add(queryChannelFieldLabel,c);
        c.gridx=1;
        attrPanel.add(queryChannelTextField,c);

        JLabel maxHitsFieldLabel  = new JLabel("Max Hits:");
        maxHitsTextField = new JTextField(40);
        // Use the previous setting; otherwise, suggest the default max hits setting
        if (null!=SessionMgr.getSessionMgr().getModelProperty(PREF_MASK_SEARCH_MAX_HITS)&&
                !"".equals(SessionMgr.getSessionMgr().getModelProperty(PREF_MASK_SEARCH_MAX_HITS))) {
            maxHitsTextField.setText((String) SessionMgr.getSessionMgr().getModelProperty(PREF_MASK_SEARCH_MAX_HITS));
        }
        else {
            maxHitsTextField.setText(MaskSearchTask.DEFAULT_MAX_HITS);
        }
        c.gridx=0;c.gridy=4;
        attrPanel.add(maxHitsFieldLabel,c);
        c.gridx=1;
        attrPanel.add(maxHitsTextField,c);

        skipZeroesCheckBox = new JCheckBox("Search Non-Zero Mask Only");
        // Use the previous setting; otherwise, suggest the default max hits setting
        if (null!=SessionMgr.getSessionMgr().getModelProperty(PREF_MASK_SEARCH_SKIP_ZEROES)&&
                !"".equals(SessionMgr.getSessionMgr().getModelProperty(PREF_MASK_SEARCH_SKIP_ZEROES))) {
            skipZeroesCheckBox.setSelected(Boolean.valueOf((String) SessionMgr.getSessionMgr().getModelProperty(PREF_MASK_SEARCH_SKIP_ZEROES)));
        }
        else {
            skipZeroesCheckBox.setSelected(Boolean.valueOf(MaskSearchTask.DEFAULT_SKIP_ZEROES));
        }
        skipZeroesCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        c.gridx=1;c.gridy=5;
        attrPanel.add(skipZeroesCheckBox,c);

        mainPanel.add(attrPanel);
        add(mainPanel, BorderLayout.CENTER);

        JButton okButton = new JButton("Run");
        okButton.setToolTipText("Run the mask search");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runMaskSearchService();
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

    public void runMaskSearchService() {

        Utils.setWaitingCursor(this);

        final String inputDirPath = pathTextField.getText().trim();
        final String topLevelFolderName = folderField.getText().trim();
        final String matrixValue = matrixTextField.getText().trim();
        final String queryChannel = queryChannelTextField.getText().trim();
        final String maxHits = maxHitsTextField.getText().trim();
        final String skipZeroes = Boolean.toString(skipZeroesCheckBox.isSelected());

        // Update user Preferences
        if (null!=topLevelFolderName) {
            SessionMgr.getSessionMgr().setModelProperty(PREF_MASK_SEARCH_SERVICE_FOLDER_NAME,topLevelFolderName);
        }
        if (null!=inputDirPath) {
            SessionMgr.getSessionMgr().setModelProperty(PREF_MASK_SEARCH_SERVICE_INPUT_DIR,inputDirPath);
        }
        if (null!=matrixValue) {
            SessionMgr.getSessionMgr().setModelProperty(PREF_MASK_SEARCH_SERVICE_MATRIX,matrixValue);
        }
        if (null!=queryChannel) {
            SessionMgr.getSessionMgr().setModelProperty(PREF_MASK_SEARCH_QUERY_CHANNEL,queryChannel);
        }
        if (null!=maxHits) {
            SessionMgr.getSessionMgr().setModelProperty(PREF_MASK_SEARCH_MAX_HITS,maxHits);
        }
        if (null!=skipZeroes) {
            SessionMgr.getSessionMgr().setModelProperty(PREF_MASK_SEARCH_SKIP_ZEROES,skipZeroes);
        }
        // Prompt a save of the user settings because we can't trust the Mac exit yet
        SessionMgr.getSessionMgr().saveUserSettings();

        SimpleWorker executeWorker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                startMaskSearch(inputDirPath, topLevelFolderName, matrixValue, queryChannel, maxHits, skipZeroes);
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(MaskSearchDialog.this);
//                Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
//                browser.setPerspective(Perspective.TaskMonitoring);
                setVisible(false);
            }

            @Override
            protected void hadError(Throwable error) {
                error.printStackTrace();
                Utils.setDefaultCursor(MaskSearchDialog.this);
                JOptionPane.showMessageDialog(MaskSearchDialog.this,
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
     */
    private void startMaskSearch(String path, String topLevelFolderName, String matrixValue, String queryChannel,
                                 String maxHits, String skipZeroes) {
        try {
            String owner = SessionMgr.getSubjectKey();
            String process = "MaskSearch";
            Task task = new MaskSearchTask(new HashSet<Node>(), owner, new ArrayList<org.janelia.it.jacs.model.tasks.Event>(),
                    new HashSet<TaskParameter>(), path, topLevelFolderName, matrixValue, queryChannel, maxHits, skipZeroes);
            task.setJobName("Mask Search Task");
            task = ModelMgr.getModelMgr().saveOrUpdateTask(task);
            currentTaskId = task.getObjectId();
            searchRequest = ModelMgr.getModelMgr().submitJob(process, task);
            final TaskRequestStatus taskStatus = searchRequest.getTaskRequestStatus();
            if (searchRequest.getTaskFilter().getTaskFilterStatus().isCompleted()) {
                taskStatus.setTaskRequestState(TaskRequestStatus.COMPLETE);
                return;
            }
            taskStatus.setPendingTaskRequestAndStateToWaiting(searchRequest);
            final SearchWatcher searchWatcher = new SearchWatcher(searchRequest);
            ModelMgr.getModelMgr().getLoaderThreadQueue().addQueue(searchWatcher);
            waitForLoading(taskStatus);
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    /**
     * Wait for the loadRequestStatus to finish before continuing
     */
    protected void waitForLoading(final TaskRequestStatus ls) {
        if (ls.getTaskRequestState() == TaskRequestStatus.RUNNING || ls.getTaskRequestState() == TaskRequestStatus.WAITING) {
            ls.addTaskRequestStatusObserver(new TaskObserver(Thread.currentThread()), false);
            synchronized (this) {
                try {
                    wait(10000); //max of 10000 to avoid deadlock
                }
                catch (Exception ex) {
                } //expect inturrupted exception, do nothing
            }
        }
    }

    final class SearchWatcher extends TaskThreadBase {

        boolean monitorState = true;
        public SearchWatcher(final TaskRequest request) {
            super(request);
        }

        public SearchWatcher(final TaskRequest request, final boolean monitorState) {
            super(request);
            this.monitorState = monitorState;
        }

        public void run() {
            if (monitorState) {
                taskRequestStatus.setTaskRequestState(TaskRequestStatus.RUNNING);
            }
            try {
                Task task = ModelMgr.getModelMgr().getTaskById(this.taskRequest.getTaskFilter().getTaskId());
                if (monitorState) {
                    if (task.isDone()) {
                        taskRequestStatus.setTaskRequestState(TaskRequestStatus.COMPLETE);
                    }
                }

            }
            catch (Exception ex) {
                SessionMgr.getSessionMgr().handleException(ex);
            }
        }
    }

    static final class TaskObserver extends TaskRequestStatusObserverAdapter {
        final Thread waitingThread;

        public TaskObserver(final Thread waitingThread) {
            this.waitingThread = waitingThread;
        }

        public void stateChanged(final TaskRequestStatus loadRequestStatus, final TaskRequestState newState) {
            if (loadRequestStatus.getTaskRequestState() == TaskRequestStatus.COMPLETE) {
                loadRequestStatus.removeTaskRequestStatusObserver(this);
                waitingThread.interrupt();
            }
        }
    }


}
