package org.janelia.it.workstation.browser.nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.integration.framework.domain.DomainObjectHelper;
import org.janelia.it.jacs.integration.framework.domain.ServiceAcceptorHelper;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A child factory for tree nodes (i.e. folders). Supports adding and removing 
 * children dynamically.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TreeNodeChildFactory extends ChildFactory<DomainObject> {

    private final static Logger log = LoggerFactory.getLogger(TreeNodeChildFactory.class);
    
    private TreeNode treeNode;

    TreeNodeChildFactory(TreeNode treeNode) {
        if (treeNode==null) {
            throw new IllegalArgumentException("Cannot create child factory with null tree node");
        }
        this.treeNode = treeNode;
    }

    public void update(TreeNode treeNode) {
        if (treeNode==null) {
            throw new IllegalArgumentException("Cannot set null tree node for child factory");
        }
        this.treeNode = treeNode;
    }

    private boolean isSupportedAsChild(Class<? extends DomainObject> clazz) {
        try {
            // TODO: this should use the other isCompatible() method which takes a class, 
            // instead of constructing a dummy object
            DomainObject dummyChild = (DomainObject)clazz.newInstance();
            DomainObjectHelper provider = ServiceAcceptorHelper.findFirstHelper(dummyChild);
            if (provider!=null) {
                return true;
            }
        }
        catch (InstantiationException | IllegalAccessException e) {
            log.error("Error instantiating purported domain class "+clazz, e);
        }
        return false;
    }
    
    public boolean hasNodeChildren() {
        for(Reference reference : treeNode.getChildren()) {
            if (reference==null) continue;
            Class<? extends DomainObject> clazz = DomainUtils.getObjectClassByName(reference.getTargetClassName());
            if (isSupportedAsChild(clazz)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean createKeys(List<DomainObject> list) {
        try {
            if (treeNode==null) {
                throw new IllegalStateException("No tree node is set for this child factory");
            }

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
                        if (isSupportedAsChild(obj.getClass())) {
                            temp.add(obj);
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
            DomainObjectHelper provider = ServiceAcceptorHelper.findFirstHelper(key);
            if (provider!=null) {
                return provider.getNode(key, this);
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
}