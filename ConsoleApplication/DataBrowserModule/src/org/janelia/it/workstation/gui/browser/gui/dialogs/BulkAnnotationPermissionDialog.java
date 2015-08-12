package org.janelia.it.workstation.gui.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.support.MongoUtils;

import org.janelia.it.workstation.gui.browser.api.DomainDAO;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.model.DomainObjectId;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.support.SubjectComboBoxRenderer;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.workstation.gui.dialogs.ModalDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog for adding or deleting EntityActorPermissions for all accessible 
 * annotations on a set of entities. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class BulkAnnotationPermissionDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(BulkAnnotationPermissionDialog.class);
    
    private static final String INFO_MESSAGE = "<html>"
            + "Will modify permissions for the selected user on all<br>"
            + "accessible annotations across all selected entities</html>";
    
    private static final Font separatorFont = new Font("Sans Serif", Font.BOLD, 12);
    
    private final JPanel attrPanel;
    private final JComboBox subjectCombobox;
    private final JCheckBox readCheckbox;
    private final JCheckBox writeCheckbox;

    private final DomainObjectSelectionModel selectionModel;
    private final List<DomainObjectId> selected = new ArrayList<>();

    public BulkAnnotationPermissionDialog() {

        // TODO: get the correct selection model
        this.selectionModel = null;
            
        setTitle("Add or remove permissions for annotations");

        attrPanel = new JPanel(new MigLayout("wrap 2, ins 20"));
        add(attrPanel, BorderLayout.CENTER);

        attrPanel.add(new JLabel(INFO_MESSAGE), "gap para, span 2");
        
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
        readCheckbox.setSelected(true);
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

    public void showForSelectedDomainObjects() {

        selected.clear();
        selected.addAll(selectionModel.getSelectedIds());
                
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(SessionMgr.getMainFrame(),
                    "Select some items to bulk-edit permissions", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } 
        
        try {
            DomainMgr mgr = DomainMgr.getDomainMgr();
            List<Subject> subjects = mgr.getSubjects();
            DefaultComboBoxModel model = (DefaultComboBoxModel) subjectCombobox.getModel();
            model.removeAllElements();
            for (Subject subject : subjects) {
                model.addElement(subject);
            }

            packAndShow();
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    private void saveAndClose() {

        final String annotationType = MongoUtils.getCollectionName(Annotation.class);
        
        Utils.setWaitingCursor(SessionMgr.getMainFrame());

        final Subject subject = (Subject) subjectCombobox.getSelectedItem();
        final boolean read = readCheckbox.isSelected();
        final boolean write = writeCheckbox.isSelected();
        
        SimpleWorker worker = new SimpleWorker() {

            private int numAnnotationsModified;
            
            @Override
            protected void doStuff() throws Exception {
                
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                Collection<Long> selectedIds = new ArrayList<>();
                for(DomainObjectId id : selected) {
                    selectedIds.add(id.getId());
                }
                
                for(Annotation annotation : model.getAnnotations(selectedIds)) {
                    
                    // Must be owner to grant access
                    if (!DomainUtils.isOwner(annotation)) continue;

                    model.changePermissions(annotationType, selectedIds, subject.getKey(), "r", read);
                    model.changePermissions(annotationType, selectedIds, subject.getKey(), "w", write);
                    
                    numAnnotationsModified++;
                }
                
                // TODO: invalidate or force refresh somehow
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(SessionMgr.getMainFrame());
                    JOptionPane.showMessageDialog(SessionMgr.getMainFrame(),
                        "Modified permissions for "+numAnnotationsModified+" annotations on "+selected.size()+" items", "Shared", JOptionPane.INFORMATION_MESSAGE);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                Utils.setDefaultCursor(SessionMgr.getMainFrame());
            }
        };
        worker.setProgressMonitor(new IndeterminateProgressMonitor(SessionMgr.getMainFrame(), "Changing permissions...", ""));
        worker.execute();

        setVisible(false);
    }
}
