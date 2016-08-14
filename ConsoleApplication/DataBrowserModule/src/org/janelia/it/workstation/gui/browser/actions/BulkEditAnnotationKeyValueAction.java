package org.janelia.it.workstation.gui.browser.actions;

import java.util.List;

import javax.swing.ProgressMonitor;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.gui.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.gui.ontology.AnnotationEditor;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

import com.google.common.collect.ListMultimap;

/**
 * Change the annotation value on a single type of annotation, across multiple objects.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class BulkEditAnnotationKeyValueAction implements NamedAction {

    private final List<DomainObject> selectedObjects;
    private Annotation annotation;

    public BulkEditAnnotationKeyValueAction(List<DomainObject> selectedObjects, Annotation annotation) {
        this.selectedObjects = selectedObjects;
        this.annotation = annotation;
    }

    @Override
    public String getName() {
        return selectedObjects.size() > 1 ? "Edit \"" + annotation.getName() + "\" Annotation On " + selectedObjects.size() + " Items" : "Edit Annotation";
    }

    @Override
    public void doAction() {

        ActivityLogHelper.logUserAction("BulkEditAnnotationKeyValueAction.doAction", annotation);

        final String originalAnnotationName = annotation.getName();
        final DomainModel model = DomainMgr.getDomainMgr().getModel();
        
        SimpleWorker worker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                
                Ontology ontology = model.getDomainObject(Ontology.class, annotation.getKeyTerm().getOntologyId());
                AnnotationEditor editor = new AnnotationEditor(ontology, annotation);
                String newValue = editor.showEditor();
                if (newValue==null) return;
                
                try {
                    ListMultimap<Long,Annotation> annotationsByDomainObjectId = DomainUtils.getAnnotationsByDomainObjectId(model.getAnnotations(DomainUtils.getReferences(selectedObjects)));
                    
                    int i = 1;
                    for (DomainObject selectedObject : selectedObjects) {
                        List<Annotation> annotations = annotationsByDomainObjectId.get(selectedObject.getId());
                        if (annotation==null) continue;
                        for (Annotation annotation : annotations) {
                            if (annotation.getName().equals(originalAnnotationName)) {
                                annotation.setValue(newValue);
                                String tmpName = annotation.getName();
                                // TODO: move this business logic to the DAO
                                String namePrefix = tmpName.substring(0, tmpName.indexOf("=") + 2);
                                annotation.setName(namePrefix + newValue);
                                model.save(annotation);
                            }
                        }
                        setProgress(i++, selectedObjects.size());
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
}
