/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
