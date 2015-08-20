package org.janelia.it.workstation.gui.browser.gui.support;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.List;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.framework.viewer.TagCloudPanel;

/**
 * A tag cloud of Entity-based annotations which support context menu operations such as deletion.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationTagCloudPanel extends TagCloudPanel<Annotation> implements AnnotationView {

    private static final Logger log = LoggerFactory.getLogger(AnnotationTagCloudPanel.class);

    private void deleteTag(final Annotation tag) {

        Utils.setWaitingCursor(SessionMgr.getMainFrame());

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                DomainMgr.getDomainMgr().getModel().remove(tag);
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(SessionMgr.getMainFrame());
            }

            @Override
            protected void hadError(Throwable error) {
                log.error("Error deleting annotation", error);
                Utils.setDefaultCursor(SessionMgr.getMainFrame());
                JOptionPane.showMessageDialog(AnnotationTagCloudPanel.this, "Error deleting annotation", "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        worker.execute();
    }

    @Override
    protected void showPopupMenu(final MouseEvent e, final Annotation tag) {

//        List<String> selectionIds = ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(viewer.getSelectionCategory());
//        List<RootedEntity> rootedEntityList = new ArrayList<>();
//        for (String entityId : selectionIds) {
//            rootedEntityList.add(viewer.getRootedEntityById(entityId));
//        }

        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);

        // TODO: domain object selection
        
//        if (rootedEntityList.size()>1) {
//            JMenuItem titleItem = new JMenuItem("(Multiple Selected)");
//            titleItem.setEnabled(false);
//            popupMenu.add(titleItem);
//
//            if (SessionMgr.getSubjectKey().equals(tag.getOwner())) {
//                final RemoveAnnotationTermAction termAction = new RemoveAnnotationTermAction(tag.getKeyEntityId(), tag.getKeyString());
//                JMenuItem deleteByTermItem = new JMenuItem(termAction.getName());
//                deleteByTermItem.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent actionEvent) {
//                        termAction.doAction();
//                    }
//                });
//                popupMenu.add(deleteByTermItem);
//
//                try {
//                    String tmpOntKeyId = tag.getEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID);
//                    Entity tmpOntologyTerm = ModelMgr.getModelMgr().getEntityById(tmpOntKeyId);
//                    if (null!=tmpOntologyTerm) {
//                        String tmpOntologyTermType = tmpOntologyTerm.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE);
//                        if (EntityConstants.VALUE_ONTOLOGY_TERM_TYPE_ENUM.equals(tmpOntologyTermType)
//                                ||EntityConstants.VALUE_ONTOLOGY_TERM_TYPE_ENUM_TEXT.equals(tmpOntologyTermType)
//                                ||EntityConstants.VALUE_ONTOLOGY_TERM_TYPE_INTERVAL.equals(tmpOntologyTermType)
//                                ||EntityConstants.VALUE_ONTOLOGY_TERM_TYPE_TEXT.equals(tmpOntologyTermType)) {
//                            final BulkEditAnnotationKeyValueAction bulkEditAction = new BulkEditAnnotationKeyValueAction(tag);
//                            JMenuItem editByValueItem = new JMenuItem("  "+bulkEditAction.getName());
//                            editByValueItem.addActionListener(new ActionListener() {
//                                public void actionPerformed(ActionEvent actionEvent) {
//                                    bulkEditAction.doAction();
//                                }
//                            });
//                            popupMenu.add(editByValueItem);
//                            final RemoveAnnotationKeyValueAction valueAction = new RemoveAnnotationKeyValueAction(tag);
//                            JMenuItem deleteByValueItem = new JMenuItem("  "+valueAction.getName());
//                            deleteByValueItem.addActionListener(new ActionListener() {
//                                public void actionPerformed(ActionEvent actionEvent) {
//                                    valueAction.doAction();
//                                }
//                            });
//                            popupMenu.add(deleteByValueItem);
//                        }
//                    }
//                    else {
//                        log.warn("Cannot create menu item because ontology term no longer exists.");
//                    }
//                }
//                catch (Exception e1) {
//                    SessionMgr.getSessionMgr().handleException(e1);
//                }
//            }
//
//        }
//        else {
//            JMenuItem titleItem = new JMenuItem(tag.getEntity().getName());
//            titleItem.setEnabled(false);
//            popupMenu.add(titleItem);
//
//            if (EntityUtils.hasWriteAccess(tag.getEntity(), SessionMgr.getSubjectKeys())) {
//                JMenuItem deleteItem = new JMenuItem("  Delete Annotation");
//                deleteItem.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent actionEvent) {
//                        deleteTag(tag);
//                    }
//                });
//                popupMenu.add(deleteItem);
//            }
//
//            if (null!=tag.getValueString()) {
//                JMenuItem editItem = new JMenuItem("  Edit Annotation");
//                editItem.addActionListener(new ActionListener() {
//                    @Override
//                    public void actionPerformed(ActionEvent e) {
//                        AnnotationBuilderDialog dialog = new AnnotationBuilderDialog();
//                        dialog.setAnnotationValue(tag.getValueString());
//                        dialog.setVisible(true);
//                        String value = dialog.getAnnotationValue();
//                        if (null==value) {
//                            value = "";
//                        }
//                        tag.setValueString(value);
//                        tag.getEntity().setValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_TERM, value);
//                        String tmpName = tag.getEntity().getName();
//                        String namePrefix = tmpName.substring(0, tmpName.indexOf("=")+2);
//                        tag.getEntity().setName(namePrefix+value);
//                        try {
//                            Entity tmpAnnotatedEntity = ModelMgr.getModelMgr().getEntityById(tag.getTargetEntityId());
//                            ModelMgr.getModelMgr().saveOrUpdateAnnotation(tmpAnnotatedEntity, tag.getEntity());
//                        }
//                        catch (Exception e1) {
//                            log.error("Error editing annotation", e1);
//                            SessionMgr.getSessionMgr().handleException(e1);
//                        }
//                    }
//                });
//                popupMenu.add(editItem);
//            }
//
//            JMenuItem copyItem = new JMenuItem("  Copy Annotation");
//            copyItem.addActionListener(new ActionListener() {
//                @Override
//                public void actionPerformed(ActionEvent actionEvent) {
//                    ModelMgr.getModelMgr().setCurrentSelectedOntologyAnnotation(tag);
//                }
//            });
//            popupMenu.add(copyItem);
//
//            JMenuItem detailsItem = new JMenuItem("  View Details");
//            detailsItem.addActionListener(new ActionListener() {
//                public void actionPerformed(ActionEvent actionEvent) {
//                    OntologyOutline.viewAnnotationDetails(tag);
//                }
//            });
//            popupMenu.add(detailsItem);
//        }

//        if (tag.isComputation()) {
//            final AcceptComputationalAnnotationsAction acceptAction = new AcceptComputationalAnnotationsAction();
//            JMenuItem acceptItem = new JMenuItem(acceptAction.getName());
//            acceptItem.addActionListener(new ActionListener() {
//                public void actionPerformed(ActionEvent actionEvent) {
//                    acceptAction.doAction();
//                }
//            });
//            popupMenu.add(acceptItem);
//        }

        popupMenu.show(e.getComponent(), e.getX(), e.getY());
        e.consume();
    }

    @Override
    protected JLabel createTagLabel(Annotation tag) {
        JLabel label = super.createTagLabel(tag);

        // TODO: port over user color mapping
        label.setBackground(ModelMgr.getModelMgr().getUserColorMapping().getColor(tag.getOwnerKey()));

//        if (tag.isComputation()) {
//            label.setToolTipText("This annotation was computationally inferred");
//            label.setIcon(Icons.getIcon("computer.png"));
//        }
//        else {
//            label.setToolTipText("This annotation was made by "+tag.getOwner());
//        }

        return label;
    }

    @Override
    public List<Annotation> getAnnotations() {
        return getTags();
    }

    @Override
    public void setAnnotations(List<Annotation> annotations) {
        setTags(annotations);
    }

    @Override
    public void removeAnnotation(Annotation annotation) {
        removeTag(annotation);
    }

    @Override
    public void addAnnotation(Annotation annotation) {
        addTag(annotation);
    }
}
