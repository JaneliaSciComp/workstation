package org.janelia.it.workstation.gui.browser.nodes;


import java.awt.Image;

import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.Children;

/**
 * An object set in the user tree view.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UserViewObjectSetNode extends DomainObjectNode {
            
    public UserViewObjectSetNode(ObjectSet objectSet) {
        super(null, Children.LEAF, objectSet);
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
        return false;
    }
}
