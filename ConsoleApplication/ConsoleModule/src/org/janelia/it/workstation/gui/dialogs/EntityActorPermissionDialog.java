package org.janelia.it.workstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;

import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.outline.EntityDetailsPanel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.workstation.gui.util.SubjectComboBoxRenderer;

/**
 * A dialog for viewing, editing, or adding an EntityActorPermission.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityActorPermissionDialog extends ModalDialog {

    private static final Font separatorFont = new Font("Sans Serif", Font.BOLD, 12);

    private final EntityDetailsPanel parent;
    private final JPanel attrPanel;
    private final JComboBox subjectCombobox;
    private final JCheckBox readCheckbox;
    private final JCheckBox writeCheckbox;
    private final JCheckBox recursiveCheckbox;

    private Entity entity;
    private EntityActorPermission eap;

    public EntityActorPermissionDialog(EntityDetailsPanel parent) {

        this.parent = parent;

        setTitle("Add permission");

        attrPanel = new JPanel(new MigLayout("wrap 2, ins 20"));
        add(attrPanel, BorderLayout.CENTER);

        addSeparator(attrPanel, "User");

        subjectCombobox = new JComboBox();
        subjectCombobox.setEditable(false);
        subjectCombobox.setToolTipText("Choose a user or group");

        SubjectComboBoxRenderer renderer = new SubjectComboBoxRenderer();
        subjectCombobox.setRenderer(renderer);
        subjectCombobox.setMaximumRowCount(20);

        attrPanel.add(subjectCombobox, "gap para, span 2");

        addSeparator(attrPanel, "Permissions");

        readCheckbox = new JCheckBox("Read");
        readCheckbox.setEnabled(false);
        attrPanel.add(readCheckbox, "gap para, span 2");

        writeCheckbox = new JCheckBox("Write");
        attrPanel.add(writeCheckbox, "gap para, span 2");

        recursiveCheckbox = new JCheckBox("Apply permission changes to all subfolders");
        recursiveCheckbox.setSelected(true);

        addSeparator(attrPanel, "Options");
        attrPanel.add(recursiveCheckbox, "gap para, span 2");

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

    private void addSeparator(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setFont(separatorFont);
        panel.add(label, "split 2, span, gaptop 10lp");
        panel.add(new JSeparator(SwingConstants.HORIZONTAL), "growx, wrap, gaptop 10lp");
    }

    public void showForNewPermission(Entity entity) {
        this.entity = entity;
        showForPermission(null);
    }

    public void showForPermission(final EntityActorPermission eap) {

        this.eap = eap;
        if (eap != null) {
            this.entity = eap.getEntity();
        }

        DefaultComboBoxModel model = (DefaultComboBoxModel) subjectCombobox.getModel();
        model.removeAllElements();

        String currSubjectKey = eap==null?null:eap.getSubjectKey();
        Subject currSubject = null;
        for (Subject subject : parent.getUnusedSubjects(currSubjectKey)) {
            if (entity != null && !entity.getOwnerKey().equals(subject.getKey())) {
                model.addElement(subject);
            }
            if (eap != null && eap.getSubjectKey().equals(subject.getKey())) {
                currSubject = subject;
            }
        }

        if (currSubject != null) {
            model.setSelectedItem(currSubject);
        }

        readCheckbox.setSelected(eap == null || eap.getPermissions().contains("r"));
        writeCheckbox.setSelected(eap != null && eap.getPermissions().contains("w"));

        if (entity.getEntityTypeName().equals(EntityConstants.TYPE_ANNOTATION)) {
            recursiveCheckbox.setEnabled(false);
        }
        
        packAndShow();
    }

    private void saveAndClose() {

        Utils.setWaitingCursor(parent);

        final Subject subject = (Subject) subjectCombobox.getSelectedItem();
        final boolean recursive = recursiveCheckbox.isSelected();

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                String permissions = (readCheckbox.isSelected() ? "r" : "") + "" + (writeCheckbox.isSelected() ? "w" : "");
                if (eap == null) {
                    eap = ModelMgr.getModelMgr().grantPermissions(entity.getId(), subject.getKey(), permissions, recursive);
                }
                else {
                    eap.setSubjectKey(subject.getKey());
                    eap.setPermissions(permissions);
                    ModelMgr.getModelMgr().saveOrUpdatePermission(eap);
                }
                ModelMgr.getModelMgr().invalidateCache(entity, recursive);
            }

            @Override
            protected void hadSuccess() {
                parent.refresh();
                Utils.setDefaultCursor(parent);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                Utils.setDefaultCursor(parent);
            }
        };
        worker.setProgressMonitor(new IndeterminateProgressMonitor(parent, "Granting permissions...", ""));
        worker.execute();

        setVisible(false);
    }
}
