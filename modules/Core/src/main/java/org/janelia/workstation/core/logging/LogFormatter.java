package org.janelia.workstation.core.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.xml.sax.SAXParseException;
 
/**
 * Customize the NetBeans log formatter to prepend date/time and thread names to each event.  
 * 
 * Most of this code was copied from org.netbeans.core.startup.logging to avoid implementation dependency on org.netbeans.core.startup.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LogFormatter extends Formatter {

    public static final java.util.logging.Formatter FORMATTER = new LogFormatter();
    
    private static final DateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    private static String lineSeparator = System.getProperty("line.separator"); // NOI18N
    
    @Override
    public String format(LogRecord record) {
        
        // Look up the thread name
        ThreadInfo threadInfo = threadMXBean.getThreadInfo(record.getThreadID());
        
        StringBuilder sb = new StringBuilder();
        // Prepend the date/time
        sb.append(dateFormatter.format(new Date()));
        sb.append(" [");
        // Prepend the thread name
        sb.append(threadInfo==null ? "UnknownThread-"+record.getThreadID() : threadInfo.getThreadName());
        sb.append("] ");

        // Print the usual NetBeans message
        print(sb, record, new HashSet<Throwable>());
        String r = sb.toString();
        if (NbLogging.DEBUG != null) {
            NbLogging.DEBUG.print("received: " + r); // NOI18N
        }
        return r;
    }

    private void print(StringBuilder sb, LogRecord record, Set<Throwable> beenThere) {
        String message = formatMessage(record);
        if (message != null && message.indexOf('\n') != -1 && record.getThrown() == null) {
            // multi line messages print witout any wrappings
            sb.append(message);
            if (message.charAt(message.length() - 1) != '\n') {
                sb.append(lineSeparator);
            }
            return;
        }
        if ("stderr".equals(record.getLoggerName()) && record.getLevel() == Level.INFO) {
            // NOI18N
            // do not prefix stderr logging...
            sb.append(message);
            return;
        }
        sb.append(record.getLevel().getName());
        addLoggerName(sb, record);
        if (message != null) {
            sb.append(": ");
            sb.append(message);
        }
        sb.append(lineSeparator);
        if (record.getThrown() != null && record.getLevel().intValue() != 1973) {
            // 1973 signals ErrorManager.USER
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                // All other kinds of throwables we check for a stack trace.
                printStackTrace(record.getThrown(), pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ex) {
            }
            LogRecord[] arr = extractDelegates(sb, record.getThrown(), beenThere);
            if (arr != null) {
                for (LogRecord r : arr) {
                    print(sb, r, beenThere);
                }
            }
            specialProcessing(sb, record.getThrown(), beenThere);
        }
    }

    private static void addLoggerName(StringBuilder sb, java.util.logging.LogRecord record) {
        String name = record.getLoggerName();
        if (!"".equals(name)) {
            sb.append(" [");
            sb.append(name);
            sb.append(']');
        }
    }

    private static LogRecord[] extractDelegates(StringBuilder sb, Throwable t, Set<Throwable> beenThere) {
        if (!beenThere.add(t)) {
            sb.append("warning: cyclic dependency between annotated throwables"); // NOI18N
            return null;
        }
        if (t instanceof Callable) {
            Object rec = null;
            try {
                rec = ((Callable) t).call();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (rec instanceof LogRecord[]) {
                return (LogRecord[]) rec;
            }
        }
        if (t == null) {
            return null;
        }
        return extractDelegates(sb, t.getCause(), beenThere);
    }

    private void specialProcessing(StringBuilder sb, Throwable t, Set<Throwable> beenThere) {
        // MissingResourceException should be printed nicely... --jglick
        if (t instanceof MissingResourceException) {
            MissingResourceException mre = (MissingResourceException) t;
            String cn = mre.getClassName();
            if (cn != null) {
                LogRecord rec = new LogRecord(Level.CONFIG, null);
//                rec.setResourceBundle(NbBundle.getBundle(TopLogging.class));
                rec.setMessage("Missing resource from class: {0}");
                rec.setParameters(new Object[]{cn});
                print(sb, rec, beenThere);
            }
            String k = mre.getKey();
            if (k != null) {
                LogRecord rec = new LogRecord(Level.CONFIG, null);
//                rec.setResourceBundle(NbBundle.getBundle(TopLogging.class));
                rec.setMessage("Key which was not found: {0}");
                rec.setParameters(new Object[]{k});
                print(sb, rec, beenThere);
            }
        }
        if (t instanceof SAXParseException) {
            // For some reason these fail to come with useful data, like location.
            SAXParseException spe = (SAXParseException) t;
            String pubid = spe.getPublicId();
            String sysid = spe.getSystemId();
            if (pubid != null || sysid != null) {
                int col = spe.getColumnNumber();
                int line = spe.getLineNumber();
                String msg;
                Object[] param;
                if (col != -1 || line != -1) {
                    msg = "Parse error in file {1} line {3} column {2} (PUBLIC {0})"; // NOI18N
                    param = new Object[]{String.valueOf(pubid), String.valueOf(sysid), col, line};
                } else {
                    msg = "Parse error in file {1} (PUBLIC {0})"; // NOI18N
                    param = new Object[]{String.valueOf(pubid), String.valueOf(sysid)};
                }
                LogRecord rec = new LogRecord(Level.CONFIG, null);
//                rec.setResourceBundle(NbBundle.getBundle(TopLogging.class));
                rec.setMessage(msg);
                rec.setParameters(param);
                print(sb, rec, beenThere);
            }
        }
    }
    private static final Map<Throwable, Integer> catchIndex = Collections.synchronizedMap(new WeakHashMap<Throwable, Integer>()); // #190623
    

    /**
     * For use also from NbErrorManager.
     *
     * @param t throwable to print
     * @param pw the destination
     */
    public static void printStackTrace(Throwable t, PrintWriter pw) {
        doPrintStackTrace(pw, t, null);
    }

    /**
     * #91541: show stack traces in a more natural order.
     */
    private static void doPrintStackTrace(PrintWriter pw, Throwable t, Throwable higher) {
        //if (t != null) {t.printStackTrace(pw);return;}//XxX
        try {
            if (t.getClass().getMethod("printStackTrace", PrintWriter.class).getDeclaringClass() != Throwable.class) { // NOI18N
                // Hmm, overrides it, we should not try to bypass special logic here.
                //System.err.println("using stock printStackTrace from " + t.getClass());
                t.printStackTrace(pw);
                return;
            }
            //System.err.println("using custom printStackTrace from " + t.getClass());
        } catch (NoSuchMethodException e) {
            assert false : e;
        }
        Throwable lower = t.getCause();
        if (lower != null) {
            doPrintStackTrace(pw, lower, t);
            pw.print("Caused: "); // NOI18N
        }
        String summary = t.toString();
        if (lower != null) {
            String suffix = ": " + lower;
            if (summary.endsWith(suffix)) {
                summary = summary.substring(0, summary.length() - suffix.length());
            }
        }
        pw.println(summary);
        StackTraceElement[] trace = t.getStackTrace();
        int end = trace.length;
        if (higher != null) {
            StackTraceElement[] higherTrace = higher.getStackTrace();
            while (end > 0) {
                int higherEnd = end + higherTrace.length - trace.length;
                if (higherEnd <= 0 || !higherTrace[higherEnd - 1].equals(trace[end - 1])) {
                    break;
                }
                end--;
            }
        }
        Integer caughtIndex = catchIndex.get(t);
        for (int i = 0; i < end; i++) {
            if (caughtIndex != null && i == caughtIndex) {
                // Translate following tab -> space since formatting is bad in
                // Output Window (#8104) and some mail agents screw it up etc.
                pw.print("[catch] at "); // NOI18N
            } else {
                pw.print("\tat "); // NOI18N
            }
            pw.println(trace[i]);
        }
    }

    static void registerCatchIndex(Throwable t, int index) {
        catchIndex.put(t, index);
    }
}
