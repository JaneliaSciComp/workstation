package org.janelia.workstation.browser.nodes;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.janelia.workstation.browser.nb_action.AddToFolderAction;
import org.janelia.workstation.common.nb_action.PopupLabelAction;
import org.janelia.workstation.browser.nb_action.RemoveAction;
import org.janelia.workstation.browser.nb_action.RenameAction;
import org.janelia.workstation.common.actions.CopyToClipboardAction;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.model.domain.workspace.GroupedFolder;
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
    
    @Override
    public Action[] getActions(boolean context) {
        List<Action> actions = new ArrayList<>();
        actions.add(PopupLabelAction.get());
        actions.add(null);
        actions.add(new CopyToClipboardAction("Name", getName()));
        actions.add(new CopyToClipboardAction("GUID", getId()+""));
        actions.add(null);
        actions.add(new OpenInViewerAction());
        actions.add(new OpenInNewViewerAction());
        actions.add(null);
        actions.add(new ViewDetailsAction());
        actions.add(new ChangePermissionsAction());
        actions.add(AddToFolderAction.get());
        actions.add(RenameAction.get());
        actions.add(RemoveAction.get());
        actions.add(null);
//        actions.add(SearchHereAction.get());
//        actions.add(DownloadAction.get());
//        actions.add(ExportFoldersAction.get());
        return actions.toArray(new Action[actions.size()]);
    }
}
