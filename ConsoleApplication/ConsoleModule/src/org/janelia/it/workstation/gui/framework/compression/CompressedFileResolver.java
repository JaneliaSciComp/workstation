package org.janelia.it.workstation.gui.framework.compression;

import org.janelia.it.jacs.integration.framework.compression.CompressionAlgorithm;
import org.janelia.it.jacs.integration.framework.compression.CompressedFileResolverI;
import org.openide.util.lookup.ServiceProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    private static final Logger log = LoggerFactory.getLogger(CompressedFileResolver.class);
    
    private List<CompressionAlgorithm> chain;
    public CompressedFileResolver() {
        chain = new ArrayList<>();

        try {
            chain.add(new Mj2ExecutableCompressionAlgorithm());
        }
        catch (Exception e) {
            log.error("Error instantiating Mj2ExecutableCompressionAlgorithm: "+e.getMessage());
        }

        try {
            chain.add( new TrivialCompression() );
        }
        catch (Exception e) {
            log.error("Error instantiating TrivialCompression: "+e.getMessage());
        }

        //chain.add( new LZ4Compression() );
    }
    
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
     * Tells if this resolver has a means of expanding the file whose name
     * was given.
     * 
     * @param infile file to test.
     * @return T=can uncompress; F=otherwise
     */
    @Override
    public boolean canDecompress(File infile) {
        if (chain.isEmpty()) {
            return false;
        }
        File tgtFile = getDecompressedNameForFile(infile);
        return (! tgtFile.equals(infile));
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
