package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.lang.reflect.Constructor;
import java.util.concurrent.Callable;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.*;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the viewers in the central panel, deciding, for instance, when to use a particular viewer class. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ViewerManager {

    private static final Logger log = LoggerFactory.getLogger(ViewerManager.class);
    
	private ViewerSplitPanel viewerContainer = new ViewerSplitPanel();
	
	public ViewerManager() {
		ensureViewerClass(viewerContainer.getMainViewerPane(), IconDemoPanel.class);
	}
	
	public void clearAllViewers() {
	    log.debug("Clearing all viewers");
	    if (getMainViewerPane()!=null) {
	        getMainViewerPane().clearViewer();
	    }
	    if (getSecViewerPane()!=null) {
	        getSecViewerPane().closeViewer();
	        viewerContainer.setSecViewerVisible(false);
	    }
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

	public ViewerPane getActiveViewerPane() {
		return viewerContainer.getActiveViewerPane();
	}

	public ViewerPane getMainViewerPane() {
		return viewerContainer.getMainViewerPane();
	}

	public ViewerPane getSecViewerPane() {
		return viewerContainer.getSecViewerPane();
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
		viewerContainer.setSecViewerVisible(true);
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
    
	public void showEntityInViewerPane(RootedEntity rootedEntity, ViewerPane viewerPane, Callable<Void> callable) {
		Class<?> viewerClass = getViewerClass(rootedEntity);
		ensureViewerClass(viewerPane, viewerClass);
		viewerContainer.setActiveViewerPane(viewerPane);
		viewerPane.loadEntity(rootedEntity, callable);
	}

	public void showEntityInActiveViewer(RootedEntity rootedEntity, Callable<Void> callable) {
		showEntityInViewerPane(rootedEntity, viewerContainer.getActiveViewerPane(), callable);
	}
	
	public void showEntityInMainViewer(RootedEntity rootedEntity, Callable<Void> callable) {
		showEntityInViewerPane(rootedEntity, viewerContainer.getMainViewerPane(), callable);
	}
	
	public void showEntityInSecViewer(RootedEntity rootedEntity, Callable<Void> callable) {
		viewerContainer.setSecViewerVisible(true);
		showEntityInViewerPane(rootedEntity, viewerContainer.getSecViewerPane(), callable);
	}
	
	public void showEntityInActiveViewer(RootedEntity rootedEntity) {
		showEntityInActiveViewer(rootedEntity, null);
	}
	
	public void showEntityInMainViewer(RootedEntity rootedEntity) {
		showEntityInMainViewer(rootedEntity, null);
	}
	
	public void showEntityInSecViewer(RootedEntity rootedEntity) {
		showEntityInSecViewer(rootedEntity, null);
	}
	
	private Class getViewerClass(RootedEntity rootedEntity) {
		Class viewerClass = IconDemoPanel.class;
		String type = rootedEntity.getEntity().getEntityType().getName();
		
		if (EntityConstants.TYPE_ERROR.equals(type)) {
			viewerClass = ErrorViewer.class;
		}
        else if (EntityConstants.TYPE_ALIGNMENT_BOARD.equals(type)) {
            viewerClass = AlignmentBoardViewer.class;
        }
		return viewerClass;
	}

	public ViewerSplitPanel getViewerContainer() {
		return viewerContainer;
	}
}
