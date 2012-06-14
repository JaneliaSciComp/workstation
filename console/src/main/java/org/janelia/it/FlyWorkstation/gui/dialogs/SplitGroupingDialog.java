package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityTreeCellRenderer;
import org.janelia.it.FlyWorkstation.gui.framework.outline.SelectionTreePanel;
import org.janelia.it.FlyWorkstation.gui.framework.outline.SplitPickingPanel;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.IconDemoPanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.gui.util.FakeProgressWorker;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * A dialog that pops up from the SplitPickingPanel and allows the user to group Screen Samples in one folder into 
 * representatives for AD's and DBD's in another folder.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SplitGroupingDialog extends ModalDialog {

	private JButton okButton;
	private JButton cancelButton;
	private SelectionTreePanel<Entity> splitAdTreePanel;
    private SelectionTreePanel<Entity> splitDbdTreePanel;
    
	private final SplitPickingPanel splitPickingPanel;

	public SplitGroupingDialog(final SplitPickingPanel splitPickingPanel) {

		this.splitPickingPanel = splitPickingPanel;

        GridBagConstraints c = new GridBagConstraints();
        
        JPanel treesPanel = new JPanel(new GridLayout(1, 2));

        splitAdTreePanel = new SelectionTreePanel<Entity>("Activation Domain", false) {
            @Override
            // Must override this because Entity does not implement equals()
            public boolean containsItem(Entity entity) {
                DefaultMutableTreeNode rootNode = getDynamicTree().getRootNode();
                for (Enumeration e = rootNode.children(); e.hasMoreElements(); ) {
                    DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
                    if (((Entity) childNode.getUserObject()).getId().equals(entity.getId())) {
                        return true;
                    }
                }
                return false;
            }
        };
        c.gridx = 0;
        c.gridy = 0;
        splitAdTreePanel.setPreferredSize(new Dimension(500, 500));
        treesPanel.add(splitAdTreePanel);

        splitDbdTreePanel = new SelectionTreePanel<Entity>("DNA Binding Domain", false) {
            @Override
            // Must override this because Entity does not implement equals()
            public boolean containsItem(Entity entity) {
                DefaultMutableTreeNode rootNode = getDynamicTree().getRootNode();
                for (Enumeration e = rootNode.children(); e.hasMoreElements(); ) {
                    DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
                    if (((Entity) childNode.getUserObject()).getId().equals(entity.getId())) {
                        return true;
                    }
                }
                return false;
            }
        };
        c.gridx = 1;
        c.gridy = 0;
        splitDbdTreePanel.setPreferredSize(new Dimension(500, 500));
        treesPanel.add(splitDbdTreePanel);
        
        add(treesPanel, BorderLayout.CENTER);

        okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            setVisible(false);
	            saveGrouping();
	            
			}
		});

        cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            setVisible(false);
			}
		});

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(cancelButton);
        add(buttonPane, BorderLayout.SOUTH);
	}
	
    public void showDialog() {

        splitAdTreePanel.createNewTree();
        splitAdTreePanel.getDynamicTree().setCellRenderer(new EntityTreeCellRenderer());
    	splitAdTreePanel.showLoadingIndicator();
        
        splitDbdTreePanel.createNewTree();
        splitDbdTreePanel.getDynamicTree().setCellRenderer(new EntityTreeCellRenderer());
    	splitDbdTreePanel.showLoadingIndicator();
        
    	okButton.setEnabled(false);
    	cancelButton.setEnabled(false);
        
		SimpleWorker worker = new SimpleWorker() {
			
			private List<Entity> represented;
			
			@Override
			protected void doStuff() throws Exception {

				RootedEntity repFolder = splitPickingPanel.getRepFolder();
				
				// Try to get it from the entity tree, in case it's already partially loaded
				Entity repEntity = null;
				Set<Entity> repEntities = SessionMgr.getBrowser().getEntityOutline().getEntitiesById(repFolder.getEntity().getId());
				for(Entity entity : repEntities) {
					if (EntityUtils.isInitialized(repEntities)) {
						repEntity = entity;
					}
				}
				
				if (repEntity==null) {
					repEntity = ModelMgr.getModelMgr().getEntityById(repFolder.getEntity().getId()+"");
				}
				
		    	List<Entity> screenSamples = ModelMgrUtils.getDescendantsOfType(repEntity, EntityConstants.TYPE_SCREEN_SAMPLE, true);  
		    	
		    	List<Long> entityIds = new ArrayList<Long>();
		    	for(Entity screenSample : screenSamples) {
		    		Set<Long> parentIds = ModelMgr.getModelMgr().getParentIdsForAttribute(screenSample.getId(), EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE);
		    		if (!parentIds.isEmpty()) {
		    			entityIds.addAll(parentIds);
		    		}
		    	}
		    	
		    	represented = ModelMgr.getModelMgr().getEntityByIds(entityIds);
			}

			@Override
			protected void hadSuccess() {
				for(Entity rep : represented) {
					String splitPart = rep.getValueByAttributeName(EntityConstants.ATTRIBUTE_SPLIT_PART);
					if ("AD".equals(splitPart)) {
						splitAdTreePanel.addItem(rep);
					}
					else if ("DBD".equals(splitPart)) {
						splitDbdTreePanel.addItem(rep);
					}
				}
		    	okButton.setEnabled(true);
		    	cancelButton.setEnabled(true);
		    	splitAdTreePanel.showTree();
		    	splitDbdTreePanel.showTree();
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
		    	okButton.setEnabled(true);
		    	cancelButton.setEnabled(true);
		    	splitAdTreePanel.showTree();
		    	splitDbdTreePanel.showTree();
			}
		};

		worker.execute();

        packAndShow();
    }
    
    private void saveGrouping() {

		SimpleWorker worker = new FakeProgressWorker() {
			private RootedEntity splitLinesFolder;
			private RootedEntity groupAdFolder;
			private RootedEntity groupDbdFolder;
			
			@Override
			protected void doStuff() throws Exception {

				if (splitPickingPanel.getSplitLinesFolder()==null) {
					splitPickingPanel.setSplitLinesFolder(ModelMgrUtils.getChildFolder(splitPickingPanel.getWorkingFolder(), SplitPickingPanel.FOLDER_NAME_SPLIT_LINES, true));
				}
				splitLinesFolder = splitPickingPanel.getSplitLinesFolder();
					
				if (splitPickingPanel.getGroupAdFolder()==null) {
					splitPickingPanel.setGroupAdFolder(ModelMgrUtils.getChildFolder(splitLinesFolder, SplitPickingPanel.FOLDER_NAME_SPLIT_LINES_AD, true));
				}
				if (splitPickingPanel.getGroupDbdFolder()==null) {
					splitPickingPanel.setGroupDbdFolder(ModelMgrUtils.getChildFolder(splitLinesFolder, SplitPickingPanel.FOLDER_NAME_SPLIT_LINES_DBD, true));
				}
				
		    	groupAdFolder = splitPickingPanel.getGroupAdFolder();
		    	groupDbdFolder = splitPickingPanel.getGroupDbdFolder();
		    	
		    	List<Long> newSplitAdIds = new ArrayList<Long>();
		    	List<Long> newSplitDbdIds = new ArrayList<Long>();
		    	
	            for (Entity rep : splitAdTreePanel.getItems()) {
	            	newSplitAdIds.add(rep.getId());
	            }
	            for (Entity rep : splitDbdTreePanel.getItems()) {
	            	newSplitDbdIds.add(rep.getId());
	            }
	            
	            // Remove current 
	            for (EntityData ed : new ArrayList<EntityData>(groupAdFolder.getEntity().getEntityData())) {
	            	if (ed.getChildEntity()!=null) {
	            		groupAdFolder.getEntity().getEntityData().remove(ed);
	            		ModelMgr.getModelMgr().removeEntityData(ed);
	            	}
	            }
	            for (EntityData ed : new ArrayList<EntityData>(groupDbdFolder.getEntity().getEntityData())) {
	            	if (ed.getChildEntity()!=null) {
	            		groupDbdFolder.getEntity().getEntityData().remove(ed);
	            		ModelMgr.getModelMgr().removeEntityData(ed);
	            	}
	            }
	            
	            // Add new 
		    	if (!newSplitAdIds.isEmpty()) {
		    		ModelMgr.getModelMgr().addChildren(groupAdFolder.getEntity().getId(), newSplitAdIds, EntityConstants.ATTRIBUTE_ENTITY);
		    	}
		    	if (!newSplitDbdIds.isEmpty()) {
		    		ModelMgr.getModelMgr().addChildren(groupDbdFolder.getEntity().getId(), newSplitDbdIds, EntityConstants.ATTRIBUTE_ENTITY);
		    	}
		    
		    	ModelMgrUtils.refreshEntityAndChildren(groupAdFolder.getEntity());
		    	ModelMgrUtils.refreshEntityAndChildren(groupDbdFolder.getEntity());
			}
			
			@Override
			protected void hadSuccess() {
				SessionMgr.getBrowser().getEntityOutline().expandByUniqueId(splitLinesFolder.getUniqueId());
				final IconDemoPanel mainViewer = (IconDemoPanel)SessionMgr.getBrowser().getViewerForCategory(EntitySelectionModel.CATEGORY_MAIN_VIEW);
				mainViewer.loadEntity(groupAdFolder);
				final IconDemoPanel secViewer = (IconDemoPanel)SessionMgr.getBrowser().showSecViewer();
				secViewer.loadEntity(groupDbdFolder);
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};

		worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getSessionMgr().getActiveBrowser(), "Saving groups...", "", 0, 100));
		worker.execute();
    }
}
