package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.Image;

import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;
import org.janelia.model.domain.gui.colordepth.ColorDepthSearch;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;

public class ColorDepthSearchNode extends AbstractDomainObjectNode<ColorDepthSearch> {
        
    public ColorDepthSearchNode(ChildFactory<?> parentChildFactory, ColorDepthSearch mask) throws Exception {
        super(parentChildFactory, Children.LEAF, mask);
    }
    
    public ColorDepthSearch getColorDepthSearch() {
        return getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getColorDepthSearch().getName();
    }
        
    @Override
    public Image getIcon(int type) {
        if (ClientDomainUtils.isOwner(getColorDepthSearch())) {
            return Icons.getIcon("script.png").getImage();
        }
        else {
            return Icons.getIcon("script.png").getImage();
        }
    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }

//    @Override
//    public Action[] getActions(boolean context) {
//        List<Action> actions = new ArrayList<>();
//        actions.add(PopupLabelAction.get());
//        actions.add(null);
//        actions.add(new CopyToClipboardAction("Name", getName()));
//        actions.add(new CopyToClipboardAction("GUID", getId()+""));
//        actions.add(null);
//        actions.add(new OpenInViewerAction());
//        actions.add(new OpenInNewViewerAction());
//        actions.add(null);
//        actions.add(new ViewDetailsAction());
//        actions.add(new ChangePermissionsAction());
//        actions.add(AddToFolderAction.get());
//        actions.add(RenameAction.get());
//        actions.add(RemoveAction.get());
//        actions.add(null);
//        actions.add(SearchHereAction.get());
//        actions.add(DownloadAction.get());
//        return actions.toArray(new Action[actions.size()]);
//    }
}
