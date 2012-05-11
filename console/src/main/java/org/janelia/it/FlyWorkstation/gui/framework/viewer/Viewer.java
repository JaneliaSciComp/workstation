package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import javax.swing.JPanel;

import org.janelia.it.FlyWorkstation.gui.framework.outline.EntitySelectionHistory;
import org.janelia.it.FlyWorkstation.gui.framework.outline.Refreshable;

/**
 * A viewer panel that is refreshable and can be placed inside a ViewerSplitPanel. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class Viewer extends JPanel implements Refreshable {

	private ViewerSplitPanel viewerContainer;
	private String selectionCategory;
	private EntitySelectionHistory entitySelectionHistory;

	public Viewer(String selectionCategory) {
		this.viewerContainer = null;
		this.selectionCategory = selectionCategory;
		this.entitySelectionHistory = new EntitySelectionHistory();
	}
	
	public Viewer(ViewerSplitPanel viewerContainer, String selectionCategory) {
		this.viewerContainer = viewerContainer;
		this.selectionCategory = selectionCategory;
		this.entitySelectionHistory = new EntitySelectionHistory();
	}
	
	/**
	 * Returns the selection category of this viewer in the EntitySelectionModel.
	 * @return EntitySelectionModel.CATEGORY_*
	 */
	public String getSelectionCategory() {
		return selectionCategory;
	}

	public EntitySelectionHistory getEntitySelectionHistory() {
		return entitySelectionHistory;
	}

	public void setAsActive() {
		if (viewerContainer!=null) viewerContainer.setAsActive(this);
	}
	
	public void setTitle(String title) {
		if (viewerContainer!=null) viewerContainer.setTitle(this, title);
	}
	
	public abstract RootedEntity getRootedEntityById(String id);
	
}
