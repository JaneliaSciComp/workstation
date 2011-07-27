package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.outline.choose.EntityChooser;
import org.janelia.it.FlyWorkstation.gui.framework.outline.choose.OntologyElementChooser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.types.*;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;

/**
 * A dialog for creating a new annotation session, or editing an existing one. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationSessionPropertyDialog extends JDialog implements ActionListener {

    private static final String CANCEL_COMMAND = "cancel";
    private static final String SESSION_SAVE_COMMAND = "session_save";
	
    private JButton okButton;
    private JButton cancelButton;
	private TextField nameValueField;
	private JLabel ownerValueLabel;
    
    private SelectionTreePanel<Entity> entityTreePanel;
    private SelectionTreePanel<OntologyElement> categoryTreePanel;
    
    private AnnotationSessionTask task;
    
	public AnnotationSessionPropertyDialog(final EntityOutline entityOutline, final OntologyOutline ontologyOutline) {

        setModalityType(ModalityType.APPLICATION_MODAL);
        setPreferredSize(new Dimension(800, 600));
        getContentPane().setLayout(new BorderLayout());
        
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
        nameValueField.setColumns(60);
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
        
        entityTreePanel = new SelectionTreePanel<Entity>("Entities to annotation") {
    		public void addClicked() {

    		    final EntityChooser entityChooser = new EntityChooser("Choose entities to annotation", entityOutline);
    			int returnVal = entityChooser.showDialog(AnnotationSessionPropertyDialog.this);
    	        if (returnVal != EntityChooser.CHOOSE_OPTION) return;
    	        
    	        Utils.setWaitingCursor(entityTreePanel);
    	        
    	        SimpleWorker worker = new SimpleWorker() {
					
    	        	private List<Entity> entities = new ArrayList<Entity>();

					protected void doStuff() throws Exception {
		    	        for(Entity entity : entityChooser.getChosenEntities()) {
		    	        	if (!entity.getEntityType().getName().equals(EntityConstants.TYPE_TIF_2D)) {
		                		List<Entity> descs = entityChooser.getEntityTree().getDescendantsOfType(entity, EntityConstants.TYPE_TIF_2D);
		                		entities.addAll(descs);
		    	        	}
		    	        	else {
		    	        		entities.add(entity);
		    	        	}
		    	        }
					}
					
					protected void hadSuccess() {
		    	        List<DefaultMutableTreeNode> nodes = new ArrayList<DefaultMutableTreeNode>();
						for(Entity entity : entities) {
	    	        		nodes.add(entityTreePanel.addItemUniquely(entity));
						}
		    	        getDynamicTree().selectAndShowNodes(nodes);
		    	        SwingUtilities.updateComponentTreeUI(AnnotationSessionPropertyDialog.this);
		    	        Utils.setDefaultCursor(entityTreePanel);
					}
					
					protected void hadError(Throwable error) {
						error.printStackTrace();
		    	        Utils.setDefaultCursor(entityTreePanel);
		    	        JOptionPane.showMessageDialog(AnnotationSessionPropertyDialog.this, "Error adding entities", "Error", JOptionPane.ERROR_MESSAGE);
					}
					
				};
				
				worker.execute();
    		}
    		
			@Override
			// Must override this because Entity does not implement equals()
			public boolean containsItem(Entity entity) {
		    	DefaultMutableTreeNode rootNode = getDynamicTree().getRootNode();
				for (Enumeration e = rootNode.children(); e.hasMoreElements();) {
					DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
					if (((Entity)childNode.getUserObject()).getId().equals(entity.getId())) {
						return true;
					}
				}
				return false;
			}
        };
        c.gridx = 0;
        c.gridy = 0;
        treesPanel.add(entityTreePanel);
        
        categoryTreePanel = new SelectionTreePanel<OntologyElement>("Annotations to complete") {
    		public void addClicked() {

    	        Utils.setWaitingCursor(categoryTreePanel);
    	        
    		    OntologyElementChooser ontologyChooser = new OntologyElementChooser("Choose annotations to complete", ontologyOutline.getCurrentOntology());
    			int returnVal = ontologyChooser.showDialog(AnnotationSessionPropertyDialog.this);
    	        if (returnVal != OntologyElementChooser.CHOOSE_OPTION) return;
    	        List<DefaultMutableTreeNode> nodes = new ArrayList<DefaultMutableTreeNode>();
    	        
    	        boolean ignoredSome = false;
    	        for(OntologyElement element : ontologyChooser.getChosenElements()) {
    	        	
    	        	if (element.getType() instanceof Tag) {
    	        		ignoredSome = true;
    	        	}
    	        	else {
    	        		nodes.add(categoryTreePanel.addItem(element));
    	        	}
    	        }
    	        
    	        if (ignoredSome) {
    	        	JOptionPane.showMessageDialog(AnnotationSessionPropertyDialog.this, 
    	        			"You selected some tags, which cannot be added to a session. Add the tags' categories instead.", 
    	        			"Ignoring tags", JOptionPane.ERROR_MESSAGE);
    	        }
    	        
    	        getDynamicTree().selectAndShowNodes(nodes);
    	        Utils.setDefaultCursor(categoryTreePanel);
    	        SwingUtilities.updateComponentTreeUI(categoryTreePanel);
    		}
        };
        
        c.gridx = 1;
        c.gridy = 0;
        treesPanel.add(categoryTreePanel);
        
        add(treesPanel, BorderLayout.CENTER);
        
        okButton = new JButton("Save");
        okButton.setActionCommand(SESSION_SAVE_COMMAND);
        okButton.setToolTipText("Save this annotation session");
        okButton.addActionListener(this);

        cancelButton = new JButton("Cancel");
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

			@Override
			public void windowActivated(WindowEvent e) {
				nameValueField.requestFocus();
				nameValueField.selectAll();
			}
		});
	}
	
	private void init() {
        if (entityTreePanel.getDynamicTree() == null) setLocationRelativeTo(SessionMgr.getSessionMgr().getActiveBrowser());
        
        entityTreePanel.createNewTree();
        entityTreePanel.getDynamicTree().setCellRenderer(new EntityTreeCellRenderer());
        
        categoryTreePanel.createNewTree();
        categoryTreePanel.getDynamicTree().setCellRenderer(new OntologyTreeCellRenderer());
	}

    public AnnotationSessionTask getTask() {
		return task;
	}
    
	public void showForNewSession(String name, List<Entity> entities) {

        init();
        
        this.task = null;
        setTitle("New Annotation Session");
        nameValueField.setText(name);
        ownerValueLabel.setText(System.getenv("USER"));

        for(Entity entity : entities) {
        	entityTreePanel.addItem(entity);
        }

        SwingUtilities.updateComponentTreeUI(this);
        setVisible(true);
	}
	
	public void showForSession(AnnotationSessionTask task) {

        init();

		this.task = task;
        setTitle("Edit Annotation Session");

		String name = task.getParameter(AnnotationSessionTask.PARAM_sessionName);
		String entityIds = task.getParameter(AnnotationSessionTask.PARAM_annotationTargets);
		String categoryIds = task.getParameter(AnnotationSessionTask.PARAM_annotationCategories);

        nameValueField.setText(name);
        ownerValueLabel.setText(task.getOwner());
        
		String[] entityIdArray = entityIds.split(",");
        for(String entityId : entityIdArray) {
        	if (Utils.isEmpty(entityId)) continue;
        	Entity entity = EJBFactory.getRemoteAnnotationBean().getEntityById(entityId);
        	entityTreePanel.addItem(entity);
        }

		String[] categoryIdArray = categoryIds.split(",");
        for(String entityId : categoryIdArray) {
        	if (Utils.isEmpty(entityId)) continue;
        	Entity entity = EJBFactory.getRemoteAnnotationBean().getEntityById(entityId);
        	categoryTreePanel.addItem(new OntologyElement(entity, null));
        }

        SwingUtilities.updateComponentTreeUI(this);
        setVisible(true);
	}
	
	protected void save() {
        
        try {
            List<String> entityIdList = new ArrayList<String>();
            for(Object o : entityTreePanel.getItems()) {
            	entityIdList.add(((Entity)o).getId().toString());
            }
            String entityIds = Task.csvStringFromCollection(entityIdList);
            
            List<String> categoryIdList = new ArrayList<String>();
            for(Object o : categoryTreePanel.getItems()) {
            	categoryIdList.add(((OntologyElement)o).getId().toString());
            }
            String categoryIds = Task.csvStringFromCollection(categoryIdList);
            
            if (task == null) {
            	task = new AnnotationSessionTask(null, System.getenv("USER"), null, null);
            }
            
            task.setParameter(AnnotationSessionTask.PARAM_sessionName, nameValueField.getText());
            task.setParameter(AnnotationSessionTask.PARAM_annotationTargets, entityIds);
            task.setParameter(AnnotationSessionTask.PARAM_annotationCategories, categoryIds);
            task = (AnnotationSessionTask)EJBFactory.getRemoteComputeBean().saveOrUpdateTask(task);
            System.out.println("Saved annotation session with taskId="+task.getObjectId());
            
            Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
            browser.getOutlookBar().setVisibleBarByName(Browser.BAR_SESSION);
            browser.getAnnotationSessionOutline().initializeTree(task.getObjectId());
        }
        catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(AnnotationSessionPropertyDialog.this, "Error creating new session", "Session Save Error", JOptionPane.ERROR_MESSAGE);
        }
	}
	
	public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

		if (CANCEL_COMMAND.equals(cmd)) {
			setVisible(false);
		} 
		else if (SESSION_SAVE_COMMAND.equals(cmd)) {
			save();
			setVisible(false);
		} 
    }
}
