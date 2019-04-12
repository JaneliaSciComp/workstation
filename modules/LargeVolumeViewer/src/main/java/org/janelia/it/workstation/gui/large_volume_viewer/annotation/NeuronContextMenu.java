package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import javax.swing.Action;
import javax.swing.JMenuItem;

import org.janelia.workstation.common.actions.CopyToClipboardAction;
import org.janelia.workstation.common.gui.support.PopupContextMenu;
import org.janelia.it.workstation.gui.large_volume_viewer.action.NeuronChooseColorAction;
import org.janelia.it.workstation.gui.large_volume_viewer.action.NeuronDeleteAction;
import org.janelia.it.workstation.gui.large_volume_viewer.action.NeuronExportCurrentAction;
import org.janelia.it.workstation.gui.large_volume_viewer.action.NeuronHideAction;
import org.janelia.it.workstation.gui.large_volume_viewer.action.NeuronHideOthersAction;
import org.janelia.it.workstation.gui.large_volume_viewer.action.NeuronRenameAction;
import org.janelia.it.workstation.gui.large_volume_viewer.action.NeuronShowAction;
import org.janelia.it.workstation.gui.large_volume_viewer.action.NeuronTagsAction;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

/**
 * Popup context menu for neurons.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NeuronContextMenu extends PopupContextMenu {

    private final TmNeuronMetadata tmNeuronMetadata;
    protected boolean multiple = false; // Support multiple selection in the future?
    
    public NeuronContextMenu(TmNeuronMetadata tmNeuronMetadata) {
        this.tmNeuronMetadata = tmNeuronMetadata;
    }

    public void addMenuItems() {

        add(getTitleItem());
        add(getCopyNameToClipboardItem());
        add(getCopyIdToClipboardItem());
        
        setNextAddRequiresSeparator(true);
        add(getRenameNeuronItem());
        add(getDeleteNeuronItem());

        setNextAddRequiresSeparator(true);
        add(getExportSWCItem());
        
        setNextAddRequiresSeparator(true);
        add(getShowNeuronItem());
        add(getHideNeuronItem());
        add(getHideOthersItem());

        setNextAddRequiresSeparator(true);
        add(getChooseStyleItem());
        add(getEditTagsItem());
    }
    
    protected JMenuItem getTitleItem() {
        String name = multiple ? "(Multiple selected)" : tmNeuronMetadata.getName();
        JMenuItem titleMenuItem = new JMenuItem(name);
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
    }

    protected JMenuItem getCopyNameToClipboardItem() {
        if (multiple) return null;
        return new JMenuItem(new CopyToClipboardAction("Name",tmNeuronMetadata.getName()));
    }

    protected JMenuItem getCopyIdToClipboardItem() {
        if (multiple) return null;
        return new JMenuItem(new CopyToClipboardAction("GUID",tmNeuronMetadata.getId().toString()));
    }

    protected JMenuItem getRenameNeuronItem() {
        Action action = new NeuronRenameAction();
        return new JMenuItem(action);
    }

    protected JMenuItem getDeleteNeuronItem() {
        Action action = new NeuronDeleteAction();
        return new JMenuItem(action);
    }
    
    protected JMenuItem getExportSWCItem() {
        Action action = new NeuronExportCurrentAction();
        return new JMenuItem(action);
    }

    protected JMenuItem getShowNeuronItem() {
        Action action = new NeuronShowAction();
        return new JMenuItem(action);
    }

    protected JMenuItem getHideNeuronItem() {        
        Action action = new NeuronHideAction();
        return new JMenuItem(action);
    }

    protected JMenuItem getHideOthersItem() {
        Action action = new NeuronHideOthersAction();
        return new JMenuItem(action);
    }

    protected JMenuItem getChooseStyleItem() {
        Action action = new NeuronChooseColorAction();
        return new JMenuItem(action);
    }
    
    protected JMenuItem getEditTagsItem() {
        Action action = new NeuronTagsAction();
        return new JMenuItem(action);
    }
    
    
    
    
}
