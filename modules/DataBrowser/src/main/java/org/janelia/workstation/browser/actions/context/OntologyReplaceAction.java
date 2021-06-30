package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.ontology.*;
import org.janelia.workstation.common.gui.support.YamlFileFilter;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.workers.IndeterminateProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Action to import an ontology to replace the selected ontology node.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "actions",
        id = "OntologyReplaceAction"
)
@ActionRegistration(
        displayName = "#CTL_OntologyReplaceAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Ontology", position = 603, separatorBefore = 599)
})
@NbBundle.Messages("CTL_OntologyReplaceAction=Replace from YAML...")
public class OntologyReplaceAction extends OntologyImportAction {

    private final static Logger log = LoggerFactory.getLogger(OntologyReplaceAction.class);

    @Override
    protected void importOntology(final OntologyTerm ontologyTerm) {

        ActivityLogHelper.logUserAction("OntologyReplaceAction.importOntology");

        final Ontology ontology = ontologyTerm.getOntology();
        final JFileChooser fc = new JFileChooser();
        FileFilter ff = new YamlFileFilter();
        fc.addChoosableFileFilter(ff);
        fc.setFileFilter(ff);

        int returnVal = fc.showOpenDialog(FrameworkAccess.getMainFrame());
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = fc.getSelectedFile();

        try (InputStream input = new FileInputStream(file)) {

            Yaml yaml = new Yaml();
            @SuppressWarnings("unchecked")
            final Map<String,Object> root = yaml.load(input);

            final DomainModel model = DomainMgr.getDomainMgr().getModel();
            model.setNotify(false);

            SimpleWorker worker = new SimpleWorker() {

                @Override
                protected void doStuff() throws Exception {
                    replaceOntologyTerms(ontology, ontologyTerm, root);
                }

                @Override
                protected void hadSuccess() {
                    model.setNotify(true);
                    SimpleWorker.runInBackground(() -> model.notifyDomainObjectChanged(ontology));
                }

                @Override
                protected void hadError(Throwable error) {
                    model.setNotify(true);
                    FrameworkAccess.handleException(error);
                }
            };

            worker.setProgressMonitor(new IndeterminateProgressMonitor(FrameworkAccess.getMainFrame(), "Replacing ontology node...", ""));
            worker.execute();

        }
        catch (IOException e) {
            FrameworkAccess.handleException(e);
        }
    }

    private void replaceOntologyTerms(Ontology ontology, OntologyTerm parentTerm, Map<String,Object> newNode) throws Exception {

        String termName = (String)newNode.get("name");
        String typeName = (String)newNode.get("type");
        if (typeName==null) typeName = "Tag";

        log.info("Replacing "+termName+" of type {}", termName, typeName);

        if (!parentTerm.getName().equals(termName)) {
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                    "The root node of your YAML must have the same name as the ontology node you are replacing.",
                    "Error replacing ontology node", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!parentTerm.getTypeName().equals(typeName)) {
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                    "The root node of your YAML must have the same type as the ontology node you are replacing.",
                    "Error replacing ontology node", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int numTermsToDelete = countTerms(parentTerm);

        int result = JOptionPane.showConfirmDialog(FrameworkAccess.getMainFrame(),
                "Replacing this term will remove "+numTermsToDelete+" in this ontology. Are you sure you want to proceed?",
                "Replace with YAML ontology", JOptionPane.OK_CANCEL_OPTION);
        if (result != 0) {
            return;
        }

        // Save the old child terms to delete
        ArrayList<OntologyTerm> ontologyTermsToDelete = new ArrayList<>(parentTerm.getTerms());

        // Add new child terms
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> childList = (List<Map<String, Object>>)newNode.get("children");
        if (childList!=null && !childList.isEmpty()) {
            for(Map<String,Object> child : childList) {
                createOntologyTerms(ontology, parentTerm, child);
            }
        }

        // Delete old child terms
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        for (OntologyTerm ontologyTermToDelete : ontologyTermsToDelete) {
            // TODO: this breaks enums, but so does everything else and no one uses them anyway
            model.removeOntologyTerm(ontology.getId(), parentTerm.getId(), ontologyTermToDelete.getId());
        }
    }

    private int countTerms(OntologyTerm term) {
        int terms = 1;
        for (OntologyTerm child : term.getTerms()) {
            terms += countTerms(child);
        }
        return terms;
    }
}
