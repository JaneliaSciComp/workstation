package org.janelia.it.workstation.browser.nodes;


import java.awt.Image;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.workspace.GroupedFolder;
import org.janelia.model.domain.workspace.TreeNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A tree node (i.e. Folder) in the data graph.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UserViewTreeNodeNode extends AbstractDomainObjectNode<TreeNode> {
        
    private final static Logger log = LoggerFactory.getLogger(UserViewTreeNodeNode.class);
    
    private final UserViewTreeNodeChildFactory childFactory;
    @SuppressWarnings("unused")
    private final UserViewConfiguration config;
    
    public UserViewTreeNodeNode(TreeNode treeNode, UserViewConfiguration config) {
        this(null, new UserViewTreeNodeChildFactory(treeNode, config), treeNode, config);
    }
    
    private UserViewTreeNodeNode(UserViewTreeNodeChildFactory parentChildFactory, final UserViewTreeNodeChildFactory childFactory, TreeNode treeNode, UserViewConfiguration config) {
        super(parentChildFactory, childFactory.hasNodeChildren()?Children.create(childFactory, false):Children.LEAF, treeNode);
        this.childFactory = childFactory;
        this.config = config;
    }
    
    public TreeNode getTreeNode() {
        return getDomainObject();
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

        public boolean hasNodeChildren() {
            TreeNode treeNode = treeNodeRef.get();
            for(Reference reference : treeNode.getChildren()) {
                if (reference==null) continue;
                if (reference.getTargetClassName().equals("TreeNode")) {
                    return true;
                }
                else if (reference.getTargetClassName().equals("Filter")) {
                    return true;
                }
                else if (reference.getTargetClassName().equals("GroupedFolder")) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected boolean createKeys(List<DomainObject> list) {
            try {
                TreeNode treeNode = treeNodeRef.get();
                if (treeNode == null) return false;
                log.trace("Creating children keys for {}", treeNode.getName());

                DomainModel model = DomainMgr.getDomainMgr().getModel();
                List<DomainObject> children = model.getDomainObjects(treeNode.getChildren());
                if (children.size() != treeNode.getNumChildren()) {
                    log.info("Got {} children but expected {}", children.size(), treeNode.getNumChildren());
                }

                Map<Long, DomainObject> map = new HashMap<>();
                for (DomainObject obj : children) {
                    map.put(obj.getId(), obj);
                }

                List<DomainObject> temp = new ArrayList<>();
                if (treeNode.hasChildren()) {
                    for (Reference reference : treeNode.getChildren()) {
                        if (reference == null) continue;
                        DomainObject obj = map.get(reference.getTargetId());
                        log.trace(reference.getTargetClassName() + "#" + reference.getTargetId() + " -> " + obj);
                        if (obj != null) {
                            if (config.getVisibleClasses().contains(TreeNode.class) && TreeNode.class.isAssignableFrom(obj.getClass())) {
                                temp.add(obj);
                            }
                            if (config.getVisibleClasses().contains(GroupedFolder.class) && GroupedFolder.class.isAssignableFrom(obj.getClass())) {
                                temp.add(obj);
                            }
                        }
                    }
                }

                list.addAll(temp);
                
            } 
            catch (Exception e) {
                ConsoleApp.handleException(e);
            }
            
            return true;
        }

        @Override
        protected Node createNodeForKey(DomainObject key) {
            try {
                // TODO: would be nice to do this dynamically, or at least with some sort of annotation
                if (config.getVisibleClasses().contains(TreeNode.class) && TreeNode.class.isAssignableFrom(key.getClass())) {
                    return new UserViewTreeNodeNode((TreeNode)key, config);
                }
                if (config.getVisibleClasses().contains(GroupedFolder.class) && GroupedFolder.class.isAssignableFrom(key.getClass())) {
                    // Should use TreeNodeObjectHelper to generate these
                    return new GroupedFolderNode(this, (GroupedFolder)key);
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
