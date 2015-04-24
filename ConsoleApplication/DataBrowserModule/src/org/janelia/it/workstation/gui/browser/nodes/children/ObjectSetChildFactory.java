package org.janelia.it.workstation.gui.browser.nodes.children;

import java.lang.ref.WeakReference;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ObjectSetChildFactory extends TreeNodeChildFactory {

    private final static Logger log = LoggerFactory.getLogger(ObjectSetChildFactory.class);
    
    private final WeakReference<ObjectSet> objectSetRef;
    
    public ObjectSetChildFactory(ObjectSet objectSet) {
        super(null);
        this.objectSetRef = new WeakReference<ObjectSet>(objectSet);
    }
    
    @Override
    protected boolean createKeys(List<DomainObject> list) {
//        ObjectSet objectSet = objectSetRef.get();
//        if (objectSet==null) return false;
//        log.trace("Creating children keys for {}",objectSet.getName());   
//        
//        DomainDAO dao = DomainExplorerTopComponent.getDao();
//        List<DomainObject> members = dao.getDomainObjects(SessionMgr.getSubjectKey(), objectSet);
//        if (members.size()!=objectSet.getNumMembers()) {
//            log.info("Got {} children but expected {}",members.size(),objectSet.getNumMembers());   
//        }
//        
//        Map<Long,DomainObject> map = new HashMap<Long,DomainObject>();
//        for (DomainObject obj : members) {
//            map.put(obj.getId(), obj);
//        }
//        
//        List<DomainObject> temp = new ArrayList<DomainObject>();
//        if (objectSet.getMembers()!=null) {
//            for(Long memberId : objectSet.getMembers()) {
//                DomainObject obj = map.get(memberId);
//                if (obj!=null) {
//                    temp.add(obj);
//                }
//                else {
//                    temp.add(new DeadReference(new Reference(objectSet.getTargetType(), memberId)));
//                }
//            }
//        }
//        
//        list.addAll(temp);
//        return true;
        return false;
    }

    @Override
    protected Node createNodeForKey(DomainObject key) {
//        try {
//            // TODO: would be nice to do this dynamically, 
//            // or at least with some sort of annotation
//            if (ObjectSet.class.isAssignableFrom(key.getClass())) {
//                return new ObjectSetNode(this, (ObjectSet)key);
//            }
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
//        }
//        catch (Exception e) {
//            log.error("Error creating node for key " + key, e);
//        }
        return null;
    }
    
    public void refresh() {
        ObjectSet objectSet = objectSetRef.get();
        log.warn("refreshing {}",objectSet.getName());
        refresh(true);
    }
    
//    public void addChild(final DomainObject domainObject) {
//        final ObjectSet objectSet = objectSetRef.get();
//        if (objectSet==null) {
//            log.warn("Cannot add child to unloaded objectSet");
//            return;
//        }   
//        log.info("Adding child {} to {}",domainObject.getId(),objectSet.getName());
//        
//        DomainDAO dao = DomainExplorerTopComponent.getDao();
//        try {
//            dao.addChild(SessionMgr.getSubjectKey(), objectSet, domainObject);
//        }
//        catch (Exception e) {
//            SessionMgr.getSessionMgr().handleException(e);
//        }
//        
//        refresh();
//    }
//
//    public void removeChild(final DomainObject domainObject) {
//        final ObjectSet objectSet = objectSetRef.get();
//        if (objectSet==null) {
//            log.warn("Cannot remove child from unloaded treeNode");
//            return;
//        }
//        
//        try {
//            log.info("removing child {} from {}", domainObject.getId(), objectSet.getName());
//            DomainDAO dao = DomainExplorerTopComponent.getDao();
//            if (domainObject instanceof DeadReference) {
//                dao.removeReference(SessionMgr.getSubjectKey(), objectSet, ((DeadReference) domainObject).getReference());
//            }
//            else {
//                dao.removeChild(SessionMgr.getSubjectKey(), objectSet, domainObject);
//            }
//        }
//        catch (Exception e) {
//            SessionMgr.getSessionMgr().handleException(e);
//        }
//        
//        refresh();
//    }


}
