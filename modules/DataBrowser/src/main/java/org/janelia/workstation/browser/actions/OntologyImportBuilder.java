package org.janelia.workstation.browser.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.janelia.model.domain.ontology.EnumText;
import org.janelia.model.domain.ontology.Interval;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyElementType;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.workstation.common.gui.support.YamlFileFilter;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.SimpleActionBuilder;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * Action to import an ontology at the selected ontology node.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=500)
public class OntologyImportBuilder extends SimpleActionBuilder {

    private final static Logger log = LoggerFactory.getLogger(OntologyImportBuilder.class);

    @Override
    protected String getName() {
        return "Import Ontology Here...";
    }

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof OntologyTerm;
    }

    @Override
    public boolean isPrecededBySeparator() {
        return true;
    }

    @Override
    protected void performAction(Object obj) {
        importOntology((OntologyTerm)obj);
    }

    private void importOntology(final OntologyTerm ontologyTerm) {

        ActivityLogHelper.logUserAction("OntologyImportBuilder.importOntology");

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
            final Map<String,Object> root = (Map<String,Object>)yaml.load(input);

            SimpleWorker worker = new SimpleWorker() {

                @Override
                protected void doStuff() throws Exception {
                    createOntologyTerms(ontology, ontologyTerm, root);
                }

                @Override
                protected void hadSuccess() {
                }

                @Override
                protected void hadError(Throwable error) {
                    log.error("Error creating ontology terms", error);
                    JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), "Error creating ontology term", "Error", JOptionPane.ERROR_MESSAGE);
                }
            };

            worker.execute();

        }
        catch (FileNotFoundException e) {
            FrameworkAccess.handleException(e);
        }
    }

    private void createOntologyTerms(Ontology ontology, OntologyTerm parentTerm, Map<String,Object> newNode) throws Exception {

        String termName = (String)newNode.get("name");
        String typeName = (String)newNode.get("type");
        if (typeName==null) typeName = "Tag";

        log.info("Importing "+termName+" of type "+typeName);

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
