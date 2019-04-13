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

    public KtxData loadStream(InputStream stream) throws IOException {
        header.loadStream(stream);
        sizeBuf.order(header.byteOrder);
        mipmaps.clear();
        for (int m = 0; m < header.numberOfMipmapLevels; ++m) {
            mipmaps.add(loadOneMipmap(stream, m));
        }
        return this;
    }

    // Version of loadStream that allows fine-grained interruptions of the load process
    public void loadStreamInterruptably(InputStream stream) throws IOException, InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        header.loadStream(stream);
        sizeBuf.order(header.byteOrder);
        mipmaps.clear();
        for (int m = 0; m < header.numberOfMipmapLevels; ++m) {
            // Check for interruption before loading only the first/largest mipmaps
            if ((m < 3) && Thread.interrupted()) {
                throw new InterruptedException();
            }
            mipmaps.add(loadOneMipmap(stream, m));
        }
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }

    private ByteBuffer loadOneMipmap(InputStream stream, int mipmapLevel) throws IOException {
        stream.read(sizeBuf.array());
        sizeBuf.rewind();
        int imageSize = (int) ((long) sizeBuf.getInt() & 0xffffffffL);
        // TODO: figure out how to stream straight into the direct buffer
        // For now, copy into the direct buffer
        byte[] b = new byte[imageSize];
        int bytesRead = 0;
        bytesRead = stream.read(b, bytesRead, imageSize - bytesRead);
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
