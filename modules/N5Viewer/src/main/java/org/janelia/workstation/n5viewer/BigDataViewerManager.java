package org.janelia.workstation.n5viewer;

import org.janelia.workstation.browser.gui.components.ViewerManager;
import org.janelia.workstation.core.events.Events;

public class BigDataViewerManager implements ViewerManager<BigDataViewerTopComponent> {

    public static BigDataViewerManager instance;
    private BigDataViewerTopComponent activeInstance;

    public static BigDataViewerManager getInstance() {
        if (instance == null) {
            instance = new BigDataViewerManager();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    @Override
    public void activate(BigDataViewerTopComponent instance) {
        activeInstance = instance;
    }

    @Override
    public boolean isActive(BigDataViewerTopComponent instance) {
        return activeInstance == instance;
    }

    @Override
    public BigDataViewerTopComponent getActiveViewer() {
        return activeInstance;
    }

    @Override
    public String getViewerName() {
        return "BigDataViewerTopComponent";
    }

    @Override
    public Class<BigDataViewerTopComponent> getViewerClass() {
        return BigDataViewerTopComponent.class;
    }

}
