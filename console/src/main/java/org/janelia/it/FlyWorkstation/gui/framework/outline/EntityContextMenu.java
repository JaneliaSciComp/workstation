package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.EntityDetailsDialog;
import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OpenInFinderAction;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OpenWithDefaultAppAction;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.IconDemoPanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.Viewer;
import org.janelia.it.FlyWorkstation.gui.util.PathTranslator;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.PreferenceConstants;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Context pop up menu for entities.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityContextMenu extends JPopupMenu {

	protected static final Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
	protected final EntityData entityData;
	protected final Entity entity; // just a shortcut to entityData.getChildEntity()
	protected final String uniqueId;
	protected boolean nextAddRequiresSeparator = false;
	
	public EntityContextMenu(EntityData entityData, String uniqueId) {
		super();
		this.entityData = entityData;
		this.entity = entityData!=null ? entityData.getChildEntity() : null;
		this.uniqueId = uniqueId;
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
		add(getDeleteItem());
        setNextAddRequiresSeparator(true);
        add(getOpenInSecondViewerItem());
    	add(getOpenInFinderItem());
    	add(getOpenWithAppItem());
        add(getNeuronAnnotatorItem());
        add(getVaa3dItem());
        setNextAddRequiresSeparator(true);
        add(getSearchHereItem());
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

	protected JMenuItem getDeleteItem() {

		JMenuItem deleteItem = new JMenuItem("  Remove '"+entity.getName()+"'");
		deleteItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {

				final List<EntityData> eds = ModelMgr.getModelMgr().getParentEntityDatas(entity.getId());

				boolean removeReference = true;
				boolean removeCommonRootTag = false;
				
				if (eds.size() <= 1 && entity.getUser().getUserLogin().equals(SessionMgr.getUsername())) {
					// Make sure its not a reference to a common root
					if (entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT)==null || eds.isEmpty()) {
						int deleteConfirmation = JOptionPane.showConfirmDialog(browser,
								"Are you sure you want to permanently delete '" + entity.getName()
										+ "' and all orphaned items underneath it?", "Delete",
								JOptionPane.YES_NO_OPTION);
						if (deleteConfirmation != 0) {
							return;
						}
						removeReference = false;
					}
					else if (entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT)!=null && entityData.getId()==null) {
						removeCommonRootTag = true;
					}
				}

				Utils.setWaitingCursor(browser);
				
				final boolean removeCommonRootTagFinal = removeCommonRootTag;
				final boolean removeReferenceFinal = removeReference;
				SimpleWorker removeTask = new SimpleWorker() {

					@Override
					protected void doStuff() throws Exception {
						// Update database
						if (removeCommonRootTagFinal) {
							EntityData rootTagEd = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT);
							ModelMgr.getModelMgr().removeEntityData(rootTagEd);
						}
						else if (removeReferenceFinal) {
							ModelMgr.getModelMgr().removeEntityData(entityData);
						} 
						else {
							ModelMgr.getModelMgr().deleteEntityTree(entity.getId());
						}
					}

					@Override
					protected void hadSuccess() {
						if (removeCommonRootTagFinal) {
							browser.getEntityOutline().refresh();
						}
						Utils.setDefaultCursor(browser);
					}

					@Override
					protected void hadError(Throwable error) {
						Utils.setDefaultCursor(browser);
						error.printStackTrace();
						JOptionPane.showMessageDialog(browser, "Error deleting entity", "Error",
								JOptionPane.ERROR_MESSAGE);
					}
				};

				removeTask.execute();
			}
		});

		if (entityData==null || !entityData.getUser().getUserLogin().equals(SessionMgr.getUsername())) {
			deleteItem.setEnabled(false);
		}
		return deleteItem;
	}
	
	protected JMenuItem getOpenInSecondViewerItem() {
		if (Utils.isEmpty(uniqueId)) return null;
        JMenuItem copyMenuItem = new JMenuItem("  Open in second viewer");
        copyMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Viewer secViewer = SessionMgr.getBrowser().getViewersPanel().getSecViewer();
				if (secViewer==null) {
					secViewer = new IconDemoPanel(SessionMgr.getBrowser().getViewersPanel(), EntitySelectionModel.CATEGORY_SEC_VIEW);
					SessionMgr.getBrowser().getViewersPanel().setSecViewer(secViewer);
				}
	            ((IconDemoPanel)secViewer).loadEntity(entityData, uniqueId);
	            secViewer.setAsActive();
	            ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, uniqueId, true);
			}
		});
        return copyMenuItem;
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
                    } 
                    catch (Exception e) {
                    	SessionMgr.getSessionMgr().handleException(e);
                    }
                }
            });
            return vaa3dMenuItem;
        }
        return null;
	}

	protected JMenuItem getVaa3dItem() {
        final String entityType = entity.getEntityType().getName();
        if (entityType.equals(EntityConstants.TYPE_IMAGE_3D) ||
            entityType.equals(EntityConstants.TYPE_ALIGNED_BRAIN_STACK) ||
            entityType.equals(EntityConstants.TYPE_LSM_STACK) ||
            entityType.equals(EntityConstants.TYPE_STITCHED_V3D_RAW) ||
            entityType.equals(EntityConstants.TYPE_SWC_FILE) ||
            entityType.equals(EntityConstants.TYPE_V3D_ANO_FILE) ||
            entityType.equals(EntityConstants.TYPE_TIF_3D)) {
            JMenuItem vaa3dMenuItem = new JMenuItem("  View in Vaa3D");
            vaa3dMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
//                        if (entity != null && ModelMgr.getModelMgr().notifyEntityViewRequestedInNeuronAnnotator(entity.getId())) {
//                            // Success
//                            return;
//                        }
                        String vaa3dExePath = (String) SessionMgr.getSessionMgr().getModelProperty(PreferenceConstants.PATH_VAA3D);
//                        vaa3dExePath = "/Applications/FlySuite.app/Contents/Resources/vaa3d64.app/Contents/MacOS/vaa3d64"; // DEBUG ONLY
                        File tmpFile = new File(vaa3dExePath);
                        if (tmpFile.exists()&&tmpFile.canExecute()) {
                            vaa3dExePath+=" -i "+ PathTranslator.convertPath(EntityUtils.getAnyFilePath(entity));
                            System.out.println("Calling to open file with: "+vaa3dExePath);
                            Runtime.getRuntime().exec(vaa3dExePath);
                        }
                        else {
                            JOptionPane.showMessageDialog(browser, "Could not launch Vaa3D", "Tool Launch ERROR", JOptionPane.ERROR_MESSAGE);
                        }
                                
                    } 
                    catch (Exception e) {
                        SessionMgr.getSessionMgr().handleException(e);
                    }
                }
            });
            return vaa3dMenuItem;
        }
        return null;
	}

	protected JMenuItem getSearchHereItem() {
        JMenuItem searchHereMenuItem = new JMenuItem("  Search here...");
        searchHereMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                	SessionMgr.getSessionMgr().getActiveBrowser().getSearchDialog().showDialog(entity);
                } 
                catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }
        });
        return searchHereMenuItem;
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
