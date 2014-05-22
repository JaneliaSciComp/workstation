package org.janelia.it.workstation.gui.framework.console;

import java.lang.reflect.Constructor;
import java.util.concurrent.Callable;

import org.janelia.it.workstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.workstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.outline.EntitySelectionHistory;
import org.janelia.it.workstation.gui.framework.outline.EntityViewerState;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.viewer.IconDemoPanel;
import org.janelia.it.workstation.gui.framework.viewer.TextFileViewer;
import org.janelia.it.workstation.gui.framework.viewer.Viewer;
import org.janelia.it.workstation.gui.framework.viewer.ViewerPane;
import org.janelia.it.workstation.gui.framework.viewer.ViewerSplitPanel;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.jacs.model.entity.Entity;
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

    private final ModelMgrAdapter modelMgrObserver;

    private boolean viewersLinked = false;

    public ViewerManager() {
        ensureViewerClass(viewerContainer.getMainViewerPane(), IconDemoPanel.class);

        this.modelMgrObserver = new ModelMgrAdapter() {

            @Override
            public void entitySelected(String category, String entityId, boolean clearAll) {
                if (!viewersLinked) {
                    return;
                }
                if (!category.equals(EntitySelectionModel.CATEGORY_MAIN_VIEW)) {
                    return;
                }
                if (!viewerContainer.isSecViewerVisible()) {
                    return;
                }
                RootedEntity rootedEntity = getMainViewerPane().getViewer().getRootedEntityById(entityId);
                if (rootedEntity != null) {
                    showEntityInSecViewer(rootedEntity, null);
                }
            }

            @Override
            public void entityDeselected(String category, String entityId) {
            }
        };

        ModelMgr.getModelMgr().addModelMgrObserver(modelMgrObserver);
    }

    public void clearAllViewers() {
        log.debug("Clearing all viewers");
        if (getMainViewerPane() != null) {
            getMainViewerPane().clearViewer();
        }
        if (getSecViewerPane() != null) {
            getSecViewerPane().closeViewer();
            viewerContainer.setSecViewerVisible(false);
        }
    }

    public Viewer getActiveViewer() {
        Viewer viewer = viewerContainer.getActiveViewerPane().getViewer();
        if (viewer == null) {
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
        else if (secViewer != null && secViewer.getSelectionCategory().equals(category)) {
            return secViewer;
        }
        else {
            throw new IllegalArgumentException("Unknown viewer category: " + category);
        }
    }

    public final Viewer ensureViewerClass(ViewerPane viewerPane, Class<?> viewerClass) {
        if (viewerClass == null) {
            return null;
        }
        if (viewerPane.getViewer() == null || !viewerPane.getViewer().getClass().isAssignableFrom(viewerClass)) {
            try {
                Constructor constructor = viewerClass.getConstructor(ViewerPane.class);
                Object obj = constructor.newInstance(viewerPane);
                Viewer viewer = (Viewer) obj;
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
        
        RootedEntity currContextRE = viewerPane.getViewer().getContextRootedEntity();
        if (currContextRE!=null && currContextRE.getId().equals(rootedEntity.getId())) {
            log.trace("Viewer has already loaded entity: "+rootedEntity.getName());
            return;
        }
        
        saveViewerState();
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

    public void showLoadingIndicatorInInspector() {
        SessionMgr.getBrowser().getEntityDetailsOutline().showLoadingIndicator();
    }

    public void showLoadingIndicatorInActiveViewer() {
        Viewer viewer = SessionMgr.getBrowser().getViewerManager().getActiveViewerPane().getViewer();
        if (viewer != null) {
            viewer.showLoadingIndicator();
        }
    }

    public void showLoadingIndicatorInMainViewer() {
        Viewer viewer = SessionMgr.getBrowser().getViewerManager().getMainViewerPane().getViewer();
        if (viewer != null) {
            viewer.showLoadingIndicator();
        }
    }

    public void showLoadingIndicatorInSecViewer() {
        Viewer viewer = SessionMgr.getBrowser().getViewerManager().getSecViewerPane().getViewer();
        if (viewer != null) {
            viewer.showLoadingIndicator();
        }
    }

    public void showEntityInInspector(Entity entity) {
        SessionMgr.getBrowser().getEntityDetailsOutline().loadEntity(entity);
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
        String type = rootedEntity.getEntity().getEntityTypeName();

        if (EntityConstants.TYPE_ERROR.equals(type)) {
            viewerClass = TextFileViewer.class;
        }
        else if (EntityConstants.TYPE_TEXT_FILE.equals(type)) {
            viewerClass = TextFileViewer.class;
        }

        return viewerClass;
    }

    public ViewerSplitPanel getViewerContainer() {
        return viewerContainer;
    }

    public boolean isViewersLinked() {
        return viewersLinked;
    }

    public void setIsViewersLinked(boolean isLinked) {
        this.viewersLinked = isLinked;
    }
    
    /**
     * Called when the viewer pane is about to navigate to a new entity.
     */
    public void saveViewerState() {
        EntitySelectionHistory history = getActiveViewerPane().getEntitySelectionHistory();
        history.pushHistory(getActiveViewer().saveViewerState());
    }
    
    /**
     * Called when the state is being restored by a navigation action.
     */
    public void restoreViewerState(EntityViewerState state) {
        ensureViewerClass(getActiveViewerPane(), state.getViewerClass());
        getActiveViewer().restoreViewerState(state);
    }
}
