package org.janelia.it.workstation.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

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
import org.janelia.it.workstation.browser.components.DomainListViewManager;
import org.janelia.it.workstation.browser.components.DomainListViewTopComponent;
import org.janelia.it.workstation.browser.events.selection.ChildSelectionModel;
import org.janelia.it.workstation.browser.gui.support.SubjectComboBoxRenderer;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.security.Subject;

import net.miginfocom.swing.MigLayout;

/**
 * A dialog for adding or deleting EntityActorPermissions for all accessible 
 * annotations on a set of entities. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class BulkAnnotationPermissionDialog extends ModalDialog {

    private static final String INFO_MESSAGE = "<html>"
            + "Will modify permissions for the selected user on all<br>"
            + "accessible annotations across all selected entities</html>";
    
    private static final Font separatorFont = new Font("Sans Serif", Font.BOLD, 12);
    
    private final JPanel attrPanel;
    private final JComboBox<Subject> subjectCombobox;
    private final JCheckBox readCheckbox;
    private final JCheckBox writeCheckbox;

    private final List<Reference> selected = new ArrayList<>();

    public BulkAnnotationPermissionDialog() {

        setTitle("Add or remove permissions for annotations");

        attrPanel = new JPanel(new MigLayout("wrap 2, ins 20"));
        add(attrPanel, BorderLayout.CENTER);

        attrPanel.add(new JLabel(INFO_MESSAGE), "gap para, span 2");
        
        addSeparator(attrPanel, "User");

        subjectCombobox = new JComboBox<Subject>();
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

    private void showSelectionMessage() {
            JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(),
                    "Select some items to bulk-edit permissions", "Error", JOptionPane.ERROR_MESSAGE);
    }
    public void showForSelectedDomainObjects() {

        DomainListViewTopComponent listView = DomainListViewManager.getInstance().getActiveViewer();
        if (listView==null || listView.getEditor()==null) {
            showSelectionMessage();
            return;
        }
        
        ChildSelectionModel<?,?> selectionModel = listView.getEditor().getSelectionModel();
        if (selectionModel==null) {
            showSelectionMessage();
            return;
        }
        
        selected.clear();
        for(Object id : selectionModel.getSelectedIds()) {
            if (id instanceof Reference) {
                selected.add((Reference)id);
            }
        }
        
        if (selected.isEmpty()) {
            showSelectionMessage();
            return;
        } 
        
        try {
            DomainMgr mgr = DomainMgr.getDomainMgr();
            List<Subject> subjects = mgr.getSubjects();
            DefaultComboBoxModel<Subject> model = (DefaultComboBoxModel<Subject>) subjectCombobox.getModel();
            model.removeAllElements();
            for (Subject subject : subjects) {
                model.addElement(subject);
            }

            ActivityLogHelper.logUserAction("BulkAnnotationPermissionDialog.showForSelectedDomainObjects");
            packAndShow();
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
    }

    private void saveAndClose() {
        
        Utils.setWaitingCursor(ConsoleApp.getMainFrame());

        final Subject subject = (Subject) subjectCombobox.getSelectedItem();
        boolean read = readCheckbox.isSelected();
        boolean write = writeCheckbox.isSelected();
        final String rights = (read?"r":"") + (write?"w":"");  
                
        SimpleWorker worker = new SimpleWorker() {

            private int numAnnotationsModified;
            
            @Override
            protected void doStuff() throws Exception {
                
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                for(Annotation annotation : model.getAnnotations(selected)) {
                    
                    // Must be owner to grant access
                    if (!ClientDomainUtils.isOwner(annotation)) continue;

                    model.changePermissions(annotation, subject.getKey(), rights);
                    
                    numAnnotationsModified++;
                }
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(ConsoleApp.getMainFrame());
                    JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(),
                        "Modified permissions for "+numAnnotationsModified+" annotations on "+selected.size()+" items", "Shared", JOptionPane.INFORMATION_MESSAGE);
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
                Utils.setDefaultCursor(ConsoleApp.getMainFrame());
            }
        };
        worker.setProgressMonitor(new IndeterminateProgressMonitor(ConsoleApp.getMainFrame(), "Changing permissions...", ""));
        worker.execute();

        setVisible(false);
    }
}
