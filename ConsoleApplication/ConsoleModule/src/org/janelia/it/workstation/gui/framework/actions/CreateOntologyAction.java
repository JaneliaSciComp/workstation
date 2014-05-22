package org.janelia.it.workstation.gui.framework.actions;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.outline.OntologyOutline;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * Create a new ontology owned by the current user. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CreateOntologyAction implements Action {
    
    private String name;

    public CreateOntologyAction() {
        this.name = "Create New Ontology";
    }
    
    public CreateOntologyAction(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public void doAction() {

        final OntologyOutline ontologyOutline = SessionMgr.getBrowser().getOntologyOutline();
        
        final String rootName = (String) JOptionPane.showInputDialog(ontologyOutline, 
                "Ontology Name:\n", "New Ontology", JOptionPane.PLAIN_MESSAGE, null, null, null);

        if ((rootName == null) || (rootName.length() <= 0)) {
            return;
        }

        SimpleWorker worker = new SimpleWorker() {

            private Entity newRoot;

            protected void doStuff() throws Exception {
                newRoot = ModelMgr.getModelMgr().createOntologyRoot(rootName);
            }

            protected void hadSuccess() {
                // Wait until the new ontology is available in the outline and then load it
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ModelMgr.getModelMgr().setCurrentOntologyId(newRoot.getId());
                    }
                });
            }

            protected void hadError(Throwable error) {
                error.printStackTrace();
                JOptionPane.showMessageDialog(ontologyOutline, "Error creating ontology", "Ontology Creation Error", JOptionPane.ERROR_MESSAGE);
            }

        };
        worker.execute();
    }
}
