package org.janelia.workstation.controller.action;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.janelia.workstation.controller.AnnotationModel;
import org.janelia.workstation.controller.infopanel.AnnotationPanel;
import org.janelia.workstation.controller.infopanel.SwcDirAndFileFilter;
import org.janelia.workstation.controller.infopanel.SwcDirListFilter;
import org.janelia.workstation.integration.util.FrameworkAccess;

/**
 * Drag the SWCs into the workspace, and make neurons.
 *
 * @author fosterl
 */
public class ImportSWCAction extends AbstractAction {

    private static Dimension dialogSize = new Dimension(1200, 800);

    private static Dimension getDialogSize() {
        return dialogSize;
    }

    private static void setDialogSize(Dimension dialogSize) {
        ImportSWCAction.dialogSize = dialogSize;
    }

    private boolean neuronPerRoot = false;
    private AnnotationPanel annotationPanel;
    private AnnotationModel annotationModel;

    public ImportSWCAction(boolean neuronPerRoot, AnnotationPanel annotationPanel, AnnotationModel annotationModel) {
        this.neuronPerRoot = neuronPerRoot;
        this.annotationPanel = annotationPanel;
        this.annotationModel = annotationModel;
    }

    public ImportSWCAction(AnnotationPanel annotationPanel, AnnotationModel annotationModel) {
        this(false, annotationPanel, annotationModel);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (annotationModel.getCurrentWorkspace() == null) {
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), "No workspace is open", "Cannot Import", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // note: when it's time to add toggle and/or options, you can look into
        //  adding an accesory view to dialog; however, not clear that it will
        //  give enough flexibility compared to doing a custom dialog from the start
        // could specify a dir to open in, but not sure what to choose
        try {
            //REFACTOR
            //JFileChooser chooser = new JFileChooser(annotationManager.getSwcDirectory());
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choose swc file or dialog");
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            final FileFilter swcAndDirFilter = new SwcDirAndFileFilter();
            chooser.setFileFilter(swcAndDirFilter);
            chooser.setPreferredSize(getDialogSize());
            int returnValue = chooser.showOpenDialog(FrameworkAccess.getMainFrame());
            setDialogSize(chooser.getSize());
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                List<File> swcFiles = getFilesList(chooser.getSelectedFile());
                if (swcFiles.size() > 0) {
                   // annotationManager.importSWCFiles(swcFiles);
                   // annotationManager.setSwcDirectory(swcFiles.get(0).getParentFile());
                }
            }
        }
        catch (Exception ex) {
            FrameworkAccess.handleException(ex);
        }
    }

    private List<File> getFilesList(File selectedFile) throws Exception {
        List<File> rtnVal = new ArrayList<>();
        List<File> rawFileList = new ArrayList<>();
        if (selectedFile.isDirectory()) {
            File[] swcFiles = selectedFile.listFiles(new SwcDirListFilter());
            rawFileList.addAll(Arrays.asList(swcFiles));
        } else {
            rawFileList.add(selectedFile);
        }

        if (neuronPerRoot) {
            // Now, we traverse list above, breaking any we see as
            // having more than one root, into multiple input files.
            for (File infile : rawFileList) {
                rtnVal.addAll(annotationModel.breakOutByRoots(infile));
            }
        } 
        else {
            rtnVal.addAll(rawFileList);
        }
        return rtnVal;
    }
}
