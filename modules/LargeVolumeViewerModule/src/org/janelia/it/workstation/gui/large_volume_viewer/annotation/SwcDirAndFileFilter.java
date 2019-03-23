package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

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