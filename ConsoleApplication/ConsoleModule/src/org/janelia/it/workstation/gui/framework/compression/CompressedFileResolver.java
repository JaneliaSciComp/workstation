package org.janelia.it.workstation.gui.framework.compression;

import org.janelia.it.jacs.integration.framework.compression.CompressionAlgorithm;
import org.janelia.it.jacs.integration.framework.compression.CompressedFileResolverI;
import org.openide.util.lookup.ServiceProvider;
import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.SeekableStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds the degetCompressedNameForFileBytesed version of an input file.
 * NOTE: This code was used only on an aborted compression implementation
 * for LVV.  However, it is general enough that it might still be employed
 * later, when the final verdict is in. LLF
 * 
 * @author fosterl
 */
@ServiceProvider(service = CompressedFileResolverI.class, path = CompressedFileResolverI.LOOKUP_PATH)
public class CompressedFileResolver implements CompressedFileResolverI {
    private List<CompressionAlgorithm> chain;
    public CompressedFileResolver() {
        chain = new ArrayList<>();
        chain.add(new Mj2ExecutableCompressionAlgorithm());
        chain.add( new TrivialCompression() );
        //chain.add( new LZ4Compression() );
    }
    
    /**
     * Uses chain-of-responsibility to work out who will decompress stuff.
     * This way, a prioritization can be established.
     * 
     * @param infile what to degetCompressedNameForFileBytes/fetch.
     * @return something usable by client, directly.
     * @throws Exception thrown by called methods.
     */
//    @Override
//    public SeekableStream resolve(File infile) throws Exception {
//        for (CompressionAlgorithm algorithm: chain) {
//            if (algorithm.canDecompress(infile)) {
//                return new ByteArraySeekableStream(algorithm.decompressAsBytes(infile));
//            }
//        }
//        return null;
//    }

    /**
     * Resolves to seekable stream, and stores intermediate bytes into the
     * provided destination--which must be the correct size for output.
     * 
     * @see #resolve(java.io.File) 
     * @param infile what to degetCompressedNameForFileBytes.
     * @param dest where to place degetCompressedNameForFileBytesed bytes.
     * @return stream wrapped around byte array.
     * @throws Exception 
     */
//    @Override
//    public SeekableStream resolve(File infile, byte[] dest) throws Exception {
//        for (CompressionAlgorithm algorithm : chain) {
//            if (algorithm.canDecompress(infile)) {
//                return new ByteArraySeekableStream(algorithm.decompressIntoByteBuf(infile, dest));
//            }
//        }
//        return null;
//    }

    /**
     * Uses chain-of-responsibility to decide how to decompress, and then
     * decompress, returning the decompressed bytes.
     * 
     * @param infile what to read
     * @return degetCompressedNameForFileBytesed version
     * @throws Exception by called methods.
     */
    @Override
    public byte[] decompressToBytes(File infile) throws Exception {
        for (CompressionAlgorithm algorithm : chain) {
            if (algorithm.canDecompress(infile)) {
                return algorithm.decompressAsBytes(infile);
            }
        }
        return null;
    }

    /**
     * As above, but specify a destination for the storage of degetCompressedNameForFileBytesed data.
     * 
     * @see #decompressToBytes(java.io.File) 
     * @param infile
     * @param dest
     * @return
     * @throws Exception 
     */
    @Override
    public byte[] decompressToBuffer(File infile, byte[] dest) throws Exception {
        for (CompressionAlgorithm algorithm : chain) {
            if (algorithm.canDecompress(infile)) {
                return algorithm.decompressIntoByteBuf(infile, dest);
            }
        }
        return null;
    }

    /**
     * Given the input file, of appropriate extension and contents, decompress
     * it into original version, and return file handle to that.
     * 
     * @param infile what to degetCompressedNameForFileBytes
     * @return degetCompressedNameForFileBytesed blob.
     * @throws CompressionException if invalid extension, or anything goes wrong
     */
    @Override
    public File decompressToFile(File infile) throws Exception {
        for (CompressionAlgorithm algorithm: chain) {
            if (algorithm.canDecompress(infile)) {
                return algorithm.decompressAsFile(infile);
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
//    public SeekableStream resolve(byte[] inbytes, File infile) throws Exception {
//        for (CompressionAlgorithm algorithm : chain) {
//            if (algorithm.canDecompress(infile)) {
//                return new ByteArraySeekableStream(algorithm.decompressAsBytes(inbytes));
//            }
//        }
//        return null;
//    }
    
    /**
     * Like above, but throw decompressed bytes into a supplied buffer which
     * must be the right size.
     * 
     * @see #resolve(byte[], java.io.File) 
     * @param inbytes
     * @param dest
     * @param infile
     * @return
     * @throws Exception 
     */
//    public SeekableStream resolve(byte[] inbytes, byte[] dest, File infile) throws Exception {
//        for (CompressionAlgorithm algorithm : chain) {
//            if (algorithm.canDecompress(infile)) {
//                return new ByteArraySeekableStream(algorithm.decompressIntoByteBuf(inbytes, dest));
//            }
//        }
//        return null;
//    }

    /**
     * This resolver can decompress the input file either with an
     * algorithm or trivially with no action.  This method will find
     * out which compression algorithm has what it needs,
     * to decompress into the root input file.
     * 
     * @param decompressedFile what we want ultimately.
     * @return what needs to be decompressed/resolved
     * @see #resolve(java.io.File) 
     * @see #getDecompressedNameForFile(java.io.File) 
     */
    @Override
    public File getCompressedNameForFile(File decompressedFile) {
        File compressedVersion = decompressedFile;
        for (CompressionAlgorithm algorithm: chain) {
            File algorithmCompressedVersion = algorithm.getCompressedNameForFile(decompressedFile);
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
    @Override
    public CompressedFileNamer getNamer(File decompressedFile) {        
        CompressedFileNamer rtnVal = null;
        for (CompressionAlgorithm algorithm : chain) {
            File algorithmCompressedVersion = algorithm.getCompressedNameForFile(decompressedFile);
            if (algorithmCompressedVersion.canRead()) {
                rtnVal = new CompressedFileNamer(algorithm);
                break;
            }
        }
        return rtnVal;
    }
    
    /**
     * Given some compressed file, this method tells its name after degetCompressedNameForFileBytesion.
     * @param compressedFile what it looks like before degetCompressedNameForFileBytes
     * @return what it looks like after degetCompressedNameForFileBytes
     * @see #getCompressedNameForFile(java.io.File) 
     */
    @Override
    public File getDecompressedNameForFile(File compressedFile) {
        File decompressedFile = compressedFile;
        for (CompressionAlgorithm algorithm: chain) {
            File algoUncompressedVersion = algorithm.getDecompressedNameForFile(compressedFile);
            if (algoUncompressedVersion != null) {
                decompressedFile = algoUncompressedVersion;
                break;
            }
        }
        return decompressedFile;
    }
    
    public static class CompressedFileNamer implements CompressedFileResolverI.CompressedFileNamer {
        private CompressionAlgorithm compressionAlgorithm;
        public CompressedFileNamer(CompressionAlgorithm algorithm) {
            this.compressionAlgorithm = algorithm;
        }
        public File getCompressedName(File decompressedFile) {
            return compressionAlgorithm.getCompressedNameForFile(decompressedFile);
        }
    }
}
