package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.events.EntityChangeEvent;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * Wraps the EntityDetailsPanel in a way to make it respond to global selection events.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityDetailsOutline extends JPanel implements Refreshable, ActivatableView {

    private static final Logger log = LoggerFactory.getLogger(EntityDetailsOutline.class);

    private final EntityDetailsPanel entityDetailsPanel;
    private ModelMgrAdapter mml;
    private Entity entity;

    public EntityDetailsOutline() {
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(0, 0));

        this.entityDetailsPanel = new EntityDetailsPanel();
        this.mml = new ModelMgrAdapter() {
            @Override
            public void entitySelected(String category, String uniqueId, boolean clearAll) {
                if (clearAll) {
                    try {
                        Long entityId = EntityUtils.getEntityIdFromUniqueId(uniqueId);
                        if (entityId != null) {
                            SessionMgr.getBrowser().getViewerManager().showEntityInInspector(ModelMgr.getModelMgr().getEntityById(entityId));
                        }
                    }
                    catch (Exception e) {
                        SessionMgr.getSessionMgr().handleException(e);
                    }
                }
            }

            @Override
            public void entityDeselected(String category, String entityId) {
                if (entity != null && entity.getId().equals(entityId)) {
                    SessionMgr.getBrowser().getViewerManager().showEntityInInspector(null);
                }
            }
        };
        add(entityDetailsPanel, BorderLayout.CENTER);
    }

    public void showLoadingIndicator() {
        entityDetailsPanel.showLoadingIndicator();
    }

    public void loadEntity(Entity entity) {
        if (entity == null) {
            return;
        }
        if (this.entity != null && this.entity.getId().equals(entity.getId())) {
            return;
        }
        this.entity = entity;
        entityDetailsPanel.loadEntity(entity, null);
    }

    @Override
    public void refresh() {
        Entity toLoad = this.entity;
        this.entity = null;
        loadEntity(toLoad);
    }

    @Override
    public void totalRefresh() {
        ModelMgr.getModelMgr().invalidateCache(entity, false);
        try {
            this.entity = ModelMgr.getModelMgr().getEntityById(entity.getId());
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
        refresh();
    }

    @Override
    public void activate() {
        log.info("Activating");
        ModelMgr.getModelMgr().registerOnEventBus(this);
        ModelMgr.getModelMgr().addModelMgrObserver(mml);
        refresh();
    }

    @Override
    public void deactivate() {
        log.info("Deactivating");
        ModelMgr.getModelMgr().unregisterOnEventBus(this);
        ModelMgr.getModelMgr().removeModelMgrObserver(mml);
    }

    @Subscribe
    public void entityChanged(EntityChangeEvent event) {
        if (this.entity != null) {
            if (event.getEntity().getId().equals(this.entity.getId())) {
                refresh();
            }
        }
    }
}
