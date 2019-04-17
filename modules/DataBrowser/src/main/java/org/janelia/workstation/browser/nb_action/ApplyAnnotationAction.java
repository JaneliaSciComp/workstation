package org.janelia.workstation.browser.nb_action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.ProgressMonitor;

import com.google.common.collect.Multimap;
import org.janelia.it.jacs.shared.utils.Progress;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.Category;
import org.janelia.model.domain.ontology.EnumItem;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.model.domain.ontology.OntologyTermReference;
import org.janelia.model.security.util.PermissionTemplate;
import org.janelia.workstation.browser.api.state.DataBrowserMgr;
import org.janelia.workstation.browser.gui.ontology.AnnotationEditor;
import org.janelia.workstation.browser.nodes.OntologyTermNode;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.events.selection.GlobalDomainObjectSelectionModel;
import org.janelia.workstation.core.workers.ResultWorker;
import org.janelia.workstation.core.workers.SimpleListenableFuture;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.janelia.workstation.core.options.OptionConstants.DUPLICATE_ANNOTATIONS_PROPERTY;

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
                if (term instanceof Category || term instanceof Ontology || term instanceof org.janelia.model.domain.ontology.Enum) {
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

        if (ontologyTerm instanceof Category || ontologyTerm instanceof org.janelia.model.domain.ontology.Enum) {
            // Cannot annotate with a category or enum
            return;
        }

        try {
            final List<Reference> selectedIds = GlobalDomainObjectSelectionModel.getInstance().getSelectedIds();

            if (selectedIds.isEmpty()) {
                // Cannot annotate nothing
                log.warn("ApplyAnnotationAction called without any objects being selected");
                return;
            }

            String keyTermValue = ontologyTerm.getName();
            Long keyTermId = ontologyTerm.getId();
            log.info("Will annotate all selected objects with: {} ({})",keyTermValue,keyTermId);

            for(Reference id : selectedIds) {
                log.debug("Selected: "+id);
            }
            
            annotateReferences(ontologyTerm, selectedIds);
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }
    
    
    public SimpleListenableFuture<List<Annotation>> annotateReferences(final OntologyTerm ontologyTerm, final List<Reference> references) {
            Ontology ontology = ontologyTerm.getOntology();

            AnnotationEditor editor = new AnnotationEditor(ontology, ontologyTerm);
            
            String value = null;
            if (editor.needsEditor()) {
                value = editor.showEditor();
                if (value==null) return null;    
            }
            
            final String finalValue = value;
            ResultWorker<List<Annotation>> worker = new ResultWorker<List<Annotation>>() {

                @Override
                protected List<Annotation> createResult() throws Exception {
                    List<Annotation> annotations = setReferenceAnnotations(references, ontologyTerm, finalValue, this);
                    return annotations;
                }
                
                @Override
                protected void hadSuccess() {
                    // UI will be updated by events
                }

                @Override
                protected void hadError(Throwable error) {
                    FrameworkAccess.handleException(error);
                }

            };

            worker.setProgressMonitor(new ProgressMonitor(FrameworkAccess.getMainFrame(), "Adding annotations", "", 0, 100));
            return worker.executeWithFuture();
    }

    public List<Annotation> setReferenceAnnotations(List<Reference> targetIds, OntologyTerm ontologyTerm, Object value) throws Exception {
        return setReferenceAnnotations(targetIds, ontologyTerm, value, null);
    }
    
    public List<Annotation> setReferenceAnnotations(List<Reference> targetIds, OntologyTerm ontologyTerm, Object value, Progress progress) throws Exception {
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        List<DomainObject> domainObjects = model.getDomainObjects(targetIds);
        return setObjectAnnotations(domainObjects, ontologyTerm, value, progress);
    }
    
    public List<Annotation> setObjectAnnotations(List<? extends DomainObject> domainObjects, OntologyTerm ontologyTerm, Object value, Progress progress) throws Exception {

        DomainModel model = DomainMgr.getDomainMgr().getModel();
        List<Reference> refs = DomainUtils.getReferences(domainObjects);
        Multimap<Long,Annotation> annotationMap = DomainUtils.getAnnotationsByDomainObjectId(model.getAnnotations(refs));
        
        List<Annotation> createdAnnotations = new ArrayList<>();
        int i = 1;
        for (DomainObject domainObject : domainObjects) {
            
            Boolean allowDups = (Boolean) FrameworkAccess.getModelProperty(DUPLICATE_ANNOTATIONS_PROPERTY);

            Annotation existingAnnotation = null;
            if (allowDups==null || !allowDups) {
                
                Collection<Annotation> annotations = annotationMap.get(domainObject.getId());
                if (annotations!=null) {
                    
                    OntologyTerm keyTerm = ontologyTerm;
                    if (keyTerm instanceof EnumItem) {
                        keyTerm = ontologyTerm.getParent();
                    }
                    
                    for(Annotation annotation : annotations) {
                        if (annotation.getKeyTerm().getOntologyTermId().equals(keyTerm.getId())) {
                            existingAnnotation = annotation;
                            break;
                        }
                    }
                }
            }
            
            if (existingAnnotation!=null) {
                log.info("Found existing annotation to update: "+existingAnnotation);
            }
            
            createdAnnotations.add(doAnnotation(domainObject, existingAnnotation, ontologyTerm, value));
            
            // Update progress
            if (progress!=null) {
                if (progress.isCancelled()) return createdAnnotations;
                progress.setProgress(i++, domainObjects.size());
            }
        }
        
        return createdAnnotations;
    }

    public Annotation addAnnotation(DomainObject target, OntologyTerm ontologyTerm, Object value) throws Exception {
        return doAnnotation(target, null, ontologyTerm, value);
    }
    
    private Annotation doAnnotation(DomainObject target, Annotation existingAnnotation, OntologyTerm ontologyTerm, Object value) throws Exception {
        
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
        
        return createAndShareAnnotation(annotation);
    }
    
    private Annotation createAndShareAnnotation(Annotation annotation) throws Exception {
        
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        Annotation savedAnnotation = model.save(annotation);
        log.info("Saved annotation: " + savedAnnotation);
        
        PermissionTemplate template = DataBrowserMgr.getDataBrowserMgr().getAutoShareTemplate();
        if (template!=null) {
            model.changePermissions(savedAnnotation, template.getSubjectKey(), template.getPermissions());
            log.info("Auto-shared annotation with " + template.getSubjectKey());
        }
        
        return savedAnnotation;
    }
}
