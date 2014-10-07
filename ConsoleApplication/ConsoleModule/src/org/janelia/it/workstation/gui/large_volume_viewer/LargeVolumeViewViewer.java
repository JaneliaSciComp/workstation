package org.janelia.it.workstation.gui.large_volume_viewer;

import org.janelia.it.workstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.workstation.api.entity_model.access.ModelMgrObserver;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.viewer.Viewer;
import org.janelia.it.workstation.gui.framework.viewer.ViewerPane;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.workstation.gui.framework.outline.EntityViewerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/3/13
 * Time: 4:42 PM
 *
 * Shows Confocal Blocks data.
 */
public class LargeVolumeViewViewer extends Viewer {
    private Entity sliceSample;
    private Entity initialEntity;

    private RootedEntity slcRootedEntity;

    private QuadViewUi viewUI;
    private ModelMgrObserver modelMgrObserver;
    private Logger logger = LoggerFactory.getLogger(LargeVolumeViewViewer.class);

    public LargeVolumeViewViewer(ViewerPane viewerPane) {
        super(viewerPane);
        setLayout(new BorderLayout());
    }

    @Override
    public void clear() {
        clearObserver();
    }

    @Override
    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()));
        revalidate();
        repaint();
    }

    @Override
    public void loadEntity(RootedEntity rootedEntity) {
        // don't reload if user tries to reload the same entity (is that a
        //  good idea?  not clear)
        if (initialEntity != null && rootedEntity.getEntity().getId() != initialEntity.getId()) {
            deleteAll();
        }
        initialEntity = rootedEntity.getEntity();

        // intial rooted entity should be a brain sample or a workspace; the QuadViewUI wants
        //  the intial entity, but we need the sample either way to be able to open it:
        if (initialEntity.getEntityTypeName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
            sliceSample = initialEntity;
        } else if (initialEntity.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
            String sampleID = initialEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_WORKSPACE_SAMPLE_IDS);
            try {
                sliceSample = ModelMgr.getModelMgr().getEntityById(sampleID);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (sliceSample == null) {
                JOptionPane.showMessageDialog(this.getParent(),
                        "Could not find sample entity for this workspace!",
                        "Could not open workspace",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

        }
        
        refresh();

        // be sure we've successfully gotten the sample before loading it!
        if (sliceSample.getEntityTypeName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
            try {
                if (!viewUI.loadFile(sliceSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH))) {
                    JOptionPane.showMessageDialog(this.getParent(),
                            "Could not open sample entity for this workspace!",
                            "Could not open workspace",
                            JOptionPane.ERROR_MESSAGE);
                }
                return;
            }
            catch (MalformedURLException e) {
                SessionMgr.getSessionMgr().handleException(e);
            }
        }
        // Listen for further changes, so can refresh again later.
        establishObserver();
    }

    @Override
    public void loadEntity(RootedEntity rootedEntity, Callable<Void> success) {
        loadEntity(rootedEntity);
        try {
            if ( success != null )
                success.call();
        } catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
        }
    }

	public RootedEntity getContextRootedEntity() {
		return slcRootedEntity;
	}
	
    @Override
    public List<RootedEntity> getRootedEntities() {
        return Arrays.asList(slcRootedEntity);
    }

    @Override
    public List<RootedEntity> getSelectedEntities() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public RootedEntity getRootedEntityById(String uniqueId) {
        return slcRootedEntity;
    }
    
    @Override
    public void close() {
        logger.info("Closing");
        ModelMgr.getModelMgr().unregisterOnEventBus(this);
        deleteAll();
    }

    @Override
    public void refresh() {
        // logger.info("Refresh called.");

        if (sliceSample != null) {
            showLoadingIndicator();

            if ( viewUI == null ) {
                viewUI = new QuadViewUi(SessionMgr.getMainFrame(), initialEntity, false);
            }
            removeAll();
            viewUI.setVisible(true);
            add(viewUI);
            revalidate();
            repaint();
        }
    }

    @Override
    public void totalRefresh() {
        refresh();
    }

    private void establishObserver() {
        modelMgrObserver = new ModelMgrListener( this, sliceSample);
        ModelMgr.getModelMgr().addModelMgrObserver(modelMgrObserver);
    }

    private void deleteAll() {
        clearObserver();
        sliceSample = null;
        initialEntity = null;
        slcRootedEntity = null;
        removeAll();
        if (viewUI != null)
        	viewUI.clearCache();
        viewUI = null;
    }

    private void clearObserver() {
        if ( modelMgrObserver != null ) {
            ModelMgr.getModelMgr().removeModelMgrObserver(modelMgrObserver);
        }
    }

    //------------------------------Inner Classes
    /** Listens for changes to the child-set of the heard-entity. */
    public static class ModelMgrListener extends ModelMgrAdapter {
        private Entity heardEntity;
        private LargeVolumeViewViewer viewer;
        ModelMgrListener( LargeVolumeViewViewer viewer, Entity e ) {
            heardEntity = e;
            this.viewer = viewer;
        }

        @Override
        public void entityChildrenChanged(long entityId) {
            if (heardEntity.getId() == entityId) {
                viewer.refresh();
            }
        }
    }
    
    @Override
    public EntityViewerState saveViewerState() {
        Set<String> selectedIds = new HashSet<String>(ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(getSelectionCategory()));
        return new EntityViewerState(getClass(), slcRootedEntity, selectedIds);
    }
    
    @Override
    public void restoreViewerState(final EntityViewerState state) {
        loadEntity(state.getContextRootedEntity(), new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // TODO: reselect the entities from state.getSelectedIds()
                return null;
            }
        }
        );
        
    }
}
