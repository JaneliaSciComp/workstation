package org.janelia.it.workstation.gui.browser.nodes;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.model.DeadReference;
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
    
    private final WeakReference<TreeNode> treeNodeRef;
    
    public TreeNodeChildFactory(TreeNode treeNode) {
        this.treeNodeRef = new WeakReference<>(treeNode);
    }
    
    @Override
    protected boolean createKeys(List<DomainObject> list) {
        TreeNode treeNode = treeNodeRef.get();
        if (treeNode==null) return false;
        log.debug("Creating children keys for {}",treeNode.getName());
        
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        List<DomainObject> children = model.getDomainObjects(treeNode.getChildren());
        if (children.size()!=treeNode.getNumChildren()) {
            log.info("Got {} children but expected {}",children.size(),treeNode.getNumChildren());   
        }
        
        Map<Long,DomainObject> map = new HashMap<>();
        for (DomainObject obj : children) {
            map.put(obj.getId(), obj);
        }
        
        List<DomainObject> temp = new ArrayList<>();
        if (treeNode.getChildren()!=null) {
            for(Reference reference : treeNode.getChildren()) {
                if (reference==null) continue;
                DomainObject obj = map.get(reference.getTargetId());
                log.trace(reference.getTargetType()+"#"+reference.getTargetId()+" -> "+obj);
                if (obj!=null) {
                    if (TreeNode.class.isAssignableFrom(obj.getClass())) {
                        temp.add(obj);
                    }
                    else if (ObjectSet.class.isAssignableFrom(obj.getClass())) {
                        temp.add(obj);
                    }
                    else if (Filter.class.isAssignableFrom(obj.getClass())) {
                        temp.add(obj);
                    }
                }
                else {
                    //temp.add(new DeadReference(reference));
                }
            }
        }
        
        list.addAll(temp);
        return true;
    }
    
    @Override
    protected Node createNodeForKey(DomainObject key) {
        try {
            // TODO: would be nice to do this dynamically, 
            // or at least with some sort of annotation
            if (TreeNode.class.isAssignableFrom(key.getClass())) {
                return new TreeNodeNode(this, (TreeNode)key);
            }
            else if (ObjectSet.class.isAssignableFrom(key.getClass())) {
                return new ObjectSetNode(this, (ObjectSet)key);
            }
            else if (Filter.class.isAssignableFrom(key.getClass())) {
                return new FilterNode(this, (Filter)key);
            }
            else {
                return null;
            }
        }
        catch (Exception e) {
            log.error("Error creating node for key " + key, e);
        }
        return null;
    }
    
    public void refresh() {
        log.info("Refreshing {}",treeNodeRef.get().getName());
        refresh(true);
    }
    
    public void addChild(final DomainObject domainObject) throws Exception {
        final TreeNode treeNode = treeNodeRef.get();
        if (treeNode==null) {
            log.warn("Cannot add child to unloaded treeNode");
            return;
        }   
        
        log.info("Adding child {} to {}",domainObject.getId(),treeNode.getName());
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        model.addChild(treeNode, domainObject);
        
        refresh();
    }

    public void removeChild(final DomainObject domainObject) throws Exception {
        final TreeNode treeNode = treeNodeRef.get();
        if (treeNode==null) {
            log.warn("Cannot remove child from unloaded treeNode");
            return;
        }

        log.info("Removing child {} from {}", domainObject.getId(), treeNode.getName());
        
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        if (domainObject instanceof DeadReference) {
            model.removeReference(treeNode, ((DeadReference) domainObject).getReference());
        }
        else {
            model.removeChild(treeNode, domainObject);
        }
            
        refresh();
    }
}
