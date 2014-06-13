package org.janelia.it.workstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import org.janelia.it.workstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.outline.EntityDetailsPanel;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.model.entity.RootedEntity;

/**
 * A dialog for viewing details about an entity.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityDetailsDialog extends ModalDialog {

    private EntityDetailsPanel entityDetailsPanel;
    private ModelMgrAdapter modelMgrAdapter;
    
    public EntityDetailsDialog() {

        setModalityType(ModalityType.MODELESS);

        this.entityDetailsPanel = new EntityDetailsPanel();
        add(entityDetailsPanel, BorderLayout.CENTER);

        this.modelMgrAdapter = new ModelMgrAdapter() {
            @Override
            public void annotationsChanged(final long entityId) {
                if (entityId != entityDetailsPanel.getEntity().getId()) return; 
                entityDetailsPanel.loadAnnotations();
            }
        };
        
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
    	showForEntity(rootedEntity.getEntity(), entityData.getEntityAttrName()==null?"":entityData.getEntityAttrName(), defaultTab);
    }

    public void showForEntity(final Entity entity) {
    	showForEntity(entity, EntityDetailsPanel.TAB_NAME_ATTRIBUTES);
    }
    
    public void showForEntity(final Entity entity, final String defaultTab) {
    	showForEntity(entity, null, defaultTab);
    }
    
    private void showForEntity(final Entity entity, final String role, String defaultTab) {
        
        // Register this dialog as a model observer
        ModelMgr.getModelMgr().addModelMgrObserver(modelMgrAdapter);
        
        entityDetailsPanel.loadEntity(entity, role, defaultTab);
        setTitle("Details: "+entity.getName());
        Component mainFrame = SessionMgr.getMainFrame();
        setPreferredSize(new Dimension((int)(mainFrame.getWidth()*0.5),(int)(mainFrame.getHeight()*0.8)));
        // Show dialog and wait
        packAndShow();
        
        // Dialog is closing, clean up observer
        ModelMgr.getModelMgr().removeModelMgrObserver(modelMgrAdapter);
    }
}
