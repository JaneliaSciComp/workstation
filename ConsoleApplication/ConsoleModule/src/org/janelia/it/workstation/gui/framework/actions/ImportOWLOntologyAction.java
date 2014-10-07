package org.janelia.it.workstation.gui.framework.actions;

import java.io.File;
import java.util.concurrent.Callable;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;

import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.outline.OntologyOutline;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.ontology.OWLDataLoader;
import org.semanticweb.owlapi.model.OWLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import a new ontology from an OWL file.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ImportOWLOntologyAction implements Action {

    private static final Logger log = LoggerFactory.getLogger(ImportOWLOntologyAction.class);

    private final String name;

    public ImportOWLOntologyAction() {
        this.name = "Load OWL file";
    }

    public ImportOWLOntologyAction(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void doAction() {

        final OntologyOutline ontologyOutline = SessionMgr.getBrowser().getOntologyOutline();

        final JFileChooser fc = new JFileChooser();
        int returnVal = fc.showOpenDialog(ontologyOutline);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            File file = fc.getSelectedFile();
            String rootName = (String) JOptionPane.showInputDialog(ontologyOutline, "New Ontology Name:\n", "Import Ontology", JOptionPane.PLAIN_MESSAGE, null, null, null);

            if ((rootName == null) || (rootName.length() <= 0)) {
                JOptionPane.showMessageDialog(ontologyOutline, "Require a valid name", "Ontology Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            OWLDataLoader owlLoader = new OWLDataLoader(file) {

                @Override
                protected void hadSuccess() {
                    ModelMgr.getModelMgr().setCurrentOntologyId(getResult().getId());
                    log.info("OWL import successful, refreshing ontology tree...");
                    ontologyOutline.refresh(true, true, null);
                }

                @Override
                protected void hadError(Throwable error) {
                    log.error("Error loading OWL file", error);
                    JOptionPane.showMessageDialog(ontologyOutline, "Error loading ontology", "Ontology Import Error", JOptionPane.ERROR_MESSAGE);

                }
            };

            owlLoader.setProgressMonitor(new ProgressMonitor(SessionMgr.getMainFrame(), "Importing OWL", "", 0, 100));
            owlLoader.setOntologyName(rootName);
            owlLoader.execute();

        }
        catch (OWLException ex) {
            log.error("Error loading OWL file", ex);
            JOptionPane.showMessageDialog(ontologyOutline, "Error reading file", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }
}
