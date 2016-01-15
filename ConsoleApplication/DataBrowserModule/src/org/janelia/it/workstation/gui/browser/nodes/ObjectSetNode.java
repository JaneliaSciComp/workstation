package org.janelia.it.workstation.gui.browser.nodes;


import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.flavors.DomainObjectFlavor;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.util.datatransfer.PasteType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A object set node in the data graph. Supports paste of additional members 
 * through drag and drop. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ObjectSetNode extends DomainObjectNode {
    
    private final static Logger log = LoggerFactory.getLogger(ObjectSetNode.class);
    
    public ObjectSetNode(ChildFactory parentChildFactory, ObjectSet objectSet) throws Exception {
        super(parentChildFactory, Children.LEAF, objectSet);
    }
    
    public ObjectSet getObjectSet() {
        return (ObjectSet)getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getObjectSet().getName();
    }
    
    @Override
    public String getExtraLabel() {
        return "("+getObjectSet().getNumMembers()+")";
    }
    
    @Override
    public Image getIcon(int type) {
        if (!getObjectSet().getOwnerKey().equals(AccessManager.getSubjectKey())) {
            // TODO: add a blue version of this icon
            return Icons.getIcon("folder_blue.png").getImage();
        }
        else {
            return Icons.getIcon("folder_image.png").getImage();
        }
    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }

    @Override
    public PasteType getDropType(final Transferable t, int action, int index) {
        
        final ObjectSet objectSet = getObjectSet();                
                
        if (t.isDataFlavorSupported(DomainObjectFlavor.LIST_FLAVOR)) {
            List<DomainObject> objects;
            try {
                objects = (List<DomainObject>) t.getTransferData(DomainObjectFlavor.LIST_FLAVOR);
            }
            catch (UnsupportedFlavorException | IOException e) {
                log.error("Error getting drop type", e);
                return null;
            }
            
            for(DomainObject domainObject : objects) {
                String className = domainObject.getClass().getName();
                if (objectSet.getClassName()!=null && !className.equals(objectSet.getClassName())) {
                    log.info("{} is incompatible with object set {}",className,objectSet.getId());
                    return null;
                }
                log.trace("Will paste {} on {}", domainObject.getId(), objectSet.getName());
            }
            
            final List<DomainObject> toPaste = objects;
            return new PasteType() {
                @Override
                public String getName() {
                    return "PasteIntoObjectSet";
                }
                @Override
                public Transferable paste() throws IOException {
                    try {
                        DomainModel model = DomainMgr.getDomainMgr().getModel();
                        model.addMembers(objectSet, toPaste);
                    }
                    catch (Exception e) {
                        throw new IOException("Error pasting into object set",e);
                    }
                    return null;
                }
            };
        }
        else {
            log.debug("Transfer data does not support domain object list flavor.");
            return null;
        }
        
    }
}
