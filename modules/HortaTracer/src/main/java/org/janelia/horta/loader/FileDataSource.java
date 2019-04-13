package org.janelia.horta.loader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 *
 * @author Christopher Bruns
 */
public class FileDataSource extends BasicDataSource implements DataSource {
    private File file;
    
    public FileDataSource(File file) throws FileNotFoundException {
        super(new BufferedInputStream(new FileInputStream(file)), file.getName());
        this.file = file;
    }
    
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
 }
