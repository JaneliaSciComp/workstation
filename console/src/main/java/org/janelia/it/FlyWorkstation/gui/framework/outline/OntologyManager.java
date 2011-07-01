/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/16/11
 * Time: 9:20 AM
 */
package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * A dialog for managing ontologies that can be loaded into the OntologyOutline.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyManager extends JDialog implements ActionListener {
	
    private static final String CLICKED_NEW = "clicked_new";
    private static final String CLICKED_OK = "clicked_ok";
    private static final String CLICKED_CANCEL = "clicked_cancel";

    private final OntologyTable privateTable;
    private final OntologyTable publicTable;
    
    private OntologyOutline ontologyOutline;
	private JTabbedPane tabbedPane;
    
    
    private abstract class OntologyTable extends JScrollPane {
    	
        private final JTable table;
        private final List<Entity> rootList = new ArrayList<Entity>();
    	
        public OntologyTable() {

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
                        	rightClick(entity);
                        }
                        // This masking is to make sure that the right button is being double clicked, not left and then right or right and then left
                        else if (e.getClickCount()==2 
                        		&& e.getButton()==MouseEvent.BUTTON1 
                        		&& (e.getModifiersEx() | InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
                        
                        	doubleClick(entity);
                        }
                    }
                	
                }
                public void mousePressed(MouseEvent e) {
                    // We have to also listen for mousePressed because OSX generates the popup trigger here
                    // instead of mouseReleased like any sane OS.
                    Entity entity = getSelectedEntity();
                    if (entity != null) {
                        if (e.isPopupTrigger()) {
                        	rightClick(entity);
                        }
                    }
                }
            });
            
            setViewportView(table);
        }
        
        public Entity getSelectedEntity() {
            int row = table.getSelectedRow();
            if (row >= 0 && row<rootList.size()) {
                return rootList.get(row);
            }
            return null;
        }
        
        public void selectEntity(Entity entity) {
        	int i = 0;
        	for(Entity e : rootList) {
        		if (e.getId().equals(entity.getId())) {
                	table.getSelectionModel().setSelectionInterval(i, i);		
        		}
        		i++;
        	}
        }
        
        protected abstract List<Entity> load();
        
        
        private void doubleClick(Entity root) {
			ontologyOutline.initializeTree(root);
            setVisible(false);
        }
        
        private void rightClick(Entity root) {
        	// TODO: implement context menu with:
        	// Load (for private)
        	// Delete (for private trees)
        	// Clone (for public trees)
        	System.out.println("right click "+root.getName());
        }
        
        /**
         * Asynchronous method to reload the data in the table. May be called from EDT.
         */
        public void reloadData(final Entity selectWhenDone) {

            SwingWorker<Void,Void> loadEntityTask = new SwingWorker<Void,Void>() {
            	
            	private TableModel tableModel;
            	
                @Override
                protected Void doInBackground() throws Exception {
                	try {    
                		// TODO: show loading animation
                		rootList.clear();
                		rootList.addAll(load());
                        tableModel = updateTableModel(rootList);
                	} 
                	catch (Exception e) {
                		e.printStackTrace();
                	}
                    return null;
                }

                @Override
                protected void done() {
                    table.setModel(tableModel);
                    Utils.autoResizeColWidth(table);
                    if (selectWhenDone != null)
                    	selectEntity(selectWhenDone);
                }
            };

            loadEntityTask.execute();
        }

        /**
         * Synchronous method for updating the JTable model. Should be called from the EDT.
         */
        private TableModel updateTableModel(List<Entity> ontologyRoots) {

            // Data formatted for the JTable
            Vector<String> columnNames = new Vector<String>();
            Vector<Vector<String>> data = new Vector<Vector<String>>();

            // Prepend the static columns
            columnNames.add("Name");
            columnNames.add("User");
            columnNames.add("Created");
            columnNames.add("Updated");
            
            // Build the data in column order
            if (ontologyRoots != null) {
    	        for(Entity entity : ontologyRoots) {
    	            Vector<String> rowData = new Vector<String>();
    	            rowData.add(entity.getName());
    	            rowData.add((entity.getUser() == null) ? "" : entity.getUser().getUserLogin());
    	            rowData.add((entity.getCreationDate() == null) ? "" : entity.getCreationDate().toString());
    	            rowData.add((entity.getUpdatedDate() == null) ? "" : entity.getUpdatedDate().toString());
    	            data.add(rowData);
    	        }
            }
            
            return new DefaultTableModel(data, columnNames) {
                public boolean isCellEditable(int rowIndex, int mColIndex) {
                    return false;
                }
            };
        }
    }
    
    public OntologyManager(OntologyOutline ontologyOutline) {

    	this.ontologyOutline = ontologyOutline;
    	
        setTitle("Ontology Manager");
        setPreferredSize(new Dimension(800, 600));
        getContentPane().setLayout(new BorderLayout());
        setLocationRelativeTo(ConsoleApp.getMainFrame());

        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);
        
        privateTable = new OntologyTable() {
        	protected List<Entity> load() {
                return EJBFactory.getRemoteAnnotationBean().getUserEntitiesByType(System.getenv("USER"),
                        EntityConstants.TYPE_ONTOLOGY_ROOT_ID);
        	}
        };
        
        tabbedPane.addTab("My Ontologies", null, privateTable, "Private ontologies");

        publicTable = new OntologyTable() {
        	protected List<Entity> load() {
                return new ArrayList<Entity>();
        	}
        };
        
        tabbedPane.addTab("Public Ontologies", null, publicTable, "Public ontologies");

        JButton newButton = new JButton("New Ontology");
        newButton.setActionCommand(CLICKED_NEW);
        newButton.setToolTipText("Create a new ontology");
        newButton.addActionListener(this);
        
        JButton okButton = new JButton("Load");
        okButton.setActionCommand(CLICKED_OK);
        okButton.setToolTipText("Load the selected ontology");
        okButton.addActionListener(this);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand(CLICKED_CANCEL);
        cancelButton.setToolTipText("Close without loading an ontology");
        cancelButton.addActionListener(this);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(newButton);
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(cancelButton);
        add(buttonPane, BorderLayout.SOUTH);

        setDefaultCloseOperation(
                JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                setVisible(false);
            }
        });
    }

	/**
	 * Reload the ontologies and show the dialog.
	 */
    public void showDialog() {
    	privateTable.reloadData(null);
    	publicTable.reloadData(null);
    	setVisible(true);
    }
    
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

		if (CLICKED_NEW.equals(cmd)) {
			String rootName = (String) JOptionPane.showInputDialog(this,
					"Ontology Root Name:\n", "New Ontology",
					JOptionPane.PLAIN_MESSAGE, null, null, null);

			if ((rootName == null) || (rootName.length() <= 0)) {
				JOptionPane.showMessageDialog(this, "Require a valid name",
						"Ontology Error", JOptionPane.WARNING_MESSAGE);
				return;
			}
			
			Entity newOntologyRoot = EJBFactory.getRemoteAnnotationBean().createOntologyRoot(System.getenv("USER"), rootName);
	    	privateTable.reloadData(newOntologyRoot);
		} 
		else if (CLICKED_OK.equals(cmd)) {

			Entity root = null;
			if (tabbedPane.getSelectedIndex() == 0) {
				root = privateTable.getSelectedEntity();
			}
			else {
				root = publicTable.getSelectedEntity();
			}
			
			if (root != null) {
				ontologyOutline.initializeTree(root);
	            setVisible(false);
			}
			else {
				JOptionPane.showMessageDialog(this, "Please select an ontology to load",
						"Ontology Error", JOptionPane.WARNING_MESSAGE);
			}
        }
        else if (CLICKED_CANCEL.equals(cmd)) {
            setVisible(false);
        }
    }


}
