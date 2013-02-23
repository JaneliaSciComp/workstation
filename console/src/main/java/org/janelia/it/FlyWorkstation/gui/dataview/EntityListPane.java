package org.janelia.it.FlyWorkstation.gui.dataview;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.*;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchConfiguration.AttrGroup;
import org.janelia.it.FlyWorkstation.gui.framework.outline.Refreshable;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicColumn;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicTable;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityType;

/**
 * A panel for displaying lists of entities. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class EntityListPane extends JPanel implements SearchConfigurationListener, Refreshable {

	/** Format for displaying dates */
	protected static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");
	
    private final JLabel titleLabel;
    private List<Entity> entities;
    private SimpleWorker loadTask;
    protected final DynamicTable resultsTable;

    private EntityType shownEntityType;
    private Entity shownEntity;

    public EntityListPane() {

        resultsTable = new DynamicTable() {
			@Override
			public Object getValue(Object userObject, DynamicColumn column) {
				return EntityListPane.this.getValue(userObject, column);
			}
        	@Override
        	protected JPopupMenu createPopupMenu(MouseEvent e) {
        		return EntityListPane.this.createPopupMenu(e);
        	}
        	@Override
        	protected void rowClicked(int row) {
                if (row>=0) entitySelected(entities.get(row));
            }
		};

        titleLabel = new JLabel("Entity");

        setLayout(new BorderLayout());
        add(titleLabel, BorderLayout.NORTH);
        add(resultsTable, BorderLayout.CENTER);
    }
    
    public abstract void entitySelected(Entity entity);

    private JPopupMenu createPopupMenu(MouseEvent e) {    	

        JTable target = (JTable) e.getSource();
        final String value = target.getValueAt(target.getSelectedRow(), target.getSelectedColumn()).toString();
    	
        List<Entity> selectedEntities = new ArrayList<Entity>();
        for (int i : resultsTable.getTable().getSelectedRows()) {
        	selectedEntities.add((Entity)resultsTable.getRows().get(i).getUserObject());
        }
        return getPopupMenu(selectedEntities, value);
    }
    
    
    protected abstract JPopupMenu getPopupMenu(List<Entity> selectedEntites, String label);

    public void showLoading() {
        resultsTable.showLoadingIndicator();
    }
    
    @Override
    public void refresh() {
        if (shownEntity != null) {
            showEntity(shownEntity);
        }
        else if (shownEntityType != null) {
            showEntities(shownEntityType);
        }
    }

	@Override
	public void totalRefresh() {
		// TODO: implement this with invalidate
		refresh();
	}
	
    /**
     * Async method for loading and displaying entities of a given type.
     */
    public void showEntities(final EntityType entityType) {

        shownEntityType = entityType;
        shownEntity = null;

        if (loadTask != null && !loadTask.isDone()) {
            System.out.println("Cancel current entity type load");
            loadTask.cancel(true);
        }

        System.out.println("Loading entities of type " + entityType.getName());

        titleLabel.setText("Entity: " + entityType.getName());
        showLoading();

        loadTask = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                List<Entity> entities = ModelMgr.getModelMgr().getEntitiesByTypeName(entityType.getName());
                if (isCancelled()) return;
                setEntities(entities);
            }

            @Override
            protected void hadSuccess() {
            	updateTableModel();
                resultsTable.showTable();
            }

            @Override
            protected void hadError(Throwable error) {
                error.printStackTrace();
            }
        };

        loadTask.execute();
    }

    public void showEntities(final List<Entity> entities) {

        if (loadTask != null && !loadTask.isDone()) {
            System.out.println("Cancel current entity type load");
            loadTask.cancel(true);
        }

        titleLabel.setText("Entity Search Results");
        showLoading();

        loadTask = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                if (isCancelled()) return;
                setEntities(entities);
            }

            @Override
            protected void hadSuccess() {
            	updateTableModel();
                resultsTable.showTable();
            }

            @Override
            protected void hadError(Throwable error) {
                error.printStackTrace();
            }
        };

        loadTask.execute();
    }

    public void showEntity(final Entity entity) {

        if (entity==null) return;
        
        shownEntityType = null;
        shownEntity = entity;

        if (loadTask != null && !loadTask.isDone()) {
            System.out.println("Cancel current entity load");
            loadTask.cancel(true);
        }

        showLoading();

        System.out.println("Loading entity " + entity.getName());
        
        loadTask = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                titleLabel.setText("Entity: " + entity.getEntityType().getName() + " (" + entity.getName() + ")");
                List<Entity> entities = new ArrayList<Entity>();
                entities.add(entity);
                setEntities(entities);
            }

            @Override
            protected void hadSuccess() {
            	updateTableModel();
                resultsTable.showTable();
                resultsTable.getTable().getSelectionModel().setSelectionInterval(0, 0);
                entitySelected(entity);
            }

            @Override
            protected void hadError(Throwable error) {
                error.printStackTrace();
            }

        };

        loadTask.execute();
    }

    private void setEntities(List<Entity> entityList) {

    	entities = (entityList == null) ? new ArrayList<Entity>() : entityList;
        
        Collections.sort(entities, new Comparator<Entity>() {
            @Override
            public int compare(Entity o1, Entity o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
    }

    protected void updateTableModel() {
    	resultsTable.removeAllRows();
    	for(Entity entity : entities) {
    		resultsTable.addRow(entity);
    	}    
		resultsTable.updateTableModel();
    }
    
    @Override
	public void configurationChange(SearchConfigurationEvent evt) {
    	SearchConfiguration searchConfig = evt.getSearchConfig();
    	Map<AttrGroup, List<SearchAttribute>> attributeGroups = searchConfig.getAttributeGroups();
    	
    	for(SearchAttribute attr : attributeGroups.get(AttrGroup.BASIC)) {
			resultsTable.addColumn(attr.getName(), attr.getLabel(), true, false, true, attr.isSortable());	
    	}
    	
    	for(SearchAttribute attr : attributeGroups.get(AttrGroup.EXT)) {
    		resultsTable.addColumn(attr.getName(), attr.getLabel(), false, false, true, true);
    	}

		revalidate();
	}
    
    /**
     * Return the value of the specified column for the given object.
     * @param userObject
     * @param column
     * @return
     */
	public Object getValue(Object userObject, DynamicColumn column) {
		Entity entity = (Entity)userObject;
		String field = column.getName();
		Object value = null;
		if ("id".equals(field)) {
			value = entity.getId();
		}
		else if ("name".equals(field)) {
			value = entity.getName();
		}
		else if ("entity_type".equals(field)) {
			value = entity.getEntityType().getName();
		}
		else if ("username".equals(field)) {
			value = entity.getOwnerKey();
		}
		else if ("creation_date".equals(field)) {
		    if (entity.getCreationDate()==null) {
		        value = "";
		    }
		    else {
		        value = df.format(entity.getCreationDate());    
		    }
		}
		else if ("updated_date".equals(field)) {
            if (entity.getUpdatedDate()==null) {
                value = "";
            }
            else {
                value = df.format(entity.getUpdatedDate());    
            }
		}
		
		return value;
	}
    
    
}