package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.swc.SWCData;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.BasicAnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.BasicAnnotationPanel;

/**
 * Drag the SWCs into the workspace, and make neurons.
 *
 * @author fosterl
 */
public class ImportSWCAction extends AbstractAction {

    private final boolean neuronPerRoot;
    private final BasicAnnotationPanel annotationPanel;
    private final AnnotationManager annotationManager;

    public ImportSWCAction(boolean neuronPerRoot, BasicAnnotationPanel annotationPanel, AnnotationManager annotationManager) {
        this.neuronPerRoot = neuronPerRoot;
        this.annotationPanel = annotationPanel;
        this.annotationManager = annotationManager;
    }

    public ImportSWCAction(BasicAnnotationPanel annotationPanel, AnnotationManager annotationManager) {
        this(false, annotationPanel, annotationManager);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (annotationManager.getCurrentWorkspace() == null) {
            JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(), "No workspace is open", "Cannot Import", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // note: when it's time to add toggle and/or options, you can look into
        //  adding an accesory view to dialog; however, not clear that it will
        //  give enough flexibility compared to doing a custom dialog from the start
        // could specify a dir to open in, but not sure what to choose
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choose swc file or directory");
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            final FileFilter swcAndDirFilter = new SwcDirAndFileFilter();
            chooser.setFileFilter(swcAndDirFilter);
            int returnValue = chooser.showOpenDialog(annotationPanel);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                List<File> swcFiles = getFilesList(chooser.getSelectedFile());
                annotationManager.importSWCFiles(swcFiles);
            }
        }
        catch (Exception ex) {
            ConsoleApp.handleException(ex);
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
                List<File> files = new SWCData().breakOutByRoots(infile);
                rtnVal.addAll(files);
            }
        } 
        else {
            rtnVal.addAll(rawFileList);
        }
        return rtnVal;
    }
    
    class SwcDirListFilter implements java.io.FileFilter {

        @Override
        public boolean accept(File file) {
            return file.isFile() && file.getName().endsWith(BasicAnnotationModel.STD_SWC_EXTENSION);
        }

    }

    class SwcDirAndFileFilter extends FileFilter {

        @Override
        public boolean accept(File f) {
            return f.getName().endsWith(BasicAnnotationModel.STD_SWC_EXTENSION) || f.isDirectory();
        }

        @Override
        public String getDescription() {
            return "*" + BasicAnnotationModel.STD_SWC_EXTENSION;
        }
    }
}

