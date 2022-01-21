package org.janelia.workstation.browser.gui.dialogs;

import net.miginfocom.swing.MigLayout;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.security.Subject;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.common.gui.support.SubjectComboBox;
import org.janelia.workstation.common.gui.util.UIUtils;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.workers.IndeterminateProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * A dialog for adding or deleting permissions for a set of domain objects, or their annotations.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class BulkChangePermissionDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(BulkChangePermissionDialog.class);

    private static final String INFO_MESSAGE_ANNOTATIONS = "<html>"
            + "Will modify permissions for the selected user on all<br>"
            + "accessible annotations across all selected entities</html>";

    private static final String INFO_MESSAGE_OBJECTS = "<html>"
            + "Will modify permissions for the selected user on all<br>"
            + "selected entities</html>";

    private static final Font separatorFont = new Font("Sans Serif", Font.BOLD, 12);
    
    private final JPanel attrPanel;
    private final JLabel informationalMessage;
    private final SubjectComboBox subjectCombobox;
    private final JCheckBox readCheckbox;
    private final JCheckBox writeCheckbox;

    private boolean annotations;

    public BulkChangePermissionDialog() {

        setTitle("Add or remove permissions for annotations");

        attrPanel = new JPanel(new MigLayout("wrap 2, ins 20"));
        add(attrPanel, BorderLayout.CENTER);


        informationalMessage = new JLabel();
        attrPanel.add(informationalMessage, "gap para, span 2");

        addSeparator(attrPanel, "User");

        subjectCombobox = new SubjectComboBox();
        attrPanel.add(subjectCombobox, "gap para, span 2");

        addSeparator(attrPanel, "Permissions");

        readCheckbox = new JCheckBox("Read");
        readCheckbox.setSelected(true);
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

    private List<Reference> selected;

    private void addSeparator(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setFont(separatorFont);
        panel.add(label, "split 2, span, gaptop 10lp");
        panel.add(new JSeparator(SwingConstants.HORIZONTAL), "growx, wrap, gaptop 10lp");
    }

    public void showForDomainObjects(List<Reference> selected, boolean annotations) {

        this.annotations = annotations;
        this.selected = selected;

        informationalMessage.setText(annotations ? INFO_MESSAGE_ANNOTATIONS : INFO_MESSAGE_OBJECTS);

        try {
            DomainMgr mgr = DomainMgr.getDomainMgr();
            List<Subject> subjects = mgr.getSubjects();
            subjectCombobox.setItems(subjects);

            ActivityLogHelper.logUserAction("BulkChangePermissionDialog.showForSelectedDomainObjects");
            packAndShow();
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

    private void saveAndClose() {
        
        UIUtils.setWaitingCursor(FrameworkAccess.getMainFrame());

        final Subject subject = (Subject) subjectCombobox.getSelectedItem();
        boolean read = readCheckbox.isSelected();
        boolean write = writeCheckbox.isSelected();
        final String rights = (read?"r":"") + (write?"w":"");  
                
        SimpleWorker worker = new SimpleWorker() {

            private int numObjectsModified;
            
            @Override
            protected void doStuff() throws Exception {
                
                DomainModel model = DomainMgr.getDomainMgr().getModel();

                if (annotations) {
                    log.info("Modifying permissions for annotations {} items", selected.size());
                    for (Annotation annotation : model.getAnnotations(selected)) {
                        // Must be group to grant access
                        if (!ClientDomainUtils.hasAdminAccess(annotation)) continue;
                        model.changePermissions(annotation, subject.getKey(), rights);
                        numObjectsModified++;
                    }
                }
                else {
                    log.info("Modifying permissions for {} items", selected);
                    for (DomainObject domainObject : model.getDomainObjects(selected)) {
                        // Must be group admin to grant access
                        if (!ClientDomainUtils.hasAdminAccess(domainObject)) continue;
                        model.changePermissions(domainObject, subject.getKey(), rights);
                        numObjectsModified++;
                    }
                }
            }

            @Override
            protected void hadSuccess() {
                String message = annotations ?
                        "Modified permissions for "+ numObjectsModified +" annotations on "+selected.size()+" items" :
                        "Modified permissions for "+ numObjectsModified +" items";
                UIUtils.setDefaultCursor(FrameworkAccess.getMainFrame());
                    JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                        message, "Shared", JOptionPane.INFORMATION_MESSAGE);
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
                UIUtils.setDefaultCursor(FrameworkAccess.getMainFrame());
            }
        };
        worker.setProgressMonitor(new IndeterminateProgressMonitor(FrameworkAccess.getMainFrame(), "Changing permissions...", ""));
        worker.execute();

        setVisible(false);
    }
}
