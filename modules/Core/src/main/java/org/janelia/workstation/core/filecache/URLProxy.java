package org.janelia.workstation.core.filecache;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Consumer;

/**
 * URL proxy
 */
public class URLProxy {

    private final URL url;
    private final Consumer<Throwable> connectionErrorHandler;

    public URLProxy(URL url) {
        this.url = url;
        this.connectionErrorHandler = (Throwable t) -> {};
    }

    public URLProxy(URL url, Consumer<Throwable> connectionErrorHandler) {
        this.url = url;
        this.connectionErrorHandler = connectionErrorHandler;
    }

    public String getProtocol() {
        return url.getProtocol();
    }

    public String getPath() {
        return url.getPath();
    }

    public String getFile() {
        return url.getFile();
    }

    @Override
    public String toString() {
        return url.toString();
    }

    public void handleError(Throwable e) {
        this.connectionErrorHandler.accept(e);
    }

    public InputStream openStream() throws IOException {
        return url.openStream();
    }
}
