package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.Image;

import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;
import org.janelia.model.domain.gui.colordepth.ColorDepthMask;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;

public class ColorDepthMaskNode extends AbstractDomainObjectNode<ColorDepthMask> {
        
    public ColorDepthMaskNode(ChildFactory<?> parentChildFactory, ColorDepthMask mask) throws Exception {
        super(parentChildFactory, Children.LEAF, mask);
    }
    
    public ColorDepthMask getColorDepthMask() {
        return getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getColorDepthMask().getName();
    }
        
    @Override
    public Image getIcon(int type) {
        if (ClientDomainUtils.isOwner(getColorDepthMask())) {
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
