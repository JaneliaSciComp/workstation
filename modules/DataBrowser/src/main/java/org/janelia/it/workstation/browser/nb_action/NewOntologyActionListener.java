package org.janelia.it.workstation.browser.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.components.OntologyExplorerTopComponent;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.domain.ontology.Ontology;

public final class NewOntologyActionListener implements ActionListener {

    public NewOntologyActionListener() {
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        ActivityLogHelper.logUserAction("NewOntologyActionListener.actionPerformed");

        final DomainModel model = DomainMgr.getDomainMgr().getModel();

        final String name = (String) JOptionPane.showInputDialog(ConsoleApp.getMainFrame(), "Ontology Name:\n",
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
                ConsoleApp.handleException(error);
            }

        };

        worker.execute();
    }
}
