package org.janelia.workstation.browser.actions.context;

import javax.swing.JOptionPane;

import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.common.nodes.NodeUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "Actions",
        id = "RemoveOntologyTermAction"
)
@ActionRegistration(
        displayName = "#CTL_RemoveOntologyTermAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Ontology", position = 160, separatorAfter = 199)
})
@NbBundle.Messages("CTL_RemoveOntologyTermAction=Delete")
public class RemoveOntologyTermAction extends BaseContextualNodeAction {

    private final static Logger log = LoggerFactory.getLogger(RemoveOntologyTermAction.class);

    private OntologyTerm selectedTerm;

    @Override
    protected void processContext() {
        if (getNodeContext().isSingleObjectOfType(OntologyTerm.class)) {
            selectedTerm = getNodeContext().getSingleObjectOfType(OntologyTerm.class);
            setEnabledAndVisible(true);
        }
        else {
            selectedTerm = null;
            setEnabledAndVisible(false);
        }
    }

    @Override
    public String getName() {
        return "Delete "+selectedTerm.getTypeName();
    }

    @Override
    public void performAction() {

        OntologyTerm ontologyTerm = selectedTerm;

        String title;
        String msg;
        if (ontologyTerm instanceof Ontology) {
            title = "Delete Ontology";
            msg = "Are you sure you want to delete this ontology?";
        }
        else {
            title = "Delete Ontology Item";
            msg = "Are you sure you want to delete the item '"+ontologyTerm.getName()+"' and all of its descendants?";
        }

        int result = JOptionPane.showConfirmDialog(FrameworkAccess.getMainFrame(),
                msg, title, JOptionPane.OK_CANCEL_OPTION);

        if (result != 0) return;

        SimpleWorker worker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                if (ontologyTerm instanceof Ontology) {
                    model.removeOntology(ontologyTerm.getId());
                    log.info("Removed ontology {}", ontologyTerm.getId());
                }
                else {
                    model.removeOntologyTerm(ontologyTerm.getOntology().getId(), ontologyTerm.getParent().getId(), ontologyTerm.getId());
                    log.info("Removed ontology term {} from ontology {}", ontologyTerm.getId(), ontologyTerm.getOntology().getId());
                }
            }
            @Override
            protected void hadSuccess() {
                // Event model will refresh UI
            }
            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };
        NodeUtils.executeNodeOperation(worker);
    }
}
