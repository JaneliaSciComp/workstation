package org.janelia.it.workstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.DataSetComboBoxRenderer;
import org.janelia.it.workstation.gui.util.MembershipListPanel;
import org.janelia.it.workstation.gui.util.SubjectComboBoxRenderer;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.util.ISO8601Utils;

import de.javasoft.swing.DateComboBox;

/**
 * A dialog for viewing the list of accessible fly line releases, editing them, and adding new ones.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FlyLineReleaseDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(FlyLineReleaseDialog.class);
    private static final Font separatorFont = new Font("Sans Serif", Font.BOLD, 12);

    private final FlyLineReleaseListDialog parentDialog;

    private JPanel attrPanel;
    private JTextField nameInput;
    private DateComboBox dateInput;
    private MembershipListPanel<Entity> dataSetPanel;
    private MembershipListPanel<Subject> annotatorsPanel;
    private MembershipListPanel<Subject> subscribersPanel;

    private Entity releaseEntity;

    public FlyLineReleaseDialog(FlyLineReleaseListDialog parentDialog) {

        this.parentDialog = parentDialog;

        setTitle("Fly Line Release Definition");

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

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
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
        String releaseOwnerKey = releaseEntity == null ? SessionMgr.getSubjectKey() : releaseEntity.getOwnerKey();

        attrPanel.removeAll();

        addSeparator(attrPanel, "Release Attributes", true);

        final JLabel ownerLabel = new JLabel("Release Owner: ");

        final JLabel ownerValue = new JLabel(releaseOwnerKey);
        attrPanel.add(ownerLabel, "gap para");
        attrPanel.add(ownerValue);

        final JLabel nameLabel = new JLabel("Release Name: ");
        nameInput = new JTextField(30);
        nameLabel.setLabelFor(nameInput);
        attrPanel.add(nameLabel, "gap para");
        attrPanel.add(nameInput);

        final JLabel dateLabel = new JLabel("Release Date: ");
        dateInput = new DateComboBox();
        dateLabel.setLabelFor(dateInput);
        attrPanel.add(dateLabel, "gap para");
        attrPanel.add(dateInput);

        JPanel bottomPanel = new JPanel(new MigLayout("wrap 3"));
        
        dataSetPanel = new MembershipListPanel<>("Data Sets", DataSetComboBoxRenderer.class);
        bottomPanel.add(dataSetPanel, "growx");

        annotatorsPanel = new MembershipListPanel<>("Additional Annotators", SubjectComboBoxRenderer.class);
        bottomPanel.add(annotatorsPanel, "growx");

        subscribersPanel = new MembershipListPanel<>("Subscribers", SubjectComboBoxRenderer.class);
        bottomPanel.add(subscribersPanel, "growx");

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

                for (Subject subject : ModelMgr.getModelMgr().getSubjects()) {
                    if (SessionMgr.getSubjectKey().equals(subject.getKey())) {
                        continue;
                    }
                    subjects.add(subject);
                    subjectMap.put(subject.getKey(), subject);
                }

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

                    String releaseDateStr = releaseEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_RELEASE_DATE);

                    if (releaseDateStr != null) {
                        dateInput.setDate(ISO8601Utils.parse(releaseDateStr));
                    }

                    String dataSetsStr = releaseEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SETS);
                    if (dataSetsStr != null) {
                        for (String identifier : dataSetsStr.split(",")) {
                            Entity dataSet = dataSetMap.get(identifier);
                            dataSetPanel.addItemToList(dataSet);
                        }
                    }

                    String annotatorsStr = releaseEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATORS);
                    if (annotatorsStr != null) {
                        for (String key : annotatorsStr.split(",")) {
                            Subject subject = subjectMap.get(key);
                            annotatorsPanel.addItemToList(subject);
                        }
                    }

                    String subscribersStr = releaseEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SUBSCRIBERS);
                    if (subscribersStr != null) {
                        for (String key : subscribersStr.split(",")) {
                            Subject subject = subjectMap.get(key);
                            subscribersPanel.addItemToList(subject);
                        }
                    }
                }
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();

        Component mainFrame = SessionMgr.getMainFrame();
        setPreferredSize(new Dimension((int) (mainFrame.getWidth() * 0.5), (int) (mainFrame.getHeight() * 0.4)));

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

        final String releaseDateStr = ISO8601Utils.format(dateInput.getDate());

        final StringBuilder dataSetsSb = new StringBuilder();
        for (Entity dataSet : dataSetPanel.getItemsInList()) {
            if (dataSetsSb.length() > 0) {
                dataSetsSb.append(",");
            }
            String identifier = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
            dataSetsSb.append(identifier);
        }

        final StringBuilder annotatorsSb = new StringBuilder();
        for (Subject subject : annotatorsPanel.getItemsInList()) {
            if (annotatorsSb.length() > 0) {
                annotatorsSb.append(",");
            }
            annotatorsSb.append(subject.getKey());
        }

        final StringBuilder subscribersSb = new StringBuilder();
        for (Subject subject : subscribersPanel.getItemsInList()) {
            if (subscribersSb.length() > 0) {
                subscribersSb.append(",");
            }
            subscribersSb.append(subject.getKey());
        }

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {

                if (releaseEntity == null) {
                    releaseEntity = ModelMgr.getModelMgr().createFlyLineRelease(nameInput.getText());
                }
                else {
                    releaseEntity.setName(nameInput.getText());
                }

                ModelMgr.getModelMgr().setOrUpdateValue(releaseEntity, EntityConstants.ATTRIBUTE_RELEASE_DATE, releaseDateStr);
                ModelMgr.getModelMgr().setOrUpdateValue(releaseEntity, EntityConstants.ATTRIBUTE_DATA_SETS, dataSetsSb.toString());
                ModelMgr.getModelMgr().setOrUpdateValue(releaseEntity, EntityConstants.ATTRIBUTE_ANNOTATORS, annotatorsSb.toString());
                ModelMgr.getModelMgr().setOrUpdateValue(releaseEntity, EntityConstants.ATTRIBUTE_SUBSCRIBERS, subscribersSb.toString());
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
        };

        worker.execute();
    }
}
