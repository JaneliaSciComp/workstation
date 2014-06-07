package org.janelia.it.workstation.gui.framework.actions;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import org.janelia.it.workstation.gui.framework.outline.OntologyOutline;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.ontology.OWLDataLoader;
import org.semanticweb.owlapi.model.OWLException;

/**
 * Import a new ontology from an OWL file.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ImportOWLOntologyAction implements Action, PropertyChangeListener {
    
    private String name;
    
    public ImportOWLOntologyAction(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    private ProgressMonitor progressMonitor;
    private OWLDataLoader owlLoader;
    
    @Override
    public void doAction() {

        final OntologyOutline ontologyOutline = SessionMgr.getBrowser().getOntologyOutline();

        final JFileChooser fc = new JFileChooser();
        int returnVal = fc.showOpenDialog(ontologyOutline);
        if (returnVal != JFileChooser.APPROVE_OPTION) return;

        try {
            File file = fc.getSelectedFile();
            String rootName = (String) JOptionPane.showInputDialog(ontologyOutline, "New Ontology Name:\n", "Import Ontology", JOptionPane.PLAIN_MESSAGE, null, null, null);

            if ((rootName == null) || (rootName.length() <= 0)) {
                JOptionPane.showMessageDialog(ontologyOutline, "Require a valid name", "Ontology Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // TODO: modify this to use the built-in ProgressMonitor support in SimpleWorker, on which OWLDataLoader is based.
            progressMonitor = new ProgressMonitor(ontologyOutline, "Importing OWL", "", 0, 100);
            progressMonitor.setProgress(0);

            owlLoader = new OWLDataLoader(file) {

                protected void hadSuccess() {
                    ontologyOutline.refresh(true, true, null);
                }

                protected void hadError(Throwable error) {
                    error.printStackTrace();
                    JOptionPane.showMessageDialog(ontologyOutline, "Error loading ontology", "Ontology Import Error", JOptionPane.ERROR_MESSAGE);
                    
                }
            };

            owlLoader.addPropertyChangeListener(this);
            owlLoader.setOntologyName(rootName);
            owlLoader.execute();

        }
        catch (OWLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(ontologyOutline, "Error reading file", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    public void propertyChange(PropertyChangeEvent e) {
        if ("progress".equals(e.getPropertyName())) {
            int progress = (Integer) e.getNewValue();
            progressMonitor.setProgress(progress);
            String message = String.format("Completed %d%%", progress);
            progressMonitor.setNote(message);
            if (progressMonitor.isCanceled()) {
                owlLoader.cancel(true);
            }
        }
    }
}
