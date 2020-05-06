/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.workstation.img_3d_loader;

import java.util.List;

import org.janelia.workstation.ffmpeg.ByteGatherAcceptor;
import org.janelia.workstation.img_3d_loader.AbstractVolumeFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A file loader to handle H.264 files for the workstation.
 * 
 * @author fosterl
 */
public class H26nFileLoadHelper {

    private Logger logger = LoggerFactory.getLogger(H26nFileLoadHelper.class);
    
    @SuppressWarnings("unused")
    void dumpMeta(ByteGatherAcceptor acceptor) {
        int[] freqs = new int[256];
        if (acceptor.isPopulated()) {
            System.out.println("Total bytes read is " + acceptor.getTotalSize());
            List<byte[]> bytes = acceptor.getBytes();
            // DEBUG: check byte content.
            for (byte[] nextBytes : bytes) {
                for (byte nextByte: nextBytes) {
                    int temp = nextByte;
                    if (temp < 0) {
                        temp += 256;
                    }
                    freqs[temp] ++;
                }
                System.out.print(" " + nextBytes.length);
            }
            System.out.println();
            System.out.println("Total pages is " + bytes.size());
            System.out.println("Byte Frequencies");
            for (int i = 0; i < freqs.length; i++) {
                System.out.println("Frequence of letter " + i + " is " + freqs[i]);
            }
        }
    }

    public void captureData(ByteGatherAcceptor acceptor, AbstractVolumeFileLoader fileLoader) throws Exception {
        fileLoader.setSx( acceptor.getWidth() );
        fileLoader.setSy( acceptor.getHeight() );
        fileLoader.setSz( acceptor.getNumPages() );
        fileLoader.setPixelBytes(acceptor.getPixelBytes());
        long totalSize = acceptor.getTotalSize();
        if (totalSize > Integer.MAX_VALUE) {            
            // Must use segmented approach.
            for (byte[] nextBytes: acceptor.getBytes()) {
                fileLoader.addTextureBytes(nextBytes);
            }
        }
        else {
            // Can load whole chunk as one.
            byte[] texByteArr = new byte[(int) acceptor.getTotalSize()];
            int nextPos = 0;
            for (byte[] nextBytes : acceptor.getBytes()) {
                System.arraycopy(nextBytes, 0, texByteArr, nextPos, nextBytes.length);
                nextPos += nextBytes.length;
            }
            fileLoader.setTextureByteArray(texByteArr);
        }
    }
}
