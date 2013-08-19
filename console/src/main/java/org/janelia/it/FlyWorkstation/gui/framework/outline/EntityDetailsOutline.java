package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps the EntityDetailsPanel in a way to make it respond to global selection events. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityDetailsOutline extends JPanel implements Refreshable, ActivatableView {

    private static final Logger log = LoggerFactory.getLogger(EntityDetailsOutline.class);
    
    private RootedEntity currRootedEntity;
    private EntityDetailsPanel entityDetailsPanel;
    private ModelMgrAdapter mml;

    public EntityDetailsOutline() {
        setLayout(new BorderLayout());
        this.entityDetailsPanel = new EntityDetailsPanel();
        this.mml = new ModelMgrAdapter() {
            @Override
            public void entitySelected(String category, String entityId, boolean clearAll) {
                SessionMgr.getBrowser().getViewerManager().showEntityInInspector(SessionMgr.getBrowser().getEntityOutline().getRootedEntity(entityId));
            }

            @Override
            public void entityDeselected(String category, String entityId) {
                SessionMgr.getBrowser().getViewerManager().showEntityInInspector(null);
            }
        };
        add(entityDetailsPanel, BorderLayout.CENTER);
    }

    public void showLoadingIndicator() {
        entityDetailsPanel.showLoadingIndicator();
    }
    
    public void loadRootedEntity(RootedEntity rootedEntity) {
        if (rootedEntity==null) return;
        if (currRootedEntity!=null && currRootedEntity.getId().equals(rootedEntity.getId())) {
            return;
        }
        this.currRootedEntity = rootedEntity;
        entityDetailsPanel.loadRootedEntity(rootedEntity, null);
    }
    
    @Override
    public void refresh() {
        RootedEntity toLoad = currRootedEntity;
        currRootedEntity = null;
        loadRootedEntity(toLoad);
    }

    @Override
    public void totalRefresh() {
        ModelMgr.getModelMgr().invalidateCache(currRootedEntity.getEntity(), false);
        try {
            currRootedEntity.setEntity(ModelMgr.getModelMgr().getEntityById(currRootedEntity.getEntityId()));
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
        refresh();
    }

    @Override
    public void activate() {
        log.info("Activating");
        ModelMgr.getModelMgr().addModelMgrObserver(mml);
        refresh();
    }

    @Override
    public void deactivate() {
        log.info("Deactivating");
        ModelMgr.getModelMgr().removeModelMgrObserver(mml);
    }

    
}
