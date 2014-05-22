package org.janelia.it.workstation.gui.framework.access;

/**
 * Lets a component define its own accessibility in the GUI. If a component implements this interface and 
 * isAccessible return false, then its parent should not display it, and possibly display an error message.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface Accessibility {

	public boolean isAccessible();
}
