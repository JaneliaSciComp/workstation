package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.workstation.gui.browser.actions.CopyToClipboardAction;
import org.janelia.it.workstation.gui.browser.gui.support.PopupContextMenu;
import org.janelia.it.workstation.gui.large_volume_viewer.action.NeuronTagsAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Popup context menu for neurons.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NeuronContextMenu extends PopupContextMenu {

    private final static Logger log = LoggerFactory.getLogger(NeuronContextMenu.class);
    
    private final AnnotationManager annotationMgr;
    private final TmNeuronMetadata tmNeuronMetadata;
    protected boolean multiple = false; // Support multiple selection in the future?
    
    public NeuronContextMenu(AnnotationManager annotationMgr, TmNeuronMetadata tmNeuronMetadata) {
        this.annotationMgr = annotationMgr;
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
        Action action = new AbstractAction("Rename") {
            @Override
            public void actionPerformed(ActionEvent e) {
                annotationMgr.renameNeuron();
            }
        };
        action.setEnabled(annotationMgr.editsAllowed());
        return new JMenuItem(action);
    }

    protected JMenuItem getDeleteNeuronItem() {
        Action action = new AbstractAction("Delete") {
            @Override
            public void actionPerformed(ActionEvent e) {
                annotationMgr.deleteCurrentNeuron();
            }
        };
        action.setEnabled(annotationMgr.editsAllowed());
        return new JMenuItem(action);
    }
    
    protected JMenuItem getShowNeuronItem() {
        if (tmNeuronMetadata.isVisible()) return null;
        Action action = new AbstractAction("Show neuron") {
            @Override
            public void actionPerformed(ActionEvent e) {
                annotationMgr.setNeuronVisibility(true);
            }
        };
        action.setEnabled(annotationMgr.editsAllowed());
        return new JMenuItem(action);
    }

    protected JMenuItem getHideNeuronItem() {
        if (!tmNeuronMetadata.isVisible()) return null;
        Action action = new AbstractAction("Hide neuron") {
            @Override
            public void actionPerformed(ActionEvent e) {
                annotationMgr.setNeuronVisibility(false);
            }
        };
        action.setEnabled(annotationMgr.editsAllowed());
        return new JMenuItem(action);
    }

    protected JMenuItem getHideOthersItem() {
        Action action = new AbstractAction("Hide others") {
            @Override
            public void actionPerformed(ActionEvent e) {
                annotationMgr.hideUnselectedNeurons();
            }
        };
        action.setEnabled(annotationMgr.editsAllowed());
        return new JMenuItem(action);
    }

    protected JMenuItem getChooseStyleItem() {
        Action action = new AbstractAction("Choose neuron style...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                annotationMgr.chooseNeuronStyle();
            }
        };
        action.setEnabled(annotationMgr.editsAllowed());
        return new JMenuItem(action);
    }
    
    protected JMenuItem getEditTagsItem() {
        Action action = new NeuronTagsAction(annotationMgr.getAnnotationModel());
        action.setEnabled(annotationMgr.editsAllowed());
        return new JMenuItem(action);
    }
    
    
    
    
}
