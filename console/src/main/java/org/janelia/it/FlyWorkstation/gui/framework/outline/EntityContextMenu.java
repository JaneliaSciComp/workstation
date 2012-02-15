package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.EntityDetailsDialog;
import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OpenInFinderAction;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OpenWithDefaultAppAction;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Context pop up menu for entities.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityContextMenu extends JPopupMenu {

	protected static final Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
	protected Entity entity;
	protected boolean nextAddRequiresSeparator = false;
	
	public EntityContextMenu(Entity entity) {
		super();
		this.entity = entity;
	}
	
	@Override
	public JMenuItem add(JMenuItem menuItem) {
		
		if (menuItem == null) return null;
		
		if (nextAddRequiresSeparator) {
			addSeparator();
			nextAddRequiresSeparator = false;
		}
		
		return super.add(menuItem);
	}

	public void setNextAddRequiresSeparator(boolean nextAddRequiresSeparator) {
		this.nextAddRequiresSeparator = nextAddRequiresSeparator;
	}

	public void addMenuItems() {
        add(getTitleItem());
        add(getCopyNameToClipboardItem());
        add(getCopyIdToClipboardItem());
        add(getDetailsItem());
        add(getRenameItem());
        setNextAddRequiresSeparator(true);
    	add(getOpenInFinderItem());
    	add(getOpenWithAppItem());
        add(getNeuronAnnotatorItem());
	}

	protected JMenuItem getTitleItem() {
		String name = entity == null ? "Data" : entity.getName();
        JMenuItem titleMenuItem = new JMenuItem(name);
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
	}
	
	protected JMenuItem getDetailsItem() {
        JMenuItem detailsMenuItem = new JMenuItem("  View details");
        detailsMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
		        new EntityDetailsDialog().showForEntity(entity);
			}
		});
        return detailsMenuItem;
	}
	
	protected JMenuItem getCopyNameToClipboardItem() {
        JMenuItem copyMenuItem = new JMenuItem("  Copy name to clipboard");
        copyMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            Transferable t = new StringSelection(entity.getName());
	            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
			}
		});
        return copyMenuItem;
	}

	protected JMenuItem getCopyIdToClipboardItem() {
        JMenuItem copyMenuItem = new JMenuItem("  Copy GUID to clipboard");
        copyMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            Transferable t = new StringSelection(entity.getId().toString());
	            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
			}
		});
        return copyMenuItem;
	}
	
	protected JMenuItem getRenameItem() {
		JMenuItem renameItem = new JMenuItem("  Rename");
        renameItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                String newName = (String) JOptionPane.showInputDialog(browser, "Name:\n", "Rename "+entity.getName(), JOptionPane.PLAIN_MESSAGE, null, null, entity.getName());
                if ((newName == null) || (newName.length() <= 0)) {
                    return;
                }
	            
	            try {
	            	// Make sure we have the latest entity, then we can rename it
	            	Entity dbEntity = ModelMgr.getModelMgr().getEntityById(""+entity.getId());
	            	dbEntity.setName(newName);
	            	ModelMgr.getModelMgr().saveOrUpdateEntity(dbEntity);
	            }
                catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(SessionMgr.getSessionMgr().getActiveBrowser(), "Error renaming entity", "Error", JOptionPane.ERROR_MESSAGE);
				}
				
            }
        });
		if (!entity.getUser().getUserLogin().equals(SessionMgr.getUsername())) {
			renameItem.setEnabled(false);
		}
        return renameItem;
	}

	protected JMenuItem getOpenInFinderItem() {
		if (!OpenInFinderAction.isSupported()) return null;
    	String filepath = EntityUtils.getAnyFilePath(entity);
        if (!Utils.isEmpty(filepath)) {
        	return getActionItem(new OpenInFinderAction(entity));
        }
        return null;
	}
	
	protected JMenuItem getOpenWithAppItem() {
        if (!OpenWithDefaultAppAction.isSupported()) return null;
    	String filepath = EntityUtils.getAnyFilePath(entity);
        if (!Utils.isEmpty(filepath)) {
        	return getActionItem(new OpenWithDefaultAppAction(entity));
        }
        return null;
	}
	
	protected JMenuItem getNeuronAnnotatorItem() {
        final String entityType = entity.getEntityType().getName();
        if (entityType.equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT) || entityType.equals(EntityConstants.TYPE_NEURON_FRAGMENT)) {
            JMenuItem vaa3dMenuItem = new JMenuItem("  View in Vaa3D (Neuron Annotator)");
            vaa3dMenuItem.addActionListener(new ActionListener() {
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
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            return vaa3dMenuItem;
        }
        return null;
	}

	private JMenuItem getActionItem(final Action action) {
        JMenuItem actionMenuItem = new JMenuItem("  "+action.getName());
        actionMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				action.doAction();
			}
		});
        return actionMenuItem;
	}
}
