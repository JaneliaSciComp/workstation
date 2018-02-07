package org.janelia.it.workstation.browser.nb_action;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.janelia.it.jacs.shared.file_chooser.FileChooser;
import org.janelia.it.jacs.shared.utils.Progress;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.actions.OpenWithDefaultAppAction;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.nodes.TreeNodeNode;
import org.janelia.it.workstation.browser.workers.IndeterminateNoteProgressMonitor;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.gui.search.Filter;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.workspace.Node;
import org.janelia.model.domain.workspace.TreeNode;

import com.google.common.collect.Multimap;

/**
 * Action which implements Export for folder structures.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class ExportFoldersAction extends NodePresenterAction {

    /**
     * Default directory for exports
     */
    protected static final String DEFAULT_EXPORT_DIR = System.getProperty("user.home");
    
    private final static ExportFoldersAction singleton = new ExportFoldersAction();
    public static ExportFoldersAction get() {
        return singleton;
    }

    private DomainModel model = DomainMgr.getDomainMgr().getModel();
    
    private ExportFoldersAction() {
    }
    
    @Override
    public String getName() {
        return "Export Folder Structure...";
    }

    @Override
    protected void performAction (org.openide.nodes.Node[] activatedNodes) {
        List<Node> nodes = new ArrayList<>();
        for(org.openide.nodes.Node node : getSelectedNodes()) {
            if (node instanceof TreeNodeNode) {
                TreeNodeNode treeNodeNode = (TreeNodeNode)node;
                nodes.add(treeNodeNode.getNode());
            }
            else {
                throw new IllegalStateException("Download can only be called on DomainObjectNode");
            }
        }
        
        String folderName = "";
        if (nodes.size()==1) {
            folderName = "_"+nodes.iterator().next().getName().replaceAll("\\s+", "_");
        }

        String filePrefix = "FolderExport" + folderName;
        
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select File Destination");
        chooser.setFileSelectionMode(FileChooser.FILES_ONLY);
        File defaultFile = new File(DEFAULT_EXPORT_DIR, filePrefix + ".xls");

        int i = 1;
        while (defaultFile.exists() && i < 10000) {
            defaultFile = new File(DEFAULT_EXPORT_DIR, filePrefix + "_" + i + ".xls");
            i++;
        }

        chooser.setSelectedFile(defaultFile);
        chooser.setFileFilter(new FileFilter() {
            @Override
            public String getDescription() {
                return "Tab-delimited Files (*.xls, *.txt)";
            }

            @Override
            public boolean accept(File f) {
                return !f.isDirectory();
            }
        });

        if (chooser.showDialog(ConsoleApp.getMainFrame(), "OK") == FileChooser.CANCEL_OPTION) {
            return;
        }

        final String destFile = chooser.getSelectedFile().getAbsolutePath();
        if ((destFile == null) || destFile.equals("")) {
            return;
        }

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                export(nodes, destFile, this);
            }

            @Override
            protected void hadSuccess() {
                int rv = JOptionPane.showConfirmDialog(ConsoleApp.getMainFrame(), "Data was successfully exported to " + destFile + ". Open file in default viewer?",
                        "Export successful", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (rv == JOptionPane.YES_OPTION) {
                    OpenWithDefaultAppAction openAction = new OpenWithDefaultAppAction(destFile);
                    openAction.actionPerformed(null);
                }
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };

        worker.setProgressMonitor(new IndeterminateNoteProgressMonitor(ConsoleApp.getMainFrame(), "Exporting data", ""));
        worker.execute();   
    }
    
    private void export(List<Node> nodes, String destFile, Progress progress) throws Exception {
        
        try (FileWriter writer = new FileWriter(destFile)) {

            writer.write("GUID\t");
            writer.write("Type\t");
            writer.write("Name\t");
            writer.write("Annotations\t");
            writer.write("Folder Path\n");
            
            for (Node node : nodes) {
                if (node instanceof TreeNode) {
                    Node root = model.getDomainObject(TreeNode.class, node.getId());
                    walkSubtree(root, 0, writer, "", progress);
                }
                // TODO: handle other types of nodes
            }
        }
    }
    
    private void walkSubtree(Node node, int level, FileWriter writer, String path, Progress progress) throws Exception {
            
        if (progress.isCancelled()) return;
        
        SwingUtilities.invokeLater(() -> {
            progress.setStatus("Exporting "+node.getName());    
        });
        
        Multimap<Long, Annotation> annotationMap = DomainUtils.getAnnotationsByDomainObjectId(model.getAnnotations(node.getChildren()));
        
        for(DomainObject child : model.getDomainObjects(node.getChildren())) {
            if (child instanceof Node) {
                walkSubtree((Node)child, level+1, writer, path+"\t"+node.getName(), progress);
            }
            else if (child instanceof Filter) {
                // TODO: maybe implement this in the future?
            }
            else {
                StringBuilder sb = new StringBuilder();
                for(Annotation annotation : annotationMap.get(child.getId())) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(annotation.getName().replaceAll("\\s+", " "));
                }
                
                writer.write(child.getId()+"\t");
                writer.write(child.getType()+"\t");
                writer.write(child.getName()+"\t");
                writer.write(sb.toString());
                writer.write(path+"\t"+node.getName()+"\n");
               
            }
        }
           
    }
}
