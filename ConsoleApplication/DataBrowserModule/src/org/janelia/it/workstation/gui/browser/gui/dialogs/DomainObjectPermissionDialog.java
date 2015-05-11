package org.janelia.it.workstation.gui.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.workstation.gui.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.it.workstation.gui.browser.model.DomainObjectPermission;
import org.janelia.it.workstation.gui.dialogs.ModalDialog;
import org.janelia.it.workstation.gui.util.SubjectComboBoxRenderer;

/**
 * A dialog for viewing, editing, or adding an EntityActorPermission.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectPermissionDialog extends ModalDialog {

    private static final Font separatorFont = new Font("Sans Serif", Font.BOLD, 12);

    private final DomainInspectorPanel parent;
    private final JPanel attrPanel;
    private final JComboBox subjectCombobox;
    private final JCheckBox readCheckbox;
    private final JCheckBox writeCheckbox;
    private final JCheckBox recursiveCheckbox;

    private DomainObjectPermission dop;

    public DomainObjectPermissionDialog(DomainInspectorPanel parent) {

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

    public void showForNewPermission(final DomainObject domainObject) {
        showForPermission(null);
    }
    
    public void showForPermission(final DomainObjectPermission dop) {

        this.dop = dop;

        DefaultComboBoxModel model = (DefaultComboBoxModel) subjectCombobox.getModel();
        model.removeAllElements();

        // TODO: logic
//        String currSubjectKey = eap==null?null:eap.getSubjectKey();
//        Subject currSubject = null;
//        for (Subject subject : parent.getUnusedSubjects(currSubjectKey)) {
//            if (domainObject != null && !domainObject.getOwnerKey().equals(subject.getKey())) {
//                model.addElement(subject);
//            }
//            if (eap != null && eap.getSubjectKey().equals(subject.getKey())) {
//                currSubject = subject;
//            }
//        }
//
//        if (currSubject != null) {
//            model.setSelectedItem(currSubject);
//        }
//
        readCheckbox.setSelected(dop == null || dop.isRead());
        writeCheckbox.setSelected(dop != null && dop.isWrite());
        if (dop!=null) {
            recursiveCheckbox.setEnabled((dop.getDomainObject() instanceof TreeNode));
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
                dop.setRead(readCheckbox.isSelected());
                dop.setWrite(writeCheckbox.isSelected());
//                String permissions = (readCheckbox.isSelected() ? "r" : "") + "" + (writeCheckbox.isSelected() ? "w" : "");
                // TODO: save to mongo
//                if (eap == null) {
//                    eap = ModelMgr.getModelMgr().grantPermissions(domainObject.getId(), subject.getKey(), permissions, recursive);
//                }
//                else {
//                    eap.setSubjectKey(subject.getKey());
//                    eap.setPermissions(permissions);
//                    ModelMgr.getModelMgr().saveOrUpdatePermission(eap);
//                }
//                ModelMgr.getModelMgr().invalidateCache(domainObject, recursive);
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
