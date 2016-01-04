package org.janelia.it.workstation.gui.browser.actions;

import java.util.List;

import javax.swing.ProgressMonitor;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.events.selection.GlobalDomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.dialogs.AnnotationBuilderDialog;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.ImageModel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ListMultimap;

/**
 * Change the annotation value on a single type of annotation, across multiple objects.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class BulkEditAnnotationKeyValueAction<T,S> implements NamedAction {

    private final static Logger log = LoggerFactory.getLogger(BulkEditAnnotationKeyValueAction.class);
    
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

            final List<Reference> selectedIds = GlobalDomainObjectSelectionModel.getInstance().getSelectedIds();
            
            if (selectedIds.isEmpty()) {
                // Cannot annotate nothing
                log.warn("BulkEditAnnotationKeyValueAction called without any objects being selected");
                return;
            }
            
            final DomainModel model = DomainMgr.getDomainMgr().getModel();
            
            SimpleWorker worker = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    AnnotationBuilderDialog dialog = new AnnotationBuilderDialog();
                    dialog.setAnnotationValue(tag.getValue());
                    dialog.setVisible(true);
                    String value = dialog.getAnnotationValue();
                    String originalAnnotationValue = dialog.getOriginalAnnotationText();
                    if (null == value) {
                        value = "";
                    }
                    if (value.equals(originalAnnotationValue)) {
                        System.out.println("Doing nothing.  The annotation was not changed.");
                        return;
                    }
                    try {
                        int i = 1;
                        List<DomainObject> selectedDomainObjects = model.getDomainObjects(selectedIds);
                        for (DomainObject selectedObject : selectedDomainObjects) {
                            ListMultimap<Long,Annotation> annotationsByDomainObjectId = DomainUtils.getAnnotationsByDomainObjectId(model.getAnnotations(selectedIds));
                            List<Annotation> annotations = annotationsByDomainObjectId.get(selectedObject.getId());
                            if (annotations == null) {
                                continue;
                            }
                            for (Annotation annotation : annotations) {
                                if (annotation.getValue().equals(originalAnnotationValue)) {
                                    annotation.setValue(value);
                                    String tmpName = annotation.getName();
                                    String namePrefix = tmpName.substring(0, tmpName.indexOf("=") + 2);
                                    annotation.setName(namePrefix + value);
                                    try {
                                        model.save(annotation);
                                    }
                                    catch (Exception e1) {
                                        SessionMgr.getSessionMgr().handleException(e1);
                                    }
                                }
                            }
                            setProgress(i++, selectedDomainObjects.size());
                        }
                    }
                    catch (Exception e1) {
                        SessionMgr.getSessionMgr().handleException(e1);
                    }
                }

                @Override
                protected void hadSuccess() {
                    // No need to do anything
                }

                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }
            };

            worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getMainFrame(), "Editing Annotations", "", 0, 100));
            worker.execute();
        }
        catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
        }

    }
}
