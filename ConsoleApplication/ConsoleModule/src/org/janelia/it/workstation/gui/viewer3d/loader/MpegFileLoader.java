package org.janelia.it.workstation.gui.viewer3d.loader;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IOpenCoderEvent;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;

import java.awt.image.BufferedImage;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/6/13
 * Time: 3:33 PM
 *
 *
 */
public class MpegFileLoader extends TextureDataBuilder implements VolumeFileLoaderI {
    @Override
    protected TextureDataI createTextureDataBean() {
        return new TextureDataBean(argbTextureIntArray, sx, sy, sz );
    }

    @Override
    public void loadVolumeFile( String fileName ) {
        this.unCachedFileName = fileName;
        loadMpegVideo( fileName );
    }

    private boolean loadMpegVideo(String fileName) {
        IMediaReader mediaReader = ToolFactory.makeReader(fileName);
        // use premultiplied alpha for this opengl mip technique
        mediaReader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
        mediaReader.addListener(new VolumeFrameListener());
        while (mediaReader.readPacket() == null);
        return true;
    }

    private class VolumeFrameListener extends MediaListenerAdapter {
        // mpeg loading state variables
        private int mVideoStreamIndex = -1;
        private int frameIndex = 0;

        @Override
        public void onOpenCoder(IOpenCoderEvent event) {
            IContainer container = event.getSource().getContainer();
            // Duration might be useful for computing number of frames
            long duration = container.getDuration(); // microseconds
            int numStreams = container.getNumStreams();
            for (int i = 0; i < numStreams; ++i) {
                IStream stream = container.getStream(i);
                IStreamCoder coder = stream.getStreamCoder();
                ICodec.Type type = coder.getCodecType();
                if (type != ICodec.Type.CODEC_TYPE_VIDEO)
                    continue;
                double frameRate = coder.getFrameRate().getDouble();
                frameIndex = 0;
                sx = sy = sz = 0;
                sx = coder.getWidth();
                sy = coder.getHeight();
                sz = (int)(frameRate * duration / 1e6 + 0.5);
                argbTextureIntArray = new int[sx*sy*sz];
                channelCount = 3;
                pixelBytes = 4;
                return;
            }
        }

        @Override
        public void onVideoPicture(IVideoPictureEvent event) {
            if (event.getStreamIndex() != mVideoStreamIndex) {
                // if the selected video stream id is not yet set, go ahead an
                // select this lucky video stream
                if (mVideoStreamIndex == -1)
                    mVideoStreamIndex = event.getStreamIndex();
                    // no need to show frames from this video stream
                else
                    return;
            }
            storeFramePixels(frameIndex, event.getImage());
            ++frameIndex;
        }
    }

    private void storeFramePixels(int frameIndex, BufferedImage image) {
        // System.out.println("Reading frame " + frameIndex);
        int offset = frameIndex * sx * sy;
        image.getRGB(0, 0, sx, sy,
                argbTextureIntArray,
                offset, sx);
    }

    private void zeroColors() {
        int numVoxels = argbTextureIntArray.length;
        for (int v = 0; v < numVoxels; ++v)
            argbTextureIntArray[v] = 0;
    }

}
