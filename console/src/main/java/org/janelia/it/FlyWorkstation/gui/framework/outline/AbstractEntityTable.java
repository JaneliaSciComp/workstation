package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.table.TableModel;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * A generic table for displaying entities. Support asynchronous data loading and various user interactions.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AbstractEntityTable extends JScrollPane {
	
    private final JTable table;
    private final List<Entity> entityList = new ArrayList<Entity>();
	private final JComponent loadingView;
    
    public AbstractEntityTable() {

    	loadingView = new JLabel(Icons.loadingIcon);
    	
    	table = new JTable();
    	table.setFillsViewportHeight(true);
    	table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    	table.setColumnSelectionAllowed(false);
    	table.setRowSelectionAllowed(true);

        table.addMouseListener(new MouseAdapter(){
            public void mouseReleased(MouseEvent e) {
                Entity entity = getSelectedEntity();
                if (entity != null) {
                    if (e.isPopupTrigger()) {
                    	rightClick(entity, e);
                    }
                    // This masking is to make sure that the right button is being double clicked, not left and then right or right and then left
                    else if (e.getClickCount()==2 
                    		&& e.getButton()==MouseEvent.BUTTON1 
                    		&& (e.getModifiersEx() | InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
                    
                    	doubleClick(entity, e);
                    }
                }
            	
            }
            public void mousePressed(MouseEvent e) {
                // We have to also listen for mousePressed because OSX generates the popup trigger here
                // instead of mouseReleased like any sane OS.
                Entity entity = getSelectedEntity();
                if (entity != null) {
                    if (e.isPopupTrigger()) {
                    	rightClick(entity, e);
                    }
                }
            }
        });
        
        setViewportView(table);
    }
    
    /**
     * Get the entity which is the currently selected row in the table.
     * @return
     */
    public Entity getSelectedEntity() {
        int row = table.getSelectedRow();
        if (row >= 0 && row<entityList.size()) {
            return entityList.get(row);
        }
        return null;
    }
    
    /**
     * Select the given entity's row in the table.
     * @param entity
     */
    public void selectEntity(Entity entity) {
    	int i = 0;
    	for(Entity e : entityList) {
    		if (e.getId().equals(entity.getId())) {
            	table.getSelectionModel().setSelectionInterval(i, i);		
    		}
    		i++;
    	}
    }

    public void setLoading(boolean loading) {
        setViewportView(loading ? loadingView : table);
    }
    
    /**
     * Asynchronous method to reload the data in the table. May be called from EDT.
     * This method will call load() in a separate worker thread and populate the table with the results.
     * @param selectWhenDone The Entity to select when we are done
     */
    public void reloadData(final Entity selectWhenDone) {

    	setLoading(true);
        
        SimpleWorker worker = new SimpleWorker() {

        	private TableModel tableModel;
        	
            protected void doStuff() throws Exception {
        		entityList.clear();
        		entityList.addAll(load());
                tableModel = updateTableModel(entityList);
            }

			protected void hadSuccess() {
                table.setModel(tableModel);
                Utils.autoResizeColWidth(table);
                if (selectWhenDone != null) {
                	selectEntity(selectWhenDone);
                }
                setLoading(false);
			}
			
			protected void hadError(Throwable error) {
				error.printStackTrace();
			}
            
        };

        worker.execute();
    }
    
    /**
     * Implement this method to load the entities that are to be displayed in the table.
     * @return
     */
    protected abstract List<Entity> load();
    
    /**
     * Implement this method to decide what happens when an entity is double clicked.
     * @param root
     * @param e
     */
    protected abstract void doubleClick(Entity entity, MouseEvent e);
    
    /**
     * Implement this method to decide what happens when an entity is right clicked.
     * @param root
     * @param e
     */
    protected abstract void rightClick(Entity entity, MouseEvent e);
    
    /**
     * Implement this method to build the TableModel from the list of entities.
     * @param entities
     * @return
     */
    protected abstract TableModel updateTableModel(List<Entity> entities);
}
