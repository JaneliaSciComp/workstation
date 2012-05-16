package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

import org.janelia.it.FlyWorkstation.gui.framework.outline.EntitySelectionHistory;
import org.janelia.it.FlyWorkstation.gui.framework.outline.Refreshable;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * A viewer panel that is refreshable and can be placed inside a ViewerContainer. Has its own entity selection 
 * category and associated selection history. 
 * 
 * A viewer must also be able to lookup and return the Entities and RootedEntities that it is currently displaying.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class Viewer extends JPanel implements Refreshable {

	private ViewerContainer viewerContainer;
	private String selectionCategory;
	private EntitySelectionHistory entitySelectionHistory;

	public Viewer(String selectionCategory) {
		this(null, selectionCategory);
	}
	
	public Viewer(ViewerContainer viewerContainer, String selectionCategory) {
		this.viewerContainer = viewerContainer;
		this.selectionCategory = selectionCategory;
		this.entitySelectionHistory = new EntitySelectionHistory();

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				setAsActive();
			}
		});
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
	
	/**
	 * Returns the RootedEntity with the given uniqueId, assuming that its currently loaded in the viewer.
	 * @param uniqueId
	 * @return
	 */
	public abstract RootedEntity getRootedEntityById(String uniqueId);
	
	/**
	 * Returns the Entity with the given id, assuming that its currently loaded in the viewer.
	 * @param id
	 * @return
	 */
	public abstract Entity getEntityById(String id);
	
}
