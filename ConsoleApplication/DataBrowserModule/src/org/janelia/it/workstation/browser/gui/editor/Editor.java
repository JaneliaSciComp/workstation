package org.janelia.it.workstation.browser.gui.editor;

/**
 * Basic interface for a pluggable editor. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface Editor {

    public String getName();
    
    public Object getEventBusListener();

    public void activate();
    
    public void deactivate();
}
