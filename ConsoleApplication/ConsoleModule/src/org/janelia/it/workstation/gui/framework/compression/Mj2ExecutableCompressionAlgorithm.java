/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.framework.compression;

import org.janelia.it.jacs.integration.framework.compression.CompressionException;
import org.janelia.it.jacs.integration.framework.compression.CompressionAlgorithm;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.janelia.it.jacs.shared.utils.SystemCall;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * A compression algorithm compatible with MJ2 / JPEG2000 decompression,
 * using a supplied binary executable.
 *
 * @author fosterl
 */
public class Mj2ExecutableCompressionAlgorithm implements CompressionAlgorithm {
//    public static final String DECOMPRESSION_BINARY = "R:\\decompress_forLes\\decompressForLes.exe";
    public static final String ASSUMED_OUTPUT_TYPE_EXT = ".tif";
    public static final String DECOMPRESSION_BINARY = "\"C:\\Program Files\\JaneliaWorkstation\\bin\\decompressForLes.exe\"";
    public static final String TMPDIR_ROOT="C:\\Program Files\\JaneliaWorkstation\\tmp";
            //"C:\\Users\\FOSTERL\\gitfiles\\master\\janelia-workstation\\jpeg2000\\forLes\\decompress_forLes\\decompressForLes.exe";
    public static final String BUILD_RAMDISK_COMMAND = "C:\\Windows\\system32\\imdisk.exe -a -t vm -s 1G -p \"/fs:ntfs /q /y\" -m R:";
    //public static final File RAMDISK_ROOT = new File("C:\\data\\for_mj2\\"); //new File("R:\\data\\");
    public static final File RAMDISK_ROOT = new File("R:\\data\\");
    public static final String TARGET_EXTENSION = ".mj2";
    private static final String FILE_PROTOCOL = "file:/";
    private static final String FILE_MID_STR = "_comp-";
    private final Logger log = LoggerFactory.getLogger(Mj2ExecutableCompressionAlgorithm.class);
    
    private int compressionLevel = 10;
    private int zDepth = 251;
    
    private String decompBinaryLocation = DECOMPRESSION_BINARY;

    private static boolean tmpInitialized=false;
    
    public Mj2ExecutableCompressionAlgorithm() {
        File rDrive = new File("R:\\");
        if (! rDrive.exists()) {
            try {
                SystemCall sysCall = new SystemCall();
                int cmdRtn = sysCall.emulateCommandLine(BUILD_RAMDISK_COMMAND, false);
                if (cmdRtn != 0) {
                    log.error(BUILD_RAMDISK_COMMAND + " failed with error " + cmdRtn);
                }
            } catch (Exception ex) {
                log.error("Exception on create RAM Disk, using the command: " + BUILD_RAMDISK_COMMAND);
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }

        /*
        URL binUrl = this.getClass().getClassLoader().getResource("resources/binaries/decompressForLes.exe");
        if (binUrl != null  &&  (! binUrl.getFile().contains(".jar!"))) {
            decompBinaryLocation = binUrl.getFile();
            if (decompBinaryLocation.startsWith(FILE_PROTOCOL)) {
                decompBinaryLocation = decompBinaryLocation.substring(FILE_PROTOCOL.length());
            }
        }
        */
    }
    
    /**
     * This setting is the percentage of the original file size, that will result from compression.
     * It effects the file name.
     * 
     * @param compressionLevel final percentage-of-size.
     */
    public void setCompessionLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }
    
    /**
     * This value is required to properly de-compress these MJ2's.  It is
     * the number of "sheets" in the stack.
     * 
     * @param zDepth how 'thick' is this thing?
     */
    public void setZDepth(int zDepth) {
        this.zDepth = zDepth;
    }

    @Override
    public boolean canDecompress(File infile) {
        return infile.getName().endsWith(TARGET_EXTENSION);
    }

    @Override
    public File getCompressedNameForFile(File infile) {
        return new File(infile.getParentFile(), getMJ2Name(infile));
    }

    @Override
    public File getDecompressedNameForFile(File compressedFile) {
        File rtnVal = null;
        final String fileName = compressedFile.getName();
        if (fileName.endsWith(TARGET_EXTENSION)) {
            rtnVal = new File(compressedFile.getParentFile(), fileName.substring(0, fileName.length() - (getCompressedFileNameSuffix()).length()));
        }
        return rtnVal;
    }

    /**
     * Given the input file, of appropriate extension and contents, decompress
     * it into original version, and return file handle to that.
     *
     * @param infile what to decompressAsBytes
     * @return decompressed file.
     * @throws CompressionException if invalid extension, or anything goes wrong
     */
    @Override
    public File decompressAsFile(File infile) throws CompressionException {
        File rtnVal = null;
        if (canDecompress(infile)) {
            // Must run the operation in its own process, and use the output
            // file name.
            try {
                if (! RAMDISK_ROOT.exists()) {
                    if (! RAMDISK_ROOT.mkdirs()) {
                        throw new IOException("Non-existent, and failed to create " + RAMDISK_ROOT);
                    }
                }
                if (! RAMDISK_ROOT.canWrite()) {
                    throw new Exception("Ram disk/root not writable or not yet created " + RAMDISK_ROOT);
                }
                FileUtils.cleanDirectory(RAMDISK_ROOT);
                String targetFileName = getDecompressedNameForFile(infile).getName();
                if (! targetFileName.toLowerCase().endsWith(ASSUMED_OUTPUT_TYPE_EXT)) {
                    targetFileName += ASSUMED_OUTPUT_TYPE_EXT;
                }
                File tempFile = new File(RAMDISK_ROOT, targetFileName);
                //File tempFile = File.createTempFile("JPEG2KMJ2", getDecompressedNameForFile(infile).getName(), RAMDISK_ROOT);
                // Form a command line.
                SystemCall sysCall = new SystemCall();
                final String commandLine = decompBinaryLocation + " " + infile.getAbsolutePath() + " " + tempFile.getAbsolutePath() + " " + zDepth;
                // Win-only version.
                System.out.println(commandLine);
                int cmdRtn = sysCall.emulateCommandLine(commandLine, false);
                if (cmdRtn == 0) {
                    rtnVal = tempFile;
                }
                else {
                    throw new Exception(commandLine + " failed with error " + cmdRtn);
                }
            } catch (Exception ex) {
                throw new CompressionException(ex);
            }
        } else {
            throw new CompressionException("Cannot decompress file without extens " + TARGET_EXTENSION);
        }
        return rtnVal;
    }

    @Override
    public byte[] decompressAsBytes(File infile) throws CompressionException {
        if (canDecompress(infile)) {
            try {
                File tempFile = decompressAsFile(infile);
                Date startTime = new Date();
                byte[] b = collectBytes(tempFile);
                Date endFileRead = new Date();

                log.info("Time required for file-read: {}s.", (endFileRead.getTime() - startTime.getTime()) / 1000);
                return b;

            } catch (Exception ex) {
                throw new CompressionException(ex);
            }

        } else {
            throw new CompressionException("Cannot decompress file without extens " + TARGET_EXTENSION);
        }
    }

    @Override
    public byte[] decompressAsBytes(byte[] inbytes) throws CompressionException {
        throw new UnsupportedOperationException("Cannot decompress raw bytes.");
    }

    @Override
    public byte[] decompressIntoByteBuf(File infile, byte[] outbytes) throws CompressionException {
        if (canDecompress(infile)) {
            try {
                File tempFile = decompressAsFile(infile);
                FileCollector collector = new FileCollector();
                Date startTime = new Date();
                collector.collectFile(tempFile, outbytes);
                Date endFileRead = new Date();
                
                log.info("Time required for file-read: {}s.", (endFileRead.getTime() - startTime.getTime()) / 1000);
                return outbytes;

            } catch (Exception ex) {
                throw new CompressionException(ex);
            }

        } else {
            throw new CompressionException("Cannot decompress file without extens " + TARGET_EXTENSION);
        }
    }

    @Override
    public byte[] decompressIntoByteBuf(byte[] inbytes, byte[] outbytes) throws CompressionException {
        throw new UnsupportedOperationException("Cannot decompress raw bytes.");
    }
    
    public byte[] collectBytes(File infile) throws Exception {
        FileCollector collector = new FileCollector();
        collector.collectFile(infile);
        byte[] b = collector.getData();
        return b;
    }
    
    private String getMJ2Name(File infile) {
        return infile.getName() + getCompressedFileNameSuffix();
    }

    private String getCompressedFileNameSuffix() {
        return FILE_MID_STR + Integer.toString(compressionLevel) + TARGET_EXTENSION;
    }

    private static synchronized void createTmpDir() throws CompressionException {
        File mj2TmpDir=new File(TMPDIR_ROOT, "mj2");
        File tmpDir=new File(TMPDIR_ROOT);
        if (!tmpDir.exists()) {
            if (!tmpDir.mkdir()) {
                throw new CompressionException("Could not create dir="+TMPDIR_ROOT);
            }
        }
        if (!mj2TmpDir.exists()) {
            if (!mj2TmpDir.mkdir()) {
                throw new CompressionException("Could not create dir="+mj2TmpDir.getAbsolutePath());
            }
        }
    }

    public static byte[] decompressAsBytesInTmpDir(byte[] inBytes, int zDepth) throws Exception {
        File mj2TmpDir=new File(TMPDIR_ROOT, "mj2");
        if (!tmpInitialized) {
            createTmpDir();
            tmpInitialized=true;
        }
        String uniqueDirName=new Long(System.nanoTime()).toString();
        File tmpDir=new File(mj2TmpDir, uniqueDirName);
        if (!tmpDir.mkdir()) {
            throw new CompressionException("Could not create tmpDir="+tmpDir.getAbsolutePath());
        }
        File dataInFile=new File(tmpDir, "data.mj2");
        File dataOutFile=new File(tmpDir, "data.tif");
        FileUtils.writeByteArrayToFile(dataInFile, inBytes);

        SystemCall sysCall = new SystemCall();
        final String commandLine = DECOMPRESSION_BINARY + " " + dataInFile.getAbsolutePath() + " " + dataOutFile.getAbsolutePath() + " " + zDepth;
        int cmdRtn = sysCall.emulateCommandLine(commandLine, false);
        if (cmdRtn != 0) {
            throw new Exception(commandLine + " failed with error code " + cmdRtn);
        }

        byte[] result=Files.readAllBytes(dataOutFile.toPath());
        FileUtils.cleanDirectory(tmpDir);
        tmpDir.delete();

        return result;
    }


}
