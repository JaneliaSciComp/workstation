package org.janelia.it.FlyWorkstation.api.entity_model.access.observer;

import org.janelia.it.FlyWorkstation.gui.framework.navigation_tools.NavigationPath;

/**
 * Title:        Genome Browser Client
 * Description:  This project is for JBuilder 4.0
 * @author Peter Davies (peter.davies)
 * @version $Id: NavigationObserver.java,v 1.1 2006/11/09 21:35:57 rjturner Exp $
 */

public interface NavigationObserver {

  /**
   * Called when the navigation paths are ready to be returned to the caller
   */
  void noteNavigationPathsArrived(NavigationPath[] navigationPaths,
                                  String searchType, String searchString);

  /**
   * Called if there is an error message returned from the server.
   */
  void noteNavigationError(String errorMessage);
}