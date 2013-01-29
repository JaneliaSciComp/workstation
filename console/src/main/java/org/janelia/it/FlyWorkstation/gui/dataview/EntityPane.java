package org.janelia.it.FlyWorkstation.gui.dataview;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchConfiguration;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchResultsPanel;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
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
    
    private final SearchPane searchPanel;
    private final EntityDataPane entityParentsPane;
    private final EntityDataPane entityChildrenPane;
    
    public enum ResultViewType {
    	ENTITY,
    	SOLR
    }
    private ResultViewType currentView;
    
    public EntityPane(final SearchConfiguration searchConfig, final SearchPane searchPanel, 
    		final EntityDataPane entityParentsPane, final EntityDataPane entityChildrenPane) {

    	setLayout(new BorderLayout());
    	
    	this.searchPanel = searchPanel;
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
        
        this.searchResultsPanel = new SearchResultsPanel(searchPanel.getSolrPanel()) {
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
                error.printStackTrace();
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
                error.printStackTrace();
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
                error.printStackTrace();
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
                error.printStackTrace();
                JOptionPane.showMessageDialog(EntityPane.this, "Error finding entity", "Entity Search Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        searchWorker.execute();
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
    	
    	try {
//
//        	String[] roots = new String[] { "../groovy/src" };
//        	GroovyScriptEngine gse = new GroovyScriptEngine(roots);
//        	
//        	Binding binding = new Binding();
//        	binding.setVariable("m", ModelMgr.getModelMgr());
//        	gse.run("hello.groovy", binding);
//        	System.out.println(binding.getVariable("output"));
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    	
    	// TODO: implement
//    	entityListPane.showEntities(entities);
	}

    public SearchPane getSearchPane() {
        return searchPanel;
    }
    
	public SearchResultsPanel getSearchResultsPanel() {
        return searchResultsPanel;
    }

    protected DataviewContextMenu getPopupMenu(List<Entity> selectedEntities, String label) {
		// Create context menu
		return new DataviewContextMenu(selectedEntities, label);
	}
}
