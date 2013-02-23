package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.AnnotationBuilderDialog;
import org.janelia.it.FlyWorkstation.gui.framework.actions.RemoveAnnotationKeyValueAction;
import org.janelia.it.FlyWorkstation.gui.framework.actions.RemoveAnnotationTermAction;
import org.janelia.it.FlyWorkstation.gui.framework.outline.OntologyOutline;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.utils.AnnotationSession;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * A tag cloud of Entity-based annotations which support context menu operations such as deletion.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationTagCloudPanel extends TagCloudPanel<OntologyAnnotation> implements AnnotationView {
	
	private static final Logger log = LoggerFactory.getLogger(AnnotationTagCloudPanel.class);
	
    private void deleteTag(final OntologyAnnotation tag) {
    	
        Utils.setWaitingCursor(SessionMgr.getSessionMgr().getActiveBrowser());

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
            	ModelMgr.getModelMgr().removeAnnotation(tag.getId());
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(SessionMgr.getSessionMgr().getActiveBrowser());
            }

            @Override
            protected void hadError(Throwable error) {
                error.printStackTrace();
                Utils.setDefaultCursor(SessionMgr.getSessionMgr().getActiveBrowser());
                JOptionPane.showMessageDialog(AnnotationTagCloudPanel.this, "Error deleting annotation", "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        worker.execute();
    }

    @Override
    protected void showPopupMenu(final MouseEvent e, final OntologyAnnotation tag) {
    	
    	Viewer viewer = SessionMgr.getBrowser().getViewerManager().getActiveViewer();
		List<String> selectionIds = ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(viewer.getSelectionCategory());
		List<RootedEntity> rootedEntityList = new ArrayList<RootedEntity>();
		for (String entityId : selectionIds) {
			rootedEntityList.add(viewer.getRootedEntityById(entityId));
		}
		
		
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);
        
        if (rootedEntityList.size()>1) {
            JMenuItem titleItem = new JMenuItem("(Multiple Selected)");
            titleItem.setEnabled(false);
            popupMenu.add(titleItem);
            
        	if (SessionMgr.getSubjectKey().equals(tag.getOwner())) {
        		final RemoveAnnotationTermAction termAction = new RemoveAnnotationTermAction(tag.getKeyEntityId(), tag.getKeyString());
                JMenuItem deleteByTermItem = new JMenuItem("  "+termAction.getName());
                deleteByTermItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        termAction.doAction();
                    }
                });
                popupMenu.add(deleteByTermItem);

                try {
                    String tmpOntKeyId = tag.getEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID);
                    Entity tmpOntologyTerm = ModelMgr.getModelMgr().getEntityById(tmpOntKeyId);
                    if (null!=tmpOntologyTerm) {
                        String tmpOntologyTermType = tmpOntologyTerm.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE);
                        if (EntityConstants.VALUE_ONTOLOGY_TERM_TYPE_ENUM.equals(tmpOntologyTermType) ||
                                EntityConstants.VALUE_ONTOLOGY_TERM_TYPE_ENUM_TEXT.equals(tmpOntologyTermType)||
                                EntityConstants.VALUE_ONTOLOGY_TERM_TYPE_INTERVAL.equals(tmpOntologyTermType)||
                                EntityConstants.VALUE_ONTOLOGY_TERM_TYPE_TEXT.equals(tmpOntologyTermType)) {
                            final RemoveAnnotationKeyValueAction valueAction = new RemoveAnnotationKeyValueAction(tag);
                            JMenuItem deleteByValueItem = new JMenuItem("  "+valueAction.getName());
                            deleteByValueItem.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent actionEvent) {
                                    valueAction.doAction();
                                }
                            });
                            popupMenu.add(deleteByValueItem);
                        }
                    }
                    else {
                        log.warn("Cannot create menu item because ontology term no longer exists.");
                    }
                }
                catch (Exception e1) {
                    SessionMgr.getSessionMgr().handleException(e1);
                }
            }
            
        }
        else {
            JMenuItem titleItem = new JMenuItem(tag.getEntity().getName());
            titleItem.setEnabled(false);
            popupMenu.add(titleItem);
            
        	if (SessionMgr.getSubjectKey().equals(tag.getOwner())) {
                JMenuItem deleteItem = new JMenuItem("  Delete Annotation");
                deleteItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        deleteTag(tag);
                    }
                });
                popupMenu.add(deleteItem);
        	}

            if (null!=tag.getValueString()){
                JMenuItem editItem = new JMenuItem("  Edit Annotation");
                editItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        AnnotationBuilderDialog dialog = new AnnotationBuilderDialog();
                        dialog.setAnnotationValue(tag.getValueString());
                        dialog.setVisible(true);
                        String value = dialog.getAnnotationValue();
                        if (null==value) { value=""; }
                        tag.setValueString(value);
                        tag.getEntity().setValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_TERM, value);
                        String tmpName = tag.getEntity().getName();
                        String namePrefix = tmpName.substring(0,tmpName.indexOf("=")+2);
                        tag.getEntity().setName(namePrefix+value);
                        try {
                            Entity tmpAnnotatedEntity = ModelMgr.getModelMgr().getEntityById(tag.getTargetEntityId());
                            ModelMgr.getModelMgr().saveOrUpdateAnnotation(tmpAnnotatedEntity, tag.getEntity());
                        }
                        catch (Exception e1) {
                            e1.printStackTrace();
                            SessionMgr.getSessionMgr().handleException(e1);
                        }
                    }
                });
                popupMenu.add(editItem);
            }

            JMenuItem copyItem = new JMenuItem("  Copy Annotation");
            copyItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    ModelMgr.getModelMgr().setCurrentSelectedOntologyAnnotation(tag);
                }
            });
            popupMenu.add(copyItem);

            JMenuItem detailsItem = new JMenuItem("  View Details");
            detailsItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                	OntologyOutline.viewAnnotationDetails(tag);
                }
            });
            popupMenu.add(detailsItem);
        }
        

        popupMenu.show(e.getComponent(), e.getX(), e.getY());
        e.consume();
    }

    
	@Override
	protected JLabel createTagLabel(OntologyAnnotation tag) {
		JLabel label = super.createTagLabel(tag);
		
		label.setBackground(ModelMgr.getModelMgr().getUserColorMapping().getColor(tag.getOwner()));
		
		AnnotationSession currentSession = ModelMgr.getModelMgr().getCurrentAnnotationSession();
		if (currentSession != null) {
			if (tag.getSessionId() != null && tag.getSessionId().equals(currentSession.getId())) {
				// This annotation is in the current session, so display it normally.	
			}
			else {
				// Dim the annotations from other sessions.
				Color dimFgColor = label.getBackground().darker();
			    Border paddingBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
			    Border lineBorder = BorderFactory.createLineBorder(dimFgColor, 1);
			    Border border = BorderFactory.createCompoundBorder(lineBorder, paddingBorder);
				label.setBorder(border);
				label.setForeground(dimFgColor);
			}
		}
		else {
			// We're not even in a session, just display everything normally.
		}
		
		return label;
	}

	@Override
    public List<OntologyAnnotation> getAnnotations() {
        return getTags();
    }

	@Override
    public void setAnnotations(List<OntologyAnnotation> annotations) {
        setTags(annotations);
    }

	@Override
    public void removeAnnotation(OntologyAnnotation annotation) {
        removeTag(annotation);
    }

	@Override
    public void addAnnotation(OntologyAnnotation annotation) {
        addTag(annotation);
    }
}
