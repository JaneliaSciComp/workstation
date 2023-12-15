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

import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.access.LoadedWorkspaceCreator;
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
public class ImportSWCFolderAction extends AbstractAction {

    private boolean neuronPerRoot = false;
    private TmModelManager model;
    private NeuronManager neuronManager;

    public ImportSWCFolderAction(boolean neuronPerRoot) {
        this.neuronPerRoot = neuronPerRoot;
        model = TmModelManager.getInstance();
        neuronManager = NeuronManager.getInstance();
    }

    public ImportSWCFolderAction() {
        this(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (model.getCurrentWorkspace() == null) {
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), "No workspace is open", "Cannot Import", JOptionPane.ERROR_MESSAGE);
            return;
        }

        TmWorkspace workspace = TmModelManager.getInstance().getCurrentWorkspace();
        TmSample sample = TmModelManager.getInstance().getCurrentSample();

        LoadedWorkspaceCreator.loadSWCsIntoWorkspace(sample, workspace.getName(), true);

    }
}
