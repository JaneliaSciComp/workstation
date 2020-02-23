package org.janelia.workstation.controller.infopanel;

import org.janelia.workstation.controller.AnnotationModel;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author fosterl
 */
public class SwcDirAndFileFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
        return f.getName().endsWith(AnnotationModel.STD_SWC_EXTENSION) || f.isDirectory();
    }

    @Override
    public String getDescription() {
        return "*" + AnnotationModel.STD_SWC_EXTENSION;
    }
}