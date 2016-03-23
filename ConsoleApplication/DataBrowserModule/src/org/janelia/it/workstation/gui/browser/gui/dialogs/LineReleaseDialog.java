package org.janelia.it.workstation.gui.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LineRelease;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.gui.support.DataSetComboBoxRenderer;
import org.janelia.it.workstation.gui.browser.gui.support.SubjectComboBoxRenderer;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.MembershipListPanel;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.workstation.shared.workers.TaskMonitoringWorker;

import de.javasoft.swing.DateComboBox;
import net.miginfocom.swing.MigLayout;

/**
 * A dialog for viewing the list of accessible fly line releases, editing them,
 * and adding new ones.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LineReleaseDialog extends ModalDialog {

    private static final Font separatorFont = new Font("Sans Serif", Font.BOLD, 12);

    private final LineReleaseListDialog parentDialog;

    private JPanel attrPanel;
    private JTextField nameInput = new JTextField(30);
    private DateComboBox dateInput = new DateComboBox();
    private JTextField lagTimeInput = new JTextField(10);
    private JCheckBox sageSyncCheckbox;
    private MembershipListPanel<DataSet> dataSetPanel;
    private MembershipListPanel<Subject> annotatorsPanel;
    private MembershipListPanel<Subject> subscribersPanel;
    private JButton okButton;

    private LineRelease releaseEntity;

    public LineReleaseDialog(LineReleaseListDialog parentDialog) {

        super(parentDialog);
        this.parentDialog = parentDialog;

        setTitle("Fly Line Release Definition");

        lagTimeInput.setToolTipText("Number of months between release date and the completion date of any samples included in the release");

        attrPanel = new JPanel(new MigLayout("wrap 2, ins 20"));

        add(attrPanel, BorderLayout.CENTER);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        this.okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAndClose();
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);
    }

    public void showForNewRelease() {
        showForRelease(null);
    }

    public void showForRelease(final LineRelease releaseEntity) {

        this.releaseEntity = releaseEntity;

        if (releaseEntity == null) {
            okButton.setText("Create Folder Hierarchy");
            okButton.setToolTipText("Create the release and corresponding folder hierarchy of all the lines due to be released");
        } else {
            okButton.setText("OK");
            okButton.setToolTipText("Close and save changes");
        }

        boolean editable = releaseEntity == null;
        String releaseOwnerKey = releaseEntity == null ? AccessManager.getSubjectKey() : releaseEntity.getOwnerKey();

        attrPanel.removeAll();

        addSeparator(attrPanel, "Release Attributes", true);

        final JLabel ownerLabel = new JLabel("Annotator: ");

        attrPanel.add(ownerLabel, "gap para");
        attrPanel.add(new JLabel(releaseOwnerKey));

        final JLabel nameLabel = new JLabel("Release Name: ");
        attrPanel.add(nameLabel, "gap para");

        nameInput.setEnabled(editable);

        if (editable) {
            nameLabel.setLabelFor(nameInput);
            attrPanel.add(nameInput);
        } else if (releaseEntity != null) {
            attrPanel.add(new JLabel(releaseEntity.getName()));
        }

        final JLabel dateLabel = new JLabel("Target Release Date: ");
        dateLabel.setLabelFor(dateInput);
        attrPanel.add(dateLabel, "gap para");
        attrPanel.add(dateInput);
        dateInput.setEnabled(editable);

        final JLabel lagTimeLabel = new JLabel("Lag Time Months (Optional): ");
        lagTimeLabel.setLabelFor(lagTimeInput);
        attrPanel.add(lagTimeLabel, "gap para");
        attrPanel.add(lagTimeInput);
        lagTimeInput.setEnabled(editable);

        sageSyncCheckbox = new JCheckBox("Synchronize to SAGE");
        attrPanel.add(sageSyncCheckbox, "gap para, span 2");

        attrPanel.add(Box.createVerticalStrut(10), "span 2");

        JPanel bottomPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 1;
        dataSetPanel = new MembershipListPanel<>("Data Sets", DataSetComboBoxRenderer.class);
        dataSetPanel.setEditable(editable);
        bottomPanel.add(dataSetPanel, c);

        c.gridx = 1;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 1;
        annotatorsPanel = new MembershipListPanel<>("Additional Annotators", SubjectComboBoxRenderer.class);
        bottomPanel.add(annotatorsPanel, c);

        c.gridx = 2;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 1;
        subscribersPanel = new MembershipListPanel<>("Subscribers", SubjectComboBoxRenderer.class);
        bottomPanel.add(subscribersPanel, c);

        attrPanel.add(bottomPanel, "span 2");

        Utils.setWaitingCursor(LineReleaseDialog.this);

        SimpleWorker worker = new SimpleWorker() {

            private final List<DataSet> dataSets = new ArrayList<>();
            private final List<Subject> subjects = new ArrayList<>();
            private final Map<String, DataSet> dataSetMap = new HashMap<>();
            private final Map<String, Subject> subjectMap = new HashMap<>();

            @Override
            protected void doStuff() throws Exception {

                for (DataSet dataSet : DomainMgr.getDomainMgr().getModel().getDataSets()) {
                    if (!ClientDomainUtils.isOwner(dataSet)) continue;
                    dataSets.add(dataSet);
                    String identifier = dataSet.getIdentifier();
                    dataSetMap.put(identifier, dataSet);
                }

                for (Subject subject : DomainMgr.getDomainMgr().getSubjects()) {
                    if (SessionMgr.getSubjectKey().equals(subject.getKey())) {
                        continue;
                    }
                    
                    subjects.add(subject);
                    subjectMap.put(subject.getKey(), subject);
                }

                Collections.sort(dataSets, new Comparator<DataSet>() {
                    @Override
                    public int compare(DataSet o1, DataSet o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });

                DomainUtils.sortSubjects(subjects);
            }

            @Override
            protected void hadSuccess() {
                dataSetPanel.init(dataSets);
                annotatorsPanel.init(subjects);
                subscribersPanel.init(subjects);

                if (releaseEntity != null) {
                    nameInput.setText(releaseEntity.getName());

                    Integer lagTimeMonths = releaseEntity.getLagTimeMonths();
                    if (lagTimeMonths != null) {
                        lagTimeInput.setText(lagTimeMonths.toString());
                    }

                    Date releaseDate = releaseEntity.getReleaseDate();

                    if (releaseDate != null) {
                        dateInput.setDate(releaseDate);
                    }

                    sageSyncCheckbox.setSelected(releaseEntity.isSageSync());

                    List<String> dataSets = releaseEntity.getDataSets();
                    if (dataSets!=null) {
                        for (String identifier : dataSets) {
                            DataSet dataSet = dataSetMap.get(identifier);
                            if (dataSet!=null) {
                                dataSetPanel.addItemToList(dataSet);
                            }
                        }
                    }

                    List<String> annotators = releaseEntity.getAnnotators();
                    if (annotators!=null) {
                        for (String key : annotators) {
                            Subject subject = subjectMap.get(key);
                            if (subject!=null) {
                                annotatorsPanel.addItemToList(subject);
                            }
                        }
                    }

                    List<String> subscribers = releaseEntity.getSubscribers();
                    if (subscribers!=null) {
                        for (String key : subscribers) {
                            Subject subject = subjectMap.get(key);
                            if (subject!=null) {
                                subscribersPanel.addItemToList(subject);
                            }
                        }
                    }
                } 
                else {
                    nameInput.setText("");
                    dateInput.setDate(new Date());
                    lagTimeInput.setText("");
                }

                pack();
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();

        packAndShow();
    }

    public void addSeparator(JPanel panel, String text, boolean first) {
        JLabel label = new JLabel(text);
        label.setFont(separatorFont);
        panel.add(label, "split 2, span" + (first ? "" : ", gaptop 10lp"));
        panel.add(new JSeparator(SwingConstants.HORIZONTAL), "growx, wrap, gaptop 10lp");
    }

    private void saveAndClose() {

        Utils.setWaitingCursor(LineReleaseDialog.this);

        if (StringUtils.isEmpty(nameInput.getText().trim())) {
            JOptionPane.showMessageDialog(LineReleaseDialog.this, "The release name cannot be blank", "Cannot save release", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer lagTime = null;
        try {
            String lagTimeStr = lagTimeInput.getText().trim();
            if (!StringUtils.isEmpty(lagTimeStr)) {
                lagTime = Integer.parseInt(lagTimeStr);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(LineReleaseDialog.this, "Lag time must be a number", "Cannot save release", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (releaseEntity==null) {
            for (LineRelease release : parentDialog.getReleases()) {
                if (release.getName().equals(nameInput.getText())) {
                    JOptionPane.showMessageDialog(LineReleaseDialog.this, "A release with this name already exists", "Cannot save release", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }
        
        final List<String> dataSets = new ArrayList<>();
        for (DataSet dataSet : dataSetPanel.getItemsInList()) {
            if (dataSet==null) continue;
            dataSets.add(dataSet.getIdentifier());
        }

        if (dataSets.isEmpty()) {
            JOptionPane.showMessageDialog(LineReleaseDialog.this, "A release must include at least one data set", "Cannot save release", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final List<String> annotators = new ArrayList<>();
        for (Subject subject : annotatorsPanel.getItemsInList()) {
            if (subject==null) continue;
            annotators.add(subject.getKey());
        }

        final List<String> subscribers = new ArrayList<>();
        for (Subject subject : subscribersPanel.getItemsInList()) {
            if (subject==null) continue;
            subscribers.add(subject.getKey());
        }

        final Integer lagTimeFinal = lagTime;
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {

                DomainModel model = DomainMgr.getDomainMgr().getModel();
                
                boolean syncFolders = false;
                if (releaseEntity == null) {
                    releaseEntity = model.createLineRelease(nameInput.getText(), dateInput.getDate(), lagTimeFinal, dataSets);
                    syncFolders = true;
                }

                releaseEntity.setAnnotators(annotators);
                releaseEntity.setSubscribers(subscribers);
                releaseEntity.setSageSync(sageSyncCheckbox.isSelected());
                releaseEntity = model.update(releaseEntity);
                
                if (syncFolders) {
                    launchSyncTask();
                }
            }

            @Override
            protected void hadSuccess() {
                parentDialog.refresh();
                Utils.setDefaultCursor(LineReleaseDialog.this);
                setVisible(false);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                Utils.setDefaultCursor(LineReleaseDialog.this);
                setVisible(false);
            }
        };

        worker.execute();
    }

    private void launchSyncTask() {

        Task task;
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("release entity id", releaseEntity.getId().toString(), null));
            task = ModelMgr.getModelMgr().submitJob("ConsoleSyncReleaseFolders", "Sync Release Folders", taskParameters);
        } catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
            return;
        }

        TaskMonitoringWorker taskWorker = new TaskMonitoringWorker(task.getObjectId()) {

            @Override
            public String getName() {
                return "Creating fly line folder structures";
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
