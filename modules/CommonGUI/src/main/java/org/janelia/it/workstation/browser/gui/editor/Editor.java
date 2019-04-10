package org.janelia.it.workstation.browser.gui.editor;

/**
 * Basic interface for a pluggable editor. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface Editor {

    String getName();
    
    Object getEventBusListener();

    void activate();
    
    void deactivate();
}
