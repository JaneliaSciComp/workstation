package org.janelia.it.workstation.gui.dataview;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A panel for displaying either lists of entities or Solr search results.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityPane extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(EntityPane.class);
    
    private final EntityListPane entityListPane;
    private final EntityDataPane entityParentsPane;
    private final EntityDataPane entityChildrenPane;

    public EntityPane(final SearchPane searchPanel,
            final EntityDataPane entityParentsPane, final EntityDataPane entityChildrenPane) {

        setLayout(new BorderLayout());

        this.entityParentsPane = entityParentsPane;
        this.entityChildrenPane = entityChildrenPane;

        this.entityListPane = new EntityListPane() {
            @Override
            public void entitySelected(Entity entity) {
                populateEntityDataPanes(entity);
            }

            @Override
            protected JPopupMenu getPopupMenu(List<Entity> selectedEntites, String label) {
                return EntityPane.this.getPopupMenu(selectedEntites, label);
            }
        };

        add(entityListPane, BorderLayout.CENTER);
    }

    public void populateEntityDataPanes(final Entity entity) {

        log.info("Populate data panes with " + entity);

        entityParentsPane.showLoading();
        entityChildrenPane.showLoading();

        SimpleWorker parentLoadTask = new SimpleWorker() {

            List<EntityData> eds;

            @Override
            protected void doStuff() throws Exception {
                eds = ModelMgr.getModelMgr().getParentEntityDatas(entity.getId());
            }

            @Override
            protected void hadSuccess() {
                entityParentsPane.showEntityData(eds);
            }

            @Override
            protected void hadError(Throwable error) {
                log.error("Error loading parent EDs",error);
            }
        };

        SimpleWorker childLoadTask = new SimpleWorker() {

            private Entity fullEntity;

            @Override
            protected void doStuff() throws Exception {
                fullEntity = ModelMgr.getModelMgr().loadLazyEntity(entity, false);
            }

            @Override
            protected void hadSuccess() {
                entityChildrenPane.showEntityData(fullEntity.getOrderedEntityData());
            }

            @Override
            protected void hadError(Throwable error) {
                log.error("Error loading lazy entity",error);
            }
        };

        parentLoadTask.execute();
        childLoadTask.execute();
    }

    public void clearEntityDataPanes() {
        entityParentsPane.showEmpty();
        entityChildrenPane.showEmpty();
    }

    public void performSearchById(final Long entityId) {

        SimpleWorker searchWorker = new SimpleWorker() {

            private Entity entity;

            protected void doStuff() throws Exception {
                entity = ModelMgr.getModelMgr().getEntityById(entityId);
            }

            protected void hadSuccess() {
                showEntity(entity);
            }

            protected void hadError(Throwable error) {
                log.error("Error finding entity with id "+entityId,error);
                JOptionPane.showMessageDialog(EntityPane.this, "Error finding entity", "Entity Search Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        searchWorker.execute();
    }

    public void performSearchByName(final String entityName) {

        SimpleWorker searchWorker = new SimpleWorker() {

            private List<Entity> entities;

            protected void doStuff() throws Exception {
                entities = ModelMgr.getModelMgr().getEntitiesByName(entityName);
            }

            protected void hadSuccess() {
                showEntities(entities);
            }

            protected void hadError(Throwable error) {
                log.error("Error finding entity with name "+entityName,error);
                JOptionPane.showMessageDialog(EntityPane.this, "Error finding entity", "Entity Search Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        searchWorker.execute();
    }

    public void showEntity(final Entity entity) {
        clearEntityDataPanes();
        entityListPane.showEntity(entity);
    }

    public void showEntities(final List<Entity> entities) {
        clearEntityDataPanes();
        entityListPane.showEntities(entities);
    }

    protected DataviewContextMenu getPopupMenu(List<Entity> selectedEntities, String label) {
        // Create context menu
        return new DataviewContextMenu(selectedEntities, label);
    }
}
