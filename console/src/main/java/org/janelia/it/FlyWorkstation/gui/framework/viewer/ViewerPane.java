package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntitySelectionHistory;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * A wrapper around a Viewer that provides a title bar, a close button, and entity navigation history.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ViewerPane extends JPanel {
	
	private static final Font titleLabelFont = new Font("Sans Serif", Font.PLAIN, 12);
	
	private ViewerContainer viewerContainer;
	private EntitySelectionHistory entitySelectionHistory;
	private String selectionCategory;
	private JLabel titleLabel;
	private Viewer viewer;

	protected RootedEntity contextRootedEntity;
	protected List<RootedEntity> rootedAncestors;
	protected SimpleWorker ancestorLoadingWorker;
	
	public ViewerPane(ViewerContainer viewerContainer, String selectionCategory, boolean showHideButton) {
		
		setLayout(new BorderLayout());

		this.viewerContainer = viewerContainer;
		this.selectionCategory = selectionCategory;
		this.entitySelectionHistory = new EntitySelectionHistory();
		
        titleLabel = new JLabel("");
        titleLabel.setBorder(BorderFactory.createEmptyBorder(1, 5, 3, 0));
        titleLabel.setFont(titleLabelFont);
        titleLabel.setHorizontalAlignment(SwingConstants.LEFT);
        
        JPanel mainTitlePane = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 1;
        
        mainTitlePane.add(titleLabel, c);
        
		if (showHideButton) {
	        JButton hideButton = new JButton(Icons.getIcon("close_red.png"));
	        hideButton.setPreferredSize(new Dimension(16, 16));
	        hideButton.setBorderPainted(false);
	        hideButton.setToolTipText("Close this viewer");
	        hideButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					closeButtonPressed();
				}
			});

	        c.gridx = 1;
	        c.gridy = 0;
	        c.insets = new Insets(0, 0, 0, 0);
	        c.fill = GridBagConstraints.NONE;
	        c.anchor = GridBagConstraints.LINE_END;
	        c.weightx = 0;
	        hideButton.setBorder(BorderFactory.createLineBorder(Color.red));
	        mainTitlePane.add(hideButton, c);
		}
		
        add(mainTitlePane, BorderLayout.NORTH);
	}
	
	protected void closeButtonPressed() {
		throw new UnsupportedOperationException("This method has not been implemented for this ViewerPane instance");
	}
	
	public void clearViewer() {
		if (this.viewer!=null) {
			remove(this.viewer);
		}
		viewer = null;
	}

	public void setViewer(Viewer viewer) {
		clearViewer();
		this.viewer = viewer;
		if (viewer!=null) {
			add(viewer, BorderLayout.CENTER);
		}
		revalidate();
		repaint();
	}
	
	public Viewer getViewer() {
		return viewer;
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
	
	public void setTitle(String title) {
		titleLabel.setText(title);
	}

	public void setAsActive() {
		if (viewerContainer!=null) viewerContainer.setActiveViewerPane(this);
	}

	public ViewerContainer getViewerContainer() {
		return viewerContainer;
	}

	public void loadEntity(RootedEntity rootedEntity) {
		loadEntity(rootedEntity, null);
	}
	
	public synchronized void loadEntity(RootedEntity rootedEntity, final Callable<Void> success) {

		this.contextRootedEntity = rootedEntity;
		if (contextRootedEntity==null) return;
		
		Entity entity = contextRootedEntity.getEntity();

		getEntitySelectionHistory().pushHistory(contextRootedEntity.getUniqueId());
		setTitle(entity.getName());

		if (ancestorLoadingWorker != null && !ancestorLoadingWorker.isDone()) {
			ancestorLoadingWorker.disregard();
		}

		ancestorLoadingWorker = new SimpleWorker() {

			private List<RootedEntity> ancestors = new ArrayList<RootedEntity>();
				
			protected void doStuff() throws Exception {
				List<String> uniqueIds = EntityUtils.getPathFromUniqueId(contextRootedEntity.getUniqueId());
				List<Long> entityIds = new ArrayList<Long>();
				for(String uniqueId : uniqueIds) {
					entityIds.add(EntityUtils.getEntityIdFromUniqueId(uniqueId));
				}
				Map<Long,Entity>entityMap = EntityUtils.getEntityMap(ModelMgr.getModelMgr().getEntityByIds(entityIds));

				for(String uniqueId : uniqueIds) {
					Long entityId = EntityUtils.getEntityIdFromUniqueId(uniqueId);
					Entity entity = entityMap.get(entityId);
					if (entity!=null) {
						EntityData entityData = new EntityData();
						entityData.setChildEntity(entity);
						ancestors.add(new RootedEntity(uniqueId, entityData));
					}
				}
				
				Collections.reverse(ancestors);
			}

			protected void hadSuccess() {
				setRootedAncestors(ancestors);
				ancestorLoadingWorker = null;
			}

			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
				
			}
		};
		ancestorLoadingWorker.execute();
		
		viewer.loadEntity(rootedEntity);
	}

	private synchronized void setRootedAncestors(List<RootedEntity> rootedAncestors) {
		this.rootedAncestors = rootedAncestors;
		StringBuffer buf = new StringBuffer();
		for(int i=rootedAncestors.size()-1; i>=0; i--) {
			RootedEntity ancestor = rootedAncestors.get(i);
			if (buf.length()>0) buf.append(" : ");
			buf.append(ancestor.getEntity().getName());
		}
		setTitle(buf.toString());
	}

	public List<RootedEntity> getRootedAncestors() {
		return rootedAncestors;
	}
}