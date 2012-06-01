package org.janelia.it.FlyWorkstation.gui.dataview;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchConfiguration;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchResultsPanel;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.jacs.compute.api.support.SolrQueryBuilder;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;

/**
 * A panel for displaying either lists of entities or Solr search results. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityPane extends JPanel {

    private final EntityListPane entityListPane;
    private final SearchResultsPanel searchResultsPanel;
    
    private final EntityDataPane entityParentsPane;
    private final EntityDataPane entityChildrenPane;
    
    public enum ResultViewType {
    	ENTITY,
    	SOLR
    }
    private ResultViewType currentView;
    private SimpleWorker loadTask;
    
    public EntityPane(final SearchConfiguration searchConfig, final SearchPane searchPanel, 
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
        
        this.searchResultsPanel = new SearchResultsPanel() {
			@Override
			protected SolrQueryBuilder getQueryBuilder() {
				return searchPanel.getSolrPanel().getQueryBuilder();
			}
        	@Override
        	public void entitySelected(Entity entity) {
        		populateEntityDataPanes(entity);
        	}
        	@Override
        	protected JPopupMenu getPopupMenu(List<Entity> selectedEntites, String label) {
        		return EntityPane.this.getPopupMenu(selectedEntites, label);
        	}
        };

        searchConfig.addConfigurationChangeListener(entityListPane);
        searchConfig.addConfigurationChangeListener(searchResultsPanel);
        
        setActiveView(ResultViewType.ENTITY);
    }
    
    public void setActiveView(ResultViewType type) {
    	if (currentView!=type) {
    		removeAll();
    	}
    	this.currentView = type;
    	switch (currentView) {
    	case ENTITY:
    		add(entityListPane, BorderLayout.CENTER);
    		break;
    	case SOLR:
    		add(searchResultsPanel, BorderLayout.CENTER);
    		break;
    	}
    	revalidate();
    	repaint();
    }

    public void populateEntityDataPanes(final Entity entity) {

        System.out.println("Populate data panes with " + entity);
        
        entityParentsPane.showLoading();
        
        loadTask = new SimpleWorker() {

            List<EntityData> eds;

            @Override
            protected void doStuff() throws Exception {
            	ModelMgrUtils.loadLazyEntity(entity, false);
                eds = ModelMgr.getModelMgr().getParentEntityDatas(entity.getId());
            }

            @Override
            protected void hadSuccess() {
                entityChildrenPane.showEntityData(entity.getOrderedEntityData());
                entityParentsPane.showEntityData(eds);
            }

            @Override
            protected void hadError(Throwable error) {
                error.printStackTrace();
            }
        };

        loadTask.execute();
    }
    
    public void clearEntityDataPanes() {
        entityParentsPane.showEmpty();
        entityChildrenPane.showEmpty();
    }
    
    public void performSearch(boolean clear) {
    	clearEntityDataPanes();
    	setActiveView(ResultViewType.SOLR);
    	searchResultsPanel.performSearch(clear, clear, true);
    }
    
    public void showEntity(final Entity entity) {
    	clearEntityDataPanes();
    	setActiveView(ResultViewType.ENTITY);
    	entityListPane.showEntity(entity);
    }
    
    public void showEntities(final EntityType entityType) {
    	clearEntityDataPanes();
    	setActiveView(ResultViewType.ENTITY);
    	entityListPane.showEntities(entityType);
    }
    
    public void showEntities(final List<Entity> entities) {
    	clearEntityDataPanes();
    	setActiveView(ResultViewType.ENTITY);
    	entityListPane.showEntities(entities);
    }

	public void runGroovyCode(String code) {
    	clearEntityDataPanes();
    	setActiveView(ResultViewType.ENTITY);
    	
    	// TODO: implement
//    	entityListPane.showEntities(entities);
	}

	protected DataviewContextMenu getPopupMenu(List<Entity> selectedEntities, String label) {
		// Create context menu
		return new DataviewContextMenu(selectedEntities, label);
	}
}
