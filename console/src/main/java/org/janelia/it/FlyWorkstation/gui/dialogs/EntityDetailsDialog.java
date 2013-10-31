package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityDetailsPanel;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * A dialog for viewing details about an entity.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityDetailsDialog extends ModalDialog {

    private EntityDetailsPanel entityDetailsPanel;
    
    public EntityDetailsDialog() {

        setModalityType(ModalityType.MODELESS);

        this.entityDetailsPanel = new EntityDetailsPanel();
        add(entityDetailsPanel, BorderLayout.CENTER);
        
        // Buttons
        
        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            setVisible(false);
			}
		});

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);
        
        add(buttonPane, BorderLayout.SOUTH);
    }
    
    public void showForRootedEntity(RootedEntity rootedEntity) {
    	showForRootedEntity(rootedEntity, EntityDetailsPanel.TAB_NAME_ATTRIBUTES);
    }
    
    public void showForRootedEntity(RootedEntity rootedEntity, String defaultTab) {
    	EntityData entityData = rootedEntity.getEntityData();
    	showForEntity(rootedEntity.getEntity(), entityData.getEntityAttribute()==null?"":entityData.getEntityAttribute().getName(), defaultTab);
    }

    public void showForEntity(final Entity entity) {
    	showForEntity(entity, EntityDetailsPanel.TAB_NAME_ATTRIBUTES);
    }
    
    public void showForEntity(final Entity entity, final String defaultTab) {
    	showForEntity(entity, null, defaultTab);
    }
    
    private void showForEntity(final Entity entity, final String role, String defaultTab) {
        entityDetailsPanel.loadEntity(entity, role, defaultTab);
        setTitle("Entity Details: "+entity.getName());
        Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
        setPreferredSize(new Dimension((int)(browser.getWidth()*0.5),(int)(browser.getHeight()*0.8)));
        // Show dialog and wait
        packAndShow();
    }
}
