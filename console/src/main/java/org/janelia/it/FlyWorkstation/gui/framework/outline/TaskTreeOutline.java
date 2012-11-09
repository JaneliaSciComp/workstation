/*
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 2:09 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.TaskDetailsDialog;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tree.DynamicTree;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;
import org.janelia.it.jacs.model.tasks.utility.ContinuousExecutionTask;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.Timer;

/**
 * Provides a tree of the user's Tasks and provides ways to manipulate and view them.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TaskTreeOutline extends JPanel {

    private static final int REFRESH_SECS = 10;
    
    private List<Task> tasks = new ArrayList<Task>();
    private Task selectedTask;
    private Browser consoleFrame;
    protected final JPanel treesPanel;
    private DynamicTree dynamicTree;
    private SimpleWorker loadingWorker;
    private Timer refreshTimer;
    private TaskDetailsDialog detailsDialog = new TaskDetailsDialog();

//    private TableCellRenderer taskTableCellRenderer = new DefaultTableCellRenderer() {
//
//		@Override
//		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
//				boolean hasFocus, int rowIndex, int columnIndex) {
//			JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);
//			DynamicRow row = dynamicTable.getRows().get(rowIndex);
//			Task task = (Task)row.getUserObject();
//			if (task.isDone()) {
//				label.setForeground(Color.gray);
//			}
//			else {
//				label.setForeground(Color.black);
//			}
//			return label;
//		}
//    };
    
    public TaskTreeOutline(Browser consoleFrame) {
        super(new BorderLayout());

        this.consoleFrame = consoleFrame;

        treesPanel = new JPanel(new BorderLayout());
        add(treesPanel, BorderLayout.CENTER);

        showLoadingIndicator();
        this.updateUI();
        loadTasks();
        
        TimerTask refreshTask = new TimerTask() {
			@Override
			public void run() {
		        loadTasks();
			}
		};
		
		refreshTimer = new Timer();
        refreshTimer.schedule(refreshTask, REFRESH_SECS*1000, REFRESH_SECS*1000);
    }

    public synchronized void showLoadingIndicator() {
        treesPanel.removeAll();
        treesPanel.add(new JLabel(Icons.getLoadingIcon()));
    }

    public synchronized void loadTasks() {

        loadingWorker = new SimpleWorker() {

        	List<Task> myTasks;
        	
            protected void doStuff() throws Exception {
            	myTasks = ModelMgr.getModelMgr().getUserTasks();
            }

            protected void hadSuccess() {
                try {
                    initializeTree(myTasks);
                    // Must selectTaskById because the objects have changed
                    if (selectedTask != null) selectTaskById(selectedTask.getObjectId());
                }
                catch (Exception e) {
                    hadError(e);
                }
                loadingWorker = null;
            }

            protected void hadError(Throwable error) {
                error.printStackTrace();
                treesPanel.removeAll();
                TaskTreeOutline.this.updateUI();
                loadingWorker = null;
            }

        };

        loadingWorker.execute();
    }

    public DynamicTree getDynamicTree() {
		return dynamicTree;
	}

	public synchronized void initializeTree(List<Task> myTasks) {

		tasks.clear();
		for(Task task : myTasks) {
			if (AnnotationSessionTask.TASK_NAME.equals(task.getTaskName())) continue;
			tasks.add(task);
		}
		
        // Create a new tree and add all the nodes to it

        createNewTree();
        addNodes(null);

        // Replace the tree in the panel

        treesPanel.removeAll();
        treesPanel.add(dynamicTree);

        // Prepare for display and update the UI

        dynamicTree.expandAll(true);
        updateUI();
    }

    private Task getCurrentTask() {
        return (Task) getDynamicTree().getCurrentNode().getUserObject();
    }
    
    protected synchronized void createNewTree() {

    	dynamicTree = new DynamicTree("", false, false) {

            protected void showPopupMenu(MouseEvent e) {
                TaskTreeOutline.this.showPopupMenu(e);
            }

            protected void nodeClicked(MouseEvent e) {
				try {
					selectedTask = getCurrentTask();
				} catch (ArrayIndexOutOfBoundsException x) {
					x.printStackTrace();
				}
            }

            protected void nodeDoubleClicked(MouseEvent e) {
				try {
					selectedTask = getCurrentTask();
					detailsDialog.showForTask(selectedTask);
				} catch (ArrayIndexOutOfBoundsException x) {
					x.printStackTrace();
				}
            }

			@Override
			public void refresh() {
				TaskTreeOutline.this.loadTasks();
			}
        };

        // Replace the cell renderer

        dynamicTree.setCellRenderer(new TaskTreeCellRenderer());
        dynamicTree.getTree().setRootVisible(false);
    }

    private Map<Long,List<Task>> taskTree = new HashMap<Long,List<Task>>();
    
    protected synchronized void addNodes(DefaultMutableTreeNode parentNode) {

    	// Reverse id order (basically reverse chronological order since the ids are time-based)
        Collections.sort(tasks, new Comparator<Task>() {
			@Override
			public int compare(Task o1, Task o2) {
				return o2.getObjectId().compareTo(o1.getObjectId());
			}
		});
        
    	// Derive tree relationships
        for (Task task : tasks) {
        	Long parentId = task.getParentTaskId();
        	if (parentId != null) {
        		List<Task> children = taskTree.get(parentId);
        		if (children==null) {
        			children = new ArrayList<Task>();
        			taskTree.put(parentId, children);
        		}
        		children.add(task);
        	}
        }

        for (Task task : tasks) {
        	if (task.getParentTaskId()==null) {
        		addNodes(dynamicTree.getRootNode(), task);
        	}
        }
    }
    
    protected synchronized void addNodes(DefaultMutableTreeNode parentNode, Task task) {
    	DefaultMutableTreeNode newNode = dynamicTree.addObject(parentNode, task);
    	List<Task> children = taskTree.get(task.getObjectId());
    	if (children != null) {
	        for (Task child : children) {
	            addNodes(newNode, child);
	        }
    	}
    }

    protected void showPopupMenu(MouseEvent e) {
    	
        if (dynamicTree.getCurrentNode() == null) return;

        final JPopupMenu popupMenu = new JPopupMenu();
        
        TreePath[] selections = dynamicTree.getTree().getSelectionPaths();
        
		if (selections.length == 1) { 

        	final DefaultMutableTreeNode node = (DefaultMutableTreeNode)selections[0].getLastPathComponent();
        	final Task task = (Task)node.getUserObject();
	        final String value = task.getJobName();

			// Select this task 
			
			try {
				selectedTask = task;
			} catch (ArrayIndexOutOfBoundsException x) {
				x.printStackTrace();
			}

			// Items available if only a single item is selected
			
	        JMenuItem titleMenuItem = new JMenuItem(value);
	        titleMenuItem.setEnabled(false);
	        popupMenu.add(titleMenuItem);
	        
	        JMenuItem copyMenuItem = new JMenuItem("  Copy To Clipboard");
	        copyMenuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
		            Transferable t = new StringSelection(value);
		            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
				}
			});
	        popupMenu.add(copyMenuItem);
	        
            if (task instanceof ContinuousExecutionTask) {
            	final ContinuousExecutionTask cet = (ContinuousExecutionTask)task;
            	if (cet.isStillEnabled() && !cet.isDone()) {
	                JMenuItem deleteMenuItem = new JMenuItem("  Stop");
	                deleteMenuItem.addActionListener(new ActionListener() {
	                    public void actionPerformed(ActionEvent actionEvent) {
	                    	try {
		                    	ModelMgr.getModelMgr().stopContinuousExecution(cet);
		                        JOptionPane.showMessageDialog(consoleFrame, 
		                        		"Continuous execution will end after the current execution is complete", 
		                        		"Stopped", JOptionPane.INFORMATION_MESSAGE);
	                    	} 
	                    	catch (Exception e) {
	                    		e.printStackTrace();
	                    	}
	                    }
	                });
	                popupMenu.add(deleteMenuItem);
            	}
            }

            JMenuItem deleteMenuItem = new JMenuItem("  View Details");
            deleteMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
					detailsDialog.showForTask(selectedTask);
                }
            });
            popupMenu.add(deleteMenuItem);
		}
		else {
	        JMenuItem titleMenuItem = new JMenuItem("(Multiple Items Selected)");
	        titleMenuItem.setEnabled(false);
	        popupMenu.add(titleMenuItem);
		}
		
        
//		ListSelectionModel lsm = dynamicTable.getTable().getSelectionModel();
//		if (lsm.getMinSelectionIndex() != lsm.getMaxSelectionIndex() || !task.isDone()) {
//            JMenuItem deleteMenuItem = new JMenuItem("  Cancel");
//            deleteMenuItem.addActionListener(new ActionListener() {
//                public void actionPerformed(ActionEvent actionEvent) {
//                    cancelTasks();
//                }
//            });
//            popupMenu.add(deleteMenuItem);
//    	}
        
//        JMenuItem deleteMenuItem = new JMenuItem("  Delete");
//        deleteMenuItem.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent actionEvent) {
//                deleteTasks();
//            }
//        });
//        popupMenu.add(deleteMenuItem);

        if (popupMenu!=null) popupMenu.show((JComponent) e.getSource(), e.getX(), e.getY());
    }

	
//	private synchronized void cancelTasks() {
//
//        final List<Task> toCancel = new ArrayList<Task>();
//        for (int i : dynamicTable.getTable().getSelectedRows()) {
//        	Task task = tasks.get(i);
//            if (!task.getOwner().equals(SessionMgr.getUsername())) {
//                JOptionPane.showMessageDialog(consoleFrame, 
//                		"Only the owner may cancel a task", "Cannot Cancel", JOptionPane.ERROR_MESSAGE);
//                return;
//            }
//            toCancel.add(task);
//        }
//
//
//        int deleteConfirmation = JOptionPane.showConfirmDialog(consoleFrame, 
//        		"Are you sure you want to cancel the selected tasks? ", "Delete Tasks", JOptionPane.YES_NO_OPTION);
//        if (deleteConfirmation != 0) return;
//
//        try {
//            for (Task task : toCancel) {
//                ModelMgr.getModelMgr().cancelTaskById(task.getObjectId());
//            }
//            
//            loadTasks();
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//            JOptionPane.showMessageDialog(consoleFrame, 
//            		"Error canceling session", "Error", JOptionPane.ERROR_MESSAGE);
//        }
//	}
	
    private synchronized void deleteTasks() {
    	
        final List<Task> toDelete = new ArrayList<Task>();
        
        for(TreePath path : dynamicTree.getTree().getSelectionPaths()) {
        	DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
        	Task task = (Task)node.getUserObject();
            if (!task.getOwner().equals(SessionMgr.getUsername())) {
                JOptionPane.showMessageDialog(consoleFrame, 
                		"Only the owner may delete a task", "Cannot Delete", JOptionPane.ERROR_MESSAGE);
                return;
            }
            toDelete.add(task);
        }

        int deleteConfirmation = JOptionPane.showConfirmDialog(consoleFrame, 
        		"Are you sure you want to delete the selected tasks? ", "Delete Tasks", JOptionPane.YES_NO_OPTION);
        if (deleteConfirmation != 0) return;

        try {
            for (Task task : toDelete) {
                ModelMgr.getModelMgr().deleteTaskById(task.getObjectId());
                dynamicTree.removeNode(dynamicTree.getNodeForUserObject(task));
                tasks.remove(task);
            }
            SwingUtilities.updateComponentTreeUI(dynamicTree);
        }
        catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(consoleFrame, 
            		"Error deleting session", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public Task getTaskById(long taskId) {
    	for(Task task : tasks) {
            if (task.getObjectId().equals(taskId)) {
                return task;
            }
    	}
        return null;
    }

    public void selectTaskById(long taskId) {
    	selectTask(getTaskById(taskId));
    }

    public synchronized void selectTask(Task task) {
		dynamicTree.navigateToNodeWithObject(task);
		this.selectedTask = task;
    }
}
