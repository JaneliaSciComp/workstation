/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.integration.framework.compression;

import java.io.File;

/**
 * Implement this to resolve compression algorithms.
 *
 * @author fosterl
 */
public interface CompressedFileResolverI {
    public static final String LOOKUP_PATH = "CompressedFileResolverI/Location/Nodes";

    /**
     * This resolver can decompressed the input file either with an
     * algorithm or trivially with no action.  This method will find
     * out which decompression algorithm has what it needs, to uncompress
     * into the root input file.
     *
     * @param decompressedFile what we want ultimately.
     * @return what needs to be decompressed/resolved
     * @see #resolve(java.io.File)
     * @see #getDecompressedNameForFile(java.io.File)
     */
    File getCompressedNameForFile(File decompressedFile);

    /**
     * Given some compressed file, this method tells its name after decompression.
     * @param compressedFile what it looks like before uncompress
     * @return what it looks like after uncompress
     * @see #getCompressedNameForFile(java.io.File)
     */
    File getDecompressedNameForFile(File compressedFile);

    /**
     * Given an example file name, return an object which can pick the name
     * for anything 'like it'.  The similarity will (at t-o-w) be 'resides
     * in same directory structure'.
     *
     * @param decompressedFile example file
     * @return something which can apply names to all like it.
     */
    CompressedFileNamer getNamer(File decompressedFile);

    /**
     * Uses chain-of-responsibility to work out who will decompress stuff.
     * This way, a prioritization can be established.
     *
     * @param infile what to uncompress/fetch.
     * @return something usable by client, directly.
     * @throws Exception thrown by called methods.
     */
    //SeekableStream resolve(File infile) throws Exception;

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
    //SeekableStream resolve(File infile, byte[] dest) throws Exception;

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
    //SeekableStream resolve(byte[] inbytes, File infile) throws Exception;

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
    //SeekableStream resolve(byte[] inbytes, byte[] dest, File infile) throws Exception;

    /**
     * Uses chain-of-responsibility to decide how to decompress, and then
     * uncompresses, returning the uncompressed bytes.
     *
     * @param infile what to read
     * @return uncompressed version
     * @throws Exception by called methods.
     */
    byte[] decompressToBytes(File infile) throws Exception;

    /**
     * As above, but specify a destination for the storage of decompressed data.
     *
     * @see #decompressToBytes(java.io.File)
     * @param infile
     * @param dest
     * @return
     * @throws Exception
     */
    byte[] decompressToBuffer(File infile, byte[] dest) throws Exception;

    /**
     * Given the input file, of appropriate extension and contents, uncompress
     * it into original version, and return file handle to that.
     *
     * @param infile what to uncompress
     * @return uncompressed blob.
     * @throws CompressionException if invalid extension, or anything goes wrong
     */
    File decompressToFile(File infile) throws Exception;
            
    public static interface CompressedFileNamer {
        File getCompressedName(File decompressedFile);
    }

}
