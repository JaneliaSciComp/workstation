package org.janelia.it.workstation.gui.browser.nb_action;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JFileChooser;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.util.YamlFileFilter;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.yaml.snakeyaml.Yaml;
import org.openide.nodes.Node;

import org.janelia.it.jacs.model.domain.ontology.*;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.gui.browser.nodes.OntologyTermNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Imports an ontology into YAML format.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyImportAction extends NodePresenterAction   {
    private final static Logger log = LoggerFactory.getLogger(OntologyImportAction.class);
    private final static OntologyImportAction singleton = new OntologyImportAction();
    public static OntologyImportAction get() {
        return singleton;
    }

    private OntologyImportAction() {
    }

    @Override
    public synchronized JMenuItem getPopupPresenter() {

        List<Node> selectedNodes = getSelectedNodes();

        assert !selectedNodes.isEmpty() : "No nodes are selected";

        JMenuItem importMenuItem = new JMenuItem("Import Ontology Here");
        Node selectedNode = selectedNodes.get(0);
        final OntologyTermNode termNode = (OntologyTermNode) selectedNode;

        importMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                importOntology(termNode);
            }
        });
        return importMenuItem;

    }

    private void importOntology (OntologyTermNode termNode) {
        final OntologyTerm ontologyTerm = termNode.getOntologyTerm();
        final Ontology ontology = termNode.getOntology();
        final JFileChooser fc = new JFileChooser();
        FileFilter ff = new YamlFileFilter();
        fc.addChoosableFileFilter(ff);
        fc.setFileFilter(ff);

        int returnVal = fc.showOpenDialog(SessionMgr.getMainFrame());
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = fc.getSelectedFile();

        try {
            InputStream input = new FileInputStream(file);
            Yaml yaml = new Yaml();
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
                    JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "Error creating ontology term", "Error", JOptionPane.ERROR_MESSAGE);
                }
            };

            worker.execute();

        }
        catch (FileNotFoundException e) {
            SessionMgr.getSessionMgr().handleException(e);
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
        parentTerm = DomainUtils.findTerm(updatedOntology, parentTerm.getId());
        for (OntologyTerm childTerm: parentTerm.getTerms()) {
            if (childTerm.getName().equals(termName)) {
                newTerm = childTerm;
            }
        }
        List<Map<String,Object>> childList = (List)newNode.get("children");
        if (childList!=null && !childList.isEmpty()) {
            for(Map<String,Object> child : childList) {
                createOntologyTerms(ontology,newTerm, child);
            }
        }
    }
}
