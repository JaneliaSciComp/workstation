package org.janelia.it.workstation.browser.logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * Copied from org.netbeans.core.startup.logging to avoid implementation dependency on org.netbeans.core.startup.
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
final class MessagesHandler extends StreamHandler {
    private final File dir;
    private final File[] files;
    private final long limit;
    
    MessagesHandler(File dir) {
        this(dir, -1, 1024 * 1024);
    }
    
    MessagesHandler(File dir, int count, long limit) {
        this.dir = dir;
    
        if (count == -1) {
            count = Integer.getInteger("org.netbeans.log.numberOfFiles", 3); // NOI18N
            if (count < 3) {
                count = 3;
            }
        }
        File[] arr = new File[count];
        arr[0] = new File(dir, "messages.log"); // NOI18N
        for (int i = 1; i < arr.length; i++) {
            arr[i] = new File(dir, "messages.log." + i); // NOI18N
        }
        this.files = arr;
        this.limit = limit;
        setFormatter(LogFormatter.FORMATTER);
        setLevel(Level.ALL);
        
        checkRotate(true);
        initStream();
    }
    
    private boolean checkRotate(boolean always) {
        if (!always && files[0].length() < limit) {
            return false;
        }
        flush();
        doRotate();
        return true;
    }
    
    private void initStream() {
        try {
            setOutputStream(new FileOutputStream(files[0], false));
        } catch (FileNotFoundException ex) {
            setOutputStream(System.err);
        }
    }

    @Override
    public synchronized void publish(LogRecord record) {
        super.publish(record);
        if (checkRotate(false)) {
            initStream();
        }
    }
    
    private synchronized void doRotate() {
        close();
        int n = files.length;
        if (files[n - 1].exists()) {
            files[n - 1].delete();
        }
        for (int i = n - 2; i >= 0; i--) {
            if (files[i].exists()) {
                files[i].renameTo(files[i + 1]);
            }
        }
    }
}
