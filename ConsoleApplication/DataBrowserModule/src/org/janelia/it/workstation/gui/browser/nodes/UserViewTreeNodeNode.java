package org.janelia.it.workstation.gui.browser.nodes;


import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A tree node (i.e. Folder) in the data graph.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UserViewTreeNodeNode extends DomainObjectNode {
        
    private final static Logger log = LoggerFactory.getLogger(UserViewTreeNodeNode.class);
    
    private final UserViewTreeNodeChildFactory childFactory;
    private final UserViewConfiguration config;
    
    public UserViewTreeNodeNode(TreeNode treeNode, UserViewConfiguration config) {
        this(null, new UserViewTreeNodeChildFactory(treeNode, config), treeNode, config);
    }
    
    private UserViewTreeNodeNode(UserViewTreeNodeChildFactory parentChildFactory, final UserViewTreeNodeChildFactory childFactory, TreeNode treeNode, UserViewConfiguration config) {
        super(parentChildFactory, treeNode.getNumChildren()==0?Children.LEAF:Children.create(childFactory, false), treeNode);
        this.childFactory = childFactory;
        this.config = config;
    }
    
    public TreeNode getTreeNode() {
        return (TreeNode)getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getTreeNode().getName();
    }
    
    @Override
    public String getExtraLabel() {
        return "("+getTreeNode().getNumChildren()+")";
    }
    
    @Override
    public Image getIcon(int type) {
        if (!getTreeNode().getOwnerKey().equals(AccessManager.getSubjectKey())) {
            return Icons.getIcon("folder_blue.png").getImage();
        }
        else {
            return Icons.getIcon("folder.png").getImage();    
        }
    }
    
    @Override
    public boolean canDestroy() {
        return false;
    }
    
//    @Override
//    public Action[] getActions(boolean context) {
//        List<Action> actions = new ArrayList<>();
//        return actions.toArray(new Action[0]);
//    }

    public void refresh() {
        childFactory.refresh();
    }
    
    private static class UserViewTreeNodeChildFactory extends ChildFactory<DomainObject> {

        private final WeakReference<TreeNode> treeNodeRef;
        private final UserViewConfiguration config;

        UserViewTreeNodeChildFactory(TreeNode treeNode, UserViewConfiguration config) {
            this.treeNodeRef = new WeakReference<>(treeNode);
            this.config = config;
        }
        
        @Override
        protected boolean createKeys(List<DomainObject> list) {
            TreeNode treeNode = treeNodeRef.get();
            if (treeNode==null) return false;
            log.trace("Creating children keys for {}",treeNode.getName());   

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
            if (treeNode.hasChildren()) {
                for(Reference reference : treeNode.getChildren()) {
                    if (reference==null) continue;
                    DomainObject obj = map.get(reference.getTargetId());
                    log.trace(reference.getTargetClassName()+"#"+reference.getTargetId()+" -> "+obj);
                    if (obj!=null) {
                        if (config.getVisibleClasses().contains(TreeNode.class) && TreeNode.class.isAssignableFrom(obj.getClass())) {
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
                // TODO: would be nice to do this dynamically, or at least with some sort of annotation
                if (config.getVisibleClasses().contains(TreeNode.class) && TreeNode.class.isAssignableFrom(key.getClass())) {
                    return new UserViewTreeNodeNode((TreeNode)key, config);
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
            refresh(true);
        }
    }
}
