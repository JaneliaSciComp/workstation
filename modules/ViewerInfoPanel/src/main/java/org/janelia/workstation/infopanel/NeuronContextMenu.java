package org.janelia.workstation.infopanel;

import javax.swing.*;

import org.janelia.workstation.common.actions.CopyToClipboardAction;
import org.janelia.workstation.common.gui.support.PopupContextMenu;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.action.*;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.eventbus.CreateNeuronReviewEvent;
import org.janelia.workstation.controller.task_workflow.TaskWorkflowViewTopComponent;

import java.awt.event.ActionEvent;

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
        add(getNeuronReviewItem());

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

    protected JMenuItem getNeuronReviewItem() {
        AbstractAction generateReviewPointList = new AbstractAction("Show Neuron Tree") {
            @Override
            public void actionPerformed(ActionEvent e) {
                CreateNeuronReviewEvent neuronReviewEvent = new CreateNeuronReviewEvent(this,
                        tmNeuronMetadata);
                ViewerEventBus.postEvent(neuronReviewEvent);
            }
        };
        return new JMenuItem(generateReviewPointList);
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
