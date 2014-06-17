package org.janelia.it.workstation.gui.browser.children;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.NeuronFragment;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.Sample;
import org.janelia.it.jacs.model.domain.TreeNode;
import org.janelia.it.workstation.gui.browser.DomainDAO;
import org.janelia.it.workstation.gui.browser.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.nodes.DeadReference;
import org.janelia.it.workstation.gui.browser.nodes.DeadReferenceNode;
import org.janelia.it.workstation.gui.browser.nodes.NeuronFragmentNode;
import org.janelia.it.workstation.gui.browser.nodes.SampleNode;
import org.janelia.it.workstation.gui.browser.nodes.TreeNodeNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.Utils;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TreeNodeChildFactory extends ChildFactory<DomainObject> {

    private Logger log = LoggerFactory.getLogger(TreeNodeChildFactory.class);
    
    private WeakReference<TreeNode> treeNodeRef;
    
    public TreeNodeChildFactory(TreeNode treeNode) {
        this.treeNodeRef = new WeakReference<TreeNode>(treeNode);
    }
    
    @Override
    protected boolean createKeys(List<DomainObject> list) {
        TreeNode treeNode = treeNodeRef.get();
        if (treeNode==null) return false;
        log.info("Creating children keys for {}",treeNode.getName());   
        
        DomainDAO dao = DomainExplorerTopComponent.getDao();
        List<DomainObject> children = dao.getDomainObjects(SessionMgr.getSubjectKey(), treeNode.getChildren());
        if (children.size()!=treeNode.getChildren().size()) {
            log.info("Did not get all children: {}!={}",children.size(),treeNode.getChildren().size());   
        }
        
        Map<Long,DomainObject> map = new HashMap<Long,DomainObject>();
        for (DomainObject obj : children) {
            map.put(obj.getId(), obj);
        }
        
        //log.info("{} has the following references: ",treeNode.getName());
        for(Reference reference : treeNode.getChildren()) {
            //log.info("  {}#{}",reference.getTargetType(),reference.getTargetId());
            DomainObject obj = map.get(reference.getTargetId());
            if (obj!=null) {
                list.add(obj);
            }
            else {
                list.add(new DeadReference(reference));
            }
        }
        
        return true;
    }

    @Override
    protected Node createNodeForKey(DomainObject key) {
        try {
            if (TreeNode.class.isAssignableFrom(key.getClass())) {
                return new TreeNodeNode(this, (TreeNode) key);
            }
            else if (Sample.class.isAssignableFrom(key.getClass())) {
                return new SampleNode((Sample) key);
            }
            else if (NeuronFragment.class.isAssignableFrom(key.getClass())) {
                return new NeuronFragmentNode(null, (NeuronFragment) key);
            }
            else if (DeadReference.class.isAssignableFrom(key.getClass())) {
                return new DeadReferenceNode((DeadReference) key);
            }
            else {
                log.warn("Cannot handle type: " + key.getClass().getName());
            }
        }
        catch (Exception e) {
            log.error("Error creating node for tree node child " + key.getId(), e);
        }
        return null;
    }
    
    public void refresh() {
        TreeNode treeNode = treeNodeRef.get();
        log.warn("refreshing {}",treeNode.getName());
        refresh(true);
    }
    
    
    public void addChild(final DomainObject domainObject) {
        final TreeNode treeNode = treeNodeRef.get();
        if (treeNode==null) {
            log.warn("Cannot add child to unloaded treeNode");
            return;
        }
        Utils.runOffEDT(new Runnable() {
            public void run() {
                log.warn("adding child {} to {}",domainObject.getId(),treeNode.getName());
                DomainDAO dao = DomainExplorerTopComponent.getDao();
                dao.addChild(SessionMgr.getSubjectKey(), treeNode, domainObject);
            }
        },new Runnable() {
            public void run() {
                refresh();
            }
        });
    }

    public void removeChild(final DomainObject domainObject) {
        final TreeNode treeNode = treeNodeRef.get();
        if (treeNode==null) {
            log.warn("Cannot remove child from unloaded treeNode");
            return;
        }
        Utils.runOffEDT(new Runnable() {
            public void run() {
                log.warn("removing child {} from {}",domainObject.getId(),treeNode.getName());
                DomainDAO dao = DomainExplorerTopComponent.getDao();
                dao.removeChild(SessionMgr.getSubjectKey(), treeNode, domainObject);
            }
        },new Runnable() {
            public void run() {
                refresh();
            }
        });
    }


}
