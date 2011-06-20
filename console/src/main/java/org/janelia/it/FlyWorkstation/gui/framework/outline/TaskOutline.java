package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.FlyWorkstation.gui.framework.console.ConsoleFrame;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 2:09 PM
 * This class is the initial outline of the data file tree
 */
public class TaskOutline extends JScrollPane implements Cloneable {
    public static final String NO_DATASOURCE = "Tasks Unreachable";
    private ConsoleFrame consoleFrame;
    private JTree tree;
    private static final String ANNOTATION_SESSIONS = "Annotation Sessions";

    public TaskOutline(ConsoleFrame consoleFrame) {
        this.consoleFrame = consoleFrame;
        tree = new JTree();
        //rebuildTreeModel();
        tree.addTreeExpansionListener(new TreeExpansionListener() {
            public void treeExpanded(TreeExpansionEvent event) {
            }

            public void treeCollapsed(TreeExpansionEvent event) {
            }
        });
        tree.addTreeSelectionListener((new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
//                System.out.println("Selected "+treeSelectionEvent.getPath());
                TreePath tmpPath = treeSelectionEvent.getPath();
                if (tmpPath.getLastPathComponent().toString().equals(NO_DATASOURCE)) {return;}
                String tmpTask = tmpPath.getLastPathComponent().toString();
                if (null!=tmpTask && !"".equals(tmpTask)) {
                    TaskOutline.this.consoleFrame.setMostRecentFileOutlinePath(tmpTask);
                }
            }
        }));
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                handleMouseEvents(mouseEvent);
            }
        });
        // todo Change the root to not visible
        tree.setRootVisible(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);


        // Load the tree in the background so that the app starts up first
        SwingWorker<Void, Void> loadTasks = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    rebuildTreeModel();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                setViewportView(tree);
                return null;
            }
        };

        loadTasks.execute();
    }

    public void rebuildTreeModel(){
        DefaultMutableTreeNode newRootNode = buildTreeModel();
        DefaultTreeModel newModel = new DefaultTreeModel(newRootNode);
        tree.setModel(newModel);
    }

    private void handleMouseEvents(MouseEvent e) {

        if (null==tree || null==tree.getLastSelectedPathComponent()) return;
        String treePath = tree.getLastSelectedPathComponent().toString();
        if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) > 0) {
            System.out.println("TaskOutline Rt. button mouse pressed clicks: " + e.getClickCount() + " " + System.currentTimeMillis());
//            if (treePath.equals(ANNOTATION_SESSIONS)) {
//                getAnnotationPopupMenu(e);
//            }
        }
        if (tree.getLastSelectedPathComponent() instanceof DefaultMutableTreeNode) {    //if not a DefaultMutableTreeNode, punt
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            Object userObj = node.getUserObject();
//            tree.setSelectionPath(previousTreeSelectionPath);
        }
    }

//    private void getAnnotationPopupMenu(MouseEvent e) {
//        actionPopup = new JPopupMenu();
//        JMenuItem newAnnotationSessionButton = new JMenuItem("New Annotation Session");
//        newAnnotationSessionButton.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent actionEvent) {
//                SessionTask newSession = new SessionTask();
//
//                System.out.println("DEBUG: " + tmpCmd);
//                try {
//                    Runtime.getRuntime().exec(tmpCmd);
//                }
//                catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        JMenuItem stackInfoItem = new JMenuItem("Show Image Info");
//        stackInfoItem.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent actionEvent) {
//                System.out.println("Calling for tree info...");
//                JOptionPane.showMessageDialog(actionPopup, "Calling for TIF Info...", "Show Image Info", JOptionPane.PLAIN_MESSAGE);
//            }
//        });
//        actionPopup.add(v3dMenuItem);
//        if (treePath.getAbsolutePath().toLowerCase().endsWith(".tif")|| treePath.getAbsolutePath().toLowerCase().endsWith(".lsm")) {
//            actionPopup.add(stackInfoItem);
//        }
//        actionPopup.show(tree, e.getX(), e.getY());
//    }

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
                        DefaultMutableTreeNode tmpNode = new DefaultMutableTreeNode(tmpTasks.get(i).getObjectId());
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


    public void selectSession(String currentAnnotationSessionTaskId) {
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) tree.getModel().getRoot();
        selectSessionNode(rootNode, currentAnnotationSessionTaskId);
    }

    private boolean selectSessionNode(DefaultMutableTreeNode rootNode, String currentAnnotationSessionTaskId) {
        if (rootNode.toString().equals(currentAnnotationSessionTaskId)) {
            tree.getSelectionModel().setSelectionPath(new TreePath(rootNode.getPath()));
            return true;
        }
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            boolean walkSuccess = selectSessionNode((DefaultMutableTreeNode)rootNode.getChildAt(i), currentAnnotationSessionTaskId);
            if (walkSuccess) {return true;}
        }
        return false;
    }

    public void clearSelection() {
        tree.clearSelection();
    }
}
