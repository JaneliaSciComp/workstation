package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.ontology.*;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Action to import an ontology at the selected ontology node.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "actions",
        id = "OntologyImportAction"
)
@ActionRegistration(
        displayName = "#CTL_OntologyImportAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Ontology", position = 602, separatorBefore = 599)
})
@NbBundle.Messages("CTL_OntologyImportAction=Add from YAML...")
public class OntologyImportAction extends BaseContextualNodeAction {

    private final static Logger log = LoggerFactory.getLogger(OntologyImportAction.class);

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
    public void performAction() {
        importOntology(selectedTerm);
    }

    protected void importOntology(final OntologyTerm ontologyTerm) {

        ActivityLogHelper.logUserAction("OntologyImportAction.importOntology");

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

        try {
            InputStream input = new FileInputStream(file);
            Yaml yaml = new Yaml();
            @SuppressWarnings("unchecked")
            final Map<String,Object> root = yaml.load(input);

            final DomainModel model = DomainMgr.getDomainMgr().getModel();
            model.setNotify(false);

            SimpleWorker worker = new SimpleWorker() {

                @Override
                protected void doStuff() throws Exception {
                    createOntologyTerms(ontology, ontologyTerm, root);
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

            worker.setProgressMonitor(new IndeterminateProgressMonitor(FrameworkAccess.getMainFrame(), "Importing YAML...", ""));
            worker.execute();

        }
        catch (FileNotFoundException e) {
            FrameworkAccess.handleException(e);
        }
    }

    protected void createOntologyTerms(Ontology ontology, OntologyTerm parentTerm, Map<String,Object> newNode) throws Exception {

        String termName = (String)newNode.get("name");
        String typeName = (String)newNode.get("type");
        if (typeName==null) typeName = "Tag";
        if ("Ontology".equals(typeName)) typeName = "Category";

        log.info("Importing "+termName+" of type {}", termName, typeName);

        OntologyTerm newTerm = OntologyElementType.createTypeByName(typeName);
        newTerm.setName(termName);

        if (newTerm instanceof Interval) {
            Object lowerBound = newNode.get("lowerBound");
            if (lowerBound==null) {
                throw new IllegalArgumentException("lowerBound property must be specified for term "+termName+" of type Interval");
            }
            Object upperBound = newNode.get("upperBound");
            if (upperBound==null) {
                throw new IllegalArgumentException("lowerBound property must be specified for term "+termName+" of type Interval");
            }
            ((Interval) newTerm).init(Long.parseLong(lowerBound.toString()), Long.parseLong(upperBound.toString()));
        }
        else if (newTerm instanceof EnumText) {
            Object enumId = newNode.get("enumId");
            if (enumId==null) {
                throw new IllegalArgumentException("enumId property must be specified for term "+termName+" of type EnumText");
            }
            // TODO: should be able to reference an enum in the ontology being loaded
            ((EnumText) newTerm).init(Long.parseLong(enumId.toString()));
        }

        Ontology updatedOntology = DomainMgr.getDomainMgr().getModel().addOntologyTerm(ontology.getId(), parentTerm.getId(), newTerm);
        parentTerm = updatedOntology.findTerm(parentTerm.getId());
        for (OntologyTerm childTerm: parentTerm.getTerms()) {
            if (childTerm.getName().equals(termName)) {
                newTerm = childTerm;
            }
        }
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> childList = (List<Map<String, Object>>)newNode.get("children");
        if (childList!=null && !childList.isEmpty()) {
            for(Map<String,Object> child : childList) {
                createOntologyTerms(ontology,newTerm, child);
            }
        }
    }
}
