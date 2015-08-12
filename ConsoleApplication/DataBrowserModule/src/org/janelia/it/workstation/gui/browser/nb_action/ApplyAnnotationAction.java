package org.janelia.it.workstation.gui.browser.nb_action;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.*;
import org.janelia.it.jacs.model.domain.support.MongoUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.browser.components.DomainListViewTopComponent;
import org.janelia.it.workstation.gui.browser.model.DomainObjectId;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.nodes.OntologyTermNode;
import org.janelia.it.workstation.gui.dialogs.AnnotationBuilderDialog;
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
    protected boolean enable (Node[] activatedNodes) {
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
        
        log.info("performAction");
        if (!enable(activatedNodes)) return;

        for(OntologyTermNode node : selected) {
            performAction(node);
        }
    }
    
    private void performAction(OntologyTermNode node) {
        
        OntologyTerm ontologyTerm = node.getOntologyTerm();
        Long keyTermId = node.getId();
        String keyTermValue = node.getDisplayName();

        log.info("Will annotate all selected objects with: {} ({})",keyTermValue,keyTermId);
        
        DomainListViewTopComponent listView = DomainListViewTopComponent.getActiveInstance();
        DomainObjectSelectionModel selectionModel = listView.getEditor().getSelectionModel();
        List<DomainObjectId> selectedIds = selectionModel.getSelectedIds();
        
        if (selectedIds.isEmpty()) {
            // Cannot annotate nothing
            log.warn("ApplyAnnotationAction called without any objects being selected");
            return;
        }
        
        // TODO: get objects
        final List<DomainObject> selectedDomainObjects = null;
        if (true) {
            log.info("ANNOTATION IS NOT YET IMPLEMENTED");
            return;
        }
        
        if (ontologyTerm instanceof Category || ontologyTerm instanceof org.janelia.it.jacs.model.domain.ontology.Enum) {
            // Cannot annotate with a category or enum
            return;
        }

        // Get the input value, if required

        Object value = null;
        if (ontologyTerm instanceof Interval) {
            value = JOptionPane.showInputDialog(SessionMgr.getMainFrame(), 
            		"Value:\n", ontologyTerm.getName(), JOptionPane.PLAIN_MESSAGE, null, null, null);

            if (StringUtils.isEmpty((String)value)) return;
            Double dvalue = Double.parseDouble((String)value);
            Interval interval = (Interval) ontologyTerm;
            if (dvalue < interval.getLowerBound().doubleValue() || dvalue > interval.getUpperBound().doubleValue()) {
                JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), 
                		"Input out of range [" + interval.getLowerBound() + "," + interval.getUpperBound() + "]");
                return;
            }
        }
        else if (ontologyTerm instanceof EnumText) {

            // TODO: get enum by id
            Long valueEnumId = ((EnumText) ontologyTerm).getValueEnumId();
            OntologyTermNode enumNode = node.getOntologyNode().getNodeById(valueEnumId);

            if (enumNode == null) {
                Exception error = new Exception(ontologyTerm.getName() + " has no supporting enumeration.");
                SessionMgr.getSessionMgr().handleException(error);
                return;
            }

            List<OntologyTerm> children = new ArrayList<>();
            for(Node childNode : enumNode.getChildren().getNodes()) {
                OntologyTermNode childTermNode = (OntologyTermNode)childNode;
                children.add(childTermNode.getOntologyTerm());
            }
            
            int i = 0;
            Object[] selectionValues = new Object[children.size()];
            for (OntologyTerm child : children) {
                selectionValues[i++] = child;
            }

            value = JOptionPane.showInputDialog(SessionMgr.getMainFrame(),
                    "Value:\n", ontologyTerm.getName(), JOptionPane.PLAIN_MESSAGE, null, selectionValues, null);
            if (value == null)
                return;
        }
        else if (ontologyTerm instanceof Text) {
            AnnotationBuilderDialog dialog = new AnnotationBuilderDialog();
            dialog.setVisible(true);
            value = dialog.getAnnotationValue();
            if (value==null || value.equals("")) return;
        }
        
        final OntologyTermNode finalTermNode = node;
        final Object finalValue = value;

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                int i = 1;
                for (DomainObject domainObject : selectedDomainObjects) {
                    doAnnotation(domainObject, finalTermNode, finalValue);
                    setProgress(i++, selectedDomainObjects.size());
                }
            }

            @Override
            protected void hadSuccess() {
                //ConcurrentUtils.invokeAndHandleExceptions(doSuccess);
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

        final Annotation annotation = new Annotation();
        
        OntologyTermReference keyTermRef = new OntologyTermReference();
        keyTermRef.setOntologyId(ontology.getId());
        keyTermRef.setOntologyTermId(keyTerm.getId());
        annotation.setKeyTerm(keyTermRef);
        
        if (valueTerm!=null) {
            OntologyTermReference valueTermRef = new OntologyTermReference();
            valueTermRef.setOntologyId(ontology.getId());
            valueTermRef.setOntologyTermId(valueTerm.getId());
            annotation.setValueTerm(valueTermRef);
        }
        
        annotation.setKey(keyString);
        annotation.setValue(valueString);
        
        Reference targetRef = new Reference();
        targetRef.setTargetType(MongoUtils.getCollectionName(target.getClass()));
        targetRef.setTargetId(target.getId());
        annotation.setTarget(targetRef);
        
        createAndShareAnnotation(annotation);
    }
    
    private void createAndShareAnnotation(Annotation annotation) throws Exception {
        
        log.info("TODO: persist annotation "+annotation.getKey()+" on "+annotation.getTarget());
        // TODO: persist
//        Entity annotationEntity = ModelMgr.getModelMgr().createOntologyAnnotation(annotation);
//        log.info("Saved annotation as " + annotationEntity.getId());
//        
//        PermissionTemplate template = SessionMgr.getBrowser().getAutoShareTemplate();
//        if (template!=null) {
//            ModelMgr.getModelMgr().grantPermissions(annotationEntity.getId(), template.getSubjectKey(), template.getPermissions(), false);
//            log.info("Auto-shared annotation with " + template.getSubjectKey());
//        }
    }
    
}
