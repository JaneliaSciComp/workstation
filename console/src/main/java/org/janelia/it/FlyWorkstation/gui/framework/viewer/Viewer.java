package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import javax.swing.JPanel;

import org.janelia.it.FlyWorkstation.gui.framework.outline.Refreshable;

/**
 * A viewer panel that is refreshable and can be placed inside a ViewerSplitPanel. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class Viewer extends JPanel implements Refreshable {

	private ViewerSplitPanel viewerContainer;
	
	public Viewer(ViewerSplitPanel viewerContainer) {
		this.viewerContainer = viewerContainer;
	}
	
	/**
	 * Returns the selection category of this viewer in the EntitySelectionModel.
	 * @return EntitySelectionModel.CATEGORY_*
	 */
	public abstract String getSelectionCategory();
	
	public void setAsActive() {
		viewerContainer.setAsActive(this);
	}
	
	public void setTitle(String title) {
		viewerContainer.setTitle(this, title);
	}
	
	
}
