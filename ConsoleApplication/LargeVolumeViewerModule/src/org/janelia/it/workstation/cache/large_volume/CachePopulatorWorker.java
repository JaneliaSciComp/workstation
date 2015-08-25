/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

import java.io.File;
import org.janelia.it.workstation.gui.large_volume_viewer.compression.CompressedFileResolver;
import org.janelia.it.workstation.gui.large_volume_viewer.compression.FileCollector;

/**
 * Pull one file's data in, to be cached.
 * 
 * @author fosterl
 */
public class CachePopulatorWorker implements java.util.concurrent.Callable {

    private File infile;
    
    public CachePopulatorWorker(File infile) {
        this.infile = infile;
    }

    @Override
    public Object call() throws Exception {
        FileCollector collector = new FileCollector();
        collector.collectFile(infile);
        CompressedFileResolver resolver = new CompressedFileResolver();
        return resolver.uncompress(infile);
    }

}
