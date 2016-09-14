package org.janelia.it.workstation.gui.browser.nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.integration.framework.domain.DomainObjectHelper;
import org.janelia.it.jacs.integration.framework.domain.ServiceAcceptorHelper;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A child factory for tree nodes (i.e. folders). Supports adding and removing 
 * children dynamically.
 *
 * TODO: this class needs an overhaul so that there is one place where we define which nodes appear as children of folders,
 * and everything else that happens in here is a consequence of that.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TreeNodeChildFactory extends ChildFactory<DomainObject> {

    private final static Logger log = LoggerFactory.getLogger(TreeNodeChildFactory.class);
    
    private TreeNode treeNode;

    TreeNodeChildFactory(TreeNode treeNode) {
        this.treeNode = treeNode;
    }

    public void update(TreeNode treeNode) {
        this.treeNode = treeNode;
    }

    public boolean hasNodeChildren() {
        for(Reference reference : treeNode.getChildren()) {
            if (reference==null) continue;

            if (reference.getTargetClassName().equals("TreeNode")) {
                return true;
            }
            else if (reference.getTargetClassName().equals("Filter")) {
                return true;
            }
            
            Class<? extends DomainObject> clazz = DomainUtils.getObjectClassByName(reference.getTargetClassName());
            
            try {
                DomainObject dummyChild = (DomainObject)clazz.newInstance();
                DomainObjectHelper provider = ServiceAcceptorHelper.findFirstHelper(dummyChild);
                if (provider!=null) {
                    return true;
                }
            }
            catch (InstantiationException | IllegalAccessException e) {
                log.error("Error instantiating purported domain class "+reference.getTargetClassName(), e);
            }
            
        }
        return false;
    }

    @Override
    protected boolean createKeys(List<DomainObject> list) {
        try {
            if (treeNode==null) return false;

            log.debug("Creating children keys for {}",treeNode.getName());

            DomainModel model = DomainMgr.getDomainMgr().getModel();
            List<DomainObject> children = model.getDomainObjects(treeNode.getChildren());
            if (children.size()!=treeNode.getNumChildren()) {
                log.info("Got {} children but expected {}",children.size(),treeNode.getNumChildren());
            }
            log.debug("Got children: {}",children);

            Map<Long,DomainObject> map = new HashMap<>();
            for (DomainObject obj : children) {
                map.put(obj.getId(), obj);
            }

            List<DomainObject> temp = new ArrayList<>();
            if (treeNode.hasChildren()) {
                for(Reference reference : treeNode.getChildren()) {
                    if (reference==null) continue;
                    DomainObject obj = map.get(reference.getTargetId());
                    log.trace(reference.getTargetClassName()+"#"+reference.getTargetId()+" -> "+obj);
                    if (obj!=null) {
                        if (TreeNode.class.isAssignableFrom(obj.getClass())) {
                            temp.add(obj);
                        }
                        else if (Filter.class.isAssignableFrom(obj.getClass())) {
                            temp.add(obj);
                        }
                        else {
                            DomainObjectHelper provider = ServiceAcceptorHelper.findFirstHelper(obj);
                            if (provider!=null) {
                                temp.add(obj);
                            }
                        }
                    }
                    else {
                        log.warn("Dead reference detected: "+reference);
                    }
                }
            }

            list.addAll(temp);
        }
        catch (Exception ex) {
            log.error("Error creating tree node child keys",ex);
            return false;
        }
        return true;
    }

    @Override
    protected Node createNodeForKey(DomainObject key) {
        log.debug("Creating node for '{}'",key.getName());
        try {
            if (TreeNode.class.isAssignableFrom(key.getClass())) {
                return new TreeNodeNode(this, (TreeNode)key);
            }
            else if (Filter.class.isAssignableFrom(key.getClass())) {
                return new FilterNode(this, (Filter)key);
            }
            else {
                DomainObjectHelper provider = ServiceAcceptorHelper.findFirstHelper(key);
                if (provider!=null) {
                    return provider.getNode(key, this);
                }
            }
            return null;
        }
        catch (Exception e) {
            log.error("Error creating node for '"+key+"'", e);
        }
        return null;
    }

    public void refresh() {
        log.debug("Refreshing child factory for: {}",treeNode.getName());
        refresh(true);
    }

    public void addChildren(List<DomainObject> domainObjects) throws Exception {
        if (treeNode==null) {
            log.warn("Cannot add child to unloaded treeNode");
            return;
        }   

        for(DomainObject domainObject : domainObjects) {
            log.info("Adding child '{}' to '{}'",domainObject.getName(),treeNode.getName());
        }
        
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        model.addChildren(treeNode, domainObjects);
    }
    
    public void addChildren(List<DomainObject> domainObjects, int index) throws Exception {
        if (treeNode==null) {
            log.warn("Cannot add child to unloaded treeNode");
            return;
        }   

        int i = 0;
        for(DomainObject domainObject : domainObjects) {
            log.info("Adding child '{}' to '{}' at {}",domainObject.getName(),treeNode.getName(),index+i);
            i++;
        }
        
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        model.addChildren(treeNode, domainObjects, index);
    }
    
    public void removeChild(final DomainObject domainObject) throws Exception {
        // Modifying the domain model is now taken care of by the RemoveItemsFromFolderAction
    }
}