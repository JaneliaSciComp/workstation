package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tree.DynamicTree;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 2:09 PM
 * This class is the initial outline of the data file tree
 */
public class SessionOutline extends JScrollPane implements Cloneable {
	
    private static final String NO_DATASOURCE = "No Tasks Available";
    private static final String ANNOTATION_SESSIONS = "Annotation Sessions";
    
    private Browser consoleFrame;
    private JPopupMenu popupMenu;
    private DynamicTree treePanel;

    public SessionOutline(Browser consoleFrame) {
        this.consoleFrame = consoleFrame;

        treePanel = new DynamicTree(ANNOTATION_SESSIONS) {
			@Override
			protected void showPopupMenu(MouseEvent e) {
				handlePopupMenu(e);
			}

			@Override
			protected void nodeClicked(MouseEvent e) {
                TreePath tmpPath = new TreePath(getCurrentNode().getPath());
                if (tmpPath.getLastPathComponent().toString().equals(NO_DATASOURCE)) {return;}
                String tmpTask = tmpPath.getLastPathComponent().toString();
                if (null!=tmpTask && !"".equals(tmpTask)) {
                    SessionOutline.this.consoleFrame.setMostRecentFileOutlinePath(tmpTask);
                }
			}
			
        };

        // todo Change the root to not visible
        treePanel.getTree().setRootVisible(true);
        rebuildDataModel();
    }

    private void rebuildTreeModel() {
        DefaultMutableTreeNode newRootNode = buildTreeModel();
        DefaultTreeModel newModel = new DefaultTreeModel(newRootNode);
        treePanel.getTree().setModel(newModel);
    }

    private void handlePopupMenu(MouseEvent e) {
    	
    	Object o = treePanel.getCurrentNode().getUserObject();
    	
    	if (o instanceof AnnotationSession) {
	    	final AnnotationSession session = (AnnotationSession)o;
	    	
	        JMenuItem editMenuItem = new JMenuItem("Edit");
	        editMenuItem.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent actionEvent) {
	        		SessionMgr.getSessionMgr().getActiveBrowser().getAnnotationSessionPropertyPanel().showForSession(session.getTask());
	            }
	        });
	        JMenuItem deleteMenuItem = new JMenuItem("Delete");
	        deleteMenuItem.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent actionEvent) {
	            	
	            }
	        });
	        
	    	popupMenu = new JPopupMenu();
	        popupMenu.add(deleteMenuItem);
	        popupMenu.add(editMenuItem);
	        popupMenu.show((Component)e.getSource(), e.getX(), e.getY());
    	}
    }

    private DefaultMutableTreeNode buildTreeModel() {
        // Prep the null node, just in case
        DefaultMutableTreeNode nullNode = new DefaultMutableTreeNode(NO_DATASOURCE);
        nullNode.setUserObject(NO_DATASOURCE);
        nullNode.setAllowsChildren(false);
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            if (null!=computeBean) {
                DefaultMutableTreeNode top = new DefaultMutableTreeNode();
                try {
                    List<Task> tmpTasks = computeBean.getUserTasksByType(AnnotationSessionTask.TASK_NAME, System.getenv("USER"));
                    if (null==tmpTasks || tmpTasks.size()<=0) {
                        return nullNode;
                    }
                    top.setUserObject(ANNOTATION_SESSIONS);
                    for (int i = 0; i < tmpTasks.size(); i++) {
                    	AnnotationSessionTask task = (AnnotationSessionTask)tmpTasks.get(i);
                        DefaultMutableTreeNode tmpNode = new DefaultMutableTreeNode(new AnnotationSession(task));
                        top.insert(tmpNode,i);
                        // Add the properties under the items
                        int paramCount = 0;
                        for (TaskParameter tmpParam : tmpTasks.get(i).getTaskParameterSet()) {
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
            return nullNode;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return nullNode;
    }
    
    public void rebuildDataModel() {
    	SimpleWorker loadTask = new SimpleWorker() {
    		
			protected void doStuff() throws Exception {
                rebuildTreeModel();
			}

			protected void hadSuccess() {
                setViewportView(treePanel);
			}

			protected void hadError(Throwable error) {
				error.printStackTrace();
			}
        };

        loadTask.execute();
    }

    public void selectSession(String currentAnnotationSessionTaskId) {
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treePanel.getTree().getModel().getRoot();
        selectSessionNode(rootNode, currentAnnotationSessionTaskId);
    }

    private boolean selectSessionNode(DefaultMutableTreeNode rootNode, String currentAnnotationSessionTaskId) {
        if (rootNode.toString().equals(currentAnnotationSessionTaskId)) {
            treePanel.getTree().getSelectionModel().setSelectionPath(new TreePath(rootNode.getPath()));
            return true;
        }
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            boolean walkSuccess = selectSessionNode((DefaultMutableTreeNode)rootNode.getChildAt(i), currentAnnotationSessionTaskId);
            if (walkSuccess) {
                return true;
            }
        }
        return false;
    }

//    public void clearSelection() {
//        treePanel.getTree().clearSelection();
//    }
}
