package org.janelia.horta.loader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Helper class used when user drags-and-drops a file onto HortaWorkspaceNode or 
 * onto NeuronTracerTopComponent.
 * 
 * @author Christopher Bruns
 */
public class DroppedFileHandler implements FileHandler
{
    private final Collection<FileTypeLoader> typeLoaders = new ArrayList<>();
    
    public void addLoader(FileTypeLoader loader) {
        typeLoaders.add(loader);
    }
    
    public boolean handleFile(File f) 
            throws FileNotFoundException, IOException 
    {
        DataSource source = new FileDataSource(f);
        boolean result = handleDataSource(source);
        // source.getInputStream().close();
        return result;
    }
    
    public boolean handleStream(InputStream stream, String fileName) 
            throws IOException 
    {
        DataSource source = new BasicDataSource(stream, fileName);
        return handleDataSource(source);
    }
    
    @Override
    public boolean handleDataSource(DataSource source) 
            throws IOException 
    {
        for (FileTypeLoader loader : typeLoaders) {
            if (loader.supports(source)) {
                if (loader.load(source, this)) {
                    return true;
                }
            }
        }
        return false;
    }
}
