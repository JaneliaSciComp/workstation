package org.janelia.utils;

import java.io.IOException;
import java.io.InputStream;

public class FakeInputStream extends InputStream {

    private boolean open = true;
    private long bytesTotal;
    private long bytesRead;
    
    public FakeInputStream(long bytes) {
        this.bytesTotal = bytes;
    }
    
    @Override
    public int read() throws IOException {
        if (!open) {
            return -1;
        }
        bytesRead += 1;
        if (bytesRead>=bytesTotal) {
            open = false;
        }
        return 1;
    }

    @Override
    public int available() throws IOException {
        if (!open) return 0;
        return (int)(bytesTotal-bytesRead);
    }

    @Override
    public void close() throws IOException {
        open = false;
    }
}
