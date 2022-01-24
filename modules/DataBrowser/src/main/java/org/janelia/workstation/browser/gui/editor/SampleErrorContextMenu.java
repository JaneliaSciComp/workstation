package org.janelia.workstation.browser.gui.editor;

import org.janelia.model.domain.sample.SamplePipelineRun;
import org.janelia.workstation.browser.actions.OpenInFinderAction;
import org.janelia.workstation.browser.actions.OpenWithDefaultAppAction;
import org.janelia.workstation.common.actions.CopyToClipboardAction;
import org.janelia.workstation.common.gui.support.PopupContextMenu;
import org.janelia.workstation.core.util.StringUtilsExtra;

import javax.swing.*;

/**
 * Right-click context menu for sample pipeline errors presented in the Sample Editor. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleErrorContextMenu extends PopupContextMenu {

    private final SamplePipelineRun run;

    public SampleErrorContextMenu(SamplePipelineRun run) {
        if (!run.hasError()) {
            throw new IllegalArgumentException("Cannot create context menu for non-error run");
        }
        this.run = run;
    }
    
    public void addMenuItems() {
        
        if (run==null) {
            JMenuItem titleMenuItem = new JMenuItem("Nothing selected");
            titleMenuItem.setEnabled(false);
            add(titleMenuItem);
            return;
        }
        
        add(getTitleItem());
        add(getCopyNameToClipboardItem());
        
        setNextAddRequiresSeparator(true);
        add(getOpenInFinderItem());
        add(getOpenWithAppItem());
    }

    protected JMenuItem getTitleItem() {
        String title = run.getParent().getObjective()+" "+ StringUtilsExtra.splitCamelCase(run.getError().getClassification());
        JMenuItem titleMenuItem = new JMenuItem(title);
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
    }
    
    protected JMenuItem getCopyNameToClipboardItem() {
        return getNamedActionItem(new CopyToClipboardAction("Name",run.getName()));
    }
    
    protected JMenuItem getOpenInFinderItem() {
        if (!OpenInFinderAction.isSupported()) return null;
        String path = run.getError().getFilepath();
        if (path==null) return null;
        return getNamedActionItem(new OpenInFinderAction(path));
    }

    protected JMenuItem getOpenWithAppItem() {
        if (!OpenWithDefaultAppAction.isSupported()) return null;
        String path = run.getError().getFilepath();
        if (path==null) return null;
        return getNamedActionItem(new OpenWithDefaultAppAction(path));
    }
}
