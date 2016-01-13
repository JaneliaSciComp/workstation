package org.janelia.it.workstation.gui.browser.nb_action;

import java.util.ArrayList;
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
import org.janelia.it.jacs.model.util.PermissionTemplate;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.events.selection.GlobalDomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.ontology.AnnotationEditor;
import org.janelia.it.workstation.gui.browser.nodes.OntologyTermNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        for(OntologyTermNode node : selected) {
            performAction(node);
        }
    }
    
    private void performAction(final OntologyTermNode node) {
        
        OntologyTerm ontologyTerm = node.getOntologyTerm();
        Long keyTermId = node.getId();
        String keyTermValue = node.getDisplayName();

        if (ontologyTerm instanceof Category || ontologyTerm instanceof org.janelia.it.jacs.model.domain.ontology.Enum) {
            // Cannot annotate with a category or enum
            return;
        }
        
        log.info("Will annotate all selected objects with: {} ({})",keyTermValue,keyTermId);
        
        List<Reference> selectedIds = GlobalDomainObjectSelectionModel.getInstance().getSelectedIds();
        
        if (selectedIds.isEmpty()) {
            // Cannot annotate nothing
            log.warn("ApplyAnnotationAction called without any objects being selected");
            return;
        }
        
        for(Reference id : selectedIds) {
            log.debug("Selected: "+id);
        }
        
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        final List<DomainObject> selectedDomainObjects = model.getDomainObjects(selectedIds);

        AnnotationEditor editor = new AnnotationEditor(node.getOntology(), ontologyTerm);
        final String value = editor.showEditor();

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                int i = 1;
                for (DomainObject domainObject : selectedDomainObjects) {
                    doAnnotation(domainObject, node, value);
                    setProgress(i++, selectedDomainObjects.size());
                }
            }

            @Override
            protected void hadSuccess() {
                // UI will be updated by events
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }

        };

        worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getMainFrame(), "Adding annotations", "", 0, 100));
        worker.execute();
    }
    
    public void doAnnotation(DomainObject target, OntologyTermNode termNode, Object value) throws Exception {
        
        Ontology ontology = termNode.getOntology();
        
        // Save the annotation
        OntologyTerm keyTerm = termNode.getOntologyTerm();
        OntologyTerm valueTerm = null;
        String keyString = keyTerm.getName();
        String valueString = value == null ? null : value.toString();

        if (keyTerm instanceof EnumItem) {
            keyTerm = termNode.getParent().getOntologyTerm();
            valueTerm = termNode.getOntologyTerm();
            keyString = keyTerm.getName();
            valueString = valueTerm.getName();
        }
        
        log.info("keyTerm:"+keyTerm);
        log.info("valueTerm:"+valueTerm);
        log.info("keyString:"+keyString);
        log.info("valueString:"+valueString);

        final Annotation annotation = new Annotation();
        annotation.setKey(keyString);
        annotation.setValue(valueString);
        annotation.setTarget(Reference.createFor(target));
        
        annotation.setKeyTerm(new OntologyTermReference(ontology, keyTerm));
        if (valueTerm!=null) {
            annotation.setValueTerm(new OntologyTermReference(ontology, valueTerm));
        }
        
        // TODO: move this business logic to the DAO
        String tag = (annotation.getValue()==null ? annotation.getKey() : 
                     annotation.getKey() + " = " + annotation.getValue());
        annotation.setName(tag);
        
        createAndShareAnnotation(annotation);
    }
    
    private void createAndShareAnnotation(Annotation annotation) throws Exception {
        
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        Annotation savedAnnotation = model.create(annotation);
        log.info("Saved annotation as " + savedAnnotation.getId());
        
        PermissionTemplate template = SessionMgr.getBrowser().getAutoShareTemplate();
        if (template!=null) {
            model.changePermissions(annotation, template.getSubjectKey(), template.getPermissions(), true);  
            log.info("Auto-shared annotation with " + template.getSubjectKey());
        }
    }
}
