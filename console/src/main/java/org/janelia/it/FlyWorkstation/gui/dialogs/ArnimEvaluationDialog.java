package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.Annotations;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.OntologyElement;

/**
 * A dialog that may do something in the future.
 * 
 * For now it serves as a listener for rearranging screen samples when Arnim annotates them with a new 
 * intensity or distribution.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ArnimEvaluationDialog extends ModalDialog {

	private static final String SCORE_ONTOLOGY_NAME = "Expression Pattern Evaluation";
	private static final String MAA_INTENSITY_NAME = "MAA Intensity Score";
	private static final String MAA_DISTRIBUTION_NAME = "MAA Distribution Score";
	private static final String CA_INTENSITY_NAME = "CA Intensity Score";
	private static final String CA_DISTRIBUTION_NAME = "CA Distribution Score";
	
	private JButton okButton;
	private JButton cancelButton;
    
	
	public ArnimEvaluationDialog() {

		addListeners();
		
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
	}
	
	private void init() {

//		
//		for(OntologyElement element : ModelMgr.getModelMgr().getCurrentOntology().getChildren()) {
//			if (SCORE_INTENSITY_NAME.equals(element.getName())) {
//				
//			}
//		}
		
	}
	
	private void addListeners() {
	
		String username = SessionMgr.getUsername();
		if (!"rokickik".equals(username) && !"saffordt".equals(username) && !"jenetta".equals(username)) {
			return;
		}
		
		ModelMgr.getModelMgr().addModelMgrObserver(new ModelMgrAdapter() {
			@Override
			public void annotationsChanged(final long entityId) {
				
				if (!ModelMgr.getModelMgr().getCurrentOntology().getName().equals(SCORE_ONTOLOGY_NAME)) {
					return;
				}
				
				SimpleWorker worker = new SimpleWorker() {
					
					@Override
					protected void doStuff() throws Exception {
						
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
							
							int i = Integer.parseInt(""+intensity.charAt(1));
							int d = Integer.parseInt(""+distribution.charAt(1));
							
							Entity distFolder = null;
							for(Entity parent : ModelMgr.getModelMgr().getParentEntities(entityId)) {
								if (parent.getName().startsWith("Distribution")) {
									distFolder = parent;
									break;
								}
							}
							
							if (distFolder == null) return;

							Entity intFolder = null;
							for(Entity parent : ModelMgr.getModelMgr().getParentEntities(entityId)) {
								if (parent.getName().startsWith("Intensity")) {
									intFolder = parent;
									break;
								}
							}
							
							if (intFolder == null) return;
							
							String currDistFolderName = intFolder.getName();
							int di = Integer.parseInt(currDistFolderName.substring(currDistFolderName.indexOf(' ')+1));
							
							
							
							
							
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
		});
	}
	
    public void showDialog() {
        packAndShow();
    }
    
}
