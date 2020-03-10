package org.janelia.workstation.browser.actions.context;

import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.shared.file_chooser.FileChooser;
import org.janelia.it.jacs.shared.utils.Progress;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.search.Filter;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.workspace.Node;
import org.janelia.model.domain.workspace.TreeNode;
import org.janelia.model.domain.workspace.Workspace;
import org.janelia.workstation.browser.actions.OpenWithDefaultAppAction;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.workers.IndeterminateNoteProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ActionID(
        category = "Actions",
        id = "ExportFolderStructureAction"
)
@ActionRegistration(
        displayName = "#CTL_ExportFolderStructureAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 405, separatorAfter = 499)
})
@NbBundle.Messages("CTL_ExportFolderStructureAction=Export Folder Structure")
public class ExportFolderStructureAction extends BaseContextualNodeAction {

    private static final String DEFAULT_EXPORT_DIR = System.getProperty("user.home");

    private Collection<Node> nodes = new ArrayList<>();

    @Override
    protected void processContext() {
        this.nodes = new ArrayList<>();
        nodes.clear();
        if (getNodeContext().isOnlyObjectsOfType(Node.class)) {
            nodes.addAll(getNodeContext().getOnlyObjectsOfType(Node.class));
            setEnabledAndVisible(true);
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    public void performAction() {
        Collection<Node> nodes = new ArrayList<>(this.nodes);
        exportFolders(nodes);
    }

    private static void exportFolders(Collection<Node> nodes) {

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

        if (chooser.showDialog(FrameworkAccess.getMainFrame(), "OK") == FileChooser.CANCEL_OPTION) {
            return;
        }

        final String destFile = chooser.getSelectedFile().getAbsolutePath();
        if (StringUtils.isBlank(destFile)) {
            return;
        }

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                export(nodes, destFile, this);
            }

            @Override
            protected void hadSuccess() {
                int rv = JOptionPane.showConfirmDialog(FrameworkAccess.getMainFrame(), "Data was successfully exported to " + destFile + ". Open file in default viewer?",
                        "Export successful", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (rv == JOptionPane.YES_OPTION) {
                    OpenWithDefaultAppAction openAction = new OpenWithDefaultAppAction(destFile);
                    openAction.actionPerformed(null);
                }
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };

        worker.setProgressMonitor(new IndeterminateNoteProgressMonitor(FrameworkAccess.getMainFrame(), "Exporting data", ""));
        worker.execute();
    }

    private static void export(Collection<Node> nodes, String destFile, Progress progress) throws Exception {

        try (FileWriter writer = new FileWriter(destFile)) {

            writer.write("GUID\t");
            writer.write("Type\t");
            writer.write("Name\t");
            writer.write("Annotations\t");
            writer.write("Folder Path\n");

            for (Node node : nodes) {
                if (node instanceof Workspace) {
                    Node root = DomainMgr.getDomainMgr().getModel().getDomainObject(Workspace.class, node.getId());
                    if (root==null) {
                        throw new IllegalStateException("Could not find workspace: "+node.getId());
                    }
                    walkSubtree(root, 0, writer, "", progress);
                }
                else if (node instanceof TreeNode) {
                    Node root = DomainMgr.getDomainMgr().getModel().getDomainObject(TreeNode.class, node.getId());
                    if (root==null) {
                        throw new IllegalStateException("Could not find root folder: "+node.getId());
                    }
                    walkSubtree(root, 0, writer, "", progress);
                }
                else {
                    // TODO: handle other types of nodes
                    FrameworkAccess.handleExceptionQuietly(
                            new UnsupportedOperationException("Cannot export node of type "+node.getType()));
                }
            }
        }
    }

    private static void walkSubtree(Node node, int level, FileWriter writer, String path, Progress progress) throws Exception {

        if (progress.isCancelled()) return;

        SwingUtilities.invokeLater(() -> {
            progress.setStatus("Exporting "+node.getName());
        });

        DomainModel model = DomainMgr.getDomainMgr().getModel();
        List<Reference> children = node.getChildren();
        List<Annotation> annotations = model.getAnnotations(children);
        Multimap<Long, Annotation> annotationMap = DomainUtils.getAnnotationsByDomainObjectId(annotations);

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