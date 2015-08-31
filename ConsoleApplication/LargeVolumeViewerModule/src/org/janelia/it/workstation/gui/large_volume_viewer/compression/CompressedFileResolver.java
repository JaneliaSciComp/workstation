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
        chain.add( new TrivialCompression() );
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

    /**
     * Uses chain-of-responsibility to decide how to uncompress, and then
     * uncompresses, returning the uncompressed bytes.
     * 
     * @param infile what to read
     * @return uncompressed version
     * @throws Exception by called methods.
     */
    public byte[] uncompress(File infile) throws Exception {
        for (CompressionAlgorithm algorithm : chain) {
            if (algorithm.canUncompress(infile)) {
                return algorithm.uncompress(infile);
            }
        }
        return null;
    }

    /**
     * Given a bunch of bytes that came-from the infile, use the infile's name
     * to decide how to decompress the bytes.  THen invoke the algorithm for
     * the decompression.
     * 
     * @param inbytes bytes in compressed format.
     * @param infile name of file from which compressed bytes came, originally.
     * @return stream into the decompressed version of the inbytes.
     * @throws Exception for any called methods.
     */
    public SeekableStream resolve(byte[] inbytes, File infile) throws Exception {
        for (CompressionAlgorithm algorithm : chain) {
            if (algorithm.canUncompress(infile)) {
                return new ByteArraySeekableStream(algorithm.uncompress(inbytes));
            }
        }
        return null;
    }
    
    /**
     * This resolver can uncompressed the input file either with an
     * algorithm or trivially with no action.  This method will find
     * out which uncompression algorithm has what it needs, to uncompress
     * into the root input file.
     * 
     * @param decompressedFile what we want ultimately.
     * @return what needs to be uncompressed/resolved
     * @see #resolve(java.io.File) 
     * @see #decompressedAs(java.io.File) 
     */
    public File compressAs(File decompressedFile) {
        File compressedVersion = decompressedFile;
        for (CompressionAlgorithm algorithm: chain) {
            File algorithmCompressedVersion = algorithm.compressedVersion(decompressedFile);
            if (algorithmCompressedVersion.canRead()) {
                compressedVersion = algorithmCompressedVersion;
                break;
            }
        }
        return compressedVersion;
    }
    
    /**
     * Given some compressed file, this method tells its name after uncompression.
     * @param compressedFile what it looks like before uncompress
     * @return what it looks like after uncompress
     * @see #compressAs(java.io.File) 
     */
    public File decompressedAs(File compressedFile) {
        File decompressedFile = compressedFile;
        for (CompressionAlgorithm algorithm: chain) {
            File algoUncompressedVersion = algorithm.uncompressedVersion(compressedFile);
            if (algoUncompressedVersion != null) {
                decompressedFile = algoUncompressedVersion;
                break;
            }
        }
        return decompressedFile;
    }
}
