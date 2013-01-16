package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.ontology.OntologyRoot;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * A table for displaying ontology roots
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AbstractOntologyTable extends AbstractEntityTable {

    private final List<OntologyRoot> ontologyRoots = new ArrayList<OntologyRoot>();

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
            for (Entity entity : entities) {
                Vector<String> rowData = new Vector<String>();
                rowData.add(entity.getName());
                
                String owner = entity.getOwnerKey().split(":")[1];
                rowData.add(owner);
                
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

    @Override
    protected void postLoad() {
        ontologyRoots.clear();
        for (Entity entity : getEntityList()) {
            ontologyRoots.add(new OntologyRoot(entity));
        }
        super.postLoad();
    }

    public List<OntologyRoot> getOntologyRoots() {
        return ontologyRoots;
    }

    /**
     * Get the ontology which is the currently selected row in the table.
     *
     * @return
     */
    public OntologyRoot getSelectedOntology() {
        int row = table.getSelectedRow();
        if (row >= 0 && row < ontologyRoots.size()) {
            return ontologyRoots.get(row);
        }
        return null;
    }
}
