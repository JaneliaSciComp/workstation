package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tree.DynamicTree;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 2:09 PM
 * This class is the initial outline of the data file tree
 */
public class SessionOutline extends JPanel{
	
    private static final String NO_DATASOURCE = "No Tasks Available";
    private static final String ANNOTATION_SESSIONS = "Annotation Sessions";
    
    private Browser consoleFrame;
    private JPopupMenu popupMenu;
    
	protected final JPanel treesPanel;
    private DynamicTree dynamicTree;

    public SessionOutline(Browser consoleFrame) {
        super(new BorderLayout());
        
        this.consoleFrame = consoleFrame;

        treesPanel = new JPanel(new BorderLayout());
        add(treesPanel, BorderLayout.CENTER);
        
        initializeTree();
    }

    public void showLoadingIndicator() {
        treesPanel.removeAll();
		treesPanel.add(new JLabel(Icons.loadingIcon));
    }

    public void initializeTree() {
    	initializeTree((Long)null);
    }
    
    public void initializeTree(final Long taskIdToSelect) {

    	showLoadingIndicator();
        this.updateUI();
        
		SimpleWorker loadingWorker = new SimpleWorker() {

			private List<Task> tasks;

            protected void doStuff() throws Exception {
            	tasks = EJBFactory.getRemoteComputeBean().getUserTasksByType(
            			AnnotationSessionTask.TASK_NAME, SessionMgr.getUsername());
            }

			protected void hadSuccess() {
				try {
					initializeTree(tasks);
	                if (taskIdToSelect != null) selectSession(taskIdToSelect);
				}
				catch (Exception e) {
					hadError(e);
				}
			}

			protected void hadError(Throwable error) {
				error.printStackTrace();
				SessionOutline.this.updateUI();
			}

        };

        loadingWorker.execute();
    }

    public void initializeTree(List<Task> tasks) {
    	
    	createNewTree();
    	
        DefaultMutableTreeNode newRootNode = buildTreeModel(tasks);
        DefaultTreeModel newModel = new DefaultTreeModel(newRootNode);
        dynamicTree.getTree().setModel(newModel);

        dynamicTree.expand(dynamicTree.getRootNode(), true);
        
        treesPanel.removeAll();
        treesPanel.add(dynamicTree);
        
        SessionOutline.this.updateUI();
    }

    private void createNewTree() {

        dynamicTree = new DynamicTree(ANNOTATION_SESSIONS) {
			@Override
			protected void showPopupMenu(MouseEvent e) {
		    	Object o = dynamicTree.getCurrentNode().getUserObject();
		    	
		    	if (o instanceof AnnotationSession) {
			    	final AnnotationSession session = (AnnotationSession)o;

			    	popupMenu = new JPopupMenu();
			    	
			        JMenuItem editMenuItem = new JMenuItem("Edit");
			        editMenuItem.addActionListener(new ActionListener() {
			            public void actionPerformed(ActionEvent actionEvent) {
			        		SessionMgr.getSessionMgr().getActiveBrowser().getAnnotationSessionPropertyPanel().showForSession(session);
			            }
			        });
			        popupMenu.add(editMenuItem);
			        
			        if (session.getTask().getOwner().equals(SessionMgr.getUsername())) {
				        JMenuItem deleteMenuItem = new JMenuItem("Delete");
				        deleteMenuItem.addActionListener(new ActionListener() {
				            public void actionPerformed(ActionEvent actionEvent) {
				            	deleteSession(session);
				            }
				        });
				        popupMenu.add(deleteMenuItem);
			        }
			        
			        popupMenu.show((Component)e.getSource(), e.getX(), e.getY());
		    	}
			}

			@Override
			protected void nodeClicked(MouseEvent e) {
		    	Object o = dynamicTree.getCurrentNode().getUserObject();
		    	if (o instanceof AnnotationSession) {
			    	final AnnotationSession session = (AnnotationSession)o;
			    	session.clearDerivedProperties();
			    	SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel().loadImageEntities(session);
                    ModelMgr.getModelMgr().setCurrentAnnotationSesisonTask(session.getTask());
		    	}
			}
			
        };
    }
    
    private DefaultMutableTreeNode buildTreeModel(List<Task> tasks) {
    	
        // Prep the null node, just in case
        DefaultMutableTreeNode nullNode = new DefaultMutableTreeNode(NO_DATASOURCE);
        nullNode.setUserObject(NO_DATASOURCE);
        nullNode.setAllowsChildren(false);
        try {
            DefaultMutableTreeNode top = new DefaultMutableTreeNode();
            try {
                
                if (null==tasks || tasks.size()<=0) {
                    return nullNode;
                }
                top.setUserObject(ANNOTATION_SESSIONS);
                for (Task task : tasks) {
                	if (task.isTaskDeleted()) continue;
                	AnnotationSessionTask asTask = (AnnotationSessionTask)task;
                	AnnotationSession session = new AnnotationSession(asTask);
                    DefaultMutableTreeNode tmpNode = new DefaultMutableTreeNode(session);
                    top.add(tmpNode);
                    // Add the properties under the items
                    int paramCount = 0;
                    for (TaskParameter tmpParam : task.getTaskParameterSet()) {
                        tmpNode.insert(new DefaultMutableTreeNode(tmpParam.getName()+":"+tmpParam.getValue()),paramCount);
                        paramCount++;
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return top;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return nullNode;
    }
    
    private void deleteSession(AnnotationSession session) {
    	
    	if (!session.getTask().getOwner().equals(SessionMgr.getUsername())) {
			JOptionPane.showMessageDialog(consoleFrame, "Only the owner may delete a session", "Cannot Delete", JOptionPane.ERROR_MESSAGE);
    		return;
    	}
    	
		int deleteConfirmation = JOptionPane.showConfirmDialog(consoleFrame, "Are you sure you want to delete this session? All annotations made in this session will be lost.",
				"Delete Session", JOptionPane.YES_NO_OPTION);
		
		if (deleteConfirmation != 0) {
			return;
		}
		
		try {
			// Remove all annotations
			EJBFactory.getRemoteAnnotationBean().removeAllOntologyAnnotationsForSession(
					SessionMgr.getUsername(), session.getTask().getObjectId());
			
			// Remove the task
            EJBFactory.getRemoteComputeBean().deleteTaskById(session.getTask().getObjectId());
            
            // Update Tree UI
            dynamicTree.removeNode(dynamicTree.getCurrentNode());
            SwingUtilities.updateComponentTreeUI(dynamicTree);
		}
		catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(consoleFrame, "Error deleting session", "Error", JOptionPane.ERROR_MESSAGE);
		}
    }
    
    public AnnotationSession getSessionById(long taskId) {
    	DefaultMutableTreeNode rootNode = getDynamicTree().getRootNode();
		for (Enumeration e = rootNode.children(); e.hasMoreElements();) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.nextElement();
			AnnotationSession session = (AnnotationSession)node.getUserObject();
			if (session.getTask().getObjectId().equals(taskId)) {
				return session;
			}
		}
		return null;
    }
    
    public void selectSession(long taskId) {
    	AnnotationSession session = getSessionById(taskId);
    	dynamicTree.navigateToNodeWithObject(session);
		SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel().loadImageEntities(session);
        ModelMgr.getModelMgr().setCurrentAnnotationSesisonTask(session.getTask());
    }
    
    public DynamicTree getDynamicTree() {
    	return dynamicTree;
    }
}
