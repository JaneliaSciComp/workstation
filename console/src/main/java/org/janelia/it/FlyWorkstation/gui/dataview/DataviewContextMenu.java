package org.janelia.it.FlyWorkstation.gui.dataview;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.EntityDetailsDialog;
import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OpenInFinderAction;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OpenWithDefaultAppAction;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Context pop up menu for entities in the data viewer.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataviewContextMenu extends JPopupMenu {

	protected static final Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();

	protected List<Entity> selectedEntities;
	protected Entity entity;
	protected String label;
	protected boolean nextAddRequiresSeparator = false;


	public DataviewContextMenu(List<Entity> selectedEntities, String label) {
		this.selectedEntities = selectedEntities;
		this.entity = selectedEntities.get(0);
		this.label = label;
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
		if (selectedEntities.size()>1) {
			add(getTitleItem("(Multiple items selected)"));
	        add(getDeleteItem());
	        add(getDeleteTreeItem());
		}
		else {
			if (!StringUtils.isEmpty(label)) {
				add(getTitleItem(label));
		        add(getCopyToClipboardItem());
			}
			add(getTitleItem("Entity '"+entity.getName()+"'"));
	        add(getDetailsItem());
	        add(getRenameItem());
	        add(getDeleteItem());
	        add(getDeleteTreeItem());
	        setNextAddRequiresSeparator(true);
	    	add(getOpenInFinderItem());
	    	add(getOpenWithAppItem());
		}
	}

	protected JMenuItem getTitleItem(String title) {
        JMenuItem titleMenuItem = new JMenuItem(title);
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
	}
	
	protected JMenuItem getCopyToClipboardItem() {
		if (selectedEntities.size()>1) return null;
        JMenuItem copyMenuItem = new JMenuItem("  Copy to clipboard");
        copyMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            Transferable t = new StringSelection(label);
	            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
			}
		});
        return copyMenuItem;
	}
	
	protected JMenuItem getDetailsItem() {
		if (selectedEntities.size()>1) return null;
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
		if (selectedEntities.size()>1) return null;
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

		String name = selectedEntities.size()>1 ? "Delete entities" : "Delete entity";
		
		JMenuItem deleteEntityMenuItem = new JMenuItem("  "+name);
        deleteEntityMenuItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
	            int deleteConfirmation = confirm("Are you sure you want to delete " + selectedEntities.size() + 
	            		" entities? This can potentially orphan their children, if they have any.");
	            if (deleteConfirmation != 0) return;

	            final List<Entity> toDelete = new ArrayList<Entity>(selectedEntities);

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

		String name = selectedEntities.size()>1 ? "Delete entity trees" : "Delete entity tree";
		
        JMenuItem deleteTreeMenuItem = new JMenuItem("  "+name);
        deleteTreeMenuItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
	            int deleteConfirmation = confirm("Are you sure you want to delete " + selectedEntities.size() + " entities and all their descendants?");
	            if (deleteConfirmation != 0) {
	                return;
	            }

	            final List<Entity> toDelete = new ArrayList<Entity>(selectedEntities);

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

	private int confirm(String message) {
		return JOptionPane.showConfirmDialog(browser, message, "Are you sure?", JOptionPane.YES_NO_OPTION);
	}

}
