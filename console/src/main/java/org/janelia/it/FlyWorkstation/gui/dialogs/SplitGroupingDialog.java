package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
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

/**
 * A dialog that pops up from the SplitPickingPanel and allows the user to group Screen Samples in one folder into 
 * representatives for AD's and DBD's in another folder.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SplitGroupingDialog extends ModalDialog {

	private final SplitPickingPanel splitPickingPanel;

	public SplitGroupingDialog(final SplitPickingPanel splitPickingPanel) {

		this.splitPickingPanel = splitPickingPanel;

		JPanel mainPane = new JPanel();
		mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.PAGE_AXIS));
		
		mainPane.add(new JLabel("Currently this just puts half the samples in each category."));
		add(mainPane, BorderLayout.CENTER);
		
        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            setVisible(false);
	            
				SimpleWorker worker = new FakeProgressWorker() {
					private RootedEntity splitLinesFolder;
					private RootedEntity groupAdFolder;
					private RootedEntity groupDbdFolder;
					
					@Override
					protected void doStuff() throws Exception {
				
						RootedEntity repFolder = splitPickingPanel.getRepFolder();

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
				    	
				    	Entity repEntity = ModelMgr.getModelMgr().getEntityById(repFolder.getEntity().getId()+"");
				    	List<Entity> screenSamples = ModelMgrUtils.getDescendantsOfType(repEntity, EntityConstants.TYPE_SCREEN_SAMPLE, true);  
				    	
						int max = screenSamples.size()+1;
				    	setProgress(1, max);
				    	
				    	Set<Long> splitAdIds = new HashSet<Long>();
				    	for(EntityData ed : groupAdFolder.getEntity().getEntityData()) {
				    		if (ed.getChildEntity()!=null) {
				    			splitAdIds.add(ed.getChildEntity().getId());
				    		}
				    	}
				    	
				    	Set<Long> splitDbdIds = new HashSet<Long>();
				    	for(EntityData ed : groupDbdFolder.getEntity().getEntityData()) {
				    		if (ed.getChildEntity()!=null) {
				    			splitDbdIds.add(ed.getChildEntity().getId());
				    		}
				    	}

				    	List<Long> newSplitAdIds = new ArrayList<Long>();
				    	List<Long> newSplitDbdIds = new ArrayList<Long>();
				    	
				    	int i = 1;
				    	for(Entity screenSample : screenSamples) {
				    		Long sid = screenSample.getId();
				    		if (i++ > screenSamples.size()/2) {
				    			if (!splitAdIds.contains(sid)) {
				    				splitAdIds.add(sid);
				    				newSplitAdIds.add(sid);
				    			}
				    		}
				    		else {
				    			if (!splitDbdIds.contains(sid)) {
				    				splitDbdIds.add(sid);
				    				newSplitDbdIds.add(sid);
				    			}
				    		}
				    	}
				    	
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
		});

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(new ActionListener() {
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
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(cancelButton);
        add(buttonPane, BorderLayout.SOUTH);
	}

    public void showDialog() {

    	// TODO: prepopulate a grouping
    	
    	
        packAndShow();
    }
    
}
