package org.janelia.it.workstation.gui.large_volume_viewer.compression;

import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.SeekableStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds the uncompressed version of an input file.
 * NOTE: This code was used only on an aborted compression implementation
 * for LVV.  However, it is general enough that it might still be employed
 * later, when the final verdict is in. LLF
 * 
 * @author fosterl
 */
public class CompressedFileResolver {
    private List<CompressionAlgorithm> chain;
    public CompressedFileResolver() {
        chain = new ArrayList<>();
        chain.add( new LZ4Compression() );
    }
    
    /**
     * Uses chain-of-responsibility to work out who will compress stuff.
     * This way, a prioritization can be established.
     * 
     * @param infile what to uncompress/fetch.
     * @return something usable by client, directly.
     * @throws Exception thrown by called methods.
     */
    public SeekableStream resolve(File infile) throws Exception {
        for (CompressionAlgorithm algorithm: chain) {
            if (algorithm.canUncompress(infile)) {
                return new ByteArraySeekableStream(algorithm.uncompress(infile));
            }
        }
        return null;
    }

    public SeekableStream resolve(byte[] inbytes, File infile) throws Exception {
        for (CompressionAlgorithm algorithm : chain) {
            if (algorithm.canUncompress(infile)) {
                return new ByteArraySeekableStream(algorithm.uncompress(inbytes));
            }
        }
        return null;
    }
}
