package org.janelia.it.workstation.gui.browser.nb_action;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.openide.nodes.Node;

import org.janelia.it.jacs.model.domain.ontology.*;
import org.janelia.it.workstation.gui.browser.nodes.OntologyTermNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.YamlFileFilter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Export an ontology into YAML format. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyExportAction extends NodePresenterAction   {

    private static final String SAVE_FILE_EXTENSION = "yaml";

    private final static OntologyExportAction singleton = new OntologyExportAction();
    public static OntologyExportAction get() {
        return singleton;
    }

    private OntologyExportAction() {
    }

    @Override
    public synchronized JMenuItem getPopupPresenter() {

        List<Node> selectedNodes = getSelectedNodes();

        assert !selectedNodes.isEmpty() : "No nodes are selected";

        JMenuItem exportMenuItem = new JMenuItem("Export Ontology...");
        Node selectedNode = selectedNodes.get(0);
        final OntologyTermNode termNode = (OntologyTermNode) selectedNode;
        final OntologyTerm ontologyTerm = termNode.getOntologyTerm();

        exportMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportOntology(ontologyTerm);
            }
        });
        return exportMenuItem;
    }

    private void exportOntology(OntologyTerm ontologyTerm) {
        String defaultSaveFilename = ontologyTerm.getName().replaceAll("\\s+", "_") + "." + SAVE_FILE_EXTENSION;

        final JFileChooser fc = new JFileChooser();
        FileFilter ff = new YamlFileFilter();
        fc.addChoosableFileFilter(ff);
        fc.setFileFilter(ff);
        fc.setSelectedFile(new File(defaultSaveFilename));
        
        int returnVal = fc.showSaveDialog(SessionMgr.getMainFrame());
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Map<String,Object> object = convertOntologyToMap(ontologyTerm);

        try {
            FileWriter writer = new FileWriter(fc.getSelectedFile());
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);
            yaml.dump(object, writer);
        }
        catch (IOException e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    private Map<String,Object> convertOntologyToMap(OntologyTerm ontologyTerm) {

        Map<String,Object> map = new HashMap<>();
        map.put("name", ontologyTerm.getName());

        String typeName = ontologyTerm.getTypeName();
        if (!typeName.equals("Tag")) {
            map.put("type", typeName);
        }

        if (ontologyTerm instanceof Interval) {
            Interval interval = (Interval)ontologyTerm;
            map.put("lowerBound",interval.getLowerBound().toString());
            map.put("upperBound",interval.getUpperBound().toString());
        }
        else if (ontologyTerm instanceof EnumText) {
            EnumText enumText = (EnumText)ontologyTerm;
            map.put("enumId",enumText.getValueEnumId().toString());
        }

        List<OntologyTerm> children = ontologyTerm.getTerms();
        if (children!=null) {
            List<Object> childList = new ArrayList<>();
            for (OntologyTerm child : children) {
                childList.add(convertOntologyToMap(child));
            }
            map.put("children", childList);
        }

        return map;
    }
}
