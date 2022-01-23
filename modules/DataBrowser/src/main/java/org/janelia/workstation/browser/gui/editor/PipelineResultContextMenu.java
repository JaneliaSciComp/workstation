package org.janelia.workstation.browser.gui.editor;

import org.janelia.workstation.common.gui.support.PopupContextMenu;
import org.janelia.workstation.core.actions.ContextualNodeActionUtils;

import java.awt.*;

/**
 * Right-click context menu for color depth images presented in the Sample Editor. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PipelineResultContextMenu extends PopupContextMenu {

    PipelineResultContextMenu() {
    }
    
//    public void addMenuItems() {
//
//        if (result==null) {
//            JMenuItem titleMenuItem = new JMenuItem("Nothing selected");
//            titleMenuItem.setEnabled(false);
//            add(titleMenuItem);
//            return;
//        }
//
//        add(new PopupLabelAction(Collections.singletonList(result)));
//        add((new CopyToClipboardAction("Name", result.getName())));
//        add((new CopyToClipboardAction("GUID", result.getId().toString())));
//
//        setNextAddRequiresSeparator(true);
//        add(getCreateColorDepthMaskItem());
//
//        setNextAddRequiresSeparator(true);
//        add(getHudMenuItem());
//    }
//
//    public void runDefaultAction() {
//
//    }
//
//    protected JMenuItem getTitleItem() {
//        String name = result.getName();
//        JMenuItem titleMenuItem = new JMenuItem(name);
//        titleMenuItem.setEnabled(false);
//        return titleMenuItem;
//    }
//
//    protected JMenuItem getCopyNameToClipboardItem() {
//        return getNamedActionItem(new CopyToClipboardAction("Name",result.getName()));
//    }
//
//    protected JMenuItem getCopyIdToClipboardItem() {
//        return getNamedActionItem(new CopyToClipboardAction("GUID",result.getId().toString()));
//    }
//
//    protected JMenuItem getCreateColorDepthMaskItem() {
//
//        // TODO: need to define a plugin infrastructure for this
//        JMenuItem menuItem = getNamedActionItem(new CreateMaskFromSampleAction(sample, resultDescriptor, fileType.name()));
//
//        HasFiles fileProvider = DescriptorUtils.getResult(sample, resultDescriptor);
//        if (fileProvider==null) {
//            menuItem.setEnabled(false);
//        }
//
//        return menuItem;
//        return null;
//    }
//
//    protected JMenuItem getHudMenuItem() {
//        JMenuItem toggleHudMI = new JMenuItem("Show in Lightbox");
//        toggleHudMI.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                ActivityLogHelper.logUserAction("ColorDepthContextMenu.showInLightbox", result);
//                ObjectiveSample objectiveSample = result.getParentRun().getParent();
//                Sample sample = objectiveSample.getParent();
//                ArtifactDescriptor descriptor = new ResultArtifactDescriptor(result);
//                Hud.getSingletonInstance().setObjectAndToggleDialog(sample, descriptor, fileType.name(), true, true);
//            }
//        });
//
//        return toggleHudMI;
//    }

    public void addMenuItems() {
        for (Component currentContextMenuItem : ContextualNodeActionUtils.getCurrentContextMenuItems()) {
            add(currentContextMenuItem);
        }
    }
}
