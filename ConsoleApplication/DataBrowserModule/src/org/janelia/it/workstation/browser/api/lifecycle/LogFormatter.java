package org.janelia.it.workstation.browser.api.lifecycle;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.netbeans.core.startup.logging.NbFormatter;
 
/**
 * Customize the NetBeans log formatter to prepend date/time and thread names to each event.  
 * 
 * Unfortunately requires implementation dependency on org.netbeans.core.startup.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LogFormatter extends Formatter {

    private static final DateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    
    @Override
    public String format(LogRecord record) {
        
        // Look up the thread name
        ThreadInfo threadInfo = threadMXBean.getThreadInfo(record.getThreadID());
        
        // Format log message
        String logMsg = NbFormatter.FORMATTER.format(record);
        
        // Get rid of the colon
        int index = logMsg.indexOf(':');
        if (index>0) {
            logMsg = logMsg.substring(0, index) + logMsg.substring(index+1);
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(dateFormatter.format(new Date()));
        sb.append(" [");
        sb.append(threadInfo==null ? "UnknownThread-"+record.getThreadID() : threadInfo.getThreadName());
        sb.append("] ");
        sb.append(logMsg);
        
        return sb.toString();
    }
}
