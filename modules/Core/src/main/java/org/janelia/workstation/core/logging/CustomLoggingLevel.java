package org.janelia.workstation.core.logging;

import java.util.logging.Level;

/**
 * Custom JUL warning levels for better control of NetBeans exception handling.  
 *
 * The level numbers defined here should match the minimum levels configured
 * on the command line (project.properties and app.conf): 
 * -J-Dnetbeans.exception.alert.min.level=2000 
 * -J-Dnetbeans.exception.report.min.level=3000
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CustomLoggingLevel extends Level {

  /**
   * Users will be warned of any exception at this level with an icon in the lower right of the application.
   */
  public static final Level USER_WARN = new CustomLoggingLevel("USER_WARN", 2000);
  
  /**
   * Users will be alerted of any exception at this level with an error dialog.
   */
  public static final Level USER_ERROR = new CustomLoggingLevel("USER_ERROR", 3000);
  
  private CustomLoggingLevel(String name, int value) {
      super(name, value);
  }
  
}