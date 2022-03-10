package org.janelia.workstation.browser.actions;

import com.google.common.collect.ListMultimap;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.workstation.browser.gui.ontology.AnnotationEditor;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;

import javax.swing.AbstractAction;
import javax.swing.ProgressMonitor;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Change the annotation value on a single type of annotation, across multiple objects.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class BulkEditAnnotationKeyValueAction extends AbstractAction {

    private final List<DomainObject> selectedObjects;
    private Annotation annotation;

    public BulkEditAnnotationKeyValueAction(List<DomainObject> selectedObjects, Annotation annotation) {
        super(selectedObjects.size() > 1 ? "Edit \"" + annotation.getName() + "\" Annotation On " + selectedObjects.size() + " Items" : "Edit Annotation");
        this.selectedObjects = selectedObjects;
        this.annotation = annotation;
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        ActivityLogHelper.logUserAction("BulkEditAnnotationKeyValueAction.doAction", annotation);

        final String originalAnnotationName = annotation.getName();
        final DomainModel model = DomainMgr.getDomainMgr().getModel();
        
        SimpleWorker worker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {

                if (annotation.getKeyTerm()==null) throw new Exception("This annotation has no key term and cannot be modified");

                Ontology ontology = model.getDomainObject(Ontology.class, annotation.getKeyTerm().getOntologyId());
                AnnotationEditor editor = new AnnotationEditor(ontology, annotation);
                String newValue = editor.showEditor();
                if (newValue==null) return;
                
                try {
                    ListMultimap<Long,Annotation> annotationsByDomainObjectId = DomainUtils.getAnnotationsByDomainObjectId(model.getAnnotations(DomainUtils.getReferences(selectedObjects)));

                    if (selectedObjects.size()==1) {
                        // If the user has only selected a single domain object, then only update the annotation they actually clicked on
                        setAnnotationValue(annotation, newValue);
                    }
                    else {
                        // If the user has selected multiple domain objects, then update ALL the matching annotations across ALL the objects they selected
                        int i = 1;
                        for (DomainObject selectedObject : selectedObjects) {
                            List<Annotation> annotations = annotationsByDomainObjectId.get(selectedObject.getId());
                            if (annotation == null) continue;
                            for (Annotation annotation : annotations) {
                                if (annotation.getName().equals(originalAnnotationName)) {
                                    setAnnotationValue(annotation, newValue);
                                }
                            }
                            setProgress(i++, selectedObjects.size());
                        }
                    }
                }
                catch (Exception e1) {
                    FrameworkAccess.handleException(e1);
                }
            }

            private void setAnnotationValue(Annotation annotation, String newValue) throws Exception {
                model.updateAnnotation(annotation, newValue);
            }

            @Override
            protected void hadSuccess() {
                // No need to do anything
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };

        worker.setProgressMonitor(new ProgressMonitor(FrameworkAccess.getMainFrame(), "Editing Annotations", "", 0, 100));
        worker.execute();
    }
}
