package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.table.TableCellEditor;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicColumn;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicTable;
import org.janelia.it.FlyWorkstation.gui.util.MouseForwarder;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;

/**
 * A panel that shows a bunch of annotations in a table. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationTablePanel extends JPanel implements AnnotationView {
	
    private static final String COLUMN_KEY = "Annotation Term";
    private static final String COLUMN_VALUE = "Annotation Value";
    
    private DynamicTable dynamicTable;

    private List<OntologyAnnotation> annotations = new ArrayList<OntologyAnnotation>();
    
    public AnnotationTablePanel() {
        setLayout(new BorderLayout());
    }

	@Override
    public List<OntologyAnnotation> getAnnotations() {
        return annotations;
    }

	@Override
    public void setAnnotations(List<OntologyAnnotation> annotations) {
        if (annotations == null) {
            this.annotations = new ArrayList<OntologyAnnotation>();
        }
        else {
            this.annotations = annotations;
        }
        refresh();
    }

	@Override
    public void removeAnnotation(OntologyAnnotation annotation) {
        annotations.remove(annotation);
        refresh();
    }

	@Override
    public void addAnnotation(OntologyAnnotation annotation) {
        annotations.add(annotation);
        refresh();
    }
    
    private void refresh() {

        dynamicTable = new DynamicTable(false, true) {

            @Override
			public Object getValue(Object userObject, DynamicColumn column) {

            	OntologyAnnotation annotation = (OntologyAnnotation)userObject;
                if (null!=annotation) {
                    if (column.getName().equals(COLUMN_KEY)) {
                        return annotation.getKeyString();
                    }
                    if (column.getName().equals(COLUMN_VALUE)) {
                        return annotation.getValueString();
                    }
                }
                return null;
			}

			@Override
            protected JPopupMenu createPopupMenu(MouseEvent e) {
            	
            	if (dynamicTable.getCurrentRow() == null) return null;

                Object userObject = dynamicTable.getCurrentRow().getUserObject();
            	OntologyAnnotation annotation = (OntologyAnnotation)userObject;
                
            	return getPopupMenu(e, annotation);
	        };
	        

			@Override
	        public TableCellEditor getCellEditor(int row, int col) {
				if (col!=1) return null;
				
				// TODO: implement custom editors for each ontology term type
				
	        	return null;
	        }
        };
        
        dynamicTable.addMouseListener(new MouseForwarder(this, "DynamicTable->AnnotationTablePanel"));
        
        DynamicColumn keyCol = dynamicTable.addColumn(COLUMN_KEY, true, false, false);
        DynamicColumn valueCol = dynamicTable.addColumn(COLUMN_VALUE, true, false, false);
	    
        for (OntologyAnnotation annotation : annotations) {
        	dynamicTable.addRow(annotation);
        }
	            
        dynamicTable.updateTableModel();
        removeAll();
        add(dynamicTable, BorderLayout.CENTER);
                
        revalidate();
        repaint();
    }

    private void deleteAnnotation(final OntologyAnnotation annotation) {
    	
        Utils.setWaitingCursor(SessionMgr.getSessionMgr().getActiveBrowser());

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
            	ModelMgr.getModelMgr().removeAnnotation(annotation.getId());
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(SessionMgr.getSessionMgr().getActiveBrowser());
            }

            @Override
            protected void hadError(Throwable error) {
                error.printStackTrace();
                Utils.setDefaultCursor(SessionMgr.getSessionMgr().getActiveBrowser());
                JOptionPane.showMessageDialog(AnnotationTablePanel.this, "Error deleting annotation", "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        worker.execute();
    }
    
    private JPopupMenu getPopupMenu(final MouseEvent e, final OntologyAnnotation annotation) {

        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);
        
        JMenuItem titleItem = new JMenuItem(annotation.getEntity().getName());
        titleItem.setEnabled(false);
        popupMenu.add(titleItem);
        
    	if (SessionMgr.getUsername().equals(annotation.getOwner())) {
            JMenuItem deleteItem = new JMenuItem("  Delete annotation");
            deleteItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                	deleteAnnotation(annotation);
                }
            });
            popupMenu.add(deleteItem);
    	}

        JMenuItem detailsItem = new JMenuItem("  View details");
        detailsItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
            	SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel().viewAnnotationDetails(annotation);
            }
        });
        popupMenu.add(detailsItem);

        return popupMenu;
    }

    
}
