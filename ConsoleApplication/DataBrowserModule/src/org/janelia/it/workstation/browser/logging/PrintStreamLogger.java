package org.janelia.it.workstation.browser.logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.RequestProcessor;

/** 
 * Copied from org.netbeans.core.startup.logging to avoid implementation dependency on org.netbeans.core.startup.
 * 
 * a stream to delegate to logging.
 */
public final class PrintStreamLogger extends PrintStream implements Runnable {
    private Logger log;
    private final StringBuilder sb = new StringBuilder();
    private static RequestProcessor RP = new RequestProcessor("StdErr Flush");
    private RequestProcessor.Task flush = RP.create(this, true);

    private PrintStreamLogger(Logger log) {
        super(new ByteArrayOutputStream());
        this.log = log;
    }
    
    public static boolean isLogger(PrintStream ps) {
        // should work across different classloaders
        return ps.getClass().getName().equals(PrintStreamLogger.class.getName());
    }
    
    public static PrintStream create(String loggerName) {
        return new PrintStreamLogger(Logger.getLogger(loggerName)); 
    }
    
    //
    // Impl
    //

    @Override
    public void write(byte[] buf, int off, int len) {
        if (RP.isRequestProcessorThread()) {
            return;
        }
        String s = new String(buf, off, len);
        print(s);
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(int b) {
        if (RP.isRequestProcessorThread()) {
            return;
        }
        synchronized (sb) {
            sb.append((char) b);
        }
        checkFlush();
    }

    @Override
    public void print(String s) {
        if (NbLogging.DEBUG != null && !NbLogging.wantsMessage(s)) {
            new Exception().printStackTrace(NbLogging.DEBUG);
        }
        synchronized (sb) {
            sb.append(s);
        }
        checkFlush();
    }

    @Override
    public void println(String x) {
        print(x);
        print(System.getProperty("line.separator"));
    }

    @Override
    public void println(Object x) {
        String s = String.valueOf(x);
        println(s);
    }

    @Override
    public void flush() {
        boolean empty;
        synchronized (sb) {
            empty = sb.length() == 0;
        }
        if (!empty) {
            try {
                flush.schedule(0);
                flush.waitFinished(500);
            } catch (InterruptedException ex) {
                // ok, flush failed, do not even print
                // as we are inside the System.err code
            }
        }
        super.flush();
    }

    private void checkFlush() {
        //if (DEBUG != null) DEBUG.println("checking flush; buffer: " + sb); // NOI18N
        try {
            flush.schedule(100);
        } catch (IllegalStateException ex) {
            /* can happen during shutdown:
            Nested Exception is:
            java.lang.IllegalStateException: Timer already cancelled.
            at java.util.Timer.sched(Timer.java:354)
            at java.util.Timer.schedule(Timer.java:170)
            at org.openide.util.RequestProcessor$Task.schedule(RequestProcessor.java:621)
            at org.netbeans.core.startup.TopLogging$LgStream.checkFlush(TopLogging.java:679)
            at org.netbeans.core.startup.TopLogging$LgStream.write(TopLogging.java:650)
            at sun.nio.cs.StreamEncoder.writeBytes(StreamEncoder.java:202)
            at sun.nio.cs.StreamEncoder.implWrite(StreamEncoder.java:263)
            at sun.nio.cs.StreamEncoder.write(StreamEncoder.java:106)
            at java.io.OutputStreamWriter.write(OutputStreamWriter.java:190)
            at java.io.BufferedWriter.flushBuffer(BufferedWriter.java:111)
            at java.io.PrintStream.write(PrintStream.java:476)
            at java.io.PrintStream.print(PrintStream.java:619)
            at java.io.PrintStream.println(PrintStream.java:773)
            at java.lang.Throwable.printStackTrace(Throwable.java:461)
            at java.lang.Throwable.printStackTrace(Throwable.java:451)
            at org.netbeans.insane.impl.LiveEngine.trace(LiveEngine.java:180)
            at org.netbeans.insane.live.LiveReferences.fromRoots(LiveReferences.java:110)
             * just ignore it, we cannot print it at this situation anyway...
             */
        }
    }

    public void run() {
        for (;;) {
            String toLog;
            synchronized (sb) {
                if (sb.length() == 0) {
                    break;
                }
                int last = -1;
                for (int i = sb.length() - 1; i >= 0; i--) {
                    if (sb.charAt(i) == '\n') {
                        last = i;
                        break;
                    }
                }
                if (last == -1) {
                    break;
                }
                toLog = sb.substring(0, last + 1);
                sb.delete(0, last + 1);
            }
            int begLine = 0;
            while (begLine < toLog.length()) {
                int endLine = toLog.indexOf('\n', begLine);
                log.log(Level.INFO, toLog.substring(begLine, endLine + 1));
                begLine = endLine + 1;
            }
        }
    }
    
} // end of LgStream
