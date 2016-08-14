package org.janelia.it.workstation.gui.browser.components;

import org.openide.windows.TopComponent;

public interface ViewerManager<T extends TopComponent> {

    public String getViewerName();
    
    public Class<T> getViewerClass();
        
    public T getActiveViewer();

    public void activate(T viewer);

    public boolean isActive(T viewer);
}
