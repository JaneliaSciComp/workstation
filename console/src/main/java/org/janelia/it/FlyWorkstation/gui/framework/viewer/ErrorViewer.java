
package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntitySelectionHistory;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.BrowserModel;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.MouseForwarder;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * This viewer displays Error entities.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ErrorViewer extends Viewer {

	protected ViewerToolbar toolbar;
	protected JTextArea textArea;
	protected JScrollPane scrollPane;
	protected RootedEntity contextRootedEntity;
	
	public ErrorViewer(ViewerPane viewerPane) {
		super(viewerPane);
		
		setBorder(BorderFactory.createEmptyBorder());
		setLayout(new BorderLayout());
		setFocusable(true);
		
		toolbar = createToolbar();
		toolbar.addMouseListener(new MouseForwarder(this, "JToolBar->ErrorViewer"));
		add(toolbar, BorderLayout.NORTH);
		
		this.textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setLineWrap(false);
		
		this.scrollPane = new JScrollPane(textArea);
		add(scrollPane, BorderLayout.CENTER);

		textArea.addMouseListener(new MouseForwarder(this, "JTextPane->ErrorViewer"));
        
        SessionMgr.getSessionMgr().addSessionModelListener(new SessionModelListener() {
            @Override
            public void browserAdded(BrowserModel browserModel) {
            }

            @Override
            public void browserRemoved(BrowserModel browserModel) {
            }

            @Override
            public void sessionWillExit() {
            }

            @Override
            public void modelPropertyChanged(Object key, Object oldValue, Object newValue) {
                if(key == "console.serverLogin"){
                    ErrorViewer.this.clear();
                }
            }
        });
	}

	protected ViewerToolbar createToolbar() {

		return new ViewerToolbar() {

			protected void goBack() {
				final EntitySelectionHistory history = getViewerPane().getEntitySelectionHistory();
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						history.goBack();
					}
				});
			}

			protected void goForward() {
				final EntitySelectionHistory history = getViewerPane().getEntitySelectionHistory();
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						history.goForward();
					}
				});
			}

			protected void refresh() {
				ErrorViewer.this.refresh();
			}
			
			protected JPopupMenu getPopupPathMenu() {
				List<RootedEntity> rootedAncestors = getViewerPane().getRootedAncestors();
				if (rootedAncestors==null) return null;
				final JPopupMenu pathMenu = new JPopupMenu();
				for (final RootedEntity ancestor : rootedAncestors) {
					JMenuItem pathMenuItem = new JMenuItem(ancestor.getEntity().getName(), Icons.getIcon(ancestor.getEntity()));
					pathMenuItem.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, ancestor.getUniqueId(), true);
						}
					});
					pathMenuItem.setEnabled(pathMenu.getComponentCount()>0);
					pathMenu.add(pathMenuItem);
				}
				return pathMenu;
			}
			
		};
	}
	
	@Override
	public void showLoadingIndicator() {
		removeAll();
		add(new JLabel(Icons.getLoadingIcon()));
		this.updateUI();
	}
	
	@Override
	public void loadEntity(RootedEntity rootedEntity) {
		loadEntity(rootedEntity, null);
	}
	
	public synchronized void loadEntity(RootedEntity rootedEntity, final Callable<Void> success) {
		
		this.contextRootedEntity = rootedEntity;
		if (contextRootedEntity==null) return;

		showLoadingIndicator();
		
		Entity entity = contextRootedEntity.getEntity();
		
		List<EntityData> eds = entity.getOrderedEntityData();
		List<EntityData> children = new ArrayList<EntityData>();
		for(EntityData ed : eds) {
			Entity child = ed.getChildEntity();
			if (!EntityUtils.isHidden(ed) && child!=null) {
				children.add(ed);
			}
		}
		
		textArea.setText(entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_MESSAGE));
		textArea.setCaretPosition(0);

		// Update back/forward navigation
		EntitySelectionHistory history = getViewerPane().getEntitySelectionHistory();
		toolbar.getPrevButton().setEnabled(history.isBackEnabled());
		toolbar.getNextButton().setEnabled(history.isNextEnabled());

		add(toolbar, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
	}
	
	@Override
	public void refresh() {
		refresh(null);
	}
	
	public void refresh(final Callable<Void> successCallback) {

		if (contextRootedEntity==null) return;

		showLoadingIndicator();
		
		SimpleWorker refreshWorker = new SimpleWorker() {

			RootedEntity rootedEntity = contextRootedEntity;
			
			protected void doStuff() throws Exception {
				Entity entity = ModelMgr.getModelMgr().getEntityById(rootedEntity.getEntity().getId()+"");
				if (entity==null) return;
				ModelMgrUtils.loadLazyEntity(entity, false);
				rootedEntity.getEntityData().setChildEntity(entity);
			}

			protected void hadSuccess() {
				if (rootedEntity.getEntity()==null) {
					clear();
				}
				else {
					loadEntity(rootedEntity);	
				}
			}

			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};

		refreshWorker.execute();
	}

	public synchronized void clear() {
		this.contextRootedEntity = null;
		
		getViewerPane().setTitle("");
		removeAll();
		
		revalidate();
		repaint();
	}

	@Override
	public List<RootedEntity> getRootedEntities() {
		List<RootedEntity> rootedEntities = new ArrayList<RootedEntity>();
		rootedEntities.add(contextRootedEntity);
		return rootedEntities;
	}

	@Override
	public List<RootedEntity> getSelectedEntities() {
		List<RootedEntity> rootedEntities = new ArrayList<RootedEntity>();
		rootedEntities.add(contextRootedEntity);
		return rootedEntities;
	}
	
	@Override
	public RootedEntity getRootedEntityById(String uniqueId) {
		return contextRootedEntity.getUniqueId().equals(uniqueId)?contextRootedEntity:null;
	}

	@Override
	public Entity getEntityById(String id) {
		return contextRootedEntity.getId().equals(id)?contextRootedEntity.getEntity():null;
	}
}
