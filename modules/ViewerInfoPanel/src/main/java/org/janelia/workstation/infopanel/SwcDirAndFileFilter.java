package org.janelia.workstation.infopanel;

import org.janelia.workstation.controller.NeuronManager;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author fosterl
 */
public class SwcDirAndFileFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
        return f.getName().endsWith(NeuronManager.STD_SWC_EXTENSION) || f.isDirectory();
    }

    @Override
    public String getDescription() {
        return "*" + NeuronManager.STD_SWC_EXTENSION;
    }
}