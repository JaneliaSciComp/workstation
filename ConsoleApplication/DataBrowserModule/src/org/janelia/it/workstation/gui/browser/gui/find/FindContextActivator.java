package org.janelia.it.workstation.gui.browser.gui.find;

/**
 * A top component can implement this method to declare itself an activator for its ancestor find contexts. 
 * The ancestor contexts will register themselves with the top component using the included "setFindContext" 
 * method, and in return, the top component is duty-bound to activate them whenever it is activated.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface FindContextActivator {

    public void setFindContext(FindContext findContext);
}
