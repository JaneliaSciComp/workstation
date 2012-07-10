package org.janelia.it.FlyWorkstation.gui.framework.session_mgr;

import org.janelia.it.jacs.model.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * The BrowserModel is a model of the views of the browser.  It handles
 * Implicit and Explicit selection, Visible range and the scaffold path.
 * Changes to any of these elements will broadcast events notifing all listeners of
 * the change.
 * <p/>
 * Initially written by: Peter Davies
 */

public class BrowserModel extends GenericModel implements Cloneable {

    private Entity selection;
    private Entity masterEditorEntity;

    public BrowserModel() {
    }  //Constructor can only be called within the package --PED 5/13

    public Entity getCurrentSelection() {
        return selection;
    }

    public void setCurrentSelection(Entity newSelection) {
        if (newSelection != null && newSelection.equals(selection)) return;
        selection = newSelection;
        fireSelectionChangeEvent();
    }

    public void reset() {
        setMasterEditorEntity(null);
        setCurrentSelection(null);
    }

    public void addBrowserModelListener(BrowserModelListener browserModelListener) {
        addBrowserModelListener(browserModelListener, true);
    }

    public void addBrowserModelListener(BrowserModelListener browserModelListener, boolean bringUpToDate) {
        modelListeners.add(browserModelListener);
        if (bringUpToDate) {
            browserModelListener.browserMasterEditorEntityChanged(masterEditorEntity);
            browserModelListener.browserCurrentSelectionChanged(selection);
        }
    }

    public void removeBrowserModelListener(BrowserModelListener browserModelListener) {
        modelListeners.remove(browserModelListener);
    }


    public Entity getMasterEditorEntity() {
        return masterEditorEntity;
    }

    public void setMasterEditorEntity(Entity masterEditorEntity) {
        if (this.masterEditorEntity != null && this.masterEditorEntity.equals(masterEditorEntity)) return;
        this.masterEditorEntity = masterEditorEntity;
        fireMasterEditorEntityChangeEvent();
    }


    public void dispose() {
        fireBrowserClosing();
    }


    public Object clone() {
        BrowserModel browserModel = new BrowserModel();
        browserModel.selection = selection;
        browserModel.masterEditorEntity = masterEditorEntity;
        browserModel.modelListeners = new ArrayList(); //Trash the listener list of the clone
        return browserModel;
    }

    private void fireMasterEditorEntityChangeEvent() {
        BrowserModelListener browserModelListener;
        List listeners = (List) modelListeners.clone();
        for (Object listener : listeners) {
            browserModelListener = (BrowserModelListener) listener;
            browserModelListener.browserMasterEditorEntityChanged(masterEditorEntity);
        }
    }

    private void fireSelectionChangeEvent() {
        BrowserModelListener browserModelListener;
        List listeners = (List) modelListeners.clone();
        for (Object listener : listeners) {
            browserModelListener = (BrowserModelListener) listener;
            browserModelListener.browserCurrentSelectionChanged(selection);
        }
    }

    private void fireBrowserClosing() {
        List listeners = (List) modelListeners.clone();
        for (Object listener : listeners) {
            ((BrowserModelListener) listener).browserClosing();
        }
    }


}
