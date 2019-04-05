package org.janelia.it.workstation.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.it.workstation.browser.gui.support.SubjectComboBoxRenderer;
import org.janelia.it.workstation.browser.gui.util.UIUtils;
import org.janelia.it.workstation.browser.model.DomainObjectPermission;
import org.janelia.it.workstation.browser.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.workspace.Node;
import org.janelia.model.security.Subject;
import org.janelia.model.security.util.SubjectUtils;

import net.miginfocom.swing.MigLayout;

/**
 * A dialog for viewing and editing permissions for a single domain object.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectPermissionDialog extends ModalDialog {

    private static final Font separatorFont = new Font("Sans Serif", Font.BOLD, 12);

    private final DomainInspectorPanel parent;
    private final JPanel attrPanel;
    private final JComboBox<Subject> subjectCombobox;
    private final JCheckBox readCheckbox;
    private final JCheckBox writeCheckbox;

    private DomainObjectPermission dop;
    private DomainObject domainObject;

    public DomainObjectPermissionDialog(DomainInspectorPanel parent) {

        super(parent);
        this.parent = parent;

        setTitle("Add permission");

        attrPanel = new JPanel(new MigLayout("wrap 2, ins 20"));
        add(attrPanel, BorderLayout.CENTER);

        addSeparator(attrPanel, "User");

        subjectCombobox = new JComboBox<>();
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

        DefaultComboBoxModel<Subject> model = (DefaultComboBoxModel<Subject>) subjectCombobox.getModel();
        model.removeAllElements();

        String currSubjectKey = dop==null?null:dop.getSubjectKey();
        Subject currSubject = null;
        for (Subject subject : parent.getUnusedSubjects(currSubjectKey)) {
            if (domainObject != null && !domainObject.getOwnerKey().equals(subject.getKey())) {
                model.addElement(subject);
            }
            if (dop != null && dop.getSubjectKey().equals(subject.getKey())) {
                currSubject = subject;
            }
        }

        if (currSubject != null) {
            model.setSelectedItem(currSubject);
        }

        readCheckbox.setSelected(dop == null || dop.isRead());
        writeCheckbox.setSelected(dop != null && dop.isWrite());

        ActivityLogHelper.logUserAction("DomainObjectPermissionDialog.showDialog", domainObject);
        packAndShow();
    }

    private void saveAndClose() {

        UIUtils.setWaitingCursor(parent);

        final Subject subject = (Subject) subjectCombobox.getSelectedItem();
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
                    JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), 
                            "<html>Some of the items contained in this folder could not be shared because <br>"
                            + "you don't have write access to them. Please contact the item owners for write access, <br>"
                            + "or ask them to share the data on your behalf.</html>", unwriteable+" items could not be shared", JOptionPane.WARNING_MESSAGE); 
                }
                
                UIUtils.setDefaultCursor(parent);
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
                UIUtils.setDefaultCursor(parent);
            }
        };
        worker.setProgressMonitor(new IndeterminateProgressMonitor(parent, "Granting permissions...", ""));
        worker.execute();

        setVisible(false);
    }
}
