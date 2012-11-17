package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityTreeCellRenderer;
import org.janelia.it.FlyWorkstation.gui.framework.outline.SelectionTreePanel;
import org.janelia.it.FlyWorkstation.gui.framework.outline.SplitPickingPanel;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.gui.util.FakeProgressWorker;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
 * A dialog that pops up from the SplitPickingPanel and allows the user to group Screen Samples in one folder into 
 * representatives for AD's and DBD's in another folder.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SplitGroupingDialog extends ModalDialog {

	private static final Logger log = LoggerFactory.getLogger(SplitGroupingDialog.class);
	
	private JButton okButton;
	private JButton cancelButton;
	private SelectionTreePanel<Entity> splitAdTreePanel;
    private SelectionTreePanel<Entity> splitDbdTreePanel;
	private final SplitPickingPanel splitPickingPanel;
	private List<Entity> represented;

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
			
			@Override
			protected void doStuff() throws Exception {
				RootedEntity repFolder = splitPickingPanel.getRepFolder();
		    	represented = getRepresentedFlylines(ModelMgr.getModelMgr().getEntityById(repFolder.getEntity().getId()+""));
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
				
				saveRepresentedGroupings(splitAdTreePanel.getItems(), splitDbdTreePanel.getItems(), splitLinesFolder);

				if (splitPickingPanel.getGroupAdFolder()==null) {
					splitPickingPanel.setGroupAdFolder(ModelMgrUtils.getChildFolder(splitLinesFolder, SplitPickingPanel.FOLDER_NAME_SPLIT_LINES_AD, false));
				}
				if (splitPickingPanel.getGroupDbdFolder()==null) {
					splitPickingPanel.setGroupDbdFolder(ModelMgrUtils.getChildFolder(splitLinesFolder, SplitPickingPanel.FOLDER_NAME_SPLIT_LINES_DBD, false));
				}
			}
			
			@Override
			protected void hadSuccess() {
				SessionMgr.getBrowser().getEntityOutline().expandByUniqueId(splitLinesFolder.getUniqueId());
				SessionMgr.getBrowser().getViewerManager().showEntityInMainViewer(groupAdFolder);
				SessionMgr.getBrowser().getViewerManager().showEntityInSecViewer(groupDbdFolder);
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};

		worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getSessionMgr().getActiveBrowser(), "Saving groups...", "", 0, 100));
		worker.execute();
    }

    public List<Entity> getRepresentedFlylines(Entity folder) throws Exception {

    	List<Entity> screenSamples = ModelMgrUtils.getDescendantsOfType(folder, EntityConstants.TYPE_SCREEN_SAMPLE, true);  
    	
    	List<Long> entityIds = new ArrayList<Long>();
    	for(Entity screenSample : screenSamples) {
    		Set<Long> parentIds = ModelMgr.getModelMgr().getParentIdsForAttribute(screenSample.getId(), EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE);
    		if (!parentIds.isEmpty()) {
    			entityIds.addAll(parentIds);
    		}
    	}
    	
    	return ModelMgr.getModelMgr().getEntityByIds(entityIds);
    }
    
    public void saveRepresentedGroupings(List<Entity> representedAd, List<Entity> representedDbd, RootedEntity folder) throws Exception {

    	RootedEntity groupAdFolder = ModelMgrUtils.getChildFolder(folder, SplitPickingPanel.FOLDER_NAME_SPLIT_LINES_AD, true);
    	RootedEntity groupDbdFolder = ModelMgrUtils.getChildFolder(folder, SplitPickingPanel.FOLDER_NAME_SPLIT_LINES_DBD, true);

    	groupAdFolder.getEntityData().setOrderIndex(0);
    	groupDbdFolder.getEntityData().setOrderIndex(1);
    	ModelMgr.getModelMgr().saveOrUpdateEntityData(groupAdFolder.getEntityData());
    	ModelMgr.getModelMgr().saveOrUpdateEntityData(groupDbdFolder.getEntityData());
    	ModelMgrUtils.fixOrderIndicies(folder.getEntity(),new Comparator<EntityData>() {
			@Override
			public int compare(EntityData o1, EntityData o2) {
				return ComparisonChain.start()
				.compare(o1.getOrderIndex(), o2.getOrderIndex(), Ordering.natural().nullsLast())
					.compare(o1.getId(), o2.getId()).result();
			}
		});
    	
    	log.info("Saving {} represented AD lines to {}",representedAd.size(),groupAdFolder.getId());
    	log.info("Saving {} represented DBD lines to {}",representedDbd.size(),groupDbdFolder.getId());
    	
    	List<Long> newSplitAdIds = EntityUtils.getEntityIdList(representedAd);
    	List<Long> newSplitDbdIds = EntityUtils.getEntityIdList(representedDbd);
    	
        // Remove current 
    	ModelMgrUtils.removeAllChildren(groupAdFolder.getEntity());
    	ModelMgrUtils.removeAllChildren(groupDbdFolder.getEntity());
        
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
}
