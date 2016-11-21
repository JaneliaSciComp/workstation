package org.janelia.it.workstation.browser.logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Copied from org.netbeans.core.startup.logging to avoid implementation dependency on org.netbeans.core.startup.
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
public final class NbLogging {
   /** stream to send debug messages from logging to */
   public static final PrintStream DEBUG;
   
   
   static final Pattern unwantedMessages;
   static {
       PrintStream _D = null;
       String uMS = System.getProperty("TopLogging.unwantedMessages"); // NOI18N
       if (uMS != null || Boolean.getBoolean("TopLogging.DEBUG")) { // NOI18N
           try {
               File debugLog = new File(System.getProperty("java.io.tmpdir"), "TopLogging.log"); // NOI18N
               System.err.println("Logging sent to: " + debugLog); // NOI18N
               _D = new PrintStream(new FileOutputStream(debugLog), true);
           } catch (FileNotFoundException x) {
               x.printStackTrace();
           }
       }
       DEBUG = _D;
       Pattern uMP = null;
       if (uMS != null) {
           try {
               uMP = Pattern.compile(uMS);
               DEBUG.println("On the lookout for log messages matching: " + uMS); // NOI18N
           } catch (PatternSyntaxException x) {
               x.printStackTrace();
           }
       }
       unwantedMessages = uMP;
   }

   /** @return true if the message is wanted */
   public static boolean wantsMessage(String s) {
       return unwantedMessages == null || !unwantedMessages.matcher(s).find();
   }

//   /** Factory to create non-closing, dispatch handler.
//    */
//   public static Handler createDispatchHandler(Handler handler, int flushDelay) {
//       return new DispatchingHandler(handler, flushDelay);
//   }
//   
//   /** Factory that creates <em>messages.log</em> handler in provided directory.
//    * @param dir directory to store logs in
//    */
//   public static Handler createMessagesHandler(File dir) {
//       return new MessagesHandler(dir);
//   }
//
//   /** Does its best to close provided handler. Can close handlers created by
//    * {@link #createDispatchHandler(java.util.logging.Handler, int)} as well.
//    */
//   public static void close(Handler h) {
//       if (h == null) {
//           return;
//       }
//       if (h instanceof DispatchingHandler) {
//           ((DispatchingHandler)h).doClose();
//       } else {
//           h.close();
//       }
//   }
}