package org.janelia.horta.loader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UncheckedIOException;

/**
 *
 * @author Christopher Bruns
 */
public class FileDataSource extends BasicDataSource implements DataSource {
    private File file;
    
    FileDataSource(File file) {
        super(() -> {
            try {
                return new BufferedInputStream(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                throw new UncheckedIOException(e);
            }
        }, file.getName());
        this.file = file;
    }
    
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
 }
