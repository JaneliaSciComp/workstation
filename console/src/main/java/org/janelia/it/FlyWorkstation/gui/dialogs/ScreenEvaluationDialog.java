package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.concurrent.Callable;

import javax.swing.*;

import loci.plugins.config.SpringUtilities;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.access.Accessibility;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.FlyWorkstation.gui.framework.outline.Annotations;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.gui.util.SystemInfo;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.shared.screen.ScreenEvalConstants;
import org.janelia.it.jacs.shared.screen.ScreenEvalUtils;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog that may do something in the future.
 * 
 * For now it serves as a listener for rearranging screen samples when Arnim annotates them with a new 
 * intensity or distribution.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ScreenEvaluationDialog extends ModalDialog implements Accessibility {
	
	private static final Logger log = LoggerFactory.getLogger(ScreenEvaluationDialog.class);
	
	private static final String SCREEN_EVAL_ORGANIZATION_PROPERTY = "ScreenEvaluationDialog.OrganizationBehavior";
	
	private JRadioButton askAfterNavigationRadioButton;
	private JRadioButton autoMoveAfterAnnotationRadioButton;
	private JRadioButton autoMoveAfterNavigationRadioButton;
	private JButton moveChangedButton;
	private JButton moveAllButton;

	private Map<String,Entity> compEntityMap = new HashMap<String,Entity>();
	private Map<String,Entity> folderCache = new HashMap<String,Entity>();
	private Map<String,Map<Long,String>> reverseFolderCache = new HashMap<String,Map<Long,String>>();
	
	private KeyStroke moveChangedKeystroke = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, SystemInfo.isMac?java.awt.Event.META_MASK:java.awt.Event.CTRL_MASK);
	private KeyStroke moveAllKeystroke = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, (SystemInfo.isMac?java.awt.Event.META_MASK:java.awt.Event.CTRL_MASK)|java.awt.Event.SHIFT_MASK);
	
	private Browser browser;
	
	private boolean isCurrFolderDirty = false;
	private Set<Long> dirtyEntities = new HashSet<Long>();
	
	public ScreenEvaluationDialog(Browser browser) {

		this.browser = browser;
		
		if (!isAccessible()) return;

		setModalityType(ModalityType.MODELESS);
		
		Integer behavior = (Integer)SessionMgr.getSessionMgr().getModelProperty(SCREEN_EVAL_ORGANIZATION_PROPERTY);
		if (behavior==null) {
			behavior = 2;
			setBehaviorPreference(behavior);
		}
		
		JPanel attrPanel = new JPanel(new SpringLayout());
        attrPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        JLabel dirtyBitLabel = new JLabel("Alert when leaving folder");
        askAfterNavigationRadioButton = new JRadioButton();
        askAfterNavigationRadioButton.setSelected(behavior==0);
        askAfterNavigationRadioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setBehaviorPreference(0);
			}
		});
        dirtyBitLabel.setLabelFor(askAfterNavigationRadioButton);
        attrPanel.add(dirtyBitLabel);
        attrPanel.add(askAfterNavigationRadioButton);
        
        JLabel autoMoveLabel = new JLabel("Automatically move samples upon annotation");
        autoMoveAfterAnnotationRadioButton = new JRadioButton();
        autoMoveAfterAnnotationRadioButton.setSelected(behavior==1);
        autoMoveAfterAnnotationRadioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setBehaviorPreference(1);
			}
		});
        autoMoveLabel.setLabelFor(autoMoveAfterAnnotationRadioButton);
        attrPanel.add(autoMoveLabel);
        attrPanel.add(autoMoveAfterAnnotationRadioButton);

        JLabel autoMoveNavLabel = new JLabel("Automatically move samples upon navigation");
        autoMoveAfterNavigationRadioButton = new JRadioButton();
        autoMoveAfterNavigationRadioButton.setSelected(behavior==2);
        autoMoveAfterNavigationRadioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setBehaviorPreference(2);
			}
		});
        autoMoveNavLabel.setLabelFor(autoMoveAfterNavigationRadioButton);
        attrPanel.add(autoMoveNavLabel);
        attrPanel.add(autoMoveAfterNavigationRadioButton);
        
        
        ButtonGroup group = new ButtonGroup();
        group.add(askAfterNavigationRadioButton);
        group.add(autoMoveAfterAnnotationRadioButton);
        group.add(autoMoveAfterNavigationRadioButton);
        
        
        JLabel moveChangedLabel = new JLabel("Reorganize changed samples in current folder now");
        moveChangedButton = new JButton("Move ("+KeymapUtil.getKeystrokeText(moveChangedKeystroke)+")");
        moveChangedButton.setEnabled(true);
        moveChangedButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				organizeEntitiesInCurrentFolder(true);
			}
		});
        moveChangedLabel.setLabelFor(moveChangedButton);
        attrPanel.add(moveChangedLabel);
        attrPanel.add(moveChangedButton);

        JLabel moveAllLabel = new JLabel("Reorganize all samples in current folder now");
        moveAllButton = new JButton("Move ("+KeymapUtil.getKeystrokeText(moveAllKeystroke)+")");
        moveAllButton.setEnabled(true);
        moveAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				organizeEntitiesInCurrentFolder(false);
			}
		});
        moveAllLabel.setLabelFor(moveAllButton);
        attrPanel.add(moveAllLabel);
        attrPanel.add(moveAllButton);

        
        add(attrPanel, BorderLayout.CENTER);
        SpringUtilities.makeCompactGrid(attrPanel, attrPanel.getComponentCount()/2, 2, 6, 6, 6, 6);
		        
        init();
        addListeners();
	}

	public void setBehaviorPreference(int behavior) {
		SessionMgr.getSessionMgr().setModelProperty(SCREEN_EVAL_ORGANIZATION_PROPERTY, behavior);
	}
	
	public void init() {

		SimpleWorker worker = new SimpleWorker() {
			
			@Override
			protected void doStuff() throws Exception {
				
				Entity topLevelFolder = null;
				for(Entity entity : ModelMgr.getModelMgr().getEntitiesByName(ScreenEvalConstants.TOP_LEVEL_FOLDER_NAME)) {
					if (ModelMgrUtils.isOwner(entity)) {
						topLevelFolder = entity;
					}
				}
				
				if (topLevelFolder==null) {
					return;
				}
				
				topLevelFolder = ModelMgr.getModelMgr().loadLazyEntity(topLevelFolder, false);
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
	
	private void addListeners() {
	
    	InputMap inputMap = browser.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    	inputMap.put(moveChangedKeystroke,"moveChangedAction");
    	inputMap.put(moveAllKeystroke,"moveAllAction");
    	
    	ActionMap actionMap = browser.getRootPane().getActionMap();
    	actionMap.put("moveChangedAction",new AbstractAction("moveChangedAction") {
			@Override
			public void actionPerformed(ActionEvent e) {
				organizeEntitiesInCurrentFolder(true);
			}
		});
    	actionMap.put("moveAllAction",new AbstractAction("moveAllAction") {
			@Override
			public void actionPerformed(ActionEvent e) {
				organizeEntitiesInCurrentFolder(false);
			}
		});
		
		ModelMgr.getModelMgr().addModelMgrObserver(new ModelMgrAdapter() {
			@Override
			public void annotationsChanged(final long entityId) {
				
				if (!ModelMgr.getModelMgr().getCurrentOntology().getName().equals(ScreenEvalConstants.SCORE_ONTOLOGY_NAME)) {
					return;
				}
				
				if (autoMoveAfterAnnotationRadioButton.isSelected()) {
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
				else {
					isCurrFolderDirty = true;
					dirtyEntities.add(entityId);
				}
			}
		});
	}
	
	public void organizeEntitiesInCurrentFolder(boolean onlyChanged) {
		organizeEntitiesInCurrentFolder(onlyChanged, null);
	}
	
	public void organizeEntitiesInCurrentFolder(boolean onlyChanged, final Callable<Void> success) {
		
		final List<Long> entityIds = new ArrayList<Long>();
		if (onlyChanged) {
			entityIds.addAll(dirtyEntities);
		}
		else {
			for(RootedEntity rootedEntity : SessionMgr.getBrowser().getViewerManager().getActiveViewer().getRootedEntities()) {
				entityIds.add(rootedEntity.getEntityId());
			}
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
				setCurrFolderDirty(false);
				if (success!=null) {
					try {
						success.call();
					}
					catch (Exception e) {
						hadError(e);
					}
				}
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
		
		log.info("ScreenEvaluationDialog.organizeEntity: "+entityId);
		
		// TODO: should have a centralized way to do this
		Annotations annotations = new Annotations();
		annotations.reload(entityId);
		
		String intensity = null;
		String distribution = null;
		for(OntologyAnnotation annotation : annotations.getAnnotations()) {
			if (intensity==null && ScreenEvalConstants.MAA_INTENSITY_NAME.equals(annotation.getKeyString())) {
				intensity = annotation.getValueString();
			}
			else if (ScreenEvalConstants.CA_INTENSITY_NAME.equals(annotation.getKeyString())) {
				intensity = annotation.getValueString();
			}
			if (distribution==null && ScreenEvalConstants.MAA_DISTRIBUTION_NAME.equals(annotation.getKeyString())) {
				distribution = annotation.getValueString();
			}
			else if (ScreenEvalConstants.CA_DISTRIBUTION_NAME.equals(annotation.getKeyString())) {
				distribution = annotation.getValueString();
			}
		}
		
		if (intensity!=null && distribution!=null) {

			// The latest evaluation
			int i = ScreenEvalUtils.getValueFromAnnotation(intensity);
			int d = ScreenEvalUtils.getValueFromAnnotation(distribution);
			
			// The entity 
			Entity entity = ModelMgr.getModelMgr().getEntityById(entityId);
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
					ci = ScreenEvalUtils.getIntensityValueFromKey(key);
				}
				if (cd==null || cd!=d) {
					cd = ScreenEvalUtils.getDistributionValueFromKey(key);
				}
			}
			
			if (ci==null || cd==null) return;
			
			String oldKey = ScreenEvalUtils.getKey(compartment, ci, cd);
			String newKey = ScreenEvalUtils.getKey(compartment, i, d);
			log.info("  Old: "+oldKey);
			log.info("  New: "+newKey);

			if (!oldKey.equals(newKey) || currEds.size()>1) {
				Entity distNew = folderCache.get(newKey);
				moveToFolder(entityId,currEds,distNew);
			}
		}
	}

	private Map<Long,String> getDistCache(String compartment) throws Exception {
		
		Map<Long,String> distCache = reverseFolderCache.get(compartment);
		
		if (distCache==null) {
			log.info("  Caching folders for "+compartment);
			distCache = new HashMap<Long,String>();

			Entity compartmentEntity = compEntityMap.get(compartment);
			
			if (!EntityUtils.areLoaded(compartmentEntity.getEntityData())) {
				compartmentEntity = ModelMgr.getModelMgr().loadLazyEntity(compartmentEntity, false);	
			}
			
			for(Entity intChild : compartmentEntity.getChildren()) {
				int i = ScreenEvalUtils.getValueFromFolderName(intChild);
				
				if (!EntityUtils.areLoaded(intChild.getEntityData())) {
					intChild = ModelMgr.getModelMgr().loadLazyEntity(intChild, false);
				}
				
				for(Entity distChild : intChild.getChildren()) {
					int d = ScreenEvalUtils.getValueFromFolderName(distChild);
					String key = ScreenEvalUtils.getKey(compartment, i, d);
					distCache.put(distChild.getId(), key);
					log.info("    "+key+" = "+distChild.getId());
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
		log.info("added to folder "+targetFolder.getName()+" id="+targetFolder.getId());
		// Remove from old folders
		if (currEds!=null) {
			for(EntityData currEd : currEds) {
				log.info("deleted from folder "+currEd.getParentEntity().getName()+", ed.id="+currEd.getId());
				modelMgr.removeEntityData(currEd);
			}
		}
	}
	
    public void showDialog() {
        packAndShow();
    }

	public boolean isCurrFolderDirty() {
		return isCurrFolderDirty;
	}

	public void setCurrFolderDirty(boolean isCurrFolderDirty) {
		this.isCurrFolderDirty = isCurrFolderDirty;
		if (!isCurrFolderDirty) {
			dirtyEntities.clear();
		}
	}
	
	public boolean isAskAfterNavigation() {
		return askAfterNavigationRadioButton.isSelected();
	}
	
	public boolean isAutoMoveAfterNavigation() {
		return autoMoveAfterNavigationRadioButton.isSelected();
	}
	
	public boolean isAccessible() {
		return "user:jenetta".equals(SessionMgr.getSubjectKey());
	}
}
