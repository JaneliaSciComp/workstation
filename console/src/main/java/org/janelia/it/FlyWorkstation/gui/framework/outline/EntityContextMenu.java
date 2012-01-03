package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OpenInFinderAction;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OpenWithDefaultAppAction;
import org.janelia.it.FlyWorkstation.shared.util.EntityUtils;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Context pop up menu for entities.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityContextMenu extends JPopupMenu {

	private void addAction(final Action action) {
        JMenuItem revealMenuItem = new JMenuItem("  "+action.getName());
        revealMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				action.doAction();
			}
		});
        add(revealMenuItem);
	}
	
	public EntityContextMenu(final Entity entity) {
		super();
		
        JMenuItem titleMenuItem = new JMenuItem(entity.getName());
        titleMenuItem.setEnabled(false);
        add(titleMenuItem);

        // Copy to clipboard
        JMenuItem copyMenuItem = new JMenuItem("  Copy to clipboard");
        copyMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            Transferable t = new StringSelection(entity.getName());
	            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
			}
		});
        add(copyMenuItem);
        
        if (OpenInFinderAction.isSupported()) {
        	String filepath = EntityUtils.getAnyFilePath(entity);
	        if (!Utils.isEmpty(filepath)) {
	        	addAction(new OpenInFinderAction(entity));
	        }
        }

        if (OpenWithDefaultAppAction.isSupported()) {
        	String filepath = EntityUtils.getAnyFilePath(entity);
	        if (!Utils.isEmpty(filepath)) {
	        	addAction(new OpenWithDefaultAppAction(entity));
	        }
        }
        
        final String entityType = entity.getEntityType().getName();
        if (entityType.equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT) || entityType.equals(EntityConstants.TYPE_NEURON_FRAGMENT)) {
            JMenuItem v3dMenuItem = new JMenuItem("  View in V3D (Neuron Annotator)");
            v3dMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
    				try {
    					Entity result = entity;
    					if (!entityType.equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
	    					result = ModelMgr.getModelMgr().getAncestorWithType(entity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
    					}
    					
	                    if (result != null && ModelMgr.getModelMgr().notifyEntityViewRequestedInNeuronAnnotator(result.getId())) {
	                    	// Success
	                    	return;
	                    }
    				} 
    				catch (Exception e) {
    					e.printStackTrace();
    				}
                }
            });
            add(v3dMenuItem);
        }
	}
}
