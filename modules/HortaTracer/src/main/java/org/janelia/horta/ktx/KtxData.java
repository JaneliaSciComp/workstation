package org.janelia.horta.ktx;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author brunsc
 */
public class KtxData {

    public final KtxHeader header = new KtxHeader();
    public final List<ByteBuffer> mipmaps = new ArrayList<>();

    private final byte[] unused = new byte[4]; // for bulk reading of unused padding bytes
    private final ByteBuffer sizeBuf = ByteBuffer.allocate(4); // to hold binary representation of image size

    public void loadStream(InputStream stream) throws IOException, InterruptedException {
        if (stream != null) {
            header.loadStream(stream);
            sizeBuf.order(header.byteOrder);
            mipmaps.clear();
            for (int m = 0; m < header.numberOfMipmapLevels; ++m) {
                mipmaps.add(loadOneMipmap(stream, m));
            }
        }
    }

    private ByteBuffer loadOneMipmap(InputStream stream, int mipmapLevel) throws IOException, InterruptedException {
        int imageSize;
        int bytesRead;
        byte[] b;
        try {
            stream.read(sizeBuf.array());
            sizeBuf.rewind();
            imageSize = (int) ((long) sizeBuf.getInt() & 0xffffffffL);
            // TODO: figure out how to stream straight into the direct buffer
            // For now, copy into the direct buffer
            b = new byte[imageSize];
            bytesRead = stream.read(b, 0, imageSize);
        } catch (Exception e) {
            // this exception most likely occurred because of an interruption
            throw new InterruptedException("Interrupted the loading of mipmap level " + (mipmapLevel + 1));
        }
        if (bytesRead < 1) {
            throw new IOException("Error reading bytes for mipmap level " + (mipmapLevel + 1));
        }
        while (bytesRead < imageSize) {
            int moreBytes = stream.read(b, bytesRead, imageSize - bytesRead);
            if (moreBytes < 1) {
                throw new IOException("Error reading mipmap number " + mipmapLevel);
            }
            bytesRead += moreBytes;
        }
        if (bytesRead != imageSize) {
            throw new IOException("Error reading mipmap number " + mipmapLevel);
        }
        // Use a DIRECT buffer for later efficient slurping into OpenGL
        final boolean useDirect = true;
        ByteBuffer mipmap;
        if (useDirect) {
            mipmap = ByteBuffer.allocateDirect(imageSize);
            mipmap.put(b);
        } else {
            mipmap = ByteBuffer.wrap(b);
        }
        int padding = 3 - ((imageSize + 3) % 4);
        stream.read(unused, 0, padding);
        return mipmap;
    }

}
