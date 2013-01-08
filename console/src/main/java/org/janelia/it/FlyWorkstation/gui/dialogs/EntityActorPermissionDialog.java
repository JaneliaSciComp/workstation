package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.access.Accessibility;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.IndeterminateProgressMonitor;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.user_data.Subject;

/**
 * A dialog for viewing, editing, or adding an EntityActorPermission.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityActorPermissionDialog extends ModalDialog implements Accessibility {
    
	private EntityDetailsDialog parentDialog;
	
    private JPanel attrPanel;
    private JComboBox subjectCombobox;
    private JCheckBox readCheckbox;
    private JCheckBox writeCheckbox;
    private JCheckBox recursiveCheckbox;
    
    private Entity entity;
    private EntityActorPermission eap;
    
    public EntityActorPermissionDialog(EntityDetailsDialog parentDialog) {

    	this.parentDialog = parentDialog;
    	
        setTitle("Add permission");
        
        attrPanel = new JPanel(new MigLayout("wrap 2, ins 20"));
        add(attrPanel, BorderLayout.CENTER);

    	subjectCombobox = new JComboBox();
    	subjectCombobox.setEditable(false);
        subjectCombobox.setToolTipText("Choose a user or group");
        
        ComboBoxRenderer renderer = new ComboBoxRenderer();
        subjectCombobox.setRenderer(renderer);
        subjectCombobox.setMaximumRowCount(20);

		DefaultComboBoxModel model = (DefaultComboBoxModel)subjectCombobox.getModel();
		model.addElement("Choose user or group...");

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
    
    public void showForNewPermission(Entity entity) {
    	this.entity = entity;
    	showForPermission(null);
    }
    
    private Font separatorFont = new Font("Sans Serif", Font.BOLD, 12);
    
    public void addSeparator(JPanel panel, String text) {
    	JLabel label = new JLabel(text);
    	label.setFont(separatorFont);
    	panel.add(label, "split 2, span, gaptop 10lp");
    	panel.add(new JSeparator(SwingConstants.HORIZONTAL), "growx, wrap, gaptop 10lp");
    }
    
    public void showForPermission(final EntityActorPermission eap) {

    	this.eap = eap;
    	if (eap!=null) {
    		this.entity = eap.getEntity();
    	}

    	attrPanel.removeAll();

    	addSeparator(attrPanel, "User");

		DefaultComboBoxModel model = (DefaultComboBoxModel)subjectCombobox.getModel();
		model.removeAllElements();
		
		Subject currSubject = null;
		for(Subject subject : parentDialog.getUnusedSubjects()) {
			if (entity!=null && !entity.getOwnerKey().equals(subject.getKey())) {
				model.addElement(subject);
			}
			if (eap!=null && eap.getSubjectKey().equals(subject.getKey())) {
				currSubject = subject;
			}
		}
		
		if (currSubject!=null) {
			model.setSelectedItem(currSubject);
		}
		
        attrPanel.add(subjectCombobox, "gap para, span 2");

    	addSeparator(attrPanel, "Permissions");
    	
        readCheckbox = new JCheckBox("Read");
        readCheckbox.setSelected(eap==null || eap.getPermissions().contains("r"));
        readCheckbox.setEnabled(false);
        attrPanel.add(readCheckbox, "gap para, span 2");

        writeCheckbox = new JCheckBox("Write");
        writeCheckbox.setSelected(eap!=null && eap.getPermissions().contains("w"));
        attrPanel.add(writeCheckbox, "gap para, span 2");

    	addSeparator(attrPanel, "Options");

    	recursiveCheckbox = new JCheckBox("Apply permission changes to all subfolders");
    	recursiveCheckbox.setSelected(true);
        attrPanel.add(recursiveCheckbox, "gap para, span 2");
        
        packAndShow();
    }
    
    private void saveAndClose() {
        
    	Utils.setWaitingCursor(parentDialog);
    	
    	final Subject subject = (Subject)subjectCombobox.getSelectedItem();
    	final boolean recursive = recursiveCheckbox.isSelected();
    	
        SimpleWorker worker = new SimpleWorker() {

			@Override
			protected void doStuff() throws Exception {
				String permissions = (readCheckbox.isSelected()?"r":"")+""+(writeCheckbox.isSelected()?"w":"");
				if (eap==null) {
					eap = ModelMgr.getModelMgr().grantPermissions(entity.getId(), subject.getKey(), permissions, recursive);
				}
				else {
					eap.setSubjectKey(subject.getKey());
					eap.setPermissions(permissions);
					ModelMgr.getModelMgr().saveOrUpdatePermission(eap);
				}
			}
			
			@Override
			protected void hadSuccess() {	
				parentDialog.refresh();
				ModelMgr.getModelMgr().invalidateCache(entity, recursive);
				Utils.setDefaultCursor(parentDialog);
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
				Utils.setDefaultCursor(parentDialog);
			}
		};
		worker.setProgressMonitor(new IndeterminateProgressMonitor(parentDialog, "Granting permissions...", ""));
		worker.execute();

        setVisible(false);
    }

	public boolean isAccessible() {
    	return true;
    }

	private class ComboBoxRenderer extends JLabel implements ListCellRenderer {

		public ComboBoxRenderer() {
			setOpaque(true);
			setHorizontalAlignment(SwingConstants.LEFT);
			setVerticalAlignment(SwingConstants.CENTER);
		}

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			
			if (value instanceof String) {
				setIcon(null);
				setText(value.toString());
				return this;
			}
			
			Subject subject = (Subject)value;

			if (subject==null) {
				setIcon(Icons.getIcon("error.png"));
				setText("Unknown");
				return this;
			}
			
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
						
			if (subject.getKey()!=null && subject.getKey().startsWith("group:")) {
				setIcon(Icons.getIcon("group.png"));
			}
			else {
				setIcon(Icons.getIcon("user.png"));
			}
			
			setText(subject.getFullName()+" ("+subject.getName()+")");
			
			return this;
		}
	}
}
