package org.janelia.it.workstation.gui.util;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import org.openide.filesystems.FileUtil;

/**
 * File filter for saving/opening YAML files.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class YamlFileFilter extends FileFilter {
    
    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) return true;
        String extension = FileUtil.getExtension(f.getName());
        return extension.equals("yaml");
    }
    
    @Override
    public String getDescription() {
        return "YAML Files";
    }
    
}