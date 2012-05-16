package org.janelia.it.FlyWorkstation.gui.framework.viewer;

/**
 * A container for one or more viewers which supports maintaining an "active" viewer, and dynamic titles for viewers.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface ViewerContainer {
	
	/**
	 * Set the given viewer as active. The viewer must be contained in the container.
	 * @param viewer
	 */
	public void setAsActive(Viewer viewer);
	
	/**
	 * Set the title of the given viewer. The viewer must be contained in the container.
	 * @param viewer
	 * @param title
	 */
	public void setTitle(Viewer viewer, String title);

}
