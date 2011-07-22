package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * A dialog for creating a new annotation session, or editing an existing one. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationSessionPropertyDialog extends JDialog implements ActionListener {

    private static final String CANCEL_COMMAND = "cancel";
    private static final String SESSION_SAVE_COMMAND = "session_save";
	
	private TextField nameValueField;
	private JLabel ownerValueLabel;
    
    private SelectionTreePanel entityTreePanel;
    private SelectionTreePanel categoryTreePanel;
	
    private OntologyElementChooserDialog ontologyChooser;
    
	public AnnotationSessionPropertyDialog(final OntologyOutline ontologyOutline) {

        setModalityType(ModalityType.APPLICATION_MODAL);
        setPreferredSize(new Dimension(800, 600));
        getContentPane().setLayout(new BorderLayout());
        setLocationRelativeTo(ConsoleApp.getMainFrame());
        
        GridBagConstraints c = new GridBagConstraints();
        
        JPanel attrPanel = new JPanel(new GridBagLayout());
        attrPanel.setBorder(
        		BorderFactory.createCompoundBorder(
        				BorderFactory.createEmptyBorder(10, 10, 0, 10), 
        				BorderFactory.createTitledBorder(
        								BorderFactory.createEtchedBorder(), "Session Properties")));

        
        
        JLabel nameLabel = new JLabel("Name: ");
        nameLabel.setAlignmentX(RIGHT_ALIGNMENT);
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 10, 0);
        attrPanel.add(nameLabel, c);

        nameValueField = new TextField();
        nameValueField.setColumns(40);
        c.gridx = 1;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 10, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        attrPanel.add(nameValueField, c);
        
        JLabel ownerLabel = new JLabel("Owner: ");
        nameLabel.setAlignmentX(RIGHT_ALIGNMENT);
        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(0, 0, 10, 0);
        attrPanel.add(ownerLabel, c);

        ownerValueLabel = new JLabel("");
        c.gridx = 1;
        c.gridy = 1;
        c.insets = new Insets(0, 0, 10, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        attrPanel.add(ownerValueLabel, c);
        
        add(attrPanel, BorderLayout.NORTH);
        
        
        JPanel treesPanel = new JPanel(new GridLayout(1,2));
        
        entityTreePanel = new SelectionTreePanel("Entities to annotation") {
    		public void showChooser() {
    			
    		}
        };
        c.gridx = 0;
        c.gridy = 0;
        treesPanel.add(entityTreePanel);
        
        categoryTreePanel = new SelectionTreePanel("Annotations to complete") {
    		public void showChooser() {
    			ontologyChooser.showForOntology(ontologyOutline.getCurrentOntology());
    		}
        };
        c.gridx = 1;
        c.gridy = 0;
        treesPanel.add(categoryTreePanel);
        
        add(treesPanel, BorderLayout.CENTER);
        
        JButton okButton = new JButton("Save");
        okButton.setActionCommand(SESSION_SAVE_COMMAND);
        okButton.setToolTipText("Save this annotation session");
        okButton.addActionListener(this);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand(CANCEL_COMMAND);
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(this);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(cancelButton);
        add(buttonPane, BorderLayout.SOUTH);

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                setVisible(false);
            }
        });

        this.ontologyChooser = new OntologyElementChooserDialog();
        ontologyChooser.pack();
	}

	public class SelectionTreePanel extends JPanel implements ActionListener {

	    private static final String ADD_COMMAND = "add";
	    private static final String REMOVE_COMMAND = "remove";
		
	    private DynamicTree tree;		
		private JPanel treePanel;

	    public SelectionTreePanel(String title) {
	        super(new BorderLayout());
	        
	        setBorder(BorderFactory.createCompoundBorder(
	        				BorderFactory.createEmptyBorder(0, 10, 0, 10), 
	        				BorderFactory.createTitledBorder(
	        						BorderFactory.createCompoundBorder(
		    	        				BorderFactory.createEmptyBorder(),
		    	        				BorderFactory.createEmptyBorder(10, 10, 0, 10)), title)));
	        
	        treePanel = new JPanel(new BorderLayout());
	        
	        add(treePanel, BorderLayout.CENTER);

	        JPanel buttonPane = new JPanel();
	        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
	        buttonPane.setBackground(new Color(0.8f, 0.8f, 0.8f));
	        
	        buttonPane.add(Box.createHorizontalGlue());
	        
	        JButton addButton = new JButton("Add");
	        addButton.setActionCommand(ADD_COMMAND);
	        addButton.addActionListener(this);
	        buttonPane.add(addButton);

	        JButton removeButton = new JButton("Remove");
	        removeButton.setActionCommand(REMOVE_COMMAND);
	        removeButton.addActionListener(this);
	        buttonPane.add(removeButton);
	        
	        add(buttonPane, BorderLayout.SOUTH);
	        
	    }
	    
	    public DynamicTree getDynamicTree() {
			return tree;
		}

		private void createNewTree() {
	    	
	    	tree = new DynamicTree("ROOT", false, false);
	        tree.getTree().setRootVisible(false);
	        tree.getTree().getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
	        
	        // Replace the cell renderer
 	        tree.setCellRenderer(new EntityTreeCellRenderer());

	        treePanel.removeAll();
	        treePanel.add(tree);
	    }
		
		public void showChooser() {
			
		}
		
	    public void actionPerformed(ActionEvent e) {
	        String command = e.getActionCommand();

			if (REMOVE_COMMAND.equals(command)) {
				TreePath[] paths = tree.getTree().getSelectionPaths();
				if (paths == null) return;
				for(TreePath path : paths) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
					tree.removeNode(node);
				}
	            SwingUtilities.updateComponentTreeUI(this);
			}
			else if (ADD_COMMAND.equals(command)) {
				showChooser();
			}
	    }
	}
	
	public void showForNewSession(String name, List<Entity> entities) {

        setTitle("New Annotation Session");
        nameValueField.setText(name);
        ownerValueLabel.setText(System.getenv("USER"));

        entityTreePanel.createNewTree();
        
        for(Entity entity : entities) {
        	entityTreePanel.getDynamicTree().addObject(entityTreePanel.getDynamicTree().getRootNode(), entity);
        }
        
        categoryTreePanel.createNewTree();

        SwingUtilities.updateComponentTreeUI(this);
        setVisible(true);
	}
	
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

		if (CANCEL_COMMAND.equals(cmd)) {
			setVisible(false);
		} 
		else if (SESSION_SAVE_COMMAND.equals(cmd)) {
			// TODO: save
		} 
    }
}
