package org.janelia.workstation.browser.gui.dialogs;

import com.google.common.collect.Lists;
import net.miginfocom.swing.MigLayout;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.workspace.Node;
import org.janelia.model.security.Subject;
import org.janelia.model.security.util.SubjectUtils;
import org.janelia.workstation.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.common.gui.support.SubjectComboBox;
import org.janelia.workstation.common.gui.util.UIUtils;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.model.DomainObjectPermission;
import org.janelia.workstation.core.workers.IndeterminateProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;

/**
 * A dialog for viewing and editing permissions for a single domain object.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectPermissionDialog extends ModalDialog {

    private static final Font separatorFont = new Font("Sans Serif", Font.BOLD, 12);

    private final DomainInspectorPanel parent;
    private final JPanel attrPanel;
    private final SubjectComboBox subjectCombobox;
    private final JCheckBox readCheckbox;
    private final JCheckBox writeCheckbox;

    private DomainObjectPermission dop;
    private DomainObject domainObject;

    public DomainObjectPermissionDialog(ModalDialog parentDialog, DomainInspectorPanel parentPanel) {

        super(parentDialog);
        this.parent = parentPanel;

        setTitle("Add permission");

        attrPanel = new JPanel(new MigLayout("wrap 2, ins 20"));
        add(attrPanel, BorderLayout.CENTER);

        addSeparator(attrPanel, "User");

        subjectCombobox = new SubjectComboBox();
        attrPanel.add(subjectCombobox, "gap para, span 2");

        addSeparator(attrPanel, "Permissions");

        readCheckbox = new JCheckBox("Read");
        readCheckbox.setEnabled(false);
        attrPanel.add(readCheckbox, "gap para, span 2");

        writeCheckbox = new JCheckBox("Write");
        attrPanel.add(writeCheckbox, "gap para, span 2");

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(e -> setVisible(false));

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(e -> saveAndClose());

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

    public void showForPermission(final DomainObjectPermission dop) {
        this.dop = dop;
        this.domainObject = dop.getDomainObject();
        showDialog();
    }
    
    public void showForNewPermission(final DomainObject domainObject) {
        this.dop = null;
        this.domainObject = domainObject;
        showDialog();
    }
    
    private void showDialog() {

        List<Subject> subjects = Lists.newArrayList();
        String currSubjectKey = dop==null?null:dop.getSubjectKey();
        Subject currSubject = null;
        for (Subject subject : parent.getUnusedSubjects(currSubjectKey)) {
            if (domainObject != null && !domainObject.getOwnerKey().equals(subject.getKey())) {
                subjects.add(subject);
            }
            if (dop != null && dop.getSubjectKey().equals(subject.getKey())) {
                currSubject = subject;
            }
        }

        subjectCombobox.setItems(subjects, currSubject);

        readCheckbox.setSelected(dop == null || dop.isRead());
        writeCheckbox.setSelected(dop != null && dop.isWrite());

        ActivityLogHelper.logUserAction("DomainObjectPermissionDialog.showDialog", domainObject);
        packAndShow();
    }

    private void saveAndClose() {

        UIUtils.setWaitingCursor(parent);

        final Subject subject = subjectCombobox.getSelectedItem();
        final Set<String> granteeSet = SubjectUtils.getReaderSet(subject);
        final DomainModel model = DomainMgr.getDomainMgr().getModel();
        
        SimpleWorker worker = new SimpleWorker() {

            int unwriteable = 0;
            
            @Override
            protected void doStuff() throws Exception {
                if (dop == null) {
                    dop = new DomainObjectPermission(domainObject, subject.getKey());    
                }
                dop.setRead(readCheckbox.isSelected());
                dop.setWrite(writeCheckbox.isSelected());
                model.changePermissions(domainObject, dop.getSubjectKey(), dop.getPermissions());
                
                // TODO: parallelize this with the changePermission call
                if (domainObject instanceof Node) {
                    Node treeNode = (Node)domainObject;
                    unwriteable += getNumUnwriteable(treeNode);
                }
            }

            private int getNumUnwriteable(Node treeNode) throws Exception {
                
                int unwriteable = 0;

                for(DomainObject child : model.getDomainObjects(treeNode.getChildren())) {
                    
                    boolean satisfyRead = !dop.isRead() || DomainUtils.hasReadAccess(child, granteeSet);
                    boolean satisfyWrite = !dop.isWrite() || DomainUtils.hasWriteAccess(child, granteeSet);
                    
                    if (!ClientDomainUtils.hasWriteAccess(child) && (!satisfyRead || !satisfyWrite)) {
                        // If we don't have write access to the child, and the grantee's access isn't satisfied, then we have a problem.
                        unwriteable++;
                    }
                    else {
                        // In theory we can grant access to this child
                        if (child instanceof Node) {
                            unwriteable += getNumUnwriteable((Node)child);
                        }
                    }
                }
                
                return unwriteable;
            }

            @Override
            protected void hadSuccess() {
                parent.refresh();
                
                if (unwriteable>0) {
                    JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                            "<html>Some of the items contained in this folder could not be shared because <br>"
                            + "you don't have write access to them. Please contact the item owners for write access, <br>"
                            + "or ask them to share the data on your behalf.</html>", unwriteable+" items could not be shared", JOptionPane.WARNING_MESSAGE); 
                }
                
                UIUtils.setDefaultCursor(parent);
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
                UIUtils.setDefaultCursor(parent);
            }
        };
        worker.setProgressMonitor(new IndeterminateProgressMonitor(parent, "Granting permissions...", ""));
        worker.execute();

        setVisible(false);
    }
}
