package org.janelia.workstation.browser.gui.components;

import org.openide.windows.TopComponent;

public interface ViewerManager<T extends TopComponent> {

    String getViewerName();
    
    Class<T> getViewerClass();
        
    T getActiveViewer();

    void activate(T viewer);

    boolean isActive(T viewer);
}
