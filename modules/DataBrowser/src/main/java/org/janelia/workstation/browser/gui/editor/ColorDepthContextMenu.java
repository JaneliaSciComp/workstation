package org.janelia.workstation.browser.gui.editor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import org.janelia.workstation.common.actions.PopupLabelActionBuilder;
import org.janelia.workstation.common.gui.support.PopupContextMenu;
import org.janelia.workstation.core.actions.DomainObjectAcceptorHelper;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.core.model.descriptors.DescriptorUtils;
import org.janelia.workstation.core.model.descriptors.ResultArtifactDescriptor;
import org.janelia.workstation.common.actions.CopyToClipboardAction;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.browser.gui.colordepth.CreateMaskFromSampleAction;
import org.janelia.workstation.browser.gui.hud.Hud;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.ObjectiveSample;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SampleAlignmentResult;

/**
 * Right-click context menu for color depth images presented in the Sample Editor. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthContextMenu extends PopupContextMenu {

    private final Sample sample;
    private final ArtifactDescriptor resultDescriptor;
    private final SampleAlignmentResult result;
    private final FileType fileType;

    public ColorDepthContextMenu(Sample sample, ArtifactDescriptor resultDescriptor, 
            SampleAlignmentResult result, FileType fileType) {
        this.sample = sample;
        this.resultDescriptor = resultDescriptor;
        this.result = result;
        this.fileType = fileType;
    }
    
    public void addMenuItems() {
        
        if (result==null) {
            JMenuItem titleMenuItem = new JMenuItem("Nothing selected");
            titleMenuItem.setEnabled(false);
            add(titleMenuItem);
            return;
        }

        add((new PopupLabelActionBuilder()).getAction(result));
        add((new CopyToClipboardAction("Name", result.getName())));
        add((new CopyToClipboardAction("GUID", result.getId().toString())));
        
        setNextAddRequiresSeparator(true);
        add(getCreateColorDepthMaskItem());
        
        setNextAddRequiresSeparator(true);
        add(getHudMenuItem());
        addSeparator();
        addSeparator();

        for (Component currentContextMenuItem : DomainObjectAcceptorHelper.getCurrentContextMenuItems()) {
            add(currentContextMenuItem);
        }
        
    }
    
    public void runDefaultAction() {
        
    }
    
    protected JMenuItem getTitleItem() {
        String name = result.getName();
        JMenuItem titleMenuItem = new JMenuItem(name);
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
    }
    
    protected JMenuItem getCopyNameToClipboardItem() {
        return getNamedActionItem(new CopyToClipboardAction("Name",result.getName()));
    }

    protected JMenuItem getCopyIdToClipboardItem() {
        return getNamedActionItem(new CopyToClipboardAction("GUID",result.getId().toString()));
    }

    protected JMenuItem getCreateColorDepthMaskItem() {
    
        JMenuItem menuItem = getNamedActionItem(new CreateMaskFromSampleAction(sample, resultDescriptor, fileType.name()));

        HasFiles fileProvider = DescriptorUtils.getResult(sample, resultDescriptor);
        if (fileProvider==null) {
            menuItem.setEnabled(false);
        }
        
        return menuItem;
    }

    protected JMenuItem getHudMenuItem() {
        JMenuItem toggleHudMI = new JMenuItem("Show in Lightbox");
        toggleHudMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActivityLogHelper.logUserAction("ColorDepthContextMenu.showInLightbox", result);
                ObjectiveSample objectiveSample = result.getParentRun().getParent();
                Sample sample = objectiveSample.getParent();
                ArtifactDescriptor descriptor = new ResultArtifactDescriptor(result);
                Hud.getSingletonInstance().setObjectAndToggleDialog(sample, descriptor, fileType.name(), true, true);
            }
        });

        return toggleHudMI;
    }
}
