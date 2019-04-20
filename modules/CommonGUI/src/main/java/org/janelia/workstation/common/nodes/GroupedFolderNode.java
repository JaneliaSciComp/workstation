package org.janelia.workstation.common.nodes;

import java.awt.Image;

import org.janelia.model.domain.workspace.GroupedFolder;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;

/**
 * A grouped folder in the data graph. Opens in a special viewer, but it doesn't show its children as nodes. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GroupedFolderNode extends AbstractDomainObjectNode<GroupedFolder> {
        
    public GroupedFolderNode(ChildFactory<?> parentChildFactory, GroupedFolder groupedFolder) {
        super(parentChildFactory, Children.LEAF, groupedFolder);
    }
    
    public GroupedFolder getGroupedFolder() {
        return getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getDomainObject().getName();
    }

    @Override
    public Image getIcon(int type) {
        if (ClientDomainUtils.isOwner(getGroupedFolder())) {
            return Icons.getIcon("folder_database.png").getImage();
        }
        else {
            return Icons.getIcon("folder_blue_database.png").getImage();
        }
    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }
}
