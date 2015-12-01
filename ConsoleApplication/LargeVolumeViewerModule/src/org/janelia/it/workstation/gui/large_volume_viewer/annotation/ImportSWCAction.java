package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/**
 * Drag the SWCs into the workspace, and make neurons.
 *
 * @author fosterl
 */
public class ImportSWCAction extends AbstractAction {

    private boolean neuronPerRoot = false;
    private AnnotationPanel annotationPanel;
    private AnnotationModel annotationModel;
    private AnnotationManager annotationManager;

    public ImportSWCAction(boolean neuronPerRoot, AnnotationPanel annotationPanel, AnnotationModel annotationModel, AnnotationManager annotationManager) {
        this.neuronPerRoot = neuronPerRoot;
        this.annotationPanel = annotationPanel;
        this.annotationModel = annotationModel;
        this.annotationManager = annotationManager;
    }

    public ImportSWCAction(AnnotationPanel annotationPanel, AnnotationModel annotationModel, AnnotationManager annotationManager) {
        this(false, annotationPanel, annotationModel, annotationManager);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        // note: when it's time to add toggle and/or options, you can look into
        //  adding an accesory view to dialog; however, not clear that it will
        //  give enough flexibility compared to doing a custom dialog from the start
        // could specify a dir to open in, but not sure what to choose
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose swc file or directory");
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        final FileFilter swcAndDirFilter = new SwcDirAndFileFilter();
        chooser.setFileFilter(swcAndDirFilter);
        int returnValue = chooser.showOpenDialog(annotationPanel);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            List<File> swcFiles = getFilesList(chooser.getSelectedFile());
            if (swcFiles.size() > 1) {
                AtomicInteger countDownSemaphor = new AtomicInteger(swcFiles.size());
                // Unified notification across all the (possibly many) files.
                CountdownBackgroundWorker progressNotificationWorker
                        = new CountdownBackgroundWorker(
                                "Import " + chooser.getSelectedFile(),
                                countDownSemaphor
                        );
                progressNotificationWorker.setAnnotationModel(annotationModel);
                progressNotificationWorker.executeWithEvents();
                for (File swc : swcFiles) {
                    // Import all the little neurons from the file.
                    annotationManager.importSWCFile(swc, countDownSemaphor);
                }
            } else {
                annotationManager.importSWCFile(swcFiles.get(0), null);
            }
        }
    }

    private List<File> getFilesList(File selectedFile) {
        List<File> rtnVal = new ArrayList<>();
        List<File> rawFileList = new ArrayList<>();
        if (selectedFile.isDirectory()) {
            File[] swcFiles = selectedFile.listFiles(new SwcDirListFilter());
            rawFileList.addAll(Arrays.asList(swcFiles));
        } else {
            rawFileList.add(selectedFile);
        }

        if (neuronPerRoot) {
            try {
                    // Now, we traverse list above, breaking any we see as
                // having more than one root, into multiple input files.
                for (File infile : rawFileList) {
                    rtnVal.addAll(annotationModel.breakOutByRoots(infile));
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
                throw new RuntimeException(ioe);
            }
        } else {
            rtnVal.addAll(rawFileList);
        }
        return rtnVal;
    }
}
