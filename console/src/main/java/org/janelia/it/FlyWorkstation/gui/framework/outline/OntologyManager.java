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

import javax.swing.*;

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * A dialog for managing ontologies that can be loaded into the OntologyOutline.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyManager extends JDialog implements ActionListener {

    private static final String ONTOLOGY_LOAD_COMMAND = "ontology_load";
    private static final String ONTOLOGY_DELETE_COMMAND = "ontology_delete";
    private static final String ONTOLOGY_CLONE_COMMAND = "ontology_clone";
    private static final String ONTOLOGY_SHARE_COMMAND = "ontology_share";
    private static final String ONTOLOGY_NEW_COMMAND = "ontology_new";
    private static final String CANCEL_COMMAND = "clicked_cancel";

    private JPopupMenu privateMenu;
    private JPopupMenu publicMenu;
    
    private final AbstractEntityTable privateTable;
    private final AbstractEntityTable publicTable;
    
    private OntologyOutline ontologyOutline;
	private JTabbedPane tabbedPane;   
    
    public OntologyManager(final OntologyOutline ontologyOutline) {

    	this.ontologyOutline = ontologyOutline;
    	
        setTitle("Ontology Manager");
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
        
        JButton okButton = new JButton("Load");
        okButton.setActionCommand(ONTOLOGY_LOAD_COMMAND);
        okButton.setToolTipText("Load the selected ontology");
        okButton.addActionListener(this);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand(CANCEL_COMMAND);
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
    
	/**
	 * Reload the ontologies and show the dialog.
	 */
    public void showDialog() {
    	publicTable.reloadData(null);
    	privateTable.reloadData(ontologyOutline.getCurrentOntology());
    	tabbedPane.setSelectedIndex(0);
    	setVisible(true);
    }
    
    private Entity getSelectedOntology() {
		if (tabbedPane.getSelectedIndex() == 0) {
			return privateTable.getSelectedEntity();
		}
		else {
			return publicTable.getSelectedEntity();
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

    	Entity root = getSelectedOntology();
		if (root != null) {
			int deleteConfirmation = JOptionPane.showConfirmDialog(
					this, "Are you sure you want to delete the ontology named '"
					+root.getName()+"'?", "Delete Ontology", JOptionPane.YES_NO_OPTION);
			
			if (deleteConfirmation != 0) return;
			
            EJBFactory.getRemoteAnnotationBean().removeOntologyTerm(System.getenv("USER"), root.getId().toString());
            
            if (root.getId().equals(ontologyOutline.getCurrentOntology().getId())) {
                ontologyOutline.initializeTree(null);
            }
            
        	privateTable.reloadData(null);
		}
		else {
			JOptionPane.showMessageDialog(this, "Please select an ontology to delete",
					"Ontology Error", JOptionPane.WARNING_MESSAGE);
		}
    }

    private void cloneSelected() {

    	Entity root = getSelectedOntology();
		if (root != null) {

			String rootName = (String) JOptionPane.showInputDialog(this,
					"Ontology Name:\n", "New Ontology",
					JOptionPane.PLAIN_MESSAGE, null, null, null);

			if ((rootName == null) || (rootName.length() <= 0)) {
				JOptionPane.showMessageDialog(this, "Require a valid name",
						"Ontology Error", JOptionPane.WARNING_MESSAGE);
				return;
			}

			try {
				Entity newOntologyRoot = EJBFactory.getRemoteAnnotationBean().cloneEntityTree(root, System.getenv("USER"), rootName);
	        	privateTable.reloadData(newOntologyRoot);
			}
			catch (DaoException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "Error cloning ontology", "Error", JOptionPane.ERROR_MESSAGE);
			}
			
			// Move to private tab to show cloned 
			tabbedPane.setSelectedIndex(0);
		}
		else {
			JOptionPane.showMessageDialog(this, "Please select an ontology to clone",
					"Ontology Error", JOptionPane.WARNING_MESSAGE);
		}
    }
    
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

		if (ONTOLOGY_NEW_COMMAND.equals(cmd)) {
			String rootName = (String) JOptionPane.showInputDialog(this,
					"Ontology Name:\n", "New Ontology",
					JOptionPane.PLAIN_MESSAGE, null, null, null);

			if ((rootName == null) || (rootName.length() <= 0)) {
				JOptionPane.showMessageDialog(this, "Require a valid name",
						"Ontology Error", JOptionPane.WARNING_MESSAGE);
				return;
			}
			
			Entity newOntologyRoot = EJBFactory.getRemoteAnnotationBean().createOntologyRoot(System.getenv("USER"), rootName);
	    	privateTable.reloadData(newOntologyRoot);
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
