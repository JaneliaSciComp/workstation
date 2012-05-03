package org.janelia.it.FlyWorkstation.gui.dataview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.EntityDetailsDialog;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OpenInFinderAction;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OpenWithDefaultAppAction;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.context_menu.AbstractContextMenu;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Context pop up menu for entities in the data viewer.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataviewContextMenu extends AbstractContextMenu<Entity> {

	protected static final Browser browser = SessionMgr.getBrowser();

	public DataviewContextMenu(List<Entity> selectedEntities, String label) {
		super(selectedEntities, label);
	}

	@Override
	protected void addSingleSelectionItems() {
		Entity entity = getSelectedElement();
		add(getTitleItem("Entity '"+entity.getName()+"'"));
        add(getDetailsItem());
        add(getRenameItem());
        add(getDeleteItem());
        add(getDeleteTreeItem());
        setNextAddRequiresSeparator(true);
    	add(getOpenInFinderItem());
    	add(getOpenWithAppItem());
	}
	
	@Override
	protected void addMultipleSelectionItems() {
        add(getDeleteItem());
        add(getDeleteTreeItem());
	}
	
	protected JMenuItem getDetailsItem() {
		final Entity entity = getSelectedElement();
        JMenuItem detailsMenuItem = new JMenuItem("  View details");
        detailsMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
		        new EntityDetailsDialog().showForEntity(entity);
			}
		});
        return detailsMenuItem;
	}
	
	protected JMenuItem getRenameItem() {
		final Entity entity = getSelectedElement();
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
                catch (Exception error) {
                	SessionMgr.getSessionMgr().handleException(error);
				}
				
            }
        });
        return renameItem;
	}

	private JMenuItem getDeleteItem() {

		String name = isMultipleSelection() ? "Delete entities" : "Delete entity";
		
		JMenuItem deleteEntityMenuItem = new JMenuItem("  "+name);
        deleteEntityMenuItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
	            int deleteConfirmation = confirm("Are you sure you want to delete " + getSelectedElements().size() + 
	            		" entities? This can potentially orphan their children, if they have any.");
	            if (deleteConfirmation != 0) return;

	            final List<Entity> toDelete = new ArrayList<Entity>(getSelectedElements());

	            Utils.setWaitingCursor(DataviewApp.getMainFrame());
	            
	            SimpleWorker loadTask = new SimpleWorker() {

	                @Override
	                protected void doStuff() throws Exception {
    		            // Update database
    		            for (Entity entity : toDelete) {
    		                boolean success = ModelMgr.getModelMgr().deleteEntityById(entity.getId());
    		                if (!success) {
    		                	SessionMgr.getSessionMgr().handleException(
    		                			new Exception("Error deleting entity with id=" + entity.getId()));
    		                }
    		            }
	                }

	                @Override
	                protected void hadSuccess() {
	                	Utils.setDefaultCursor(DataviewApp.getMainFrame());
//	    	            reshow();
	                }

	                @Override
	                protected void hadError(Throwable error) {
	                	SessionMgr.getSessionMgr().handleException(error);
	                }

	            };

	            loadTask.execute();
			}
		});
        return deleteEntityMenuItem;
	}

	private JMenuItem getDeleteTreeItem() {

		String name = isMultipleSelection() ? "Delete entity trees" : "Delete entity tree";
		
        JMenuItem deleteTreeMenuItem = new JMenuItem("  "+name);
        deleteTreeMenuItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
	            int deleteConfirmation = confirm("Are you sure you want to delete " + getSelectedElements().size() + " entities and all their descendants?");
	            if (deleteConfirmation != 0) {
	                return;
	            }

	            final List<Entity> toDelete = new ArrayList<Entity>(getSelectedElements());

            	boolean su = false;
	            for (Entity entity : toDelete) {
                	if (!SessionMgr.getUsername().equals(entity.getUser().getUserLogin())) {
        	            int overrideConfirmation = confirm("Override owner "+entity.getUser().getUserLogin()+" to delete "+entity.getName()+"?");
        	            if (overrideConfirmation != 0) {
        	                continue;
        	            }
        	            SessionMgr.getSessionMgr().setModelProperty(SessionMgr.USER_NAME, entity.getUser().getUserLogin());
        	            su = true;
        	            break;
                	}
	            }
	            
	            final boolean didSu = su;
	            final String realUsername = SessionMgr.getUsername();
	            
	            Utils.setWaitingCursor(DataviewApp.getMainFrame());
	            
	            SimpleWorker loadTask = new SimpleWorker() {

	                @Override
	                protected void doStuff() throws Exception {
	    	            // Update database
	    	            for (Entity entity : toDelete) {
	    	            	System.out.println("Deleting "+entity.getId());
    	                    ModelMgr.getModelMgr().deleteEntityTree(entity.getId());
	    	            }
	                }

	                @Override
	                protected void hadSuccess() {
	                    if (didSu) {
	        	            SessionMgr.getSessionMgr().setModelProperty(SessionMgr.USER_NAME, realUsername);
	                    }
	                	Utils.setDefaultCursor(DataviewApp.getMainFrame());
//	    	            reshow();
	                }

	                @Override
	                protected void hadError(Throwable error) {
	                    SessionMgr.getSessionMgr().handleException(error);
	                }

	            };

	            loadTask.execute();
			}
		});
        
        return deleteTreeMenuItem;
	}
	
	protected JMenuItem getOpenInFinderItem() {
		if (!OpenInFinderAction.isSupported()) return null;
		final Entity entity = getSelectedElement();
    	String filepath = EntityUtils.getAnyFilePath(entity);
        if (!Utils.isEmpty(filepath)) {
        	return getActionItem(new OpenInFinderAction(entity));
        }
        return null;
	}
	
	protected JMenuItem getOpenWithAppItem() {
        if (!OpenWithDefaultAppAction.isSupported()) return null;
		final Entity entity = getSelectedElement();
    	String filepath = EntityUtils.getAnyFilePath(entity);
        if (!Utils.isEmpty(filepath)) {
        	return getActionItem(new OpenWithDefaultAppAction(entity));
        }
        return null;
	}

	private int confirm(String message) {
		return JOptionPane.showConfirmDialog(browser, message, "Are you sure?", JOptionPane.YES_NO_OPTION);
	}

}
