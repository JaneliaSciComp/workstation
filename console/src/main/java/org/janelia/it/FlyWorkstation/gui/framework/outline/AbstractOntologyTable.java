package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.util.List;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * A table for displaying ontologies. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AbstractOntologyTable extends AbstractEntityTable {

    /**
     * Synchronous method for updating the JTable model. Should be called from the EDT.
     */
	protected TableModel updateTableModel(List<Entity> entities) {

        // Data formatted for the JTable
        Vector<String> columnNames = new Vector<String>();
        Vector<Vector<String>> data = new Vector<Vector<String>>();

        // Prepend the static columns
        columnNames.add("Ontology Name");
        columnNames.add("Owner");
        columnNames.add("Date Created");
        columnNames.add("Date Updated");
        
        // Build the data in column order
        if (entities != null) {
	        for(Entity entity : entities) {
	            Vector<String> rowData = new Vector<String>();
	            rowData.add(entity.getName());
	            rowData.add((entity.getUser() == null) ? "" : entity.getUser().getUserLogin());
	            rowData.add((entity.getCreationDate() == null) ? "" : entity.getCreationDate().toString());
	            rowData.add((entity.getUpdatedDate() == null) ? "" : entity.getUpdatedDate().toString());
	            data.add(rowData);
	        }
        }
        
        // Return a read-only table model
        return new DefaultTableModel(data, columnNames) {
            public boolean isCellEditable(int rowIndex, int mColIndex) {
                return false;
            }
        };
    }
	
}
