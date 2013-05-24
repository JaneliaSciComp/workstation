package org.janelia.it.FlyWorkstation.gui.framework.outline;

/**
 * A view that can be activated and deactivated.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface ActivatableView {

	/**
	 * Activate this view and start listening to any relevant events. 
	 */
	public void activate();
	
	/**
	 * Deactivate and clean up any resources and listeners.
	 */
	public void deactivate();
}
