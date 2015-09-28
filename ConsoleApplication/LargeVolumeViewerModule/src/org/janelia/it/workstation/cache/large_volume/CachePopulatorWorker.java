/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

import java.io.File;
import org.janelia.it.workstation.gui.large_volume_viewer.compression.CompressedFileResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pull one file's data in, to be cached.
 * 
 * @author fosterl
 */
public class CachePopulatorWorker implements java.util.concurrent.Callable {

    private File infile;
    
    private byte[] storage;
    private Logger log = LoggerFactory.getLogger(CachePopulatorWorker.class);
    
    public CachePopulatorWorker(File infile) {
        this.infile = infile;
    }
    
    public CachePopulatorWorker(File infile, byte[] storage) {
        this(infile);
        this.storage = storage;
    }

    @Override
    public Object call() throws Exception {
        CompressedFileResolver resolver = new CompressedFileResolver();
        if (storage == null) {
            return resolver.uncompress(infile);
        }
        else {
            log.debug("Grabbing {}.", infile);
            Object rtnVal = resolver.uncompress(infile, storage);
            log.debug("Returning {}.", infile);
            return rtnVal;
        }
    }

}
