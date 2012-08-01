package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;

import loci.plugins.config.SpringUtilities;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.Annotations;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * A dialog that may do something in the future.
 * 
 * For now it serves as a listener for rearranging screen samples when Arnim annotates them with a new 
 * intensity or distribution.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ScreenEvaluationDialog extends ModalDialog {
	
	private static final String SCORE_ONTOLOGY_NAME = "Expression Pattern Evaluation";
	private static final String TOP_LEVEL_FOLDER_NAME = "FlyLight Pattern Evaluation";
	
	private static final String MAA_INTENSITY_NAME = "MAA Intensity Score";
	private static final String MAA_DISTRIBUTION_NAME = "MAA Distribution Score";
	private static final String CA_INTENSITY_NAME = "CA Intensity Score";
	private static final String CA_DISTRIBUTION_NAME = "CA Distribution Score";
	
	private static final String TOOLTIP_AUTO_MOVE = "Automatically move samples to the correct evaluation folder after annotation";
	private static final String TOOLTIP_MOVE_NOW = "Move samples in the current folder to the correct folders";
	
	private JButton cancelButton;
	private JCheckBox autoMoveCheckbox;
	private JButton moveButton;

	private Map<String,Entity> compEntityMap = new HashMap<String,Entity>();
	private Map<String,Entity> folderCache = new HashMap<String,Entity>();
	private Map<String,Map<Long,String>> reverseFolderCache = new HashMap<String,Map<Long,String>>();
	
	public ScreenEvaluationDialog() {

		if (!isAccessible()) return;

		setModalityType(ModalityType.MODELESS);
		
		JPanel attrPanel = new JPanel(new SpringLayout());
        attrPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        JLabel autoMoveLabel = new JLabel("Automatically move samples upon annotation?");
        autoMoveLabel.setToolTipText(TOOLTIP_AUTO_MOVE);
        autoMoveCheckbox = new JCheckBox();
        autoMoveCheckbox.setSelected(false);
        autoMoveCheckbox.setToolTipText(TOOLTIP_AUTO_MOVE);
        autoMoveCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				moveButton.setEnabled(!autoMoveCheckbox.isSelected());
			}
		});
        autoMoveLabel.setLabelFor(autoMoveCheckbox);
        attrPanel.add(autoMoveLabel);
        attrPanel.add(autoMoveCheckbox);
        
        JLabel moveNowLabel = new JLabel("Move samples in current folder now");
        moveNowLabel.setToolTipText(TOOLTIP_MOVE_NOW);
        moveButton = new JButton("Move");
        moveButton.setEnabled(true);
        moveButton.setToolTipText(TOOLTIP_MOVE_NOW);
        moveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				organizeEntitiesInCurrentFolder();
			}
		});
        moveNowLabel.setLabelFor(moveButton);
        attrPanel.add(moveNowLabel);
        attrPanel.add(moveButton);

        add(attrPanel, BorderLayout.CENTER);
        SpringUtilities.makeCompactGrid(attrPanel, attrPanel.getComponentCount()/2, 2, 6, 6, 6, 6);
		
        cancelButton = new JButton("Close");
        cancelButton.setToolTipText("Close this dialog");
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
        buttonPane.add(cancelButton);
        add(buttonPane, BorderLayout.SOUTH);
        
        init();
        addListeners();
	}

	public void init() {

		SimpleWorker worker = new SimpleWorker() {
			
			@Override
			protected void doStuff() throws Exception {
				
				Entity topLevelFolder = null;
				for(Entity entity : ModelMgr.getModelMgr().getEntitiesByName(TOP_LEVEL_FOLDER_NAME)) {
					if (ModelMgrUtils.isOwner(entity)) {
						topLevelFolder = entity;
					}
				}
				
				if (topLevelFolder==null) {
					throw new IllegalStateException("Cannot find top-level folder named "+TOP_LEVEL_FOLDER_NAME);
				}
				
				ModelMgrUtils.loadLazyEntity(topLevelFolder, false);
				for(Entity child : topLevelFolder.getOrderedChildren()) {
					compEntityMap.put(child.getName(), child);
				}
			}
			
			@Override
			protected void hadSuccess() {
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};
		
		worker.execute();
	}
	
	public boolean isAccessible() {
		String username = SessionMgr.getUsername();
		if (!"rokickik".equals(username) && !"saffordt".equals(username) && !"jenetta".equals(username)) {
			return false;
		}
		return true;
	}
	
	private void addListeners() {
	
		// TODO: move to after init
//		KeyStroke accel = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, SystemInfo.isMac?java.awt.Event.META_MASK:java.awt.Event.CTRL_MASK);
//    	SessionMgr.getBrowser().getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(accel,"moveAction");
//    	SessionMgr.getBrowser().getRootPane().getActionMap().put("moveAction",new AbstractAction("moveAction") {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				organizeEntitiesInCurrentFolder();
//			}
//		});
		
		ModelMgr.getModelMgr().addModelMgrObserver(new ModelMgrAdapter() {
			@Override
			public void annotationsChanged(final long entityId) {
				
				if (!autoMoveCheckbox.isSelected()) {
					return;
				}
				
				if (!ModelMgr.getModelMgr().getCurrentOntology().getName().equals(SCORE_ONTOLOGY_NAME)) {
					return;
				}
				
				SimpleWorker worker = new SimpleWorker() {
					
					@Override
					protected void doStuff() throws Exception {
						organizeEntity(entityId);
					}
					
					@Override
					protected void hadSuccess() {
					}
					
					@Override
					protected void hadError(Throwable error) {
						SessionMgr.getSessionMgr().handleException(error);
					}
				};
				
				worker.execute();
			}
		});
	}
	
	private void organizeEntitiesInCurrentFolder() {
		
		final List<Long> entityIds = new ArrayList<Long>();
		for(RootedEntity rootedEntity : SessionMgr.getBrowser().getActiveViewer().getRootedEntities()) {
			entityIds.add(rootedEntity.getEntityId());
		}
		
		SimpleWorker worker = new SimpleWorker() {
			
			@Override
			protected void doStuff() throws Exception {
				int total = entityIds.size()+1;
				int curr = 1;
				setProgress(curr, total);
				for(Long entityId : entityIds) {
					organizeEntity(entityId);
					setProgress(++curr, total);
				}
			}
			
			@Override
			protected void hadSuccess() {
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};

		worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getBrowser(), "Organizing...", "", 0, 100));
		worker.execute();
	}
	
	
	private void organizeEntity(final Long entityId) throws Exception {
		
		System.out.println("ScreenEvaluationDialog.organizeEntity: "+entityId);
		
		// TODO: should have a centralized way to do this
		Annotations annotations = new Annotations();
		annotations.reload(entityId);
		
		String intensity = null;
		String distribution = null;
		for(OntologyAnnotation annotation : annotations.getAnnotations()) {
			if (intensity==null && MAA_INTENSITY_NAME.equals(annotation.getKeyString())) {
				intensity = annotation.getValueString();
			}
			else if (CA_INTENSITY_NAME.equals(annotation.getKeyString())) {
				intensity = annotation.getValueString();
			}
			if (distribution==null && MAA_DISTRIBUTION_NAME.equals(annotation.getKeyString())) {
				distribution = annotation.getValueString();
			}
			else if (CA_DISTRIBUTION_NAME.equals(annotation.getKeyString())) {
				distribution = annotation.getValueString();
			}
		}
		
		if (intensity!=null && distribution!=null) {

			// The latest evaluation
			int i = getValueFromAnnotation(intensity);
			int d = getValueFromAnnotation(distribution);
			
			// The entity 
			Entity entity = ModelMgr.getModelMgr().getEntityById(entityId+"");
			String compartment = entity.getName();
			
			List<EntityData> parentEds = ModelMgr.getModelMgr().getParentEntityDatas(entityId);
			Map<Long,String> distCache = getDistCache(compartment);
			
			Integer ci = null;
			Integer cd = null;
			List<EntityData> currEds = new ArrayList<EntityData>();
			for(EntityData parentEd : parentEds) {
				Long parentId = parentEd.getParentEntity().getId();
				String key = distCache.get(parentId);
				if (key==null) {
					continue;
				}
				currEds.add(parentEd);
				if (ci==null || ci!=i) {
					ci = getIntensityValueFromKey(key);
				}
				if (cd==null || cd!=d) {
					cd = getDistributionValueFromKey(key);
				}
			}
			
			if (ci==null || cd==null) return;
			
			String oldKey = getKey(compartment, ci, cd);
			String newKey = getKey(compartment, i, d);
			System.out.println("  Old: "+oldKey);
			System.out.println("  New: "+newKey);

			if (!oldKey.equals(newKey) || currEds.size()>1) {
				Entity distNew = folderCache.get(newKey);
				moveToFolder(entityId,currEds,distNew);
			}
		}
	}

	private Map<Long,String> getDistCache(String compartment) throws Exception {
		
		Map<Long,String> distCache = reverseFolderCache.get(compartment);
		
		if (distCache==null) {
			System.out.println("  Caching folders for "+compartment);
			distCache = new HashMap<Long,String>();

			Entity compartmentEntity = compEntityMap.get(compartment);
			
			if (!EntityUtils.areLoaded(compartmentEntity.getEntityData())) {
				ModelMgrUtils.loadLazyEntity(compartmentEntity, false);	
			}
			
			for(Entity intChild : compartmentEntity.getChildren()) {
				int i = getValueFromFolderName(intChild);
				
				if (!EntityUtils.areLoaded(intChild.getEntityData())) {
					ModelMgrUtils.loadLazyEntity(intChild, false);
				}
				
				for(Entity distChild : intChild.getChildren()) {
					int d = getValueFromFolderName(distChild);
					String key = getKey(compartment, i, d);
					distCache.put(distChild.getId(), key);
					System.out.println("    "+key+" = "+distChild.getId());
					folderCache.put(key, distChild);
				}
			}
			
			reverseFolderCache.put(compartment, distCache);
		}
		
		return distCache;
	}
	
	private void moveToFolder(long entityId, List<EntityData> currEds, Entity targetFolder) throws Exception {
		ModelMgr modelMgr = ModelMgr.getModelMgr();
		// Add to new folder
		List<Long> childrenIds = new ArrayList<Long>();
		childrenIds.add(entityId);
		modelMgr.addChildren(targetFolder.getId(), childrenIds, EntityConstants.ATTRIBUTE_ENTITY);
		System.out.println("added to folder "+targetFolder.getName()+" id="+targetFolder.getId());
		// Remove from old folders
		if (currEds!=null) {
			for(EntityData currEd : currEds) {
			System.out.println("deleted from folder "+currEd.getParentEntity().getName()+", ed.id="+currEd.getId());
			modelMgr.removeEntityData(currEd);
			}
		}
	}
	
	private int getValueFromFolderName(Entity entity) {
		return getValueFromFolderName(entity.getName());
	}
	
	private int getValueFromFolderName(String folderName) {
		return Integer.parseInt(""+folderName.charAt(folderName.length()-1));
	}
	
	private int getValueFromAnnotation(String annotationValue) {
		return Integer.parseInt(""+annotationValue.charAt(1));
	}
	
	public String getKey(Entity compartmentEntity, Entity intEntity, Entity distEntity) {
		int i = getValueFromFolderName(intEntity);
		int d = getValueFromFolderName(distEntity);
		return getKey(compartmentEntity.getName(),i,d);
	}
	
	private String getKey(String compartment, int i, int d) {
		return compartment+"/"+i+"/"+d;
	}


	private Integer getIntensityValueFromKey(String key) {
		try {
			String[] parts = key.split("/");
			return Integer.parseInt(parts[1]);	
		}
		catch (Exception e) {
			return null;
		}
	}

	private Integer getDistributionValueFromKey(String key) {
		try {
			String[] parts = key.split("/");
			return Integer.parseInt(parts[2]);	
		}
		catch (Exception e) {
			return null;
		}
	}
	
    public void showDialog() {
        packAndShow();
    }
}
