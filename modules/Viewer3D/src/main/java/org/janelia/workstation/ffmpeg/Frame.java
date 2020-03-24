/**
 * Created by bolstadm on 10/29/14.
 */
package org.janelia.workstation.ffmpeg;
// Used for testing outside of the workstation
//package ffmpeg;

import org.bytedeco.javacpp.BytePointer;

import java.util.ArrayList;

import static org.bytedeco.javacpp.avutil.*;

public class Frame {
    public boolean keyFrame;
    public AVFrame image;
    public Object opaque;
    public AVFrame picture, picture_rgb;
    public BytePointer buffer_rgb;
    public ArrayList<byte[]> imageBytes = new ArrayList<byte[]>();

    public void release() throws Exception {
        // Free the RGB image
        if (buffer_rgb != null) {
            av_free(buffer_rgb);
            buffer_rgb = null;
        }
        if (picture_rgb != null) {
            av_free(picture_rgb);
            picture_rgb = null;
        }

        // Free the native format picture frame
        if (picture != null) {
            av_free(picture);
            picture = null;
        }

        imageBytes.clear();
    }

}

