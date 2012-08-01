package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import loci.plugins.config.SpringUtilities;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.Annotations;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityOutline;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
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

	private static final boolean DEBUG = false;
	
	private static final String SCORE_ONTOLOGY_NAME = "Expression Pattern Evaluation";
	private static final String MAA_INTENSITY_NAME = "MAA Intensity Score";
	private static final String MAA_DISTRIBUTION_NAME = "MAA Distribution Score";
	private static final String CA_INTENSITY_NAME = "CA Intensity Score";
	private static final String CA_DISTRIBUTION_NAME = "CA Distribution Score";
	
	private static final String TOOLTIP_AUTO_MOVE = "Automatically move samples to the correct evaluation folder after annotation";
	private static final String TOOLTIP_MOVE_NOW = "Move samples in the current folder to the correct folders";
	
	private JButton okButton;
	private JButton cancelButton;
	private JCheckBox autoMoveCheckbox;
	private JButton moveButton;
    	
	public ScreenEvaluationDialog() {

		JPanel attrPanel = new JPanel(new SpringLayout());
        attrPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        JLabel autoMoveLabel = new JLabel("Automatically move samples upon annotation?");
        autoMoveLabel.setToolTipText(TOOLTIP_AUTO_MOVE);
        autoMoveCheckbox = new JCheckBox();
        autoMoveCheckbox.setSelected(true);
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
        moveButton.setEnabled(false);
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
		
        okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            setVisible(false);
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
        
        addListeners();
	}
	
	public boolean isAccessible() {
		String username = SessionMgr.getUsername();
		if (!"rokickik".equals(username) && !"saffordt".equals(username) && !"jenetta".equals(username)) {
			return false;
		}
		return true;
	}
	
	private void addListeners() {
	
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
		
	}
	
	private void organizeEntity(final Long entityId) throws Exception {

		final EntityOutline entityOutline = SessionMgr.getBrowser().getEntityOutline();
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
			
			// The current evaluation
			int i = getValueFromAnnotation(intensity);
			int d = getValueFromAnnotation(distribution);
			
			// Current folders
			DefaultMutableTreeNode distNode = null;
			DefaultMutableTreeNode intNode = null;
			DefaultMutableTreeNode compartmentNode = null;
			
			Set<DefaultMutableTreeNode> nodes = entityOutline.getNodesByEntityId(entityId);
			if (nodes==null) {
				System.out.println("Entity not found in tree: "+entityId);
				return;
			}
			
			for(DefaultMutableTreeNode node : nodes) {
				DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
				Entity parentEntity = entityOutline.getEntity(parent);
				if (parentEntity!=null && parentEntity.getName().startsWith("Distribution ")) {
					DefaultMutableTreeNode grandparent = (DefaultMutableTreeNode)parent.getParent();	
					Entity grandparentEntity = entityOutline.getEntity(grandparent);
					if (grandparentEntity!=null && grandparentEntity.getName().startsWith("Intensity ")) {
						distNode = parent;
						intNode = grandparent;
						compartmentNode = (DefaultMutableTreeNode)grandparent.getParent();
						break;
					}
				}
			}
			
			if (distNode==null || intNode==null || compartmentNode==null) {
				throw new IllegalStateException("Sample not found in evaulation folder hierarchy");
			}

			Entity distEntity = entityOutline.getEntity(distNode);
			Entity intEntity = entityOutline.getEntity(intNode);
			Entity compartmentEntity = entityOutline.getEntity(compartmentNode);
			int ci = getValueFromFolderName(intEntity);
			int cd = getValueFromFolderName(distEntity);
			
			if (DEBUG) System.out.println("current compartment="+compartmentEntity.getName());
			if (DEBUG) System.out.println("current int="+intEntity.getName() +" ("+ci+" should be "+i+")");
			if (DEBUG) System.out.println("current dist="+distEntity.getName() +" ("+cd+" should be "+d+")");
			
			if (ci==i) {
				if (cd==d) {
					if (DEBUG) System.out.println("Correct folder placement");		
				}
				else {
					if (DEBUG) System.out.println("Distribution folder needs updating");
					Entity targetFolder = null;
					for(Entity distChild : intEntity.getChildren()) {
						if (distChild.getName().endsWith(""+d)) {
							targetFolder = distChild;
						}
					}
					if (targetFolder==null) {
						throw new IllegalStateException("Cannot find appropriate distribution folder");
					}
					moveToFolder(entityId,distEntity,targetFolder);
				}
			}
			else {
				if (DEBUG) System.out.println("Both folders need updating");
				if (!EntityUtils.areLoaded(compartmentEntity.getEntityData())) {
					ModelMgrUtils.loadLazyEntity(compartmentEntity, false);
				}
				Entity targetFolder = null;
				for(Entity intChild : compartmentEntity.getChildren()) {
					if (intChild.getName().endsWith(""+i)) {
						if (!EntityUtils.areLoaded(intChild.getEntityData())) {
							ModelMgrUtils.loadLazyEntity(intChild, false);
						}
						for(Entity distChild : intChild.getChildren()) {
							if (distChild.getName().endsWith(""+d)) {
								targetFolder = distChild;
							}
						}
					}
				}
				if (targetFolder==null) {
					throw new IllegalStateException("Cannot find appropriate intensity and distribution folders");
				}
				moveToFolder(entityId,distEntity,targetFolder);
			}
		}
	}
	
	private void moveToFolder(long entityId, Entity currFolder, Entity targetFolder) throws Exception {
		synchronized(currFolder) {
			ModelMgr modelMgr = ModelMgr.getModelMgr();
			// Add to new folder
			List<Long> childrenIds = new ArrayList<Long>();
			childrenIds.add(entityId);
			modelMgr.addChildren(targetFolder.getId(), childrenIds, EntityConstants.ATTRIBUTE_ENTITY);
			if (DEBUG) System.out.println("moved to folder "+targetFolder.getName()+" id="+targetFolder.getId());
			// Remove from old folder
			EntityData toDelete = null;
			for(EntityData ed : new HashSet<EntityData>(currFolder.getEntityData())) {
				if (ed.getChildEntity()!=null && ed.getChildEntity().getId()==entityId) {
					toDelete = ed;
				}
			}
			if (DEBUG) System.out.println("deleted from folder "+currFolder.getName()+" ed.id="+toDelete.getId());
			if (toDelete!=null) {
				modelMgr.removeEntityData(toDelete);
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
	
    public void showDialog() {
        packAndShow();
    }
}
