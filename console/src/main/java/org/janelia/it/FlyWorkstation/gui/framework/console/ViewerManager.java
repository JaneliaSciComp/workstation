package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.lang.reflect.Constructor;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.*;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Manages the viewers in the central panel, deciding, for instance, when to use a particular viewer class. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ViewerManager {
	
	private ViewerSplitPanel viewerContainer = new ViewerSplitPanel();
	
	public ViewerManager() {
		ensureViewerClass(viewerContainer.getMainViewerPane(), IconDemoPanel.class);
	}

	public Viewer getActiveViewer() {
		Viewer viewer = viewerContainer.getActiveViewerPane().getViewer();
		if (viewer==null) {
			ensureViewerClass(viewerContainer.getMainViewerPane(), IconDemoPanel.class);
			viewerContainer.setActiveViewerPane(viewerContainer.getMainViewerPane());
			viewer = viewerContainer.getActiveViewerPane().getViewer();
		}
		return viewer;
	}
	
	public Viewer getActiveViewer(Class viewerClass) {
		ViewerPane viewerPane = viewerContainer.getActiveViewerPane();
		ensureViewerClass(viewerPane, viewerClass);
		return viewerPane.getViewer();
	}
	
	public Viewer getMainViewer(Class viewerClass) {
		ViewerPane viewerPane = viewerContainer.getMainViewerPane();
		ensureViewerClass(viewerPane, viewerClass);
		return viewerPane.getViewer();
	}

	public Viewer getSecViewer(Class viewerClass) {
		ViewerPane viewerPane = viewerContainer.getSecViewerPane();
		ensureViewerClass(viewerPane, viewerClass);
		return viewerPane.getViewer();
	}
	
    public Viewer getViewerForCategory(String category) {
    	Viewer mainViewer = getMainViewer(null);
    	Viewer secViewer = getSecViewer(null);
    	if (mainViewer.getSelectionCategory().equals(category)) {
    		return mainViewer;
    	}
    	else if (secViewer!=null && secViewer.getSelectionCategory().equals(category)) {
    		return secViewer;
    	}
    	else {
    		throw new IllegalArgumentException("Unknown viewer category: "+category);
    	}
    }
	
    public Viewer ensureViewerClass(ViewerPane viewerPane, Class<?> viewerClass) {
    	if (viewerClass==null) return null;
		if (viewerPane.getViewer()==null || !viewerPane.getViewer().getClass().isAssignableFrom(viewerClass)) {
			try {
				Constructor constructor = viewerClass.getConstructor(ViewerPane.class);
				Object obj = constructor.newInstance(viewerPane);
				Viewer viewer = (Viewer)obj;
				viewerPane.setViewer(viewer);
				return viewer;
			}
			catch (Exception e) {
				SessionMgr.getSessionMgr().handleException(e);
				return null;
			}
		}
		return viewerPane.getViewer();
    }
    
	public void showEntityInViewerPane(RootedEntity rootedEntity, ViewerPane viewerPane) {
		Class<?> viewerClass = getViewerClass(rootedEntity);
		ensureViewerClass(viewerPane, viewerClass);
		viewerContainer.setActiveViewerPane(viewerPane);
		viewerPane.loadEntity(rootedEntity);
	}

	public void showEntityInActiveViewer(RootedEntity rootedEntity) {
		showEntityInViewerPane(rootedEntity, viewerContainer.getActiveViewerPane());
	}
	
	public void showEntityInMainViewer(RootedEntity rootedEntity) {
		showEntityInViewerPane(rootedEntity, viewerContainer.getMainViewerPane());
	}
	
	public void showEntityInSecViewer(RootedEntity rootedEntity) {
		viewerContainer.setSecViewerVisible(true);
		showEntityInViewerPane(rootedEntity, viewerContainer.getSecViewerPane());
	}
	
	private Class getViewerClass(RootedEntity rootedEntity) {
		Class viewerClass = IconDemoPanel.class;
		String type = rootedEntity.getEntity().getEntityType().getName();
		
		if (EntityConstants.TYPE_ERROR.equals(type)) {
			viewerClass = ErrorViewer.class;
		}
		return viewerClass;
	}

	public ViewerSplitPanel getViewerContainer() {
		return viewerContainer;
	}
}
