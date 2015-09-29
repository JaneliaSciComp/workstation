/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

import java.io.File;
import org.janelia.it.jacs.shared.img_3d_loader.TifVolumeFileLoader;
import org.janelia.it.workstation.gui.large_volume_viewer.compression.CompressedFileResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pull data in, and break it out of its containerized format (for example
 * tif).
 * 
 * @author fosterl
 */
public class ExtractedCachePopulatorWorker implements java.util.concurrent.Callable {

    private File infile;
    
    private byte[] storage;
    private Logger log = LoggerFactory.getLogger(ExtractedCachePopulatorWorker.class);
    
    public ExtractedCachePopulatorWorker(File infile) {
        this.infile = infile;
    }
    
    public ExtractedCachePopulatorWorker(File infile, byte[] storage) {
        this(infile);
        this.storage = storage;
    }

    @Override
    public Object call() throws Exception {
        try {
            return readBytes();
        } catch (Exception ex) {
            log.error("Failure during extraction of TIFF data.");
            ex.printStackTrace();
            throw ex;
        }
    }

    public byte[] readBytes() throws Exception {
        log.debug("Grabbing {}.", Utilities.trimToOctreePath(infile));
        CacheController controller = CacheController.getInstance();
        controller.loadInProgress(infile);

        CompressedFileResolver resolver = new CompressedFileResolver();
        TifVolumeFileLoader loader = new TifVolumeFileLoader();
        // If this storage was null, then new storage will be allocated
        // within the loader.
        loader.setTextureByteArray(storage);
        loader.setPixelBytes(2);
        byte[] uncompressedRawFile = resolver.uncompress(infile);
        loader.loadVolumeInFormat(uncompressedRawFile);
        log.debug("Returning {}.", Utilities.trimToOctreePath(infile));

        controller.loadComplete(infile);
        
        //Utilities.zeroScan(loader.getTextureByteArray(), infile.toString(), "ExtractedCachePopulatorWorker.call()::texBytes::" + loader.getTextureByteArray().hashCode());
        //Utilities.zeroScan(storage, infile.toString(), "ExtractedCachePopulatorWorker.call()::designated storage::" + storage.hashCode());
        return loader.getTextureByteArray();
    }

}
