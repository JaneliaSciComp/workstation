/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.workstation.ffmpeg;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.bytedeco.javacpp.BytePointer;

/**
 *
 * @author fosterl
 */
public class PPMFileAcceptor implements FFMPGByteAcceptor {
    private int frameNum;
    private int pixelBytes;
    public PPMFileAcceptor() {
    }
    
    public void setFrameNum( int frameNum ) {
        this.frameNum = frameNum;
    }
    
    @Override
    public void accept(BytePointer data, int linesize, int width, int height) {
        final String FILENAME_FORMAT = "image%05d.ppm";
        // NOTE:  This code must remain at Java version 1.6, for use in export
        // in a separate library.
        
        // Open file
        OutputStream stream = null;
        try {
            stream = new FileOutputStream(String.format(FILENAME_FORMAT,frameNum));
            
            // Write header
            stream.write(("P6\n" + width + " " + height + "\n255\n").getBytes());

            // Write pixel data
            byte[] bytes = new byte[width * pixelBytes];
            for (int y = 0; y < height; y++) {
                data.position(y * linesize).get(bytes);
                stream.write(bytes);
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            closeStream(stream);
        }

    }

    @Override
    public void accept(byte[] bytes, int linesize, int width, int height) {
        final String FILENAME_FORMAT = "image%05d.ppm";
        // Open file
        OutputStream stream = null;
        try {
            stream = new FileOutputStream(String.format(FILENAME_FORMAT,frameNum));

            // Write header
            stream.write(("P6\n" + width + " " + height + "\n255\n").getBytes());

            // Write pixel data
            for (int y = 0; y < height; y++) {
                stream.write(bytes, y * linesize, linesize);
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            closeStream(stream);
        }

    }
    
    @Override
    public void setPixelBytes(int pixelBytes) {
        this.pixelBytes = pixelBytes;
    }
    
    /** Supports robust closure of output stream. */
    protected void closeStream(OutputStream stream) throws RuntimeException {
        try {
            if (stream != null) {
                stream.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

}
