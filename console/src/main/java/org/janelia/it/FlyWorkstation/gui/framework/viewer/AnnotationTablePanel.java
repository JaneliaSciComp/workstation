package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.table.TableCellEditor;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicColumn;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicTable;
import org.janelia.it.FlyWorkstation.gui.util.MouseForwarder;
import org.janelia.it.FlyWorkstation.gui.util.MouseHandler;
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
    private JLabel summaryLabel;

    private List<OntologyAnnotation> annotations = new ArrayList<OntologyAnnotation>();
    
    public AnnotationTablePanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
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
    
    @Override
	public void setPreferredSize(Dimension preferredSize) {
		super.setPreferredSize(preferredSize);
		if (preferredSize.height == ImagesPanel.MIN_TABLE_HEIGHT) {
	        removeAll();
	        add(summaryLabel, BorderLayout.CENTER);
		}
		else {
	        removeAll();
	        add(dynamicTable, BorderLayout.CENTER);
		}
	}

	private void refresh() {

		summaryLabel = new JLabel(annotations.size()+" annotation"+(annotations.size()>1?"s":""));
		summaryLabel.setOpaque(false);
		summaryLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		summaryLabel.setHorizontalAlignment(SwingConstants.CENTER);
		summaryLabel.addMouseListener(new MouseHandler() {
			@Override
			protected void doubleLeftClicked(MouseEvent e) {
				JSlider slider = SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel().getTagTableSlider();
				slider.setValue(ImagesPanel.DEFAULT_TABLE_HEIGHT);
				e.consume();
			}
			
		});
		
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
        
        DynamicColumn keyCol = dynamicTable.addColumn(COLUMN_KEY, COLUMN_KEY, true, false, false, true);
        DynamicColumn valueCol = dynamicTable.addColumn(COLUMN_VALUE, COLUMN_VALUE, true, false, false, true);
	    
        for (OntologyAnnotation annotation : annotations) {
        	dynamicTable.addRow(annotation);
        }
	            
        dynamicTable.updateTableModel();
        removeAll();
        add(dynamicTable, BorderLayout.CENTER);
                
        revalidate();
        repaint();
    }

    private void deleteAnnotation(final OntologyAnnotation toDelete) {
    	
        Utils.setWaitingCursor(SessionMgr.getSessionMgr().getActiveBrowser());

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
            	ModelMgr.getModelMgr().removeAnnotation(toDelete.getId());
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
    
    private void deleteAnnotations(final List<OntologyAnnotation> toDeleteList) {
    	
        Utils.setWaitingCursor(SessionMgr.getSessionMgr().getActiveBrowser());

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
            	for(OntologyAnnotation toDelete : toDeleteList) { 
            		ModelMgr.getModelMgr().removeAnnotation(toDelete.getId());
            	}
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(SessionMgr.getSessionMgr().getActiveBrowser());
            }

            @Override
            protected void hadError(Throwable error) {
                error.printStackTrace();
                Utils.setDefaultCursor(SessionMgr.getSessionMgr().getActiveBrowser());
                JOptionPane.showMessageDialog(AnnotationTablePanel.this, "Error deleting annotations", "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        worker.execute();
    }
    
    private JPopupMenu getPopupMenu(final MouseEvent e, final OntologyAnnotation annotation) {
    	
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);
        
        JTable target = (JTable) e.getSource();
        if (target.getSelectedRow() <0) return null;

        JTable table = dynamicTable.getTable();
        
		ListSelectionModel lsm = table.getSelectionModel();
		if (lsm.getMinSelectionIndex() == lsm.getMaxSelectionIndex()) { 

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
			
		}
		else {
	        JMenuItem titleMenuItem = new JMenuItem("(Multiple items selected)");
	        titleMenuItem.setEnabled(false);
	        popupMenu.add(titleMenuItem);
	        
	        final List<OntologyAnnotation> toDeleteList = new ArrayList<OntologyAnnotation>();
            for (int i : table.getSelectedRows()) {
                int mi = table.convertRowIndexToModel(i);
            	toDeleteList.add(annotations.get(mi));
            }
	        
	    	if (SessionMgr.getUsername().equals(annotation.getOwner())) {
	            JMenuItem deleteItem = new JMenuItem("  Delete annotations");
	            deleteItem.addActionListener(new ActionListener() {
	                public void actionPerformed(ActionEvent actionEvent) {
	                	deleteAnnotations(toDeleteList);
	                }
	            });
	            popupMenu.add(deleteItem);
	    	}
		}

        return popupMenu;
    }

    
}
