package org.janelia.it.workstation.gui.browser.nodes.children;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.compartments.CompartmentSet;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.screen.FlyLine;
import org.janelia.it.jacs.model.domain.screen.ScreenSample;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.browser.api.DomainDAO;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.model.DeadReference;
import org.janelia.it.workstation.gui.browser.nodes.CompartmentSetNode;
import org.janelia.it.workstation.gui.browser.nodes.DeadReferenceNode;
import org.janelia.it.workstation.gui.browser.nodes.FlyLineNode;
import org.janelia.it.workstation.gui.browser.nodes.LSMImageNode;
import org.janelia.it.workstation.gui.browser.nodes.NeuronFragmentNode;
import org.janelia.it.workstation.gui.browser.nodes.ObjectSetNode;
import org.janelia.it.workstation.gui.browser.nodes.SampleNode;
import org.janelia.it.workstation.gui.browser.nodes.ScreenSampleNode;
import org.janelia.it.workstation.gui.browser.nodes.TreeNodeNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TreeNodeChildFactory extends ChildFactory<DomainObject> {

    private final static Logger log = LoggerFactory.getLogger(TreeNodeChildFactory.class);
    
    private final WeakReference<TreeNode> treeNodeRef;
    
    public TreeNodeChildFactory(TreeNode treeNode) {
        this.treeNodeRef = new WeakReference<TreeNode>(treeNode);
    }
    
    @Override
    protected boolean createKeys(List<DomainObject> list) {
        TreeNode treeNode = treeNodeRef.get();
        if (treeNode==null) return false;
        log.trace("Creating children keys for {}",treeNode.getName());   
        
        DomainDAO dao = DomainExplorerTopComponent.getDao();
        List<DomainObject> children = dao.getDomainObjects(SessionMgr.getSubjectKey(), treeNode.getChildren());
        if (children.size()!=treeNode.getNumChildren()) {
            log.info("Got {} children but expected {}",children.size(),treeNode.getNumChildren());   
        }
        
        Map<Long,DomainObject> map = new HashMap<Long,DomainObject>();
        for (DomainObject obj : children) {
            map.put(obj.getId(), obj);
        }
        
        List<DomainObject> temp = new ArrayList<DomainObject>();
        if (treeNode.getChildren()!=null) {
            for(Reference reference : treeNode.getChildren()) {
                DomainObject obj = map.get(reference.getTargetId());
                if (obj!=null) {
                    if (TreeNode.class.isAssignableFrom(obj.getClass())) {
                        temp.add(obj);
                    }
                    else if (ObjectSet.class.isAssignableFrom(obj.getClass())) {
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
            else {
                return null;
            }
//            else if (Sample.class.isAssignableFrom(key.getClass())) {
//                return new SampleNode(this, (Sample)key);
//            }
//            else if (NeuronFragment.class.isAssignableFrom(key.getClass())) {
//                return new NeuronFragmentNode(this, (NeuronFragment)key);
//            }
//            else if (LSMImage.class.isAssignableFrom(key.getClass())) {
//                return new LSMImageNode(this, (LSMImage)key);
//            }
//            else if (ScreenSample.class.isAssignableFrom(key.getClass())) {
//                return new ScreenSampleNode(this, (ScreenSample)key);
//            }
//            else if (FlyLine.class.isAssignableFrom(key.getClass())) {
//                return new FlyLineNode(this, (FlyLine)key);
//            }
//            else if (CompartmentSet.class.isAssignableFrom(key.getClass())) {
//                return new CompartmentSetNode(this, (CompartmentSet)key);
//            }
//            else if (DeadReference.class.isAssignableFrom(key.getClass())) {
//                return new DeadReferenceNode(this, (DeadReference)key);
//            }
//            else {
//                log.warn("Cannot handle type: " + key.getClass().getName());
//            }
        }
        catch (Exception e) {
            log.error("Error creating node for key " + key, e);
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
        log.info("Adding child {} to {}",domainObject.getId(),treeNode.getName());
        
        DomainDAO dao = DomainExplorerTopComponent.getDao();
        try {
            dao.addChild(SessionMgr.getSubjectKey(), treeNode, domainObject);
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
        
        refresh();

//            SimpleWorker worker = new SimpleWorker() {
//                @Override
//                protected void doStuff() throws Exception {
//                }
//                @Override
//                protected void hadSuccess() {
//                    refresh();
//                }
//                @Override
//                protected void hadError(Throwable error) {
//                    SessionMgr.getSessionMgr().handleException(error);
//                }
//            };
//            worker.execute();
    }

    public void removeChild(final DomainObject domainObject) {
        final TreeNode treeNode = treeNodeRef.get();
        if (treeNode==null) {
            log.warn("Cannot remove child from unloaded treeNode");
            return;
        }
        
        try {
            log.info("removing child {} from {}", domainObject.getId(), treeNode.getName());
            DomainDAO dao = DomainExplorerTopComponent.getDao();
            if (domainObject instanceof DeadReference) {
                dao.removeReference(SessionMgr.getSubjectKey(), treeNode, ((DeadReference) domainObject).getReference());
            }
            else {
                dao.removeChild(SessionMgr.getSubjectKey(), treeNode, domainObject);
            }
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
        
        refresh();
                
//        SimpleWorker worker = new SimpleWorker() {
//            @Override
//            protected void doStuff() throws Exception {
//            }
//            @Override
//            protected void hadSuccess() {
//                log.info("refreshing view after removing child");
//                refresh();
//            }
//            @Override
//            protected void hadError(Throwable error) {
//                SessionMgr.getSessionMgr().handleException(error);
//            }
//        };
//        worker.execute();
    }


}
