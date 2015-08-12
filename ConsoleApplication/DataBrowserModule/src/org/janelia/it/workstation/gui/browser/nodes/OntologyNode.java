package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import org.janelia.it.workstation.gui.browser.actions.RunNodeDefaultAction;
import org.janelia.it.workstation.gui.browser.actions.OntologyElementAction;
import org.janelia.it.workstation.gui.browser.api.DomainDAO;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

/**
 * Root node of an ontology. Manages all the nodes in the ontology, including
 * inter-ontology references and actions. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyNode extends OntologyTermNode {
    
    private final static Logger log = LoggerFactory.getLogger(OntologyNode.class);
    
    private final ConcurrentMap<Long, OntologyTermNode> nodeById = new MapMaker().weakValues().makeMap();
    private final Map<String, org.janelia.it.workstation.gui.framework.actions.Action> ontologyActionMap = new HashMap<>();
    
    public OntologyNode(Ontology ontology) {
        super(null, ontology, ontology);
        populateMaps(this);
    }
    
    private void populateMaps(OntologyTermNode node) {
        log.trace("populateMaps({})",node.getDisplayName());
        populateLookupMap(node);
        populateActionMap(node);
        for(Node childNode : node.getChildren().getNodes()) {
            if (childNode instanceof OntologyTermNode) {
                OntologyTermNode termNode = (OntologyTermNode)childNode;
                populateMaps(termNode);
            }
            else {
                log.warn("Encountered unsupported node type while traversing ontology: "+node.getClass().getName());
            }
        }
    }
    
    private void populateLookupMap(OntologyTermNode node) {
        nodeById.put(node.getId(), node);
        
    }
    private void populateActionMap(OntologyTermNode node) {
        OntologyElementAction action = new RunNodeDefaultAction();
        Long[] path = NodeUtils.createIdPath(node);
        action.init(path);
        String pathStr = NodeUtils.createPathString(path);
        log.trace("path string: {}",pathStr);
        ontologyActionMap.put(pathStr, action);
    }
    
    public org.janelia.it.workstation.gui.framework.actions.Action getActionForNode(OntologyTermNode node) {
        return ontologyActionMap.get(NodeUtils.createPathString(node));
    }
    
    public OntologyTermNode getNodeById(Long id) {
        return nodeById.get(id);
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("folder.png").getImage();    
    }
    
    @Override
    public boolean canRename() {
        return true;
    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }
    
    @Override
    public Action[] getActions(boolean context) {
        Action[] superActions = super.getActions(context);
        List<Action> actions = new ArrayList<>();
//        actions.add(RenameAction.get(RenameAction.class));
        actions.addAll(Lists.newArrayList(superActions));
        return actions.toArray(new Action[0]);
    }
    
    @Override
    public void setName(final String newName) {
        final Ontology ontology = getOntology();
        final String oldName = ontology.getName();
        ontology.setName(newName);
        SimpleWorker worker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                log.trace("Changing name from " + oldName + " to: " + newName);
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                model.updateProperty(ontology, "name", newName);
            }
            @Override
            protected void hadSuccess() {
                log.trace("Fire name change from" + oldName + " to: " + newName);
                fireDisplayNameChange(oldName, newName); 
            }
            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        worker.execute();
    }

    public Map<String, org.janelia.it.workstation.gui.framework.actions.Action> getOntologyActionMap() {
        return ontologyActionMap;
    }
}
