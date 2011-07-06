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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.FlyWorkstation.gui.ontology.OWLDataLoader;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.semanticweb.owlapi.model.OWLException;

/**
 * A dialog for managing ontologies that can be loaded into the OntologyOutline.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyManager extends JDialog implements ActionListener, PropertyChangeListener {

    private static final String ONTOLOGY_LOAD_COMMAND = "ontology_load";
    private static final String ONTOLOGY_DELETE_COMMAND = "ontology_delete";
    private static final String ONTOLOGY_CLONE_COMMAND = "ontology_clone";
    private static final String ONTOLOGY_SHARE_COMMAND = "ontology_share";
    private static final String ONTOLOGY_NEW_COMMAND = "ontology_new";
    private static final String IMPORT_OWL_COMMAND = "ontology_import";
    private static final String CANCEL_COMMAND = "clicked_cancel";

    private JPopupMenu privateMenu;
    private JPopupMenu publicMenu;
    
    private final AbstractEntityTable privateTable;
    private final AbstractEntityTable publicTable;
    
    private OntologyOutline ontologyOutline;
	private JTabbedPane tabbedPane;   
    
	private ProgressMonitor progressMonitor;
	private OWLDataLoader owlLoader;
	
    public OntologyManager(final OntologyOutline ontologyOutline) {

    	this.ontologyOutline = ontologyOutline;
    	
        setTitle("Ontology Manager");
        setModalityType(ModalityType.APPLICATION_MODAL);
        setPreferredSize(new Dimension(800, 600));
        getContentPane().setLayout(new BorderLayout());
        setLocationRelativeTo(ConsoleApp.getMainFrame());

        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);
        
        privateTable = new AbstractOntologyTable() {
        	protected List<Entity> load() {
                return EJBFactory.getRemoteAnnotationBean().getUserEntitiesByType(System.getenv("USER"),
                        EntityConstants.TYPE_ONTOLOGY_ROOT_ID);
        	}
        	
        	protected void doubleClick(Entity entity, MouseEvent e) {
            	loadSelected();
            }
            
        	protected void rightClick(Entity entity, MouseEvent e) {
        		privateMenu.show((JComponent)e.getSource(), e.getX(), e.getY());
            }
        };
        
        publicTable = new AbstractOntologyTable() {
        	protected List<Entity> load() {
                return new ArrayList<Entity>();
        	}
        	
        	protected void doubleClick(Entity entity, MouseEvent e) {
            	loadSelected();
            }
            
        	protected void rightClick(Entity entity, MouseEvent e) {
        		publicMenu.show((JComponent)e.getSource(), e.getX(), e.getY());
            }
        };

        tabbedPane.addTab("My Ontologies", null, privateTable, "Private ontologies");
        tabbedPane.addTab("Public Ontologies", null, publicTable, "Public ontologies");

        JButton newButton = new JButton("New Ontology");
        newButton.setActionCommand(ONTOLOGY_NEW_COMMAND);
        newButton.setToolTipText("Create a new ontology");
        newButton.addActionListener(this);

        JButton importButton = new JButton("Import OWL File");
        importButton.setActionCommand(IMPORT_OWL_COMMAND);
        importButton.setToolTipText("Import an ontology in OWL format");
        importButton.addActionListener(this);
        
        JButton okButton = new JButton("Load");
        okButton.setActionCommand(ONTOLOGY_LOAD_COMMAND);
        okButton.setToolTipText("Load the currently selected ontology");
        okButton.addActionListener(this);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand(CANCEL_COMMAND);
        cancelButton.setToolTipText("Close without loading an ontology");
        cancelButton.addActionListener(this);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(newButton);
        buttonPane.add(importButton);
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

    	createPopupMenus();
    }

    private void createPopupMenus() {

        privateMenu = new JPopupMenu();
        privateMenu.setLightWeightPopupEnabled(true);

        publicMenu = new JPopupMenu();
        publicMenu.setLightWeightPopupEnabled(true);
        
        JMenuItem mi = new JMenuItem("Load");
        mi.addActionListener(OntologyManager.this);
        mi.setActionCommand(ONTOLOGY_LOAD_COMMAND);
        privateMenu.add(mi);
        
        mi = new JMenuItem("Clone");
        mi.addActionListener(OntologyManager.this);
        mi.setActionCommand(ONTOLOGY_CLONE_COMMAND);
        privateMenu.add(mi);

        mi = new JMenuItem("Share");
        mi.addActionListener(OntologyManager.this);
        mi.setActionCommand(ONTOLOGY_SHARE_COMMAND);
        privateMenu.add(mi);
        
        mi = new JMenuItem("Delete");
        mi.addActionListener(OntologyManager.this);
        mi.setActionCommand(ONTOLOGY_DELETE_COMMAND);
        privateMenu.add(mi);
        
        publicMenu = new JPopupMenu();
        publicMenu.setLightWeightPopupEnabled(true);
        
        mi = new JMenuItem("Load");
        mi.addActionListener(OntologyManager.this);
        mi.setActionCommand(ONTOLOGY_LOAD_COMMAND);
        publicMenu.add(mi);
        
        mi = new JMenuItem("Clone");
        mi.addActionListener(OntologyManager.this);
        mi.setActionCommand(ONTOLOGY_CLONE_COMMAND);
        publicMenu.add(mi);
    }
    
    public void preload() {
    	privateTable.reloadData(null);
    	publicTable.reloadData(null);
    }
    
	/**
	 * Reload the ontologies and show the dialog.
	 */
    public void showDialog() {
    	privateTable.reloadData(null);
    	publicTable.reloadData(null);
//    	privateTable.reloadData(ontologyOutline.getCurrentOntology());
    	tabbedPane.setSelectedIndex(0);
    	setVisible(true);
    }
    
    public AbstractEntityTable getPrivateTable() {
		return privateTable;
	}

	public AbstractEntityTable getPublicTable() {
		return publicTable;
	}

	private Entity getSelectedOntology() {
		if (tabbedPane.getSelectedIndex() == 0) {
			return privateTable.getSelectedEntity();
		}
		else {
			return publicTable.getSelectedEntity();
		}
    }

    private void newOntology() {
    	String rootName = (String) JOptionPane.showInputDialog(this,
				"Ontology Name:\n", "New Ontology",
				JOptionPane.PLAIN_MESSAGE, null, null, null);

		if ((rootName == null) || (rootName.length() <= 0)) {
			JOptionPane.showMessageDialog(this, "Require a valid name",
					"Ontology Error", JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		Entity newOntologyRoot = EJBFactory.getRemoteAnnotationBean().createOntologyRoot(System.getenv("USER"), rootName);
    	tabbedPane.setSelectedIndex(0);
    	privateTable.reloadData(newOntologyRoot);
    }
    
    private void importOntology() {
    	
    	final JFileChooser fc = new JFileChooser();
    	int returnVal = fc.showOpenDialog(this);
        if (returnVal != JFileChooser.APPROVE_OPTION) return;
        
        try {
            File file = fc.getSelectedFile();
            owlLoader = new OWLDataLoader(file) {

				protected void hadSuccess() {
		        	privateTable.reloadData(getResult());
				}
				
				protected void hadError(Throwable error) {
					error.printStackTrace();
					JOptionPane.showMessageDialog(OntologyManager.this, "Error loading ontology",
							"Ontology Import Error", JOptionPane.ERROR_MESSAGE);
		        	privateTable.reloadData(null);
				}
        	};

			String rootName = (String) JOptionPane.showInputDialog(this,
					"New Ontology Name:\n", "Import Ontology",
					JOptionPane.PLAIN_MESSAGE, null, null, owlLoader.getOntologyName());

			if ((rootName == null) || (rootName.length() <= 0)) {
				JOptionPane.showMessageDialog(this, "Require a valid name",
						"Ontology Error", JOptionPane.WARNING_MESSAGE);
				return;
			}
			
	    	tabbedPane.setSelectedIndex(0);
	    	privateTable.setLoading(true);

	        progressMonitor = new ProgressMonitor(this,"Importing OWL","", 0, 100);
	        progressMonitor.setProgress(0);
	        
	        owlLoader.addPropertyChangeListener(this);
	        owlLoader.setOntologyName(rootName);  
	        owlLoader.execute();
        	
        }
        catch (OWLException ex) {
        	ex.printStackTrace();
        	JOptionPane.showMessageDialog(this, "Error reading file", "Error", JOptionPane.ERROR_MESSAGE);
        	return;
        }
    }
    
    /**
     * Invoked when the owl loader's progress property changes.
     */
    public void propertyChange(PropertyChangeEvent e) {
        if ("progress".equals(e.getPropertyName())) {
            int progress = (Integer) e.getNewValue();
            progressMonitor.setProgress(progress);
            String message = String.format("Completed %d%%", progress);
            progressMonitor.setNote(message);
            if (progressMonitor.isCanceled()) {
            	owlLoader.cancel(true);
            }
        }
    }

    private void loadSelected() {

    	Entity root = getSelectedOntology();
		if (root != null) {
			ontologyOutline.initializeTree(root);
            setVisible(false);
		}
		else {
			JOptionPane.showMessageDialog(this, "Please select an ontology to load",
					"Ontology Error", JOptionPane.WARNING_MESSAGE);
		}
    }
    
    private void shareSelected() {

    	Entity root = getSelectedOntology();
		if (root != null) {
			// TODO: implement sharing
		}
		else {
			JOptionPane.showMessageDialog(this, "Please select an ontology to share",
					"Ontology Error", JOptionPane.WARNING_MESSAGE);
		}
    }
    
    private void deleteSelected() {

    	final Entity root = getSelectedOntology();
		if (root != null) {
			int deleteConfirmation = JOptionPane.showConfirmDialog(
					this, "Are you sure you want to delete the ontology named '"
					+root.getName()+"'?", "Delete Ontology", JOptionPane.YES_NO_OPTION);
			
			if (deleteConfirmation != 0) return;
			
	    	tabbedPane.setSelectedIndex(0);
	    	privateTable.setLoading(true);

	        SimpleWorker worker = new SimpleWorker() {
	        	
	            protected void doStuff() throws Exception {
	            	EJBFactory.getRemoteAnnotationBean().removeOntologyTerm(System.getenv("USER"), root.getId().toString());
	            }

				protected void hadSuccess() {
		            if (root.getId().equals(ontologyOutline.getCurrentOntology().getId())) {
		                ontologyOutline.initializeTree(null);
		            }
		        	privateTable.reloadData(null);
				}
				
				protected void hadError(Throwable error) {
					error.printStackTrace();
					JOptionPane.showMessageDialog(OntologyManager.this, "Error deleting ontology",
							"Ontology Deletion Error", JOptionPane.ERROR_MESSAGE);
			    	privateTable.setLoading(false);
				}
	            
	        };
	        worker.execute();
		}
		else {
			JOptionPane.showMessageDialog(this, "Please select an ontology to delete",
					"Ontology Error", JOptionPane.WARNING_MESSAGE);
		}
    }

    private void cloneSelected() {

    	final Entity root = getSelectedOntology();
		if (root != null) {

			final String rootName = (String) JOptionPane.showInputDialog(this,
					"New Ontology Name:\n", "Clone Ontology",
					JOptionPane.PLAIN_MESSAGE, null, null, root.getName());

			if ((rootName == null) || (rootName.length() <= 0)) {
				JOptionPane.showMessageDialog(this, "Require a valid name",
						"Ontology Error", JOptionPane.WARNING_MESSAGE);
				return;
			}
			
	    	tabbedPane.setSelectedIndex(0);
	    	privateTable.setLoading(true);

	        SimpleWorker worker = new SimpleWorker() {
	        	
	        	private Entity newRoot;
	        	
	            protected void doStuff() throws Exception {
	            	newRoot = EJBFactory.getRemoteAnnotationBean().cloneEntityTree(root, System.getenv("USER"), rootName);
	            }

				protected void hadSuccess() {
		        	privateTable.reloadData(newRoot);
				}
				
				protected void hadError(Throwable error) {
					error.printStackTrace();
					JOptionPane.showMessageDialog(OntologyManager.this, "Error cloning ontology",
							"Ontology Clone Error", JOptionPane.ERROR_MESSAGE);
			    	privateTable.setLoading(false);
				}
	            
	        };
	        worker.execute();
			
		}
		else {
			JOptionPane.showMessageDialog(this, "Please select an ontology to clone",
					"Ontology Error", JOptionPane.WARNING_MESSAGE);
		}
    }
    
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

		if (ONTOLOGY_NEW_COMMAND.equals(cmd)) {
			newOntology();
		} 
        else if (IMPORT_OWL_COMMAND.equals(cmd)) {
        	importOntology();
        }
        else if (CANCEL_COMMAND.equals(cmd)) {
            setVisible(false);
        }
        else if (ONTOLOGY_LOAD_COMMAND.equals(cmd)) {
        	loadSelected();
        }
        else if (ONTOLOGY_CLONE_COMMAND.equals(cmd)) {
        	cloneSelected();
        }
        else if (ONTOLOGY_SHARE_COMMAND.equals(cmd)) {
        	shareSelected();
        }
        else if (ONTOLOGY_DELETE_COMMAND.equals(cmd)) {
            deleteSelected();
        }
    }
}
