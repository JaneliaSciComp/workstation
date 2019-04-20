package org.janelia.workstation.browser.actions;

import javax.swing.JOptionPane;

import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.workstation.common.nodes.NodeUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.SimpleActionBuilder;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=160)
public class RemoveOntologyTermBuilder extends SimpleActionBuilder {

    private final static Logger log = LoggerFactory.getLogger(RemoveOntologyTermBuilder.class);

    @Override
    protected String getName() {
        return "Remove";
    }

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof OntologyTerm;
    }

    @Override
    protected void performAction(Object contextObject) {

        OntologyTerm ontologyTerm = (OntologyTerm)contextObject;

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
