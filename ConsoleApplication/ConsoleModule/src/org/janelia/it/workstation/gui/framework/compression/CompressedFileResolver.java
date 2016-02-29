package org.janelia.it.workstation.gui.framework.compression;

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
     * Resolves to seekable stream, and stores intermediate bytes into the
     * provided destination--which must be the correct size for output.
     * 
     * @see #resolve(java.io.File) 
     * @param infile what to uncompress.
     * @param dest where to place uncompressed bytes.
     * @return stream wrapped around byte array.
     * @throws Exception 
     */
    public SeekableStream resolve(File infile, byte[] dest) throws Exception {
        for (CompressionAlgorithm algorithm : chain) {
            if (algorithm.canUncompress(infile)) {
                return new ByteArraySeekableStream(algorithm.uncompress(infile, dest));
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
     * As above, but specify a destination for the storage of uncompressed data.
     * 
     * @see #uncompress(java.io.File) 
     * @param infile
     * @param dest
     * @return
     * @throws Exception 
     */
    public byte[] uncompress(File infile, byte[] dest) throws Exception {
        for (CompressionAlgorithm algorithm : chain) {
            if (algorithm.canUncompress(infile)) {
                return algorithm.uncompress(infile, dest);
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
     * Like above, but throw uncompressed bytes into a supplied buffer which
     * must be the right side.
     * 
     * @see #resolve(byte[], java.io.File) 
     * @param inbytes
     * @param dest
     * @param infile
     * @return
     * @throws Exception 
     */
    public SeekableStream resolve(byte[] inbytes, byte[] dest, File infile) throws Exception {
        for (CompressionAlgorithm algorithm : chain) {
            if (algorithm.canUncompress(infile)) {
                return new ByteArraySeekableStream(algorithm.uncompress(inbytes, dest));
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
     * Given an example file name, return an object which can pick the name
     * for anything 'like it'.  The similarity will (at t-o-w) be 'resides
     * in same directory structure'.
     * 
     * @param decompressedFile example file
     * @return something which can apply names to all like it.
     */
    public CompressedFileNamer getNamer(File decompressedFile) {        
        CompressedFileNamer rtnVal = null;
        for (CompressionAlgorithm algorithm : chain) {
            File algorithmCompressedVersion = algorithm.compressedVersion(decompressedFile);
            if (algorithmCompressedVersion.canRead()) {
                rtnVal = new CompressedFileNamer(algorithm);
                break;
            }
        }
        return rtnVal;
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
    
    public static class CompressedFileNamer {
        private CompressionAlgorithm compressionAlgorithm;
        public CompressedFileNamer(CompressionAlgorithm algorithm) {
            this.compressionAlgorithm = algorithm;
        }
        public File getCompressedName(File decompressedFile) {
            return compressionAlgorithm.compressedVersion(decompressedFile);
        }
    }
}
