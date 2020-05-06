package org.janelia.workstation.core.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

// From http://www.java2s.com/Tutorials/Java/IO_How_to/FileChannel/Monitor_progress_of_FileChannels_transferFrom_method.htm
public class CallbackByteChannel implements ReadableByteChannel {
    
    private ReadableByteChannel rbc;
    private ProgressCallBack delegate;
    private long totalBytesRead;

    CallbackByteChannel(ReadableByteChannel rbc, ProgressCallBack delegate) {
        this.delegate = delegate;
        this.rbc = rbc;
    }

    public void close() throws IOException {
        rbc.close();
    }

    public long getTotalBytesRead() {
        return totalBytesRead;
    }

    public boolean isOpen() {
        return rbc.isOpen();
    }

    public int read(ByteBuffer bb) throws IOException {
        int n;
        if ((n = rbc.read(bb)) > 0) {
            totalBytesRead += n;
            delegate.callback(totalBytesRead);
        }
        return n;
    }

    public interface ProgressCallBack {
        void callback(long bytesWritten);
    }

}