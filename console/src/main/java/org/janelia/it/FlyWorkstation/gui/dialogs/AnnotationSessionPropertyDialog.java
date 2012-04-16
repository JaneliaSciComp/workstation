package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import loci.plugins.config.SpringUtilities;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.choose.EntityChooser;
import org.janelia.it.FlyWorkstation.gui.dialogs.choose.OntologyElementChooser;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.outline.*;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.types.Tag;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;

/**
 * A dialog for creating a new annotation session, or editing an existing one.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationSessionPropertyDialog extends ModalDialog {

    private JPanel attrPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField nameValueField;
    private JLabel ownerValueLabel;

    private SelectionTreePanel<Entity> entityTreePanel;
    private SelectionTreePanel<OntologyElement> categoryTreePanel;

    private AnnotationSessionTask task;

    public AnnotationSessionPropertyDialog(final EntityOutline entityOutline, final OntologyOutline ontologyOutline) {

        GridBagConstraints c = new GridBagConstraints();

        attrPanel = new JPanel(new SpringLayout());
        attrPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10), 
        		BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Session Properties")));

        JLabel nameLabel = new JLabel("Name:");
        nameValueField = new JTextField(40);
        nameLabel.setLabelFor(nameValueField);
        attrPanel.add(nameLabel);
        attrPanel.add(nameValueField);
        
        JLabel ownerLabel = new JLabel("Owner:");
        ownerValueLabel = new JLabel();
        nameLabel.setLabelFor(ownerValueLabel);
        attrPanel.add(ownerLabel);
        attrPanel.add(ownerValueLabel);
        
        add(attrPanel, BorderLayout.NORTH);
        SpringUtilities.makeCompactGrid(attrPanel, attrPanel.getComponentCount()/2, 2, 6, 6, 6, 6);

        JPanel treesPanel = new JPanel(new GridLayout(1, 2));

        entityTreePanel = new SelectionTreePanel<Entity>("Entities to annotate") {
            public void addClicked() {

                final EntityChooser entityChooser = new EntityChooser("Choose entities to annotate", entityOutline);
                int returnVal = entityChooser.showDialog(AnnotationSessionPropertyDialog.this);
                if (returnVal != EntityChooser.CHOOSE_OPTION) return;

                Utils.setWaitingCursor(entityTreePanel);

                SimpleWorker worker = new SimpleWorker() {

                    private List<Entity> entities = new ArrayList<Entity>();

                    protected void doStuff() throws Exception {
                        for (EntityData entityData : entityChooser.getChosenElements()) {
                        	Entity entity = entityData.getChildEntity();
                        	Entity entityTree = ModelMgr.getModelMgr().getEntityTree(entity.getId());
                            List<Entity> descs = entityTree.getDescendantsOfType(EntityConstants.TYPE_NEURON_FRAGMENT, true);
                            entities.addAll(descs);
                        }
                    }

                    protected void hadSuccess() {
                        List<DefaultMutableTreeNode> nodes = new ArrayList<DefaultMutableTreeNode>();
                        for (Entity entity : entities) {
                        	DefaultMutableTreeNode node = entityTreePanel.addItemUniquely(entity);
                        	if (node!=null) { // Node is null if it could not be added uniquely
                        		nodes.add(node);
                        	}
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
                for (Enumeration e = rootNode.children(); e.hasMoreElements(); ) {
                    DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
                    if (((Entity) childNode.getUserObject()).getId().equals(entity.getId())) {
                        return true;
                    }
                }
                return false;
            }
        };
        c.gridx = 0;
        c.gridy = 0;
        entityTreePanel.setPreferredSize(new Dimension(500, 500));
        treesPanel.add(entityTreePanel);

        categoryTreePanel = new SelectionTreePanel<OntologyElement>("Annotations to complete") {
            public void addClicked() {

                Utils.setWaitingCursor(categoryTreePanel);

                OntologyElementChooser ontologyChooser = new OntologyElementChooser("Choose annotations to complete", ontologyOutline.getCurrentOntology());
                int returnVal = ontologyChooser.showDialog(AnnotationSessionPropertyDialog.this);
                if (returnVal != OntologyElementChooser.CHOOSE_OPTION) return;
                List<DefaultMutableTreeNode> nodes = new ArrayList<DefaultMutableTreeNode>();

                boolean ignoredSome = false;
                for (OntologyElement element : ontologyChooser.getChosenElements()) {

                    if (element.getType() instanceof Tag) {
                        ignoredSome = true;
                    }
                    else {
                        nodes.add(categoryTreePanel.addItem(element));
                    }
                }

                if (ignoredSome) {
                    JOptionPane.showMessageDialog(AnnotationSessionPropertyDialog.this, "You selected some tags, which cannot be added to a session. Add the tags' categories instead.", "Ignoring tags", JOptionPane.ERROR_MESSAGE);
                }

                getDynamicTree().selectAndShowNodes(nodes);
                Utils.setDefaultCursor(categoryTreePanel);
                SwingUtilities.updateComponentTreeUI(categoryTreePanel);
            }
        };

        c.gridx = 1;
        c.gridy = 0;
        categoryTreePanel.setPreferredSize(new Dimension(500, 500));
        treesPanel.add(categoryTreePanel);

        add(treesPanel, BorderLayout.CENTER);

        okButton = new JButton("Save");
        okButton.setToolTipText("Save this annotation session");
        okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
                save();
	            setVisible(false);
			}
		});

        cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            setVisible(false);
			}
		});

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(cancelButton);
        add(buttonPane, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                nameValueField.requestFocus();
                nameValueField.selectAll();
            }
        });
    }

    private void init() {
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
        ownerValueLabel.setText(SessionMgr.getUsername());

        for (Entity entity : entities) {
            entityTreePanel.addItem(entity);
        }

        packAndShow();
    }

    public void showForSession(AnnotationSession session) {

        init();

        this.task = session.getTask();
        setTitle("Edit Annotation Session");

        nameValueField.setText(session.getName());
        ownerValueLabel.setText(session.getOwner());

        for (Entity entity : session.getEntities()) {
            entityTreePanel.addItem(entity);
        }

        for (OntologyElement element : session.getCategories()) {
            categoryTreePanel.addItem(element);
        }

        packAndShow();
    }

    protected void save() {

        try {
            List<String> entityIdList = new ArrayList<String>();
            for (Object o : entityTreePanel.getItems()) {
                entityIdList.add(((Entity) o).getId().toString());
            }
            String entityIds = Task.csvStringFromCollection(entityIdList);

            List<String> categoryIdList = new ArrayList<String>();
            for (Object o : categoryTreePanel.getItems()) {
                categoryIdList.add(((OntologyElement) o).getId().toString());
            }
            String categoryIds = Task.csvStringFromCollection(categoryIdList);

            if (task == null) {
                task = new AnnotationSessionTask(null, SessionMgr.getUsername(), null, null);
            }

            task.setParameter(AnnotationSessionTask.PARAM_sessionName, nameValueField.getText());
            task.setParameter(AnnotationSessionTask.PARAM_annotationTargets, entityIds);
            task.setParameter(AnnotationSessionTask.PARAM_annotationCategories, categoryIds);
            task = (AnnotationSessionTask) ModelMgr.getModelMgr().saveOrUpdateTask(task);
            System.out.println("Saved annotation session with taskId=" + task.getObjectId());

            Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
            browser.getOutlookBar().setVisibleBarByName(Browser.BAR_SESSIONS);
            final SessionOutline sessionOutline = browser.getAnnotationSessionOutline();
            sessionOutline.loadAnnotationSessions(new Callable<Void>() {
				public Void call() throws Exception {
					// Wait until the sessions are loaded before getting the new one and selecting it
		            ModelMgr.getModelMgr().setCurrentAnnotationSession(
		            		sessionOutline.getSessionById(task.getObjectId()));
					return null;
				}
            });
        }
        catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(AnnotationSessionPropertyDialog.this, "Error creating new session", "Session Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
