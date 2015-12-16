package org.janelia.it.workstation.gui.browser.actions;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

import javax.swing.*;
import java.util.List;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.ImageModel;

/**
 * Remove the same annotation from multiple domain objects.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RemoveAnnotationKeyValueAction<T,S> implements NamedAction {

    private final ImageModel<T,S> imageModel;
    private final List<T> selectedObjects;
    private Annotation tag;

    public RemoveAnnotationKeyValueAction(ImageModel<T,S> imageModel, List<T> selectedObjects, Annotation tag) {
        this.imageModel = imageModel;
        this.selectedObjects = selectedObjects;
        this.tag = tag;
    }

    @Override
    public String getName() {
        return selectedObjects.size() > 1 ? "  Remove \"" + tag.toString() + "\" Annotation From " + selectedObjects.size() + " Items" : "  Remove Annotation";
    }

    @Override
    public void doAction() {

        if (selectedObjects.size() > 1) {
            int deleteConfirmation = JOptionPane.showConfirmDialog(SessionMgr.getMainFrame(), "Are you sure you want to delete this annotation from all selected entities?", "Delete Annotations", JOptionPane.YES_NO_OPTION);
            if (deleteConfirmation != 0) {
                return;
            }
        }

        try {

            // TODO: port this
//            final Viewer viewer = SessionMgr.getBrowser().getViewerManager().getActiveViewer();
//            if (viewer instanceof IconDemoPanel) {
//                IconDemoPanel iconDemoPanel = (IconDemoPanel) viewer;
//                final Annotations annotations = iconDemoPanel.getAnnotations();
//                final Map<Long, List<OntologyAnnotation>> annotationMap = annotations.getFilteredAnnotationMap();
//
//                SimpleWorker worker = new SimpleWorker() {
//
//                    @Override
//                    protected void doStuff() throws Exception {
//
//                        int i = 1;
//                        for (String selectedId : selectedObjects) {
//                            RootedEntity rootedEntity = viewer.getRootedEntityById(selectedId);
//                            List<OntologyAnnotation> entityAnnotations = annotationMap.get(rootedEntity.getEntity().getId());
//                            if (entityAnnotations == null) {
//                                continue;
//                            }
//                            for (OntologyAnnotation annotation : entityAnnotations) {
//                                if (annotation.toString().equals(tag.toString())) {
//                                    ModelMgr.getModelMgr().removeAnnotation(annotation.getId());
//                                }
//                            }
//                            setProgress(i++, selectedObjects.size());
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
//                worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getMainFrame(), "Deleting Annotations", "", 0, 100));
//                worker.execute();
//            }
        }
        catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
        }

    }
}
