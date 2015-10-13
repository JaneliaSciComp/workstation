package org.janelia.it.workstation.gui.framework.outline.ontology;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;

import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.types.EnumText;
import org.janelia.it.jacs.model.ontology.types.Interval;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.workstation.gui.framework.actions.Action;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Export an ontology into YAML format. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyExportAction implements Action  {

    private OntologyElement ontologyElement;
    
    public OntologyExportAction(OntologyElement ontologyElement) {
        this.ontologyElement = ontologyElement;
    }

    @Override
    public String getName() {
        return "  Export Ontology...";
    }

    @Override
    public void doAction() {
        
        String defaultSaveFilename = ontologyElement.getName().replaceAll("\\s+", "_")+".txt";
    
        final JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(defaultSaveFilename));
        
        int returnVal = fc.showSaveDialog(SessionMgr.getMainFrame());
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
     
        Map<String,Object> object = convertOntologyToMap(ontologyElement);
        
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
    
    private Map<String,Object> convertOntologyToMap(OntologyElement ontologyElement) {
        
        Map<String,Object> map = new HashMap<>();
        map.put("name", ontologyElement.getName());
        
        OntologyElementType type = ontologyElement.getType();
        String typeName = type.getClass().getSimpleName();
        if (!typeName.equals("Tag")) {
            map.put("type", typeName);
        }
        
        if (type instanceof Interval) {
            Interval interval = (Interval)type;
            map.put("lowerBound",interval.getLowerBound().toString());
            map.put("upperBound",interval.getUpperBound().toString());
        }
        else if (type instanceof EnumText) {
            EnumText enumText = (EnumText)type;
            map.put("enumId",enumText.getValueEnumId().toString());
        }
        
        List<OntologyElement> children = ontologyElement.getChildren();
        if (!children.isEmpty()) {
            List<Object> childList = new ArrayList<>();
            for (OntologyElement child : children) {
                childList.add(convertOntologyToMap(child));
            }
            map.put("children", childList);
        }
        
        return map;
    }
}
