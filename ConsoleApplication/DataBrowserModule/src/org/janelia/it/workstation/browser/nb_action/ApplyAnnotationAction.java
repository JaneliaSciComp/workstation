package org.janelia.it.workstation.browser.nb_action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.ProgressMonitor;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Category;
import org.janelia.it.jacs.model.domain.ontology.EnumItem;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.OntologyTermReference;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.util.PermissionTemplate;
import org.janelia.it.jacs.shared.utils.Progress;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.api.StateMgr;
import org.janelia.it.workstation.browser.events.selection.GlobalDomainObjectSelectionModel;
import org.janelia.it.workstation.browser.gui.ontology.AnnotationEditor;
import org.janelia.it.workstation.browser.nodes.OntologyTermNode;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;

/**
 * Create an annotation by applying the current ontology term to the 
 * domain objects selected in the active browser. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ApplyAnnotationAction extends NodeAction {

    private final static Logger log = LoggerFactory.getLogger(ApplyAnnotationAction.class);
    
    private final static ApplyAnnotationAction singleton = new ApplyAnnotationAction();
    public static ApplyAnnotationAction get() {
        return singleton;
    }
    
    private final List<OntologyTermNode> selected = new ArrayList<>();
    
    private ApplyAnnotationAction() {
    }
    
    @Override
    public String getName() {
        return "Apply To Selected Objects";
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("");
    }

    @Override
    protected boolean asynchronous() {
        return true;
    }
    
    @Override
    protected boolean enable(Node[] activatedNodes) {
        selected.clear();
        for(Node node : activatedNodes) {
            if (node instanceof OntologyTermNode) {
                
                OntologyTermNode termNode = (OntologyTermNode)node;
                OntologyTerm term = termNode.getOntologyTerm();
                if (term instanceof Category || term instanceof Ontology || term instanceof org.janelia.it.jacs.model.domain.ontology.Enum) {
                    // Can't apply these as an annotation
                    continue;
                }
                
                selected.add((OntologyTermNode)node);
            }
        }
        // Enable state if at least one ontology term is selected
        return selected.size()>0;
    }
    
    @Override
    protected void performAction(Node[] activatedNodes) {
        if (!enable(activatedNodes)) return;
        ActivityLogHelper.logUserAction("ApplyAnnotationAction.performAction");
        for(OntologyTermNode node : selected) {
            performAction(node.getOntologyTerm());
        }
    }
    
    public void performAction(final OntologyTerm ontologyTerm) {
        try {
            Ontology ontology = ontologyTerm.getOntology();
            String keyTermValue = ontologyTerm.getName();
            Long keyTermId = ontologyTerm.getId();

            if (ontologyTerm instanceof Category || ontologyTerm instanceof org.janelia.it.jacs.model.domain.ontology.Enum) {
                // Cannot annotate with a category or enum
                return;
            }

            log.info("Will annotate all selected objects with: {} ({})",keyTermValue,keyTermId);

            final List<Reference> selectedIds = GlobalDomainObjectSelectionModel.getInstance().getSelectedIds();

            if (selectedIds.isEmpty()) {
                // Cannot annotate nothing
                log.warn("ApplyAnnotationAction called without any objects being selected");
                return;
            }

            for(Reference id : selectedIds) {
                log.debug("Selected: "+id);
            }


            AnnotationEditor editor = new AnnotationEditor(ontology, ontologyTerm);
            final String value = editor.showEditor();

            if (AnnotationEditor.CANCEL_VALUE.equals(value)) return;

            SimpleWorker worker = new SimpleWorker() {

                @Override
                protected void doStuff() throws Exception {
                    setReferenceAnnotations(selectedIds, ontologyTerm, value, this);
                }

                @Override
                protected void hadSuccess() {
                    // UI will be updated by events
                }

                @Override
                protected void hadError(Throwable error) {
                    ConsoleApp.handleException(error);
                }

            };

            worker.setProgressMonitor(new ProgressMonitor(ConsoleApp.getMainFrame(), "Adding annotations", "", 0, 100));
            worker.execute();
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
    }

    public void setReferenceAnnotations(List<Reference> targetIds, OntologyTerm ontologyTerm, Object value) throws Exception {
        setReferenceAnnotations(targetIds, ontologyTerm, value, null);
    }
    
    public void setReferenceAnnotations(List<Reference> targetIds, OntologyTerm ontologyTerm, Object value, Progress progress) throws Exception {
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        List<DomainObject> domainObjects = model.getDomainObjects(targetIds);
        setObjectAnnotations(domainObjects, ontologyTerm, value, progress);
    }
    
    public void setObjectAnnotations(List<? extends DomainObject> domainObjects, OntologyTerm ontologyTerm, Object value, Progress progress) throws Exception {

        DomainModel model = DomainMgr.getDomainMgr().getModel();

        List<Reference> refs = DomainUtils.getReferences(domainObjects);
        Multimap<Long,Annotation> annotationMap = DomainUtils.getAnnotationsByDomainObjectId(model.getAnnotations(refs));
        
        
        int i = 1;
        for (DomainObject domainObject : domainObjects) {
            
            Annotation existingAnnotation = null;
            
            Collection<Annotation> annotations = annotationMap.get(domainObject.getId());
            if (annotations!=null) {
                for(Annotation annotation : annotations) {
                    if (annotation.getKeyTerm().getOntologyTermId().equals(ontologyTerm.getId())) {
                        existingAnnotation = annotation;
                        break;
                    }
                }
            }
            
            doAnnotation(domainObject, existingAnnotation, ontologyTerm, value);
            if (progress!=null) {
                if (progress.isCancelled()) return;
                progress.setProgress(i++, domainObjects.size());
            }
        }
    }

    public void addAnnotation(DomainObject target, OntologyTerm ontologyTerm, Object value) throws Exception {
        doAnnotation(target, null, ontologyTerm, value);
    }
    
    private void doAnnotation(DomainObject target, Annotation existingAnnotation, OntologyTerm ontologyTerm, Object value) throws Exception {
        
        // TODO: after domainModel.createAnnotation is implemented in the web service, we can use it instead, like this:
//        DomainModel model = DomainMgr.getDomainMgr().getModel();
//        model.createAnnotation(Reference.createFor(target), OntologyTermReference.createFor(ontologyTerm), value);
        
        Ontology ontology = ontologyTerm.getOntology();
        
        // Save the annotation
        OntologyTerm keyTerm = ontologyTerm;
        OntologyTerm valueTerm = null;
        String keyString = keyTerm.getName();
        String valueString = value == null ? null : value.toString();

        if (keyTerm instanceof EnumItem) {
            keyTerm = ontologyTerm.getParent();
            valueTerm = ontologyTerm;
            keyString = keyTerm.getName();
            valueString = valueTerm.getName();
        }

        final Annotation annotation = existingAnnotation == null? new Annotation() : existingAnnotation;
        annotation.setKey(keyString);
        annotation.setValue(valueString);
        annotation.setTarget(Reference.createFor(target));
        
        annotation.setKeyTerm(new OntologyTermReference(ontology, keyTerm));
        if (valueTerm!=null) {
            annotation.setValueTerm(new OntologyTermReference(ontology, valueTerm));
        }
        
        String tag = (annotation.getValue()==null ? annotation.getKey() : 
                     annotation.getKey() + " = " + annotation.getValue());
        annotation.setName(tag);
        
        createAndShareAnnotation(annotation);
    }
    
    private void createAndShareAnnotation(Annotation annotation) throws Exception {
        
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        Annotation savedAnnotation = model.save(annotation);
        log.info("Saved annotation as " + savedAnnotation.getId());
        
        PermissionTemplate template = StateMgr.getStateMgr().getAutoShareTemplate();
        if (template!=null) {
            model.changePermissions(savedAnnotation, template.getSubjectKey(), template.getPermissions());
            log.info("Auto-shared annotation with " + template.getSubjectKey());
        }
    }
}
