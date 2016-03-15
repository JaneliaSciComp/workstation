package org.janelia.it.workstation.lifecycle;

import java.awt.Toolkit;
import javax.swing.JFrame;
import java.util.List;
import java.util.ArrayList;
import java.awt.AWTEvent;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.openide.windows.OnShowing;

/**
 * This carries out tasks that must be done at startup, but may only be done 
 * when the application is ready to show.
 * 
 * @author fosterl
 */
@OnShowing
public class ShowingHook implements Runnable {
    public void run() {
        JFrame frame = WindowLocator.getMainFrame();
        String title = ConsoleProperties.getString("console.Title") + " " + ConsoleProperties.getString("console.versionNumber");
        frame.setTitle( title );
        SessionMgr.getBrowser().supportMenuProcessing();
        
        // Log events.
        final InterceptingEventQueue interceptingEventQueue = new InterceptingEventQueue();
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(
                interceptingEventQueue);
        final LoggingEventListener loggingEventListener = new LoggingEventListener();
        Toolkit.getDefaultToolkit().addAWTEventListener(
                loggingEventListener, AWTEvent.MOUSE_EVENT_MASK);
        
        List<String> discriminators = new ArrayList<>();
        List<MessageSource> sources = new ArrayList<>();
        sources.add(interceptingEventQueue);
        discriminators.add(ReportRunner.MOUSE_EVENT_DISCRIMINATOR);
        //sources.add(loggingEventListener);
        //discriminators.add(ReportRunner.BUTTON_EVENT_DISCRIMINATOR);
        ReportRunner rptRunner = new ReportRunner(sources, discriminators);
    }
}
