package org.janelia.it.workstation.gui.browser.gui.editor;

import javax.swing.JMenuItem;

import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.browser.actions.CopyToClipboardAction;
import org.janelia.it.workstation.gui.browser.actions.OpenInFinderAction;
import org.janelia.it.workstation.gui.browser.actions.OpenWithDefaultAppAction;
import org.janelia.it.workstation.gui.browser.gui.support.PopupContextMenu;

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
    
    public void runDefaultAction() {
        String path = run.getError().getFilepath();
        OpenWithDefaultAppAction action = new OpenWithDefaultAppAction(path);
        action.doAction();
    }
    
    protected JMenuItem getTitleItem() {
        String title = run.getParent().getObjective()+" "+StringUtils.splitCamelCase(run.getError().getClassification());
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
