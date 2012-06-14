package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.shared.util.TifImageInfoDialog;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 2:09 PM
 * This class is the initial outline of the data file tree
 */
public class FileOutline extends JScrollPane implements Cloneable {
    // todo Remove this hard-wiring of the path - I don't think we use this class anymore
    public static final String DATA_SOURCE_PATH = ConsoleProperties.getString("remote.defaultMacPath") + "/filestore/" + (String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME);
    public static final String NO_DATASOURCE = "Data Source Unreachable";
    private Browser consoleFrame;
    private JTree tree;
    private JPopupMenu actionPopup, annotationPopup;
    //    private BrowserModel browserModel;
    private TreePath treeDrillDownPath, previousTreeSelectionPath;
    //    private SessionMgr sessionManager=SessionMgr.getSessionMgr();
//    private FacadeManagerBase facadeManager=FacadeManager.getFacadeManager();
    //    private BrowserModelObserver browserModelObserver;
    private TreeModel treeModel;

    public FileOutline(Browser consoleFrame) {
        this.consoleFrame = consoleFrame;
        tree = new JTree(buildTreeModel(DATA_SOURCE_PATH));
        treeModel = tree.getModel();
//      if (ModelMgr.getModelMgr().isModelAvailable()) {
//         tree.setModel(treeModel);
//      }
//      else {
        //buildEmptyTreeModel();
//         ModelMgr.getModelMgr().addModelMgrObserver(new OutlineModelMgrObserver());
//      }
//      browserModel=consoleFrame.getBrowserModel();
//      browserModel.addBrowserModelListener(browserModelObserver=new BrowserModelObserver());
//      tree.setCellRenderer(new Renderer(browserModel));
        tree.addTreeExpansionListener(new TreeExpansionListener() {
            public void treeExpanded(TreeExpansionEvent event) {
//                System.out.println("Expanded "+event.getPath());
                //  if (event.getPath().getLastPathComponent() instanceof GenomicEntityTreeNode)
//                  ((GenomicEntityTreeNode)event.getPath().getLastPathComponent()).loadChildren();
            }

            public void treeCollapsed(TreeExpansionEvent event) {
//                System.out.println("Collapsed "+event.getPath());
            }
        });
        tree.addTreeSelectionListener((new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
//                System.out.println("Selected "+treeSelectionEvent.getPath());
                TreePath tmpPath = treeSelectionEvent.getPath();
                if (tmpPath.getLastPathComponent().toString().equals(NO_DATASOURCE)) {
                    return;
                }
                File tmpFile = getFileForTreePath(tmpPath);
                if (null != tmpFile && tmpFile.exists()) {
                    FileOutline.this.consoleFrame.setMostRecentFileOutlinePath(tmpFile.getAbsolutePath());
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
//        tree.setShowsRootHandles(true);
//        tree.setLargeModel(true);
//        tree.setDoubleBuffered(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        setViewportView(tree);

        expandAll();
    }

    public void expandAll() {
        // expand to the last leaf from the root
        int row = 0;
        while (row < tree.getRowCount()) {
            tree.expandRow(row);
            row++;
        }
    }

    private File getFileForTreePath(TreePath tmpPath) {
        String tmpFilePath = tmpPath.toString();
        tmpFilePath = tmpFilePath.replace("[", "");
        tmpFilePath = tmpFilePath.replace("]", "");
        tmpFilePath = tmpFilePath.replace(" ", "");
        tmpFilePath = tmpFilePath.replace(",", File.separator);
        // trim off the root node
        if (tmpFilePath.indexOf("/") < 0) return null;
        tmpFilePath = tmpFilePath.substring(tmpFilePath.indexOf("/"));
        tmpFilePath = DATA_SOURCE_PATH + tmpFilePath;
        return new File(tmpFilePath);
    }

    public boolean nodesShowing() {
        return (treeModel.getChildCount(treeModel.getRoot()) != 0);
    }

    private void handleMouseEvents(MouseEvent e) {
        TreePath treePath = tree.getSelectionPath();
        if (treePath == null) return;
        java.lang.Object treeObj = treePath.getPath();
        if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) > 0) {
            System.out.println("Rt. button mouse pressed clicks: " + e.getClickCount() + " " + System.currentTimeMillis());
            if (getFileForTreePath(treePath).isFile()) {
                getImagePopupMenu(getFileForTreePath(treePath), e);
            }
            else if (getFileForTreePath(treePath).isDirectory()) {
                getAnnotationSessionPopup(treePath, e);
            }
//             if (treePath.getLastPathComponent() instanceof GenomicEntityTreeNode) {
//                 ((GenomicEntityTreeNode)treePath.getLastPathComponent()).receivedRightClick(FileOutline.this,e);
//             }
        }
//         else if (((e.getModifiers() & e.BUTTON1_MASK) >0) && e.getClickCount()==2 && treePath.getLastPathComponent() instanceof GenomicAxisTreeNode) {
//             System.out.println("Left button mouse pressed clicks: "+e.getClickCount()+" "+System.currentTimeMillis());
////             FileOutline.this.consoleFrame.drillDownToEntityUsingDefaultEditor(browserModel.getCurrentSelection());
//             consoleFrame.setView(consoleFrame.isOutlineCollapsed());
//             return;
//         }
        if (treeObj instanceof DefaultMutableTreeNode) {    //if not a DefaultMutableTreeNode, punt
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeObj;
            Object userObj = node.getUserObject();
//              if (!(userObj instanceof GenomicEntity)) { //if it's not a GenomicEntity, veto selection
            tree.setSelectionPath(previousTreeSelectionPath);
            return;
//              }
//              browserModel.setCurrentSelection((GenomicEntity)userObj);
//              previousTreeSelectionPath=treePath;  //if validation passes, reset previousTreeSelectionPath to current
//               if (treeObj instanceof GenomicEntityTreeNode) {
//                   ((GenomicEntityTreeNode)treePath.getLastPathComponent()).receivedClick(FileOutline.this,e);
//               }
        }
    }

    private void getAnnotationSessionPopup(final TreePath treePath, MouseEvent e) {
        annotationPopup = new JPopupMenu();
        JMenuItem newSessionItem = new JMenuItem("Create Annotation Session");
        newSessionItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                System.out.println("DEBUG: Creating new Annotation Session Task");
                try {
                    AnnotationSessionTask newSessionTask = new AnnotationSessionTask(null, (String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME), null, null);
//                    newSessionTask.setParameter(AnnotationSessionTask.PARAM_annotatioNode, treePath.getPath()[treePath.getPath().length-1].toString());
//                    newSessionTask.setParameter(AnnotationSessionTask.PARAM_annotationValues, "good, partially good, low quality, trash");
//                    newSessionTask.setParameter(AnnotationSessionTask.PARAM_annotationCategories, "quality");
                    AnnotationSessionTask returnSessionTask = (AnnotationSessionTask) ModelMgr.getModelMgr().saveOrUpdateTask(newSessionTask);
//                    FileOutline.this.consoleFrame.setAnnotationSessionChanged(returnSessionTask.getObjectId().toString());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        annotationPopup.add(newSessionItem);
        annotationPopup.show(tree, e.getX(), e.getY());
    }

    private void getImagePopupMenu(final File treePath, MouseEvent e) {
        actionPopup = new JPopupMenu();
        JMenuItem vaa3dMenuItem = new JMenuItem("Show in Vaa3D");
        vaa3dMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                String tmpCmd = "/Users/" + SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME) + "/Dev/NeuroAnnotator/vaa3d/v3d64.app/Contents/MacOS/v3d64 -f " + treePath.getAbsolutePath();
                System.out.println("DEBUG: " + tmpCmd);
                try {
                    Runtime.getRuntime().exec(tmpCmd);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        JMenuItem stackInfoItem = new JMenuItem("Show Image Info");
        stackInfoItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                System.out.println("Calling for tree info...");
                new TifImageInfoDialog(consoleFrame, treePath.getAbsolutePath()).setVisible(true);
                JOptionPane.showMessageDialog(actionPopup, "Calling for TIF Info...", "Show Image Info", JOptionPane.PLAIN_MESSAGE);
            }
        });
        actionPopup.add(vaa3dMenuItem);
        if (treePath.getAbsolutePath().toLowerCase().endsWith(".tif") || treePath.getAbsolutePath().toLowerCase().endsWith(".lsm")) {
            actionPopup.add(stackInfoItem);
        }
        actionPopup.show(tree, e.getX(), e.getY());
    }

    private DefaultMutableTreeNode buildTreeModel(String path) {
        File f = new File(path);
        if (!f.exists()) {
            DefaultMutableTreeNode nullNode = new DefaultMutableTreeNode(NO_DATASOURCE);
            nullNode.setUserObject(NO_DATASOURCE);
            nullNode.setAllowsChildren(false);
            return new DefaultMutableTreeNode(NO_DATASOURCE);
        }
        DefaultMutableTreeNode top = new DefaultMutableTreeNode();

        top.setUserObject(f.getName());
        if (f.isDirectory()) {
            //System.out.println("Processing Directory " + f);
            File fls[] = f.listFiles();
            for (int i = 0; i < fls.length; i++) {
                top.insert(buildTreeModel(fls[i].getPath()), i);
            }
        }
        return (top);
//      Set genomeVersions = ModelMgr.getModelMgr().getSelectedGenomeVersions();
//      HeadNodeVisitor headNodeVisitor= new HeadNodeVisitor();
//      GenomeVersion genomeVersion;
        //genomeVersion.getSpecies().acceptVisitorForSelf(headNodeVisitor);
//      for (Iterator i=genomeVersions.iterator();i.hasNext();) {
//        genomeVersion=(GenomeVersion)i.next();
//        genomeVersion.getSpecies().acceptVisitorForSelf(headNodeVisitor);
//      }
    }


//    private void addGenomeVersion(GenomeVersion genomeVersion) {
//      if (emptyTreeModel) treeModel.removeNodeFromParent(noInfoNode);
//      emptyTreeModel=false;
//      try{
//        HeadNodeVisitor headNodeVisitor= new HeadNodeVisitor();
//        if (genomeVersion == null || genomeVersion.getSpecies()==null) return;
//        genomeVersion.getSpecies().acceptVisitorForSelf(headNodeVisitor);
//       }
//      catch (Exception ex) {
//          System.out.println("Outline: ERROR!! Tree received a data exception and cannot be built.");
//          try {
//            SessionMgr.getSessionMgr().handleException(ex);
//          }
//          catch (Exception ex1) {ex.printStackTrace();}
//      }
//    }
//
//    private void removeGenomeVersion(GenomeVersion genomeVersion) {
//       TreeNode root=(TreeNode)treeModel.getRoot();
//       int rootChildren=treeModel.getChildCount(root);
//       DefaultMutableTreeNode node;
//       Object userObject;
//       for (int i=0;i<rootChildren;i++) {
//          node=(DefaultMutableTreeNode)treeModel.getChild(root,i);
//          userObject=node.getUserObject();
//          if (userObject.equals(genomeVersion)) {
//             treeModel.removeNodeFromParent(node);
//             break;
//          }
//       }
//       if (treeModel.getChildCount(root)==0) emptyTreeModel=true;
//    }
//
//
//   private class HeadNodeVisitor extends GenomicEntityVisitor {
//
//      public void visitGenomicEntity(GenomicEntity entity) {
//          ((DefaultMutableTreeNode)treeModel.getRoot()).add(noInfoNode);
//      }
//
//      public void visitSpecies(Species species) {
//          addHeadNodeToRoot(new SpeciesTreeNode(species));
//      }
//
//      private void addHeadNodeToRoot(GenomicEntityTreeNode headNode) {
//           ((DefaultMutableTreeNode)treeModel.getRoot()).add(headNode);
//           treeModel.nodesWereInserted((TreeNode)treeModel.getRoot(),new int[]{((TreeNode)treeModel.getRoot()).getChildCount()-1});
//           headNode.addGenomicEntityTreeNodeListener(new GenomicEntityTreeNodeListener() {
//              public void childrenAdded(TreeNode changedNode,int[] indicies){
//                 treeModel.nodesWereInserted(changedNode,indicies);
//              }
//              public void childrenRemoved(TreeNode changedNode,int[] indicies,Object[] children){
//                 treeModel.nodesWereRemoved(changedNode,indicies,children);
//              }
//           });
//      }
//   }
//
//
//   private class BrowserModelObserver extends BrowserModelListenerAdapter {
//      public void browserCurrentSelectionChanged(GenomicEntity newSelection) {
//       // System.out.println("Heard Last Selection Change to: "+newSelection);
//        if (newSelection==null) {
//          Outline.this.tree.removeSelectionPaths(tree.getSelectionPaths());
//          Outline.this.tree.repaint();
//          return;
//        }
//
//        DefaultMutableTreeNode drillDownNode;
//        if (treeDrillDownPath!=null)
//          drillDownNode=(DefaultMutableTreeNode)treeDrillDownPath.getLastPathComponent();
//        else drillDownNode=(DefaultMutableTreeNode)tree.getModel().getRoot();
//        if (drillDownNode.isLeaf()) return;
//        DefaultMutableTreeNode node;
//        for (Enumeration e=drillDownNode.breadthFirstEnumeration();e.hasMoreElements();) { //check if node is shown in tree at the drilldown or below
//          node=(DefaultMutableTreeNode)e.nextElement();
//          if (newSelection.equals(node.getUserObject())) { //if it is, select it
//            TreePath tp=new TreePath(node.getPath());
//            tree.setSelectionPath(tp);
//            //Fully left justify the root (GenomeVersion) nodes
//            if (tp.getPathCount()>2) {
//              tree.scrollPathToVisible(tp);
//            }
//            else {
//              tree.makeVisible(tp);
//              java.awt.Rectangle rec=tree.getPathBounds(tp);
//              rec.x=0;
//              tree.scrollRectToVisible(rec);
//            }
//            tree.repaint();
//            return;
//          }
//        }
//        previousTreeSelectionPath=null;
//        tree.removeSelectionPaths(tree.getSelectionPaths());
//        tree.repaint();
//      }
//    }
//
//
//    class OutlineModelMgrObserver extends ModelMgrObserverAdapter{
//
//         public void genomeVersionSelected(GenomeVersion genomeVersion) {
//           addGenomeVersion(genomeVersion);
//         }
//
//         public void genomeVersionUnselected(GenomeVersion genomeVersion) {
//           removeGenomeVersion(genomeVersion);
//         }
//
//         public void workSpaceCreated(GenomeVersion genomeVersion){
//            repaint();
//         }
//
//          public void workSpaceRemoved(GenomeVersion genomeVersion, Workspace workspace){
//            repaint();
//           }
//
//    }

    public void clearSelection() {
        tree.clearSelection();
    }
}
