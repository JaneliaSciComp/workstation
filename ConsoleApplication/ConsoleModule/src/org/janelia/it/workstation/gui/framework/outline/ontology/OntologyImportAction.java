package org.janelia.it.workstation.gui.framework.outline.ontology;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.types.EnumText;
import org.janelia.it.jacs.model.ontology.types.Interval;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.actions.Action;
import org.janelia.it.workstation.gui.framework.outline.OntologyOutline;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.YamlFileFilter;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * Import an ontology from YAML format.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyImportAction implements Action  {

    private static final Logger log = LoggerFactory.getLogger(OntologyOutline.class);

    private final OntologyElement ontology;
    private final OntologyElement ontologyElement;

    public OntologyImportAction(OntologyElement ontologyElement) {
        this.ontologyElement = ontologyElement;

        OntologyElement root = null;
        OntologyElement curr = ontologyElement;
        while (curr!=null) {
            root = curr;
            curr = curr.getParent();
        }
        this.ontology = root;
    }

    @Override
    public String getName() {
        return "  Import Ontology Here";
    }

    @Override
    public void doAction() {

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
                    createOntologyTerms(ontologyElement.getEntity(), root);
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
    
    private void createOntologyTerms(Entity parentEntity, Map<String,Object> newNode) throws Exception {

        String termName = (String)newNode.get("name");
        String typeName = (String)newNode.get("type");
        if (typeName==null) typeName = "Tag";
        
        log.info("Importing "+termName+" of type "+typeName);

        OntologyElementType childType = null;
        //OntologyElementType.createTypeByName(typeName);

        if (childType instanceof Interval) {
            Object lowerBound = newNode.get("lowerBound");
            if (lowerBound==null) {
                throw new IllegalArgumentException("lowerBound property must be specified for term "+termName+" of type Interval");
            }
            Object upperBound = newNode.get("upperBound");
            if (upperBound==null) {
                throw new IllegalArgumentException("lowerBound property must be specified for term "+termName+" of type Interval");
            }
            ((Interval) childType).init(lowerBound.toString(), upperBound.toString());
        }
        else if (childType instanceof EnumText) {
            Object enumId = newNode.get("enumId");
            if (enumId==null) {
                throw new IllegalArgumentException("enumId property must be specified for term "+termName+" of type EnumText");
            }
            // TODO: should be able to reference an enum in the ontology being loaded
            ((EnumText) childType).init(Long.parseLong(enumId.toString()));
        }
        
        Entity newEntity = ModelMgr.getModelMgr().createOntologyTerm(ontology.getId(), parentEntity.getId(), termName, childType, null);
        
        List<Map<String,Object>> childList = (List)newNode.get("children");
        if (childList!=null && !childList.isEmpty()) {
            for(Map<String,Object> child : childList) {
                createOntologyTerms(newEntity, child);    
            }
        }
    }
}
