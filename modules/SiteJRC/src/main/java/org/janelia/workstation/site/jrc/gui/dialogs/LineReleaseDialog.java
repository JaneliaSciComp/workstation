package org.janelia.workstation.site.jrc.gui.dialogs;

import net.miginfocom.swing.MigLayout;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.sample.DataSet;
import org.janelia.model.domain.sample.LineRelease;
import org.janelia.model.security.Subject;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.common.gui.support.ComboMembershipListPanel;
import org.janelia.workstation.common.gui.support.SubjectComboBoxRenderer;
import org.janelia.workstation.common.gui.util.UIUtils;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * A dialog for viewing the list of accessible fly line releases, editing them,
 * and adding new ones.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LineReleaseDialog extends ModalDialog {

    private static final Font separatorFont = new Font("Sans Serif", Font.BOLD, 12);

    private static final String DEFAULT_WEBSITE = "Split GAL4";
    private static final String[] WEBSITES = {DEFAULT_WEBSITE, "Gen1 MCFO"};

    private final LineReleaseListDialog parentDialog;

    private JPanel attrPanel;
    private JTextField nameInput = new JTextField(30);
    private final JComboBox<String> websiteComboBox;
    private JCheckBox sageSyncCheckbox;
    private ComboMembershipListPanel<Subject> annotatorsPanel;
    private JButton okButton;

    private LineRelease releaseEntity;

    public LineReleaseDialog(LineReleaseListDialog parentDialog) {

        super(parentDialog);
        this.parentDialog = parentDialog;

        setTitle("Fly Line Release Definition");

        attrPanel = new JPanel(new MigLayout("wrap 2, ins 20"));
        add(attrPanel, BorderLayout.CENTER);

        this.websiteComboBox = new JComboBox<>();
        websiteComboBox.setEditable(false);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(e -> setVisible(false));

        this.okButton = new JButton("Save");
        okButton.addActionListener(e -> saveSyncAndClose());
        okButton.setText("Save");
        okButton.setToolTipText("Close and save changes");

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

    public void showForRelease(final LineRelease release) {

        this.releaseEntity = release;

        boolean editable = release == null;
        String releaseOwnerKey = release == null ? AccessManager.getSubjectKey() : release.getOwnerKey();

        attrPanel.removeAll();

        addSeparator(attrPanel, "Release Attributes", true);

        final JLabel ownerLabel = new JLabel("Annotator: ");

        attrPanel.add(ownerLabel, "gap para");
        attrPanel.add(new JLabel(releaseOwnerKey));

        final JLabel nameLabel = new JLabel("Release Name: ");
        attrPanel.add(nameLabel, "gap para");

        if (editable) {
            nameLabel.setLabelFor(nameInput);
            attrPanel.add(nameInput);
        } 
        else {
            attrPanel.add(new JLabel(release.getName()));
        }

        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) websiteComboBox.getModel();
        for (String website : WEBSITES) {
            model.addElement(website);
        }
        model.setSelectedItem(DEFAULT_WEBSITE);

        final JLabel websiteLabel = new JLabel("Target Website: ");
        attrPanel.add(websiteLabel, "gap para");
        websiteLabel.setLabelFor(websiteComboBox);
        attrPanel.add(websiteComboBox);

        sageSyncCheckbox = new JCheckBox("Synchronize to SAGE");
        sageSyncCheckbox.setSelected(true); // default to true
        attrPanel.add(sageSyncCheckbox, "gap para, span 2");

        attrPanel.add(Box.createVerticalStrut(10), "span 2");

        annotatorsPanel = new ComboMembershipListPanel<>("Annotators", SubjectComboBoxRenderer.class);
        attrPanel.add(annotatorsPanel, "span 2");

        UIUtils.setWaitingCursor(LineReleaseDialog.this);

        SimpleWorker worker = new SimpleWorker() {

            private final List<DataSet> dataSets = new ArrayList<>();
            private final List<Subject> subjects = new ArrayList<>();
            private final Map<String, Subject> subjectMap = new HashMap<>();

            @Override
            protected void doStuff() throws Exception {

                for (Subject subject : DomainMgr.getDomainMgr().getSubjects()) {
                    subjects.add(subject);
                    subjectMap.put(subject.getKey(), subject);
                }

                dataSets.sort(Comparator.comparing(DataSet::getIdentifier));
                DomainUtils.sortSubjects(subjects);
            }

            @Override
            protected void hadSuccess() {
                annotatorsPanel.initItemsInCombo(subjects);

                if (release != null) {
                    nameInput.setText(release.getName());

                    sageSyncCheckbox.setSelected(release.isSageSync());

                    if (!StringUtils.isBlank(release.getTargetWebsite())) {
                        websiteComboBox.setSelectedItem(release.getTargetWebsite());
                    }
                    else {
                        websiteComboBox.setSelectedItem(DEFAULT_WEBSITE);
                    }

                    List<String> annotators = release.getAnnotators();
                    if (annotators!=null) {
                        for (String key : annotators) {
                            Subject subject = subjectMap.get(key);
                            if (subject!=null) {
                                annotatorsPanel.addItemToList(subject);
                            }
                        }
                    }
                } 
                else {
                    nameInput.setText("");
                }

                UIUtils.setDefaultCursor(LineReleaseDialog.this);
                pack();
            }

            @Override
            protected void hadError(Throwable error) {
                UIUtils.setDefaultCursor(LineReleaseDialog.this);
                FrameworkAccess.handleException(error);
            }
        };

        worker.execute();

        if (release ==null) {
            ActivityLogHelper.logUserAction("LineReleaseDialog.showForRelease");
        }
        else {
            ActivityLogHelper.logUserAction("LineReleaseDialog.showForRelease", release);
        }
        packAndShow();
    }

    public void addSeparator(JPanel panel, String text, boolean first) {
        JLabel label = new JLabel(text);
        label.setFont(separatorFont);
        panel.add(label, "split 2, span" + (first ? "" : ", gaptop 10lp"));
        panel.add(new JSeparator(SwingConstants.HORIZONTAL), "growx, wrap, gaptop 10lp");
    }
    
    private void saveSyncAndClose() {

        UIUtils.setWaitingCursor(LineReleaseDialog.this);

        if (StringUtils.isEmpty(nameInput.getText().trim())) {
            JOptionPane.showMessageDialog(LineReleaseDialog.this, "The release name cannot be blank", "Cannot save release", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (releaseEntity == null && parentDialog != null) {
            for (LineRelease release : parentDialog.getReleases()) {
                if (release.getName().equals(nameInput.getText())) {
                    JOptionPane.showMessageDialog(LineReleaseDialog.this, "A release with this name already exists", "Cannot save release", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }

        final List<String> annotators = new ArrayList<>();
        for (Subject subject : annotatorsPanel.getItemsInList()) {
            if (subject==null) continue;
            annotators.add(subject.getKey());
        }

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {

                DomainModel model = DomainMgr.getDomainMgr().getModel();

                if (releaseEntity == null) {
                    releaseEntity = model.createLineRelease(nameInput.getText(), null, null, Collections.emptyList());
                }

                releaseEntity.setTargetWebsite((String)websiteComboBox.getSelectedItem());
                releaseEntity.setAnnotators(annotators);
                releaseEntity.setSageSync(sageSyncCheckbox.isSelected());
                releaseEntity = model.update(releaseEntity);
            }

            @Override
            protected void hadSuccess() {
                if (parentDialog!=null) {
                    parentDialog.refresh();
                }
                UIUtils.setDefaultCursor(LineReleaseDialog.this);
                setVisible(false);
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
                UIUtils.setDefaultCursor(LineReleaseDialog.this);
                setVisible(false);
            }
        };

        worker.execute();
    }
}
