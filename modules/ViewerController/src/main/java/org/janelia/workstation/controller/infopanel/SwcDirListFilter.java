package org.janelia.workstation.controller.infopanel;

import org.janelia.workstation.controller.NeuronManager;

import java.io.File;

/**
 *
 * @author fosterl
 */
public class SwcDirListFilter implements java.io.FileFilter {

    @Override
    public boolean accept(File file) {
        return file.isFile() && file.getName().endsWith(NeuronManager.STD_SWC_EXTENSION);
    }

}
