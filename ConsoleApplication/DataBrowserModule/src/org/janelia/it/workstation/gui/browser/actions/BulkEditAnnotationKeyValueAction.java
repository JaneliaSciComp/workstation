package org.janelia.it.workstation.gui.browser.actions;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import java.util.List;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.OntologyTermReference;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.ImageModel;

/**
 * TBD
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class BulkEditAnnotationKeyValueAction<T,S> implements NamedAction {

    private final ImageModel<T,S> imageModel;
    private final List<T> selectedObjects;
    private Annotation tag;

    public BulkEditAnnotationKeyValueAction(ImageModel<T,S> imageModel, List<T> selectedObjects, Annotation tag) {
        this.imageModel = imageModel;
        this.selectedObjects = selectedObjects;
        this.tag = tag;
    }

    @Override
    public String getName() {
        return selectedObjects.size() > 1 ? "Edit \"" + tag.toString() + "\" Annotation On " + selectedObjects.size() + " Items" : "Edit Annotation";
    }

    @Override
    public void doAction() {
        try {
            // TODO: port this
//            final Viewer viewer = SessionMgr.getBrowser().getViewerManager().getActiveViewer();
//            if (viewer instanceof IconDemoPanel) {
//                IconDemoPanel iconDemoPanel = (IconDemoPanel) viewer;
//                final Annotations annotations = iconDemoPanel.getAnnotations();
//                final Map<Long, List<OntologyAnnotation>> annotationMap = annotations.getFilteredAnnotationMap();
//
//                SimpleWorker worker = new SimpleWorker() {
//                    @Override
//                    protected void doStuff() throws Exception {
//                        AnnotationBuilderDialog dialog = new AnnotationBuilderDialog();
//                        dialog.setAnnotationValue(tag.getValueString());
//                        dialog.setVisible(true);
//                        String value = dialog.getAnnotationValue();
//                        String originalAnnotationValue = dialog.getOriginalAnnotationText();
//                        if (null == value) {
//                            value = "";
//                        }
//                        if (value.equals(originalAnnotationValue)) {
//                            System.out.println("Doing nothing.  The annotation was not changed.");
//                            return;
//                        }
//                        try {
//                            int i = 1;
//                            for (String selectedId : selectedEntities) {
//                                RootedEntity rootedEntity = viewer.getRootedEntityById(selectedId);
//                                List<OntologyAnnotation> entityAnnotations = annotationMap.get(rootedEntity.getEntity().getId());
//                                if (entityAnnotations == null) {
//                                    continue;
//                                }
//                                for (OntologyAnnotation annotation : entityAnnotations) {
//                                    if (annotation.getValueString().equals(originalAnnotationValue)) {
//                                        annotation.setValueString(value);
//                                        annotation.getEntity().setValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_TERM, value);
//                                        String tmpName = annotation.getEntity().getName();
//                                        String namePrefix = tmpName.substring(0, tmpName.indexOf("=") + 2);
//                                        annotation.getEntity().setName(namePrefix + value);
//                                        try {
//                                            Entity tmpAnnotatedEntity = ModelMgr.getModelMgr().getEntityById(annotation.getTargetEntityId());
//                                            ModelMgr.getModelMgr().saveOrUpdateAnnotation(tmpAnnotatedEntity, annotation.getEntity());
//                                        }
//                                        catch (Exception e1) {
//                                            e1.printStackTrace();
//                                            SessionMgr.getSessionMgr().handleException(e1);
//                                        }
//                                    }
//                                }
//                                setProgress(i++, selectedEntities.size());
//                            }
//                        }
//                        catch (Exception e1) {
//                            e1.printStackTrace();
//                            SessionMgr.getSessionMgr().handleException(e1);
//                        }
//                    }
//
//                    @Override
//                    protected void hadSuccess() {
//                        // No need to do anything
//                    }
//
//                    @Override
//                    protected void hadError(Throwable error) {
//                        SessionMgr.getSessionMgr().handleException(error);
//                    }
//                };
//
//                worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getMainFrame(), "Editing Annotations", "", 0, 100));
//                worker.execute();
//            }
        }
        catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
        }

    }
}
