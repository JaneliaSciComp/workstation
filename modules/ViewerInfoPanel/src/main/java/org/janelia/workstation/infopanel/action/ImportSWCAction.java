package org.janelia.workstation.infopanel.action;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.infopanel.SwcDirAndFileFilter;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.core.workers.BackgroundWorker;
import org.janelia.workstation.swc.SWCDirectorySource;


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
    private TmModelManager model;
    private NeuronManager neuronManager;

    public ImportSWCAction(boolean neuronPerRoot) {
        this.neuronPerRoot = neuronPerRoot;
        model = TmModelManager.getInstance();
        neuronManager = NeuronManager.getInstance();
    }

    public ImportSWCAction() {
        this(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (model.getCurrentWorkspace() == null) {
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), "No workspace is open", "Cannot Import", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // note: when it's time to add toggle and/or options, you can look into
        //  adding an accesory view to dialog; however, not clear that it will
        //  give enough flexibility compared to doing a custom dialog from the start
        // could specify a dir to open in, but not sure what to choose
        try {
            JFileChooser chooser = new JFileChooser(SWCDirectorySource.getSwcDirectory());
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
                    SWCDirectorySource.setSwcDirectory(swcFiles.get(0).getParentFile());
                    NeuronManager annotationModel = NeuronManager.getInstance();
                    BackgroundWorker importer = new BackgroundWorker() {
                        @Override
                        public String getName() {
                            return swcFiles.size()>1?"Importing SWC Files":"Importing SWC File";
                        }

                        @Override
                        protected void doStuff() throws Exception {
                            int imported = 0;
                            int index = 0;
                            int total = swcFiles.size();
                            TmWorkspace workspace = TmModelManager.getInstance().getCurrentWorkspace();
                            for (File swcFile : swcFiles) {
                                setStatus(swcFile.getName());
                                if (swcFile.exists()) {
                                    annotationModel.importBulkSWCData(swcFile, workspace);
                                    imported++;
                                }
                                setProgress(index++, total);
                            }
                            setStatus("Successfully imported "+imported+" files");
                        }

                        @Override
                        protected void hadSuccess() {
                        }
                    };
                    importer.executeWithEvents();
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
            // recursively walk the directory, collecting files from all subdirs
            Stream<Path> walker = Files.walk(selectedFile.toPath());
            List<File> swcFiles = walker.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(NeuronManager.STD_SWC_EXTENSION))
                    .map(p -> p.toFile())
                    .distinct()
                    .collect(Collectors.toList());
            rawFileList.addAll(swcFiles);
        } else {
            rawFileList.add(selectedFile);
        }

        if (neuronPerRoot) {
            // Now, we traverse list above, breaking any we see as
            // having more than one root, into multiple input files.
            for (File infile : rawFileList) {

                rtnVal.addAll(neuronManager.breakOutByRoots(infile));
            }
        }
        else {
            rtnVal.addAll(rawFileList);
        }
        return rtnVal;
    }
}
