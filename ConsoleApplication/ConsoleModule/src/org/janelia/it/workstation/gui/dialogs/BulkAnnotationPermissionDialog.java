package org.janelia.it.workstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;

import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.workstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.workstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.workstation.gui.util.SubjectComboBoxRenderer;
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

    private final EntitySelectionModel esm;
    private final List<Long> entityIds = new ArrayList<>();

    public BulkAnnotationPermissionDialog() {

        this.esm = ModelMgr.getModelMgr().getEntitySelectionModel();
            
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

    public void showForSelectedEntities() {

        entityIds.clear();
        
        if (esm.getActiveCategory()==null) {
            JOptionPane.showMessageDialog(SessionMgr.getMainFrame(),
                    "Select some items to bulk-edit permissions", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } 
        
        try {
            List<String> uniqueIds = esm.getSelectedEntitiesIds(esm.getActiveCategory());

            for(String selectedEntityId : uniqueIds) {
                entityIds.add(EntityUtils.getEntityIdFromUniqueId(selectedEntityId));
            }

            if (entityIds.isEmpty()) {
                return;
            }
            
           // List<Subject> subjects = new ArrayList<>(ModelMgr.getModelMgr().getSubjects());
            //EntityUtils.sortSubjects(subjects);

            DefaultComboBoxModel model = (DefaultComboBoxModel) subjectCombobox.getModel();
            model.removeAllElements();
            /*for (Subject subject : subjects) {
                model.addElement(subject);
            }*/

            packAndShow();
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    private void saveAndClose() {

        Utils.setWaitingCursor(SessionMgr.getMainFrame());

        final Subject subject = (Subject) subjectCombobox.getSelectedItem();
        final boolean read = readCheckbox.isSelected();
        final boolean write = writeCheckbox.isSelected();
        
        SimpleWorker worker = new SimpleWorker() {

            private int numAnnotationsModified;
            
            @Override
            protected void doStuff() throws Exception {
                
                for(Entity annotationEntity : ModelMgr.getModelMgr().getAnnotationsForEntities(entityIds)) {
                    
                    // Must be owner to grant access
                    if (!ModelMgrUtils.isOwner(annotationEntity)) continue;
                        
                    EntityActorPermission eap = getPermission(annotationEntity, subject);

                    String permissions = (read ? "r" : "") + (write ? "w" : "");
                    if (eap == null) {
                        if (read) {
                            // Add permission
                            ModelMgr.getModelMgr().grantPermissions(annotationEntity.getId(), subject.getKey(), permissions, false);
                            numAnnotationsModified++;
                        }
                        else {
                            // Permission is already missing
                        }
                    }
                    else {
                        if (read) {
                            if (!eap.getPermissions().equals(permissions)) {
                                // Update permissions 
                                eap.setPermissions(permissions);
                                ModelMgr.getModelMgr().saveOrUpdatePermission(eap);
                                numAnnotationsModified++;
                            }
                            else {
                                // Permissions are already there
                            }
                        }
                        else {
                            // Remove permission
                            ModelMgr.getModelMgr().revokePermissions(annotationEntity.getId(), subject.getKey(), false);
                            numAnnotationsModified++;
                        }
                    }
                }
                
                for(Long entityId : entityIds) {
                    try {
                        ModelMgr.getModelMgr().invalidateCache(ModelMgr.getModelMgr().getEntityById(entityId), false);
                    }
                    catch (Exception e) {
                        log.error("Could not invalidate entity which has modified annotations: "+entityId,e);
                    }
                }
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(SessionMgr.getMainFrame());
                    JOptionPane.showMessageDialog(SessionMgr.getMainFrame(),
                        "Modified permissions for "+numAnnotationsModified+" annotations on "+entityIds.size()+" items", "Shared", JOptionPane.INFORMATION_MESSAGE);
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

    private EntityActorPermission getPermission(Entity entity, Subject subject) {
        EntityActorPermission found = null;
        for(EntityActorPermission eap : entity.getEntityActorPermissions()) {
            if (eap.getSubjectKey().equals(subject.getKey())) {
                if (found!=null) {
                    log.warn("Two permissions for the same subject: "+subject.getKey());
                }
                found = eap;
            }
        }
        return found;
    }
}
