package org.janelia.it.workstation.gui.browser.nb_action;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.components.OntologyExplorerTopComponent;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

/**
 * Create a new ontology owned by the current user.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "File",
        id = "NewOntologyAction"
)
@ActionRegistration(
        displayName = "#CTL_NewOntologyAction"
)
@ActionReference(path = "Menu/File/New", position = 4)
@Messages("CTL_NewOntologyAction=Ontology")
public class NewOntologyAction implements ActionListener {

    protected final Component mainFrame = SessionMgr.getMainFrame();

    public NewOntologyAction() {
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        final DomainModel model = DomainMgr.getDomainMgr().getModel();

        final String name = (String) JOptionPane.showInputDialog(mainFrame, "Ontology Name:\n",
                "Create new ontology", JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (StringUtils.isEmpty(name)) {
            return;
        }

        // Make sure the ontology explorer is visible first
        OntologyExplorerTopComponent tc = OntologyExplorerTopComponent.getInstance();
        if (!tc.isOpened()) {
            tc.open();
        }
        tc.requestVisible();

        SimpleWorker worker = new SimpleWorker() {

            private Ontology ontology;

            @Override
            protected void doStuff() throws Exception {
                ontology = new Ontology();
                ontology.setName(name);
                model.create(ontology);
            }

            @Override
            protected void hadSuccess() {
                // GUI will update due to events
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }

        };
        
        worker.execute();
    }
}
