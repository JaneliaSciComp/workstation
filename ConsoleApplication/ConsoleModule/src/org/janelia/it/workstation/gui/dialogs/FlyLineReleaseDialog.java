package org.janelia.it.workstation.gui.dialogs;

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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.ISO8601Utils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.DataSetComboBoxRenderer;
import org.janelia.it.workstation.gui.util.MembershipListPanel;
import org.janelia.it.workstation.gui.util.SubjectComboBoxRenderer;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.workstation.shared.workers.TaskMonitoringWorker;

import de.javasoft.swing.DateComboBox;
import javax.swing.JCheckBox;

/**
 * A dialog for viewing the list of accessible fly line releases, editing them,
 * and adding new ones.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FlyLineReleaseDialog extends ModalDialog {

    private static final Font separatorFont = new Font("Sans Serif", Font.BOLD, 12);

    private final FlyLineReleaseListDialog parentDialog;

    private JPanel attrPanel;
    private JTextField nameInput = new JTextField(30);
    private DateComboBox dateInput = new DateComboBox();
    private JTextField lagTimeInput = new JTextField(10);
    private JCheckBox sageSyncCheckbox;
    private MembershipListPanel<Entity> dataSetPanel;
    private MembershipListPanel<Subject> annotatorsPanel;
    private MembershipListPanel<Subject> subscribersPanel;
    private JButton okButton;

    private Entity releaseEntity;

    public FlyLineReleaseDialog(FlyLineReleaseListDialog parentDialog) {

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

    public void showForNewDataSet() {
        showForRelease(null);
    }

    public void showForRelease(final Entity releaseEntity) {

        this.releaseEntity = releaseEntity;

        if (releaseEntity == null) {
            okButton.setText("Create Folder Hierarchy");
            okButton.setToolTipText("Create the release and corresponding folder hierarchy of all the lines due to be released");
        } else {
            okButton.setText("OK");
            okButton.setToolTipText("Close and save changes");
        }

        boolean editable = releaseEntity == null;
        String releaseOwnerKey = releaseEntity.getOwnerKey();

        attrPanel.removeAll();

        addSeparator(attrPanel, "Release Attributes", true);

        final JLabel ownerLabel = new JLabel("Annotator: ");

        final JLabel ownerValue = new JLabel(releaseOwnerKey);
        attrPanel.add(ownerLabel, "gap para");
        attrPanel.add(ownerValue);

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

        Utils.setWaitingCursor(FlyLineReleaseDialog.this);

        SimpleWorker worker = new SimpleWorker() {

            private final List<Entity> dataSets = new ArrayList<>();
            private final List<Subject> subjects = new ArrayList<>();
            private final Map<String, Entity> dataSetMap = new HashMap<>();
            private final Map<String, Subject> subjectMap = new HashMap<>();

            @Override
            protected void doStuff() throws Exception {

                for (Entity dataSet : ModelMgr.getModelMgr().getDataSets()) {
                    dataSets.add(dataSet);
                    String identifier = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
                    dataSetMap.put(identifier, dataSet);
                }

                /*for (Subject subject : ModelMgr.getModelMgr().getSubjects()) {
                    if (SessionMgr.getSubjectKey().equals(subject.getKey())) {
                        continue;
                    }
                    
                    subjects.add(subject);
                    subjectMap.put(subject.getKey(), subject);
                }*/

                Collections.sort(dataSets, new Comparator<Entity>() {
                    @Override
                    public int compare(Entity o1, Entity o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });

                EntityUtils.sortSubjects(subjects);
            }

            @Override
            protected void hadSuccess() {
                dataSetPanel.init(dataSets);
                annotatorsPanel.init(subjects);
                subscribersPanel.init(subjects);

                if (releaseEntity != null) {
                    nameInput.setText(releaseEntity.getName());

                    String lagTimeMonths = releaseEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_LAG_TIME_MONTHS);
                    if (lagTimeMonths != null) {
                        lagTimeInput.setText(lagTimeMonths);
                    }

                    String releaseDateStr = releaseEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_RELEASE_DATE);

                    if (releaseDateStr != null) {
                        dateInput.setDate(ISO8601Utils.parse(releaseDateStr));
                    }

                    if (releaseEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SAGE_SYNC) != null) {
                        sageSyncCheckbox.setSelected(true);
                    }

                    String dataSetsStr = releaseEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SETS);
                    if (!StringUtils.isEmpty(dataSetsStr)) {
                        for (String identifier : dataSetsStr.split(",")) {
                            Entity dataSet = dataSetMap.get(identifier);
                            if (dataSet!=null) {
                                dataSetPanel.addItemToList(dataSet);
                            }
                        }
                    }

                    String annotatorsStr = releaseEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATORS);
                    if (!StringUtils.isEmpty(annotatorsStr)) {
                        for (String key : annotatorsStr.split(",")) {
                            Subject subject = subjectMap.get(key);
                            if (subject!=null) {
                                annotatorsPanel.addItemToList(subject);
                            }
                        }
                    }

                    String subscribersStr = releaseEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SUBSCRIBERS);
                    if (!StringUtils.isEmpty(subscribersStr)) {
                        for (String key : subscribersStr.split(",")) {
                            Subject subject = subjectMap.get(key);
                            if (subject!=null) {
                                subscribersPanel.addItemToList(subject);
                            }
                        }
                    }
                } else {
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

        Utils.setWaitingCursor(FlyLineReleaseDialog.this);

        if (StringUtils.isEmpty(nameInput.getText().trim())) {
            JOptionPane.showMessageDialog(FlyLineReleaseDialog.this, "The release name cannot be blank", "Cannot save release", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer lagTime = null;
        try {
            String lagTimeStr = lagTimeInput.getText().trim();
            if (!StringUtils.isEmpty(lagTimeStr)) {
                lagTime = Integer.parseInt(lagTimeStr);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(FlyLineReleaseDialog.this, "Lag time must be a number", "Cannot save release", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (releaseEntity==null) {
            for (Entity release : parentDialog.getReleases()) {
                if (release.getName().equals(nameInput.getText())) {
                    JOptionPane.showMessageDialog(FlyLineReleaseDialog.this, "A release with this name already exists", "Cannot save release", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }
        
        final List<String> dataSets = new ArrayList<>();
        for (Entity dataSet : dataSetPanel.getItemsInList()) {
            if (dataSet==null) continue;
            String identifier = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
            dataSets.add(identifier);
        }

        if (dataSets.isEmpty()) {
            JOptionPane.showMessageDialog(FlyLineReleaseDialog.this, "A release must include at least one data set", "Cannot save release", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final StringBuilder annotatorsSb = new StringBuilder();
        for (Subject subject : annotatorsPanel.getItemsInList()) {
            if (subject==null) continue;
            if (annotatorsSb.length() > 0) {
                annotatorsSb.append(",");
            }
            annotatorsSb.append(subject.getKey());
        }

        final StringBuilder subscribersSb = new StringBuilder();
        for (Subject subject : subscribersPanel.getItemsInList()) {
            if (subject==null) continue;
            if (subscribersSb.length() > 0) {
                subscribersSb.append(",");
            }
            subscribersSb.append(subject.getKey());
        }

        final Integer lagTimeFinal = lagTime;
        SimpleWorker worker = new SimpleWorker() {

            private final ModelMgr modelMgr = ModelMgr.getModelMgr();

            @Override
            protected void doStuff() throws Exception {

                boolean syncFolders = false;
                if (releaseEntity == null) {
                    releaseEntity = modelMgr.createFlyLineRelease(nameInput.getText(), dateInput.getDate(), lagTimeFinal, dataSets);
                    syncFolders = true;
                }

                EntityData ed = modelMgr.setOrUpdateValue(releaseEntity, EntityConstants.ATTRIBUTE_ANNOTATORS, annotatorsSb.toString());
                releaseEntity = modelMgr.getEntityById(releaseEntity.getId());
                
                ed = modelMgr.setOrUpdateValue(releaseEntity, EntityConstants.ATTRIBUTE_SUBSCRIBERS, subscribersSb.toString());
                releaseEntity = modelMgr.getEntityById(releaseEntity.getId());

                if (sageSyncCheckbox.isSelected()) {
                    modelMgr.setAttributeAsTag(releaseEntity, EntityConstants.ATTRIBUTE_SAGE_SYNC);
                } else {
                    removeDataSetAttribute(EntityConstants.ATTRIBUTE_SAGE_SYNC);
                }
                releaseEntity = modelMgr.getEntityById(releaseEntity.getId());

                if (syncFolders) {
                    launchSyncTask();
                }
            }

            @Override
            protected void hadSuccess() {
                parentDialog.refresh();
                Utils.setDefaultCursor(FlyLineReleaseDialog.this);
                setVisible(false);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                Utils.setDefaultCursor(FlyLineReleaseDialog.this);
                setVisible(false);
            }

            private void removeDataSetAttribute(String attributeType) throws Exception {
                final EntityData typeEd = releaseEntity.getEntityDataByAttributeName(attributeType);
                if (typeEd != null) {
                    releaseEntity.getEntityData().remove(typeEd);
                    modelMgr.removeEntityData(typeEd);
                }
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
